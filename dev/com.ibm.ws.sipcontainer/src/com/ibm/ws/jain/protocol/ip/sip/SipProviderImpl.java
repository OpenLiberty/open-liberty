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
package com.ibm.ws.jain.protocol.ip.sip;

import java.util.*;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.context.MessageContextFactory;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.transactions.SIPTransaction;
import com.ibm.ws.sip.stack.transaction.transactions.SIPTransactionsModel;
import com.ibm.ws.sip.stack.transaction.transactions.ct.SIPClientTranaction;
import com.ibm.ws.sip.stack.transaction.transactions.ct.SIPClientTransactionImpl;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;

import jain.protocol.ip.sip.*;
import jain.protocol.ip.sip.address.URI;
import jain.protocol.ip.sip.header.*;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

/**
 * @author Amir kalron
 *
 */
public class SipProviderImpl implements SipProvider
{

	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(SipProviderImpl.class);
	
	/** listeners for the provider */
	private List m_listenersList;
	
	/** the provider's listening point */
	private ListeningPoint m_listeningPoint;
	
	/** creator stack */
	private SipStackImpl m_stack;
	
	/** transaction stack */
	private SIPTransactionStack m_transactionStack;
	
	/** is the provider running */
	private boolean m_isRunning;
	
	
	/** construcor */
	SipProviderImpl()
	{
		init();
	}

	private void init()
	{
		m_listenersList = new ArrayList(16);
		m_isRunning = true;
	}
	
	/** 
	 *  start listening on the Listening point
	 *  call  the transport layer to start listening  
	 */ 
	void start()
	{
		m_isRunning = true;
	}
	
	/** stop listening on the listening point */
	void stop()
	{
		m_isRunning = false;
	}

	/**  return the providers listening point */
	public ListeningPoint getListeningPoint()
	{
		return m_listeningPoint;
	}
	
	/** there is a one to one relationship between a listening point to a provider */
	public void setListeningPoint( ListeningPoint  lp )
	{
		m_listeningPoint = lp ;
	}
	
	/** get CallId header*/
	public CallIdHeader getNewCallIdHeader() throws SipException
	{
		// section 8.1.1.4 - Call-ID
		//if no call id sent ,  create call id like 20.3
		
		// PM61939 - hide local IP in Call-ID
    	// getCallIdValue() returns the value of the new custom property com.ibm.ws.sip.callid.value if set
		// if not set it returns a local IP
		String cid = SIPStackUtil.generateCallIdentifier(m_listeningPoint.getCallIdValue());		
		return SipJainFactories.getInstance().getHeaderFactory().createCallIdHeader( cid ); 
	}

	/** return the handleing stack */
	public SipStack getSipStack()
	{
		return m_stack;
	}
	
	
	/** set the handling stack */
	public void setSipStack( SipStackImpl stack )
	{
		m_stack = stack ;
		m_transactionStack = stack.getTransactionStack();
	}
	

	/** get the request to the transation **/
	public Request getTransactionRequest(long transactionId,boolean isServerTransaction)
		throws TransactionDoesNotExistException
	{
		SIPTransaction transaction = getTransaction(transactionId, isServerTransaction);
		return (Request)transaction.getFirstRequest().clone(); 
	}

	/** get Response to specific transaction */
	public Response getTransactionResponse(long transactionId,boolean isServerTransaction)
		throws TransactionDoesNotExistException
	{
		SIPTransaction transaction = getTransaction(transactionId, isServerTransaction);
		return (Response)transaction.getMostRecentResponse().clone(); 
	}


	/**
	 * get the transaction associated with this transaction ID
	 * @param transactionId
	 * @param isServerTransaction
	 * @return
	 * @throws TransactionDoesNotExistException
	 */
	private SIPTransaction getTransaction(long transactionId,boolean isServerTransaction)
		throws TransactionDoesNotExistException
	{
		if (isServerTransaction) {
			return SIPTransactionsModel.instance().getServerTransaction(transactionId);
		}
		else {
			return SIPTransactionsModel.instance().getClientTransaction(transactionId);
		}
	}



