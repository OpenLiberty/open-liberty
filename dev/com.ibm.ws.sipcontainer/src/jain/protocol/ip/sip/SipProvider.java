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

import jain.protocol.ip.sip.header.CallIdHeader;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import java.util.TooManyListenersException;

/**
 * This interface must be implemented by any Object representing
 * a SIP Provider that interacts directly with a
 * proprietary (stack vendor specific) implementation of a SIP stack.
 * There is a one-to-one relationship between SipProviders and ListeningPoints.
 * This interface defines the methods that will be
 * used by any registered SIP User application (implementing the
 * SipListener interface) to send SIP messages.
 * It must be noted that any object that implements the:
 * <UL>
 * <LI>SipListener Interface is referred to as SipListenerImpl
 * <LI>SipProvider Interface is referred to as SipProviderImpl
 * <LI>SipStack Interface is referred to as SipStackImpl
 * </UL>
 *
 * @see SipListener
 * @see SipStack
 *
 * @version 1.0
 *
 */
public interface SipProvider
{
    
    /**
     * Removes SipListener from list of registered SipListeners of
     * this SipProvider
     * @param <var>SipListener</var> SipListener to be removed from this
     * SipProvider
     * @throws IllegalArgumentException if SipListener is null
     * @throws SipListenerNotRegisteredException if SipListener is not
     * registered with this SipProvider
     */
    public void removeSipListener(SipListener sipListener)
                 throws IllegalArgumentException,SipListenerNotRegisteredException;
    
    /**
     * Returns SipStack that this SipProvider is attached to.
     * @return the attached SipStack.
     */
    public SipStack getSipStack();
    
    /**
     * Returns ListeningPoint of this SipProvider
     * @return ListeningPoint of this SipProvider
     */
    public ListeningPoint getListeningPoint();
    
    /**
     * Adds SipListener to list of registered SipListeners of this
     * SipProvider
     * @param <var>SipListener</var> SipListener to be added
     * @throws IllegalArgumentException if SipListener is null
     * @throws TooManyListenersException if a limit has placed on number of
     * registered SipListeners for this SipProvider,
     * and this limit has been reached.
     * @throws SipListenerAlreadyRegisteredException if SipListener is
     * already registered with this SipProvider
     */
    public void addSipListener(SipListener sipListener)
                 throws IllegalArgumentException,TooManyListenersException,SipListenerAlreadyRegisteredException;
    
    /**
     * Sends specified Response for specified server transaction
     * @param <var>serverTransactionId</var> server transaction id (0 to send Response
     * independently of existing server transaction)
     * @param <var>response</var> the Response to send
     * @throws IllegalArgumentException if response is null or
     * if not from same JAIN SIP implementation
     * @throws TransactionDoesNotExistException if serverTransactionId does not
     * correspond to any server transaction
     * @throws SipException if implementation cannot send response for any other reason
     */
    public void sendResponse(long serverTransactionId, Response response)
                 throws IllegalArgumentException,TransactionDoesNotExistException,SipException;
    
    /**
     * Sends automatically generated Response for specified server transaction
     * @param <var>serverTransactionId</var> server transaction id
     * @param <var>statusCode</var> the status code of Response
     * @param <var>reasonPhrase</var> the reason phrase of Response
     * @throws TransactionDoesNotExistException if serverTransactionId does not
     * correspond to any server transaction
     * @throws SipParseException if statusCode is not accepted by implementation
     * @throws SipException if implementation cannot send response for any other reason
     */
    public void sendResponse(long serverTransactionId, int statusCode, String reasonPhrase)
                 throws TransactionDoesNotExistException,SipParseException,SipException;
    
    /**
     * Sends automatically generated Response for specified server transaction
     * @param <var>serverTransactionId</var> server transaction id
     * @param <var>statusCode</var> the status code of Response
     * @param <var>reasonPhrase</var> the reason phrase of Response
     * @param <var>body</var> body of Response
     * @param <var>bodyType</var> body type used to create ContentTypeHeader of
     * Response
     * @param <var>bodySubType</var> body sub-type used to create ContentTypeHeader of
     * Response
     * @throws IllegalArgumentException if body, bodyType
     * or bodySubType are null
     * @throws TransactionDoesNotExistException if serverTransactionId does not
     * correspond to any server transaction
     * @throws SipParseException if statusCode, body, bodyType or bodySubType
     * are not accepted by implementation
     * @throws SipException if implementation cannot send response for any other reason
     */
    public void sendResponse(long serverTransactionId, int statusCode, byte[] body, String bodyType, String bodySubType, String reasonPhrase)
                 throws IllegalArgumentException,TransactionDoesNotExistException,SipParseException,SipException;
    
    /**
     * Returns Request associated with specified transaction
     * @param <var>transactionId</var> transaction id
     * @param <var>isServerTransaction</var> boolean value to indicate if
     * transactionId represents a server transaction
     * @return Request associated with specified transaction
     * @throws TransactionDoesNotExistException if transactionId does not
     * correspond to a transaction
     */
    public Request getTransactionRequest(long transactionId, boolean isServerTransaction)
                    throws TransactionDoesNotExistException;
    
    /**
     * Sends specified Request and returns ID of implicitly created
     * client transaction.
     * @param <var>request</var> Request to send
     * @return client transaction id (unique to underlying SipStack)
     * @throws IllegalArgumentException if request is null or not from
     * same SIP implementation as SipProvider
     * @throws SipException if implementation cannot send request for any other reason
     */
    public long sendRequest(Request request)
                 throws IllegalArgumentException,SipException;
    
