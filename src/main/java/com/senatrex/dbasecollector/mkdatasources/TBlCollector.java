package com.senatrex.dbasecollector.mkdatasources;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapi.Subscription;
import com.bloomberglp.blpapi.SubscriptionList;
import com.senatrex.dbasecollector.marketevents.TAbstractMarketEvent;
import com.senatrex.dbasecollector.marketevents.TAskMarketEvent;
import com.senatrex.dbasecollector.marketevents.TBidMarketEvent;
import com.senatrex.dbasecollector.marketevents.TOpenInterestEvent;
import com.senatrex.dbasecollector.marketevents.TTradeMarketEvent;
import com.senatrex.dbasecollector.queues.TAbstractQueue;
import com.senatrex.dbasecollector.queues.TAsyncLogQueue;

/**
 * <p>
 * Class gets market data from bloomberg<br>
 * updated 10 авг. 2015 г.18:04:28
 *
 * @author Alexander Kumenkov
 * </p>
 */
public class TBlCollector extends TAbstractMkDataCollector {

    private boolean fIsEnabled = false;

    TBloomQueue fBloomQueue;

    class TBloomQueue extends TAbstractQueue {

        public TBloomQueue() {
            super();
        }


        /* (non-Javadoc)
                 * @see com.senatrex.dbasecollector.queues.TAbstractQueue#doWork()
         */
        @Override
        protected void doWork() {
            // TODO Auto-generated method stub
            if (fQueue.size() > 0) {
                Event event = (Event) fQueue.poll();
                if (event != null) {
                    MessageIterator msgIter = event.messageIterator();
                    while (msgIter.hasNext()) {
                        Message msg = msgIter.next();
                        if (event.eventType() == Event.EventType.SUBSCRIPTION_DATA
                                || event.eventType() == Event.EventType.SUBSCRIPTION_STATUS) {
                            String lName = (String) msg.correlationID().object();
                            TAbstractMarketEvent lAbstractMarketEvent;

                            if (msg.hasElement("ASK", true)) {

                                int lSize = 0;
                                if ( msg.hasElement( "ASK_SIZE", true ) ) {
                                    lSize = msg.getElementAsInt32("ASK_SIZE");
                                }

                                lAbstractMarketEvent = new TAskMarketEvent(lName, msg.getElementAsFloat64("ASK"), lSize);
                                fMarketEventQueue.AddRecord(lAbstractMarketEvent);
                                continue;

                            } // end if hasElement
                            if (msg.hasElement("BID", true)) {

                                int lSize = 0;
                                if (msg.hasElement("BID_SIZE", true)) {
                                    lSize = msg.getElementAsInt32("BID_SIZE");
                                }
                                lAbstractMarketEvent = new TBidMarketEvent(lName, msg.getElementAsFloat64("BID"), lSize);
                                fMarketEventQueue.AddRecord(lAbstractMarketEvent);
                                continue;

                            }
                            if (msg.hasElement("EVT_TRADE_PRICE_RT", true)
                                    && msg.hasElement("EVT_TRADE_SIZE_RT", true)) {
                                // Show the topic

                                String lConditionCode = "_";

                                if (msg.hasElement("EVT_TRADE_CONDITION_CODE_RT", true)) {

                                    lConditionCode = msg.getElementAsString("EVT_TRADE_CONDITION_CODE_RT");
                                }

                                lAbstractMarketEvent = new TTradeMarketEvent(lName, msg.getElementAsFloat64("EVT_TRADE_PRICE_RT"),
                                        msg.getElementAsInt32("EVT_TRADE_SIZE_RT"), -1, lConditionCode);
                                fMarketEventQueue.AddRecord(lAbstractMarketEvent);
                                continue;
                            }

                            if (msg.hasElement("RT_OPEN_INTEREST", true)) {
                                int lOpenInt = msg.getElementAsInt32("RT_OPEN_INTEREST");
                                lAbstractMarketEvent = new TOpenInterestEvent(lName, lOpenInt);
                                fMarketEventQueue.AddRecord(lAbstractMarketEvent);
                              
                            }

                            // System.out.println(topic + ": " + msg.asElement());
                        }
                    }
                }
            }
            //fSizeQueue = fQueue.size();
        }
    }

    public void run() {
        //	fBloomQueue =new TBloomQueue();
        try {
            startBlTranslating();
        } catch (Exception e) {
            // TODO Auto-generated catch block
             TAsyncLogQueue.getInstance( ).AddRecord( e.getLocalizedMessage(), 0);
            
        }
    }

