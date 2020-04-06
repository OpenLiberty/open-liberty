/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jain.protocol.ip.sip;

import jain.protocol.ip.sip.message.Message;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import java.util.EventObject;

/**
 * This class represents an event that is passed from a SipProvidere to all its registered
 * SipListeners. SipEvent contains the following elements:
 * <ul>
 * <li>source - SipProvider sending the SipEvent</li>
 * <li>eventId - indicates what event occurred (one of REQUEST_RECEIVED, RESPONSE_RECEIVED, TRANSACTION_TIMEOUT)</li>
 * <li>transactionId - id of transaction this event is associated with (0 indicates no transaction association)</li>
 * <li>isServerTransaction - boolean indicating whether transactionId refers to a client or transaction</li>
 * <li>message - Message received on SipProviders ListeningPoint (message will be null if event id is TRANSACTION_TIMEOUT)</li>
 * </ul>
 *
 * @version 1.0
 */
public class SipEvent extends EventObject
{
    
    /*
    * Constructs a SipEvent to indicate a Response has been received
    * @param <var>source</msg> source of SipEvent (SipProvider)
    * @param <var>clientTransactionId</var> client transaction id (0 indicates Response is
    * not associated with any client transaction)
    * @param <var>response</var> Response received
    * @throws IllegalArgumentException if source or response are null
    */
    public SipEvent(Object source, long clientTransactionId, Response response)
            throws IllegalArgumentException 
    {
        super(source);
        if(response == null)
        {
            throw new IllegalArgumentException("response cannot be null");
        }
        message = response;
        transactionId = clientTransactionId;
        isServerTransaction = false;
        eventId = RESPONSE_RECEIVED;
    }
    
    /*
    * Constructs a SipEvent to indicate a transaction timed out
    * @param <var>source</msg> source of SipEvent (SipProvider)
    * @param <var>transactionId</var> transaction id
    * @param <var>isServerTransaction</var> indicates if transaction is a server transaction
    * @throws IllegalArgumentException if source is null
    */
    public SipEvent(Object source, long transactionId, boolean isServerTransaction)
            throws IllegalArgumentException 
    {
        super(source);
        this.transactionId = transactionId;
        this.isServerTransaction = isServerTransaction;
        eventId = TRANSACTION_TIMEOUT;
    }
    
    /*
    * Gets the transaction id associated with this SipEvent (if returned transactionId is 0, then
    * the event does not correspond to any transaction i.e. stray Message received)
    * @return transaction id associated with this SipEvent
    */
    public long getTransactionId()
    {
        return transactionId;
    }
    
    /*
    * Indicates if transaction associated with this SipEvent is a server transaction
    * @return boolean value indicating if transaction associated with this SipEvent
    * is a server transaction
    */
    public boolean isServerTransaction()
    {
        return isServerTransaction;
    }
    
    /*
    * Gets the Message associated with this SipEvent
    * @return Message associated with this SipEvent (null if event id is TRANSACTION_TIMEOUT)
    */
    public Message getMessage()
    {
        return message;
    }
    
    /*
    * Constructs a SipEvent to indicate a Request has been received
    * @param <var>source</msg> source of SipEvent (SipProvider)
    * @param <var>serverTransactionId</var> server transaction id (0 indicates Request is
    * not associated with any server transaction)
    * @param <var>request</var> Request received
    * @throws IllegalArgumentException if source or request are null
    */
    public SipEvent(Object source, long serverTransactionId, Request request)
            throws IllegalArgumentException 
    {
        super(source);
        if(request == null)
        {
            throw new IllegalArgumentException("request cannot be null");
        }
        message = request;
        transactionId = serverTransactionId;
        isServerTransaction = true;
        eventId = REQUEST_RECEIVED;
    }
    
    /*
    * Gets the event id of this SipEvent
    * @return event id of this SipEvent (REQUEST_RECEIVED, RESPONSE_RECEIVED or TRANSACTION_TIMEOUT)
    */
    public int getEventId()
    {
        return eventId;
    }
    /*
     * Sets the event id of this SipEvent
     * @return event id of this SipEvent (REQUEST_RECEIVED, RESPONSE_RECEIVED or TRANSACTION_TIMEOUT, ERROR_RESPONSE_CREATED)
     */
     public void setEventId(int event_id)
     {
         eventId = event_id;
     }
    public final static int RESPONSE_RECEIVED = 1;
    public final static int REQUEST_RECEIVED = 2;
    public final static int TRANSACTION_TIMEOUT = 3;
    /**
     * This event ID identifies all errors which were created internally by the
     * SipContainer as reason of some failure during the send.
     */
    public final static int ERROR_RESPONSE_CREATED_INTERNALLY = 4;
    private Message message = null;
    private int eventId = -1;
    private boolean isServerTransaction = false;
    private long transactionId = 0;
}