	/** add a listener */
	public synchronized void addSipListener(SipListener sipListener)
		throws	IllegalArgumentException,TooManyListenersException,
				SipListenerAlreadyRegisteredException
	{
		List temp = (List)((ArrayList)m_listenersList).clone();
		temp.add(sipListener);
		m_listenersList = temp;
	}


	/**	remove a lisener */
	public synchronized void removeSipListener(SipListener sipListener)
		throws SipListenerNotRegisteredException
	{
		if (sipListener == null)
		{
			throw new IllegalArgumentException("SipListener cannot be null");
		}

		List temp = (List)((ArrayList)m_listenersList).clone();
		boolean hadListener = temp.remove(sipListener);
		m_listenersList = temp;
		
		if (!hadListener)
		{
			throw new SipListenerNotRegisteredException();
		}

	}

	/** send ACK */
	public long sendAck(	long clientTransactionId,
						 	byte[] body,
							String bodyType,
							String bodySubType)
		throws	IllegalArgumentException,TransactionDoesNotExistException,
				SipParseException,SipException
	{		
		Request ackReq = null;
		SIPClientTranaction transaction = (SIPClientTranaction)getTransaction( clientTransactionId , false ); 
		Response res = transaction.getFinalResponse();
		Request  req = transaction.getFirstRequest();
		
		if( c_logger.isTraceDebugEnabled())
		{
		c_logger.traceDebug(this,"sendAck","The response was:\n" + res.toString());
		}
		 
		String method;
		URI reqUril;
		CallIdHeader callHeader;
		CSeqHeader cseq;
		FromHeader from ;
		ToHeader   to ;
		List viaList;
		List routeHeaders;
		
		
		//ok , in my original code , the upper line would throw an TransactionDoesNotExistException
		//since the transaction already ended
		//but , the API is very easy to use this way , I supported it.
		//this means that this ACK is part of a dialog m and I should create the
		//ACK following section 12 in rfc 3261
		 
		method = Request.ACK;
		callHeader = req.getCallIdHeader();
		//ack does not neeed to be incremented since this is the same transaction
		cseq = req.getCSeqHeader();
		cseq.setMethod(method);
		//copy from the response since it will have the both tags
		from = res.getFromHeader();
		to   = res.getToHeader(); 
		//copy the via from the request
		viaList = SIPStackUtil.headerIteratorToList( req.getViaHeaders() );
		
		//if we have a record route headers , create record route ones
		routeHeaders = new ArrayList(16);
		if (res.getRecordRouteHeaders() != null && res.getRecordRouteHeaders().hasNext())
		{
			HeaderIterator iter = res.getRecordRouteHeaders();
			while (iter.hasNext())
			{
				RecordRouteHeader recordRoute = (RecordRouteHeader)iter.next();
				RouteHeader route =  SipJainFactories.getInstance().getHeaderFactory().createRouteHeader( recordRoute.getNameAddress());
				routeHeaders.add(route);
			}
			//the routes should be set in reverse order
			Collections.reverse(routeHeaders);
		}
		
		
		//set the remote target
		ContactHeader contact = (ContactHeader) res.getHeader(ContactHeader.name, true);
		if (contact==null)
		{			
			throw new SipException("Answer to Invite Must have a contact headear!!!");
		}
		
		reqUril = contact.getNameAddress().getAddress();
		
		//content type header
		ContentTypeHeader contentHeader = null;
		if( bodyType!=null && bodySubType!=null )
		{
			contentHeader = SipJainFactories.getInstance().getHeaderFactory().createContentTypeHeader( bodyType,bodySubType );
			ackReq = SipJainFactories.getInstance().getMesssageFactory().createRequest(reqUril,method,callHeader,cseq,from,to,viaList,body,contentHeader );			
		}
		else
		{
			ackReq = SipJainFactories.getInstance().getMesssageFactory().createRequest(reqUril,method,callHeader,cseq,from,to,viaList );
		}
		//add record routes
		if( routeHeaders.size()> 0 )
		{
			ackReq.setRouteHeaders( routeHeaders );
		}
		setRequestBasicParameters( ackReq );
		m_transactionStack.prossesUASipRequest( ackReq , this, -1 );		
		return clientTransactionId;
	}

	
	/** send ACK */
	public long sendAck( long clientTransactionId , Request req )
		throws	IllegalArgumentException,TransactionDoesNotExistException,
					SipParseException,SipException
	{
		setRequestBasicParameters( req );
		return m_transactionStack.prossesUASipACKRequest( clientTransactionId , req );			
	}
	
	
	/** send ACK **/
	public long sendAck(	long clientTransactionId,
							String body,
							String bodyType,
							String bodySubType)
		throws	IllegalArgumentException,TransactionDoesNotExistException,
				SipParseException,SipException
	{
		return sendAck( clientTransactionId , body.getBytes() , bodyType , bodySubType );
	}

