/**
 * 
 */
package com.senatrex.dbasecollector.pmainpac;

import java.awt.EventQueue;

import javax.swing.JFrame;

import java.awt.Component;
import java.awt.Label;
import java.awt.BorderLayout;

import javax.swing.JButton;

import com.senatrex.dbasecollector.queues.TDBaseQueue;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * @author Wellington updated 21 авг. 2015 г.16:07:10
 *
 */
public class TUserInterFace {

	private static JFrame frame;
	private static Label fSizeQueue;
	private static Label fStatusQueue;
	private static String fName;
	/**
	 * Launch the application.
	 */
	public static void main( String[ ] args ) {
		
            EventQueue.invokeLater( new Runnable( ) {
                public void run() {
                    try {
                        TUserInterFace window = new TUserInterFace();

                        window.frame.setVisible(true);
                    } catch (Exception e) {
                            e.printStackTrace();
                    }
                }
            } );
	}

	/**
	 * Create the application.
	 */
	public TUserInterFace( ) {
            initialize( );
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
            frame = new JFrame( fName );
            frame.setBounds( 100, 100, 221, 178 );
            frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

            fSizeQueue = new Label( "queue size" );
            frame.getContentPane( ).add( fSizeQueue, BorderLayout.NORTH );

            JButton btnNewButton = new JButton( "Clear buffer" );
            btnNewButton.addActionListener( new ActionListener( ) {
                public void actionPerformed( ActionEvent arg0 ) {
                    TDBaseQueue.getInstance( ).clearBuffer( );
                }
            } );
            frame.getContentPane( ).add( btnNewButton, BorderLayout.CENTER );

            fStatusQueue = new Label("status");
            frame.getContentPane().add(fStatusQueue, BorderLayout.SOUTH);
	}

	public static void changeSizeText( String aNewText ) {	
		fSizeQueue.setText( aNewText );
	}
	
	public static void changeStatusText( String aNewText ) {	
		fStatusQueue.setText( aNewText );
	}

}
