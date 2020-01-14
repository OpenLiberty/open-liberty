/*******************************************************************************
 * Copyright (c) 2003,2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transaction.transport.connections.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.parser.util.InetAddressCache;
import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;

/**
 * @author Amir Perlman
 *
 * Dedicated thread for sending outbound UDP message on a single socket. 
 * Message will be buffered in a queue and dispatched by the internal thread
 */
public class UDPSenderThread extends Thread {
    
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(UDPSenderThread.class);
    
    /**
     * Queue of pending message. 
     */
    private List<MessageContext> m_messages = new ArrayList<MessageContext>();
    
    /** 
     * UDP Socket  
     */
	private DatagramSocket m_socket;
	
	/**
	 * Flag indicating when to stop thread and exit. 
	 */
	private boolean _keepRunning = true;
	
	/**
	 * Single packet used for sending all messages.
	 */
	private DatagramPacket m_packet = new DatagramPacket(new byte[0], 0);
	
	/**
	 * Thread's name
	 */
	private static final String NAME = "UDP Sender";
	
	/**
	 * Interval on which messages will be printed to log indicate queue size. 
	 */
	private int _qSizeAlertModulus = StackProperties.TRACE_Q_SIZE_MODULUS_DEFAULT; 
	
	/**
	 * Size of queue in last message printed to trace. 
	 */
	private int _sizePrintedToTrace = 0; 
	
	/**
	 * Maximum number of messages allowed on queue. When queue reaches its limit
	 * messages will be dropped. In SIP UDP messages are retransmitted so no 
	 * session should be lost as a result of dropping some packets. We need to 
	 * protected ourself for cases where the queue gets full due to some network 
	 * failure which might result in an out of memory situation. 
	 */
	private int _maxQSize = StackProperties.MAX_UDP_QUEUE_SIZE_DEFAULT; 
	
	/**
	 * Flag indicating if we are currently in overloaded state and messages
	 * are being dropped from queue. 
	 */
	private boolean _isOverloaded = false; 
	
	/**
     * Last time an overload message has been printed to log. We we are overloaded
     * we want to print a message to the log but we need to avoid filling the 
     * log with such message. So we will print the periodically every 60 seconds
     * if needed. 
     */
    private long _lastOverLoadMessageTime;
    
	/**
	 * Construct a new UDP sender
	 * @param socket
	 */
	public UDPSenderThread(DatagramSocket socket)
	{
	    super(NAME);
	    m_socket = socket;
	    
	    _maxQSize = ApplicationProperties.getProperties().getInt(
	    							StackProperties.MAX_UDP_QUEUE_SIZE);
		
		if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "UDPSenderThread", "Max Queue Size: " + _maxQSize);
        }
		
		_qSizeAlertModulus = ApplicationProperties.getProperties().getInt(
			      					StackProperties.TRACE_Q_SIZE_MODULUS);
		
		if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "UDPSenderThread", 
                				"Queue size alert modulus: " + _qSizeAlertModulus);
        }
	}
	
	/**
	 * Add message to queue of messages waiting to be sent over the network.  
	 * @param msg
	 */
	public synchronized void addToQ(MessageContext messageSendingContext)
	{
	    if(m_messages.size() < _maxQSize)
	    {
	        m_messages.add(messageSendingContext);
	        notify();
	        _isOverloaded = false;
	    }
	    else
	    {
	        messageSendingContext.getSipMessageByteBuffer().reset(); // Put back in pool
	        MessageContext.doneWithContext(messageSendingContext);
	        
	        if(!_isOverloaded)
	        {
	            _isOverloaded = true;
	            long cTime = System.currentTimeMillis();
		        
		        //Print message to log every 60 seconds only
		        if(_lastOverLoadMessageTime + 60000 < cTime  && 
		           c_logger.isWarnEnabled())
                {			
                    c_logger.warn("warn.udp.sender.overloaded", 
                        			Situation.SITUATION_REPORT_PERFORMANCE, 
                        			null);
                    _lastOverLoadMessageTime = cTime;
                }
	        }
	           
	    }
	}
	
	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		   SipMessageByteBuffer msg = null; 
		   MessageContext messageSendingContext = null; 
	   while(_keepRunning)
	   {
	       synchronized(this)
	       {
	           if(m_messages.isEmpty())
	           {
	               try {
                    wait();
                }
                catch (InterruptedException e) {
                    logException(e);
                }
	           }
	           else
	           {
	        	   messageSendingContext = (MessageContext) m_messages.remove(0);
	        	   if (messageSendingContext != null){
	        		   msg = messageSendingContext.getSipMessageByteBuffer();
	        	   }
	           }
	       }
	       
	       if(null != msg)
	       {
	           send(messageSendingContext);
	           msg.reset(); // Put back in pool
	           msg = null;
	           
	           printQueueStatistics();
	       }
	   }
	}

    /**
     * Dumps queue statistics to log.  
     */
    private final void printQueueStatistics() {
        if (_qSizeAlertModulus <= 0) 
        {
            return;
        }
        
        int qSize = m_messages.size(); 
        if(qSize != _sizePrintedToTrace &&
           qSize % _qSizeAlertModulus == 0 )
	    {
            _sizePrintedToTrace = qSize;	
            System.out.println("Outbound UDP Queue size: " + qSize);
        }
    }

    /**
     * Does the actual sending, creates a UDP packet and send it
     * @param sipMsg
     */
    private void send(MessageContext messageSendingContext) {

		try
		{
			SipMessageByteBuffer sipMsg = messageSendingContext.getSipMessageByteBuffer();
	        InetAddress ina;
	        SIPConnection sipConnection = messageSendingContext.getSipConnection();
			String peerHost = sipConnection.getRemoteHost();
			int peerPort = sipConnection.getRemotePort();
			if( "127.0.0.1".equalsIgnoreCase( peerHost ) || 
				"localhost".equalsIgnoreCase( peerHost ) ) 
			{
				 ina = InetAddress.getLocalHost();
			}
			else
			{
				 ina = InetAddressCache.getByName(peerHost);
			}
			
			m_packet.setData(sipMsg.getBytes(), 0, sipMsg.getMarkedBytesNumber());
			m_packet.setAddress(ina);
			m_packet.setPort(peerPort);
			
			m_socket.send(m_packet);
		    messageSendingContext.writeComplete();
		}
		catch(IOException e)
		{
		    logException(e);
		    SIPConnection connection = messageSendingContext.getSipConnection();
		    if (connection != null){
		    	connection.connectionError(e);
		    }
		    messageSendingContext.writeError(e);
		}
    }
    
    
    /**
     * Utilitiy function for logging exceptions. 
     * @param e
     */
    private void logException(Exception e) 
    {
        if(c_logger.isErrorEnabled())
        {
            c_logger.error("error.exception.stack", 
                           Situation.SITUATION_CREATE, 
                    	   null, e);
        }
    }
    
    
    /**
     * @see java.lang.Thread#stop()
     */
    public synchronized void terminate()
    {
        _keepRunning = false;
        notify();
    }

	public List<MessageContext> getMessagesFromQ() {
		return m_messages;
	}
    
}
