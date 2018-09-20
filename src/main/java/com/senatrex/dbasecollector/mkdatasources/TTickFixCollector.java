package com.senatrex.dbasecollector.mkdatasources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import com.senatrex.dbasecollector.marketevents.TFixMarketEvent;
import com.senatrex.dbasecollector.marketevents.TTradeMarketEvent;
import com.senatrex.dbasecollector.marketinstruments.TMarketOperation;
import com.senatrex.dbasecollector.ptimeutilities.TTimeUtilities;
import com.senatrex.dbasecollector.queues.TAsyncLogQueue;


/**
 * <p>
 * Class gets tick market data from quik fix server<br>
 * updated 27 окт. 2015 г.15:53:02
 *
 * @author Alexander Kumenkov
 * </p>
 */
public class TTickFixCollector extends TAbstractMkDataCollector {

    private Map< String, String> fParametersMap;
    
    private int fMessageNumber;
    private Socket fSocket;
    PrintWriter fWriter;

    public TTickFixCollector(Map< String, String> aParametersMap) {
        fParametersMap = aParametersMap;
        fIsClosed = false;
        fMessageNumber = 1;
    }

   
    
    //13.08.2015 11:10:46.236	fixd	Send	8=FIX.4.29=7635=A98=0108=30141=Y34=149=SYNTRX52=20150813-08:10:46.04656=BCSMDPUMP10=2108=FIX.4.29=16035=V146=155=ATAD22=448=US6708312052207=XLON263=1264=10265=0267=2269=0269=1262=fixd#113796848#034=249=SYNTRX52=20150813-08:10:46.21656=BCSMDPUMP10=0478=FIX.4.29=15935=V146=155=GAZ22=448=US36829G1076207=XLON263=1264=10265=0267=2269=0269=1262=fixd#113796848#134=349=SYNTRX52=20150813-08:10:46.21656=BCSMDPUMP10=0328=FIX.4.29=16035=V146=155=HYDR22=448=US4662941057207=XLON263=1264=10265=0267=2269=0269=1
    public void run() {
        try {
            //String lResuillt = generateMkDataInstrumentRequest(fInstruments[40], 0, fParametersMap );
            String lLoginMessage = generateLoginMessage(fParametersMap);
            for (int i = 1; i < fInstruments.length; i++) {
                lLoginMessage += generateMkDataInstrumentRequest(fInstruments[i], 0, fParametersMap);
            }
            System.out.println("");

            fSocket = new Socket(fParametersMap.get("host"), Integer.parseInt(fParametersMap.get("port")));
            fSocket.setTcpNoDelay(true);
            fWriter = new PrintWriter(fSocket.getOutputStream(), true);

            Thread lHeartBeat = new Thread(
                    new Runnable() {
                public void run() {
                    while (fIsClosed == false) {
                        try {
                            Thread.sleep(29000);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        String lHeartbeatStr = generateHeartBeatMessage(fParametersMap);
                        fWriter.print(new String(lHeartbeatStr));
                        fWriter.flush();
                    }
                }
            });
            fWriter.print(new String(lLoginMessage));
            fWriter.flush();
            lHeartBeat.start();

            InputStream lStream = fSocket.getInputStream();
            BufferedReader lIns = new BufferedReader(new InputStreamReader(lStream));

            char[] lBuffer = new char[Integer.valueOf(fParametersMap.get("RCVBUF"))];

            String lLocalBuffer = "";
            String lIncomeString = "";
            while (fIsClosed == false) {

                int lRes = lIns.read(lBuffer);
                if (lRes == -1) {
                    fIsClosed = true;
                } else {
                    lIncomeString = new String(lBuffer);
                    int lNewMessageIndex = lIncomeString.lastIndexOf("8=FIX");
                    if (lNewMessageIndex > -1) {
                        lLocalBuffer += lIncomeString.substring(0, lNewMessageIndex);
                        if (lLocalBuffer.length() > 0) {
                            TAsyncLogQueue.getInstance().AddRecord( lLocalBuffer, 3 );
                            if (lLocalBuffer.indexOf("35=1") > -1) {
                                String lHbA = generateHeartBeatAnswear(fParametersMap, getTagValue(lLocalBuffer, "112"));
                                System.out.println("heart beat answer is " + lHbA);
                                fWriter.print(new String(lHbA));
                                fWriter.flush();
                            }
                            try {
                                parseResponse(lLocalBuffer);
                            } catch (Exception e) {
                                System.out.println(e);
                                TAsyncLogQueue.getInstance().AddRecord( e.getLocalizedMessage()+" "+lLocalBuffer, 0 );
                            }
                        }
                        lLocalBuffer = lIncomeString.substring(lIncomeString.lastIndexOf("8=FIX"), lIncomeString.length());
                    } else {
                        lLocalBuffer += lIncomeString;
                    }
                    Arrays.fill(lBuffer, '0');
                }
            }

            fSocket.close();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            try {
                fSocket.close();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            TAsyncLogQueue.getInstance().AddRecord(e.getClass().getName() + ": " + e.getMessage(), 0);
        }
    }

    private void parseResponse(String aResponse) {

        TAsyncLogQueue.getInstance().AddRecord(aResponse, 0);
        String[] lMessages = aResponse.split("8=FIX.4.2");
        for (int i = 1; i < lMessages.length; i++) {

            if (lMessages[i].indexOf("35=W") > -1) {

                String lExchangeName = getTagValue(lMessages[i], "48");

                String lPriceStr = getTagValue(lMessages[i], "270");
                String lQtyStr = getTagValue(lMessages[i], "271");
                int lSide = -1;
                double lPrice = 0.0;
                int lVolume = 0;
                try {
                    lPrice = Double.parseDouble(lPriceStr);
                    lVolume = Integer.parseInt(lQtyStr);
                } catch (Exception e) {
                }

                fMarketEventQueue.AddRecord(new TTradeMarketEvent(lExchangeName, lPrice, lVolume, lSide, "fixticks"));
                //System.out.println( lMessages[ i ] );
            }
        }
    }

    private String generateHeartBeatMessage(Map< String, String> aParametersMap) {

        String lMarketMessageBody = "49=" + aParametersMap.get("sender") + "56=" + aParametersMap.get("targer") + "34=" + fMessageNumber + "52=" + TTimeUtilities.GetCurrentTime("yyyyMMdd-HH:mm:ss.SSS") + "";
        int lLength = lMarketMessageBody.length() + 5;

        String oResuilt = "8=FIX.4.29=" + lLength + "35=0" + lMarketMessageBody;
        String lEndStr = countSumTag(oResuilt);
        oResuilt += lEndStr;
        fMessageNumber++;
        return oResuilt;

    }

    private String generateHeartBeatAnswear(Map< String, String> aParametersMap, String aTagValue) {

        String lMarketMessageBody = "49=" + aParametersMap.get("sender") + "56=" + aParametersMap.get("targer") + "34=" + fMessageNumber + "52=" + TTimeUtilities.GetCurrentTime("yyyyMMdd-HH:mm:ss.SSS") + "112=" + aTagValue + "";
        int lLength = lMarketMessageBody.length() + 5;
        String oResuilt = "8=FIX.4.29=" + lLength + "35=0" + lMarketMessageBody;
        String lEndStr = countSumTag(oResuilt);
        oResuilt += lEndStr;
        fMessageNumber++;
        return oResuilt;

    }

    /**
     * Method initializes table of instruments
     *
     * @param aMessage message, where tag situated
     * @param aTag which value need to be parsed
     * @return value of setted tag
     */
    private String getTagValue(String aMessage, String aTag) {
        int lTagPosition = aMessage.indexOf(aTag + "=");
        String oResuilt = "";
        if (lTagPosition > -1) {
            int lStartIndex = lTagPosition + aTag.length() + 1;
            int lEndIndex = aMessage.indexOf("", lTagPosition);
            if (lStartIndex == -1 || lEndIndex == -1) {
                System.out.println("pidor!");
            }
            try {
                oResuilt = aMessage.substring(lStartIndex, lEndIndex);
            } catch (java.lang.StringIndexOutOfBoundsException e) {
                TAsyncLogQueue.getInstance().AddRecord(e.getClass().getName() + ": " + e.getMessage(), 0);
            }
        }
        return oResuilt;
    }
//9=25035=W49=BCSMDPUMP56=SYNTRX52=20150817-15:56:35.25534=77262=fixd#113796848#048=USD000UTSTOM55=USD000UTSTOM22=4207=CETS268=4269=0290=1270=65.235271=6269=0290=2270=65.234271=17269=1290=1270=65.236271=42269=1290=2270=65.24271=1510=035	

    private String countSumTag(String aStringToCount) {
        char[] lCharArray = aStringToCount.toCharArray();
        int lSumm = 0;
        for (int i = 0; i < lCharArray.length; i++) {
            lSumm += (int) (lCharArray[i]);
        }
        lSumm = lSumm % 256;
        char[] oRes = new char[3];
        oRes[0] = (char) (lSumm / 100 + 48);
        oRes[1] = (char) ((lSumm % 100) / 10 + 48);
        oRes[2] = (char) (lSumm % 10 + 48);
        return "10=" + (new String(oRes)) + "";
    }

    private String generateMkDataInstrumentRequest(String[] aInstruments, int aNumber, Map< String, String> aParametersMap) { //tag 55

        String lMarketMessageBody = "146=155=" + aInstruments[2] + "22=" + aParametersMap.get("IDSource") + "48=" + aInstruments[1] + "207=" + aInstruments[3] + "263=1264=" + aParametersMap.get("MarketDepth") + "265=0267=1269=2262=fixd#113796848#" + aNumber + "34=" + fMessageNumber + "49=" + aParametersMap.get("sender") + "52=" + TTimeUtilities.GetCurrentTime("yyyyMMdd-HH:mm:ss.SSS") + "56=" + aParametersMap.get("targer") + "";
        int lLength = lMarketMessageBody.length() + 5;
        String oResuilt = "8=FIX.4.29=" + lLength + "35=V" + lMarketMessageBody;
        String lEndStr = countSumTag(oResuilt);
        oResuilt += lEndStr;
        fMessageNumber++;
        TAsyncLogQueue.getInstance().AddRecord(oResuilt, 0);
        return oResuilt;
        //Sending 8=FIX.4.4|9=142|35=V|49=TEST|56=BCSMDPUMP|34=19|
        //		52=20151027-09:21:22|146=1|55=AAPL|22=4|48=US0378331005|207=NASDAQ|262=test341|263=1|264=10|265=0|267=1|269=2|10=165|
    }

    private String generateLoginMessage(Map< String, String> aParametersMap) { //tag 55
        String lMarketMessageBody = "98=0108=30141=Y34=" + fMessageNumber + "49=" + aParametersMap.get("sender") + "52=" + TTimeUtilities.GetCurrentTime("yyyyMMdd-HH:mm:ss.SSS") + "56=" + aParametersMap.get("targer") + "";
        int lLength = lMarketMessageBody.length() + 5;
        String oResuilt = "8=FIX.4.29=" + lLength + "35=A" + lMarketMessageBody;
        String lEndStr = countSumTag(oResuilt);
        oResuilt += lEndStr;
        fMessageNumber++;
        return oResuilt;
    }

    /**
     * Method adds instruments for download market data
     *
     * @param aInstruments array of instruments
     */
    @Override
    public void addInstruments(String[][] aInstruments) {
        if (aInstruments[0].length == 4) {
            fInstruments = aInstruments;
        } else {
            System.out.println("Check instruments for fix collector!");
            TAsyncLogQueue.getInstance().AddRecord("Check instruments for fix collector!", 0);

        }
    }

    /**
     * @see
     * com.senatrex.dbasecollector.mkdatasources.TAbstractMkDataCollector#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        return !fSocket.isClosed();
    }
}