    private void startBlTranslating() throws Exception {
        String serverHost = "127.0.0.1";
        int serverPort = 8194;
        String serviceName = "//blp/mktdata";

        SessionOptions sessionOptions = new SessionOptions();
        sessionOptions.setServerHost(serverHost);
        sessionOptions.setServerPort(serverPort);

        System.out.println("Connecting to " + serverHost + ":" + serverPort);
        Session session = new Session(sessionOptions);
        if (!session.start()) {
            System.err.println("Failed to start session.");
            fIsEnabled = false;
            return;
        }

        System.out.println("Connected successfully.");
        if (!session.openService(serviceName)) {
            System.err.println("Failed to open " + serviceName);
            session.stop();
            fIsEnabled = false;
            return;
        }
        fIsEnabled = true;
        SubscriptionList subscriptions = new SubscriptionList();

        for (int i = 1; i < super.fInstruments.length; i++) {
            subscriptions.add(new Subscription(
                    fInstruments[i][1],//fInstruments[i][2],
                    "ASK,BID,EVT_TRADE_SIZE_RT,RT_OPEN_INTEREST,EVT_TRADE_CONDITION_CODE_RT",
                    "",
                    new CorrelationID(fInstruments[i][1]))); //RUB MICE Curncy
        }

        System.out.println("Subscribing...");
        session.subscribe(subscriptions);

        boolean lIsConnected = true;
        
        while ( lIsConnected ) {
            Event event = session.nextEvent();

            //fBloomQueue.AddRecord( event );
            //Event event = (Event) fQueue.poll( );	
            if (event != null) {
                MessageIterator msgIter = event.messageIterator();
                while ( msgIter.hasNext( ) ) {
                    
                    Message msg = msgIter.next();
                    if( event.eventType() == Event.EventType.TIMEOUT ) {
                        lIsConnected = false;
                        this.fIsEnabled = false;
                        TAsyncLogQueue.getInstance( ).AddRecord( "timed out!", 0 );
                    }
                    
                    if ( event.eventType() == Event.EventType.SUBSCRIPTION_DATA
                            || event.eventType() == Event.EventType.SUBSCRIPTION_STATUS ) {
                        String lName = ( String ) msg.correlationID( ).object( );
                        TAbstractMarketEvent lAbstractMarketEvent;

                        if (msg.hasElement( "ASK", true ) ) {

                            int lSize = 0;
                            if ( msg.hasElement( "ASK_SIZE", true ) ) {
                                lSize = msg.getElementAsInt32( "ASK_SIZE" );
                            }

                            lAbstractMarketEvent = new TAskMarketEvent( lName, msg.getElementAsFloat64( "ASK" ), lSize );
                            fMarketEventQueue.AddRecord( lAbstractMarketEvent );
                            continue;

                        } // end if hasElementfIsEnabled
                        if ( msg.hasElement( "BID", true ) ) {

                            int lSize = 0;
                            if (msg.hasElement("BID_SIZE", true)) {
                                lSize = msg.getElementAsInt32("BID_SIZE");
                            }
                            lAbstractMarketEvent = new TBidMarketEvent(lName, msg.getElementAsFloat64("BID"), lSize);
                            fMarketEventQueue.AddRecord(lAbstractMarketEvent);
                            continue;

                        }
                        if (msg.hasElement("EVT_TRADE_PRICE_RT", true)
                                && msg.hasElement("EVT_TRADE_SIZE_RT", true)) {
                            // Show the topic

                            String lConditionCode = "_";

                            if (msg.hasElement("EVT_TRADE_CONDITION_CODE_RT", true)) {

                                lConditionCode = msg.getElementAsString("EVT_TRADE_CONDITION_CODE_RT");
                                if( lConditionCode.length() >= 10 ) {
                                    lConditionCode = lConditionCode.substring( 0, 8 )+"~";
                                }
                            }

                            lAbstractMarketEvent = new TTradeMarketEvent(lName, msg.getElementAsFloat64("EVT_TRADE_PRICE_RT"),
                                    msg.getElementAsInt32("EVT_TRADE_SIZE_RT"), -1, lConditionCode);
                            fMarketEventQueue.AddRecord(lAbstractMarketEvent);
                            continue;
                        }

                        if (msg.hasElement("RT_OPEN_INTEREST", true)) {
                            int lOpenInt = msg.getElementAsInt32("RT_OPEN_INTEREST");
                            lAbstractMarketEvent = new TOpenInterestEvent(lName, lOpenInt);
                            fMarketEventQueue.AddRecord(lAbstractMarketEvent);
                            
                        }

                        // System.out.println(topic + ": " + msg.asElement());
                    }
                }
            } else {
                lIsConnected = false;
            }
        }
    }

    /**
     * @see
     * com.senatrex.dbasecollector.mkdatasources.TAbstractMkDataCollector#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        return fIsEnabled;
    }
}