	/** send ACK , no content **/
	public long sendAck( long clientTransactionId )
		throws TransactionDoesNotExistException, SipException
	{
		return sendAck( clientTransactionId , "" , null , null );
	}

	/**
	 * send a Bye to the dialog.
	 * I assume that the transaction ID is a transaction that was part of a dialog
	 */
	public long sendBye(long transactionId, boolean isServerTransaction)
		throws TransactionDoesNotExistException, SipException
	{


		Request byeReq = null;
		SIPTransaction transaction = getTransaction( transactionId , isServerTransaction ); 
		Response res = transaction.getMostRecentResponse();
		Request  req = transaction.getFirstRequest();
		 
		String method;
		URI reqUril;
		CallIdHeader callHeader;
		CSeqHeader cseq;
		FromHeader from ;
		ToHeader   to ;
		List viaList;
		List routeHeaders;

		//send a BYE on the DIALOG of the transaction
		//following section 12 in rfc 3261
		 
		//first , check if this is part of a dialog
		//if it has a tag in the to header
		to   = res.getToHeader();
		if( to.getTag()==null || "".equals(to.getTag()))
		{
			throw new SipException("No dilaog found for the transaction,the breanch of the To header is not set!!!");
		}
		
		//it is a dialog , construct the request like indicated in section 12			
		method = Request.BYE;
		reqUril = (URI)req.getRequestURI().clone();
		callHeader = (CallIdHeader)req.getCallIdHeader().clone();
		cseq = (CSeqHeader)req.getCSeqHeader().clone();
		cseq.setMethod(method);
		//neeed to be incremente		
		long newCseq = cseq.getSequenceNumber() + 1;
		cseq.setSequenceNumber( newCseq );		
		//copy from the response since it will have the both tags
		from = (FromHeader)res.getFromHeader().clone();
		 
		//copy the via from the request
		viaList = SIPStackUtil.headerIteratorToList( req.getViaHeaders() );
		
		//if we have a record route headers , create record route ones
		routeHeaders = new ArrayList(16);
		if (res.getRecordRouteHeaders() != null && res.getRecordRouteHeaders().hasNext())
		{
			HeaderIterator iter = res.getRecordRouteHeaders();
			while (iter.hasNext())
			{
				RecordRouteHeader recordRoute = (RecordRouteHeader)iter.next();
				RouteHeader route =  SipJainFactories.getInstance().getHeaderFactory().createRouteHeader( recordRoute.getNameAddress());
				routeHeaders.add(route);
			}
			//the routes should be set in reverse order
			Collections.reverse(routeHeaders);
		}
					
		byeReq = SipJainFactories.getInstance().getMesssageFactory().createRequest(reqUril,method,callHeader,cseq,from,to,viaList );			
		//add record routes
		if( routeHeaders.size()> 0 )
		{
			byeReq.setRouteHeaders( routeHeaders );
		}
		setRequestBasicParameters( byeReq );
		m_transactionStack.prossesUASipRequest(byeReq, this, -1);		
		return transactionId;
	}