    /**
     * Sends automatically generated ACK Request to the recipient of the INVITE Request
     * associated with specified client transaction
     * @param <var>clientTransactionId</var> client transaction id
     * @return client transaction id (unique to the underlying SipStack)
     * @throws TransactionDoesNotExistException if clientTransactionId does not
     * correspond to any client transaction
     * @throws SipException if implementation cannot send ack for any other reason
     */
    public long sendAck(long clientTransactionId)
                 throws TransactionDoesNotExistException,SipException;
    
    /**
     * Sends automatically generated Response for specified server transaction
     * @param <var>serverTransactionId</var> server transaction id
     * @param <var>statusCode</var> the status code of Response
     * @param <var>body</var> body of Response
     * @param <var>bodyType</var> body type used to create ContentTypeHeader of
     * Response
     * @param <var>bodySubType</var> body sub-type used to create ContentTypeHeader of
     * Response
     * @throws IllegalArgumentException if body, bodyType
     * or bodySubType are null
     * @throws TransactionDoesNotExistException if serverTransactionId does not
     * correspond to any server transaction
     * @throws SipParseException if statusCode, body, bodyType or bodySubType
     * are not accepted by implementation
     * @throws SipException if implementation cannot send response for any other reason
     */
    public void sendResponse(long serverTransactionId, int statusCode, String body, String bodyType, String bodySubType, String reasonPhrase)
                 throws IllegalArgumentException,TransactionDoesNotExistException,SipParseException,SipException;
    
    /**
     * Sends automatically generated ACK Request to the recipient of the INVITE Request
     * associated with specified client transaction
     * @param <var>clientTransactionId</var> client transaction id
     * @param <var>body</var> body of AckMessage
     * @param <var>bodyType</var> body type used to create ContentTypeHeader of
     * AckMessage
     * @param <var>bodySubType</var> body sub-type used to create ContentTypeHeader of
     * AckMessage
     * @return client transaction id (unique to the underlying SipStack)
     * @throws IllegalArgumentException if body, bodyType
     * or bodySubType are null
     * @throws TransactionDoesNotExistException if clientTransactionId does not
     * correspond to any client transaction
     * @throws SipParseException if body, bodyType or
     * bodySubType are not accepted by implementation
     * @throws SipException if implementation cannot send ack for any other reason
     */
    public long sendAck(long clientTransactionId, String body, String bodyType, String bodySubType)
                 throws IllegalArgumentException,TransactionDoesNotExistException,SipParseException,SipException;
    
    /**
     * Returns new CallIdHeader (unique to SipProvider)
     * @return new CallIdHeader (unique to SipProvider)
     * @throws SipException if SipProvider cannot generate a new CallIdHeader
     */
    public CallIdHeader getNewCallIdHeader()
                         throws SipException;
    
    /**
     * Sends automatically generated ACK Request to the recipient of the INVITE Request
     * associated with specified client transaction
     * @param <var>clientTransactionId</var> client transaction id
     * @param <var>body</var> body of AckMessage
     * @param <var>bodyType</var> body type used to create ContentTypeHeader of
     * AckMessage
     * @param <var>bodySubType</var> body sub-type used to create ContentTypeHeader of
     * AckMessage
     * @return client transaction id (unique to the underlying SipStack)
     * @throws IllegalArgumentException if body, bodyType
     * or bodySubType are null
     * @throws TransactionDoesNotExistException if clientTransactionId does not
     * correspond to any client transaction
     * @throws SipParseException if body, bodyType or
     * bodySubType are not accepted by implementation
     * @throws SipException if implementation cannot send ack for any other reason
     */
    public long sendAck(long clientTransactionId, byte[] body, String bodyType, String bodySubType)
                 throws IllegalArgumentException,TransactionDoesNotExistException,SipParseException,SipException;
    
    /**
     * Returns most recent Response associated with specified transaction
     * (Returns null if there is no Response associated with specified transaction)
     * @param <var>transactionId</var> transaction id
     * @return the Request associated with server transaction
     * @param <var>isServerTransaction</var> boolean value to indicate if
     * transactionId represents a server transaction
     * @throws TransactionDoesNotExistException if transactionId does not
     * correspond to a transaction
     */
    public Response getTransactionResponse(long transactionId, boolean isServerTransaction)
                     throws TransactionDoesNotExistException;
    
    /**
     * Sends automatically generated CANCEL Request to the recipient of the Request
     * associated with specified client transaction
     * @param <var>clientTransactionId</var> client transaction id
     * @throws TransactionDoesNotExistException if clientTransactionId does not
     * correspond to any client transaction
     * @throws SipException if implementation cannot send cancel for any other reason
     */
    public long sendCancel(long clientTransactionId)
                 throws TransactionDoesNotExistException,SipException;
    
    /**
     * Sends automatically generated BYE Request based on specified client or
     * server transaction (Note it is assumed that the specified transaction is
     * the most recent one of the call-leg)
     * @param <var>transactionId</var> transaction id
     * @param <var>isServertransactionId</var> boolean value indicating if
     * transactionId represents a server transaction
     * @return client transaction id (unique to the underlying SipStack)
     * @throws TransactionDoesNotExistException if transactionId does not
     * correspond to client or server transaction
     * @throws SipException if implementation cannot send bye for any other reason
     */
    public long sendBye(long transactionId, boolean isServerTransaction)
                 throws TransactionDoesNotExistException,SipException;
}