	/** send cancel **/
	public long sendCancel(long clientTransactionId)
		throws TransactionDoesNotExistException, SipException
	{
		Request cancelReq;
		Request inviteReq = getTransactionRequest(clientTransactionId , false);

		URI reqUril = inviteReq.getRequestURI();
		CallIdHeader callHeader = inviteReq.getCallIdHeader();
		CSeqHeader cseq = inviteReq.getCSeqHeader();
		cseq.setMethod(Request.CANCEL);
		
		FromHeader from = inviteReq.getFromHeader();
		ToHeader   to   = inviteReq.getToHeader(); 
		List viaList = SIPStackUtil.headerIteratorToList( inviteReq.getViaHeaders() );

		cancelReq = SipJainFactories.getInstance().getMesssageFactory().createRequest(reqUril,Request.CANCEL,
			callHeader,cseq,from,to,viaList);

		//amirk - 05/09/2004
		// change to send request - I just want to create the request by the
		// original request , not process the previouse Transaction 
		//setRequestBasicParameters( cancelReq );				
		//retVal = m_transactionStack.prossesUASipCancelRequest( clientTransactionId , cancelReq , this );
		//return retVal;

		return sendRequest( cancelReq );
	}

	/** send Request */
	public long sendRequest(Request request)
		throws IllegalArgumentException, SipException
	{
		return sendRequest(request, -1);
	}
	
	/**
	 * proprietary call to allocate a transaction ID without creating a transaction object
	 */
	public static long allocateTransactionId() {
		return SIPClientTransactionImpl.getNextTransactionId();
	}
	
	/**
	 * proprietary call to send a request on a pre-allocated transaction ID
	 * @param transactionId pre-allocated transaction ID, or -1 to allocate one now
	 * @return the same transaction ID as given in the transactionId parameter
	 *  if it was provided, or the newly allocated transaction ID if caller passed -1.
	 */
	public long sendRequest(Request request, long transactionId)
		throws IllegalArgumentException, SipException
	{
		setRequestBasicParameters(request);
		return m_transactionStack.prossesUASipRequest(request, this, transactionId);
	}

	/** send a response */
	public void sendResponse(long serverTransactionId,int statusCode,
							  byte[] body,String bodyType,String bodySubType, String reasonPhrase)
		throws	IllegalArgumentException,TransactionDoesNotExistException,
			SipParseException,SipException
	{
		
		Response response = null;
		//the request
		Request req = getTransactionRequest( serverTransactionId , true ); 

		CallIdHeader callHeader = req.getCallIdHeader();
		CSeqHeader cseq = req.getCSeqHeader();
		
		//Amirp - Use the same from and to headers as we they are not switched
		//in the response. 
		FromHeader from = req.getFromHeader();
		ToHeader   to   = req.getToHeader();
		
		List viaList = SIPStackUtil.headerIteratorToList( req.getViaHeaders() );
		ContentTypeHeader contentHeader = null;
		if( bodyType!=null && bodySubType!=null )
		{
			contentHeader = SipJainFactories.getInstance().getHeaderFactory().createContentTypeHeader( bodyType,bodySubType );
			response = SipJainFactories.getInstance().getMesssageFactory().createResponse(statusCode,callHeader,cseq,from,to,viaList,body,contentHeader );
		}
		else
		{
			response = SipJainFactories.getInstance().getMesssageFactory().createResponse(statusCode,callHeader,cseq,from,to,viaList );
		}
		
		if(reasonPhrase != null) {
			response.setReasonPhrase(reasonPhrase);
		}
		
		sendResponse(serverTransactionId, response);
	}
	/** send a response */
	public void sendResponse(long serverTransactionId,int statusCode,
							  String body,String bodyType,String bodySubType, String reasonPhrase)
		throws	IllegalArgumentException,TransactionDoesNotExistException,
				SipParseException,SipException
	{
		sendResponse( serverTransactionId,statusCode,body.getBytes(),bodyType,bodySubType, reasonPhrase);
	}

	/** send a response , no content */
	public void sendResponse(long serverTransactionId, int statusCode, String reasonPhrase)
		throws TransactionDoesNotExistException, SipParseException, SipException
	{
		sendResponse( serverTransactionId,statusCode,"",null,null, reasonPhrase);		
	}

	/** send a response */
	public void sendResponse(long serverTransactionId, Response response)
		throws	IllegalArgumentException,TransactionDoesNotExistException,SipException
	{
		m_transactionStack.prossesUASipResponse( response , serverTransactionId );
	}
	
	/**
	 * Sends the Response statelessly, that is no transaction record is
	 * associated with this action. This method implies that the application is
	 * functioning as either a stateless proxy or a stateless UAS.
	 * 
	 * @param response the Response to send statelessly.
	 * @throws SipException if the SipProvider cannot send the Response for any reason.
	 */
	public void sendResponse(Response response) throws SipException {
		MessageContext messageContext = MessageContextFactory.instance().getMessageContext(response);
		try{
			m_transactionStack.getTransportCommLayerMgr().sendMessage(
					messageContext,	this, null);
		}catch (Exception e) {
			messageContext.writeError(e); 
		}
	}
    
	/** messages from transport to UA */
	public void sendEventToUA( SipEvent event )
	{
		onTransportEvent( event );
	}
    	 
	/** notify the ua on the right event */
	public void onTransportEvent( SipEvent event )
	{
		switch ( event.getEventId() )
		{
			case SipEvent.REQUEST_RECEIVED:
				onStackRequestEvent( event );
				break;
			case SipEvent.RESPONSE_RECEIVED:
			case SipEvent.ERROR_RESPONSE_CREATED_INTERNALLY:
				onStackResponseEvent( event );
				break;
			case SipEvent.TRANSACTION_TIMEOUT:
				onStackTimeOutEvent( event );
				break;
		}
	}
	
	/**
	 * notifications from the transport layer to transaction
	 * on a response event
	 * @param responseReceivedEvent
	 */
	private void onStackResponseEvent(SipEvent responseReceivedEvent)
	{
		List listeners = m_listenersList; 
	    for (int i=0; i<listeners.size(); i++)
		{
			SipListener listener = (SipListener) listeners.get(i);
			listener.processResponse( responseReceivedEvent );
		} 
	}

	/**
	 * notifications from the transport layer to transaction
	 * on a timeout event 
	 * @param responseReceivedEvent
	 */
	private void onStackTimeOutEvent(SipEvent transactionTimeOutEvent)
	{
	    List listeners = m_listenersList; 
	    for (int i=0; i<listeners.size(); i++)
		{
			SipListener listener = (SipListener) listeners.get(i);
			listener.processTimeOut( transactionTimeOutEvent );
		} 		
	}

	/**
	 * notifications from the transport layer to transaction
	 * on a request event 
	 * @param responseReceivedEvent
	 */
	private void onStackRequestEvent(SipEvent requestReceivedEvent)
	{
	    List listeners = m_listenersList; 
	    for (int i=0; i<listeners.size(); i++)
		{
			SipListener listener = (SipListener) listeners.get(i);
			listener.processRequest( requestReceivedEvent );
		}
	}
	
	
	/** return to String representation for the Provider */
	public String toString()
	{
		return "Sip Provider for listeningPoint " + m_listeningPoint.toString(); 
	}
	
	
	/**
	 * follow section 8.1.1 in the RFC
	 * add tag to the from header in the request
	 * @throws SipParseException if tag cannot be set
	 */
	private void setRequestBasicParameters( Request req   )
		throws SipParseException
	{	

		//follow section 19.3 to create a tag , and add it
		if( req.getFromHeader().getTag()==null)
		{
			req.getFromHeader().setTag( SIPStackUtil.generateTag() );
		}
		
		ViaHeader via = ( ViaHeader )req.getHeader( ViaHeader.name , true );
		if( via==null)
		{
			throw new SipParseException("no via header in message:\n", req.toString() );
		}
		else if( via.getBranch()==null )
		{
			via.setBranch(SIPStackUtil.generateBranchId()); 			
		}
	}

	/**
	 * follow RFC 8.2.6 Generating the Response
	 * add tag to the to header in the request 
	 * @throws SipParseException if tag cannot be set 
	 */
	private void setResponeBasicParameters( Response res   )
		throws SipParseException
	{	

		//8.2.6.2 Headers and Tags 
		//add the to tag for dialog creation
		if( res.getToHeader().getTag()==null)
		{
			//this is not part of a dialog , add the tag
			res.getToHeader().setTag( SIPStackUtil.generateTag() );						  					
		}			 			
	}	
}
