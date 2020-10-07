/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.tu;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

import javax.servlet.sip.*;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import com.ibm.sip.util.log.*;
import com.ibm.ws.jain.protocol.ip.sip.message.RequestImpl;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.failover.ReplicatableImpl;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.protocol.OutboundProcessor;
import com.ibm.ws.sip.container.proxy.*;
import com.ibm.ws.sip.container.router.SipRouter;
import com.ibm.ws.sip.container.router.SipServletInvokerListener;
import com.ibm.ws.sip.container.servlets.*;
import com.ibm.ws.sip.container.sessions.SessionId;
import com.ibm.ws.sip.container.sessions.SipTransactionUserTable;
import com.ibm.ws.sip.container.timer.Invite2xxRetransmitTimer;
import com.ibm.ws.sip.container.transaction.*;
import com.ibm.ws.sip.container.util.OutboundInterface;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;
import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.parser.util.InetAddressCache;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.sip.stack.properties.StackProperties;
import com.ibm.ws.sip.stack.transaction.SIPTransactionConstants;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;

import jain.protocol.ip.sip.*;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.*;
import jain.protocol.ip.sip.message.*;

/**
 * 
 * @author anat, Nov 23, 2005
 *
 * Implements the TransactionUser interface and acts as an TU (RFC 3261) 
 * 
 * The SIP elements, that is, user agent clients and servers, stateless
 * and stateful proxies and registrars, contain a core that
 * distinguishes them from each other.  Cores, except for the stateless 
 * proxy, are transaction users.
 * Transaction User (TU): The layer of protocol processing that resides above the transaction layer.  
 * Transaction users include the UAC core, UAS core, and proxy core.
 */
public class TransactionUserImpl  extends ReplicatableImpl {

	/** Serialization UID (do not change) */
    private static final long serialVersionUID = 6551419490828855140L;
    
	/**
	 * Represents the reason phrase used when a 500 response is sent
	 * because we got another invite on the same dialog before a final
	 * response was sent on a previous one (RFC 3261, section 14.2).
	 */
	private static final String NO_FINAL_RESP_PREV_INVITE = "No final response to previous INVITE";

	/**
	 * Represents the reason phrase used when a 500 response 
	 * is sent due to incorrect cseq number.
	 */
	private static final String INCORRECT_CSEQ_REASON = "Incorrect CSeq number";

	/**
	 * Represents the number of seconds for the Retry-After header,
	 * used when implementing section 14.2 in RFC 3261.
	 */
	private static final String RETRY_AFTER_SECONDS = "10"; 

	/**
	 * Class Logger.
	 */
	private static final transient LogMgr c_logger = Log
	.get(TransactionUserImpl.class);

	/**
	 * Router for passing message to siplets.
	 */
	private static final transient SipRouter c_router = SipContainer.getInstance()
	.getRouter();

	/**
	 * The original Sip request that is associated with this Sip Session .
	 */
	private transient SipServletRequestImpl m_sipMessage;


	/**
	 * Contains the CallId associated with this TU. 
	 */
	private String m_callId;

	/**
	 * Contain SipProvider - used for failover and performance
	 */
	private transient SipProvider m_sipProvider;


	/**
	 * The local party in the call leg.
	 */
	private Address m_localParty;

	/**
	 * The remote party in the call leg.
	 */
	private Address m_remoteParty;

	/**
	 * Indicates whether the session is in server transaction or client
	 * transaction state.
	 */
	private boolean m_isServerTransaction;


	/**
	 * Holds the status of the final response that was sent for this dialog. If
	 * final response has not been sent the value hold the status of the last
	 * response sent.
	 */
	private int m_finalResponseStatus;

	/**
	 * Next CSeq number.
	 */
	private long m_localCSeq = 1;

	/**
	 * Variable that hold CSEQ that represents CSEQ for remote leg
	 * Used to ensure that new incoming request has higher CSEQ number
	 * then in the previous request
	 */
	private long m_remoteCseq = -1;

	/**
	 * Locally generated tag for client transaction initiated by application
	 * running in the container. Need to generate it only once so all requests
	 * will use the same from tag.
	 * In Proxy scenario can be persist if Application decides to send outgoing
	 * response in Addition to proxy the request out.
	 */
	private String m_localTag;

	/**
	 * Represents a remote Tag.
	 */
	private String m_remoteTag;

	/**
	 * Proxy mode only this tag represents a tag of original request's receiver.
	 */
	private String _destinationTagInProxy;


	/**
	 * Flag indicating whether we this session is in Proxy mode. For sessions in
	 * this mode we need to pass both CANCEL and subsequent requests on the same
	 * dialog to their destination.
	 */
	private boolean m_isProxying;

	/**
	 * Flag indicating whether we this session is in B2B mode. For sessions in
	 * this mode we can't create Proxy
	 */
	// TODO Create Enum mode {PROXY, B2B} ??
	private boolean _isB2B = false;

	/**
	 * Flag indicating whether we this session is in UAS mode. For sessions in
	 * this mode we can't create Proxy
	 */
	private boolean _isUAS = false;

	/**
	 * Flag indicating whether this dialog is in the Proxy mode but
	 * Application has decided to answer with response to the UAC that has sent
	 * the initial request. 
	 */
	private boolean _isCombinedMode;

	/**
	 * This tag will be generated when Application will send Response downstream
	 * Will be used in Combined Mode when Application will send Response in 
	 * addition to proxy it. This Tag will identify request that should be
	 * forwarded to Application and not proxied it to the UAS
	 */

	/**
	 * Flag indicating whether we this session is proxying in Record - Route
	 * mode. If so we need to add it the session table once the session is
	 * confirmed.
	 */
	private boolean m_isRRProxy;
	/**
	 * This flag defines if this is a virtual branch
	 */
	private boolean m_isVirtualBranch = false;

	
	/**
     * The Contact address of the remote party in this dialog. 
     */
	private Address m_remoteContact; 

	/**
	 * List of Route Header to be added to subsequent request on the same dialog
	 */
	private Vector<String> m_routeHeaders = null; 

	/**
	 * Holds all reliable responses that are relates to the session
	 */
	private transient ReliableResponsesProcessor m_reliableProcessor = null;

	/**
	 * Session Ids counter.
	 */
	private int _nextSipSessionId = 1;

	/**
	 * Constant used in Route headers for identifing new request on the same
	 * dialog.
	 */
	public static final String SESSION_RR_PARAM_KEY = "ibmsid";

	/** Separator used within tag to separate between the session identifier and
	 * unique 
	 */
	public static final char SESSION_ID_TAG_SEPARATOR = '_';


	/**
	 * The ACK generated in case of 2xx response to INVITE, we keep around 
	 * so we can send it back in case we get retransmission 
	 */
	private transient OutgoingSipServletAckRequest m_AckFor2xxRef;

	/**
	 * CSeq value of last INVITE received by this session when acting as a UAS. 
	 */
	private long m_inviteCseq;

	/**
	 * Pending CANCEL request in case the application sent a CANCEL request 
	 * prior to receiving a provisional response.
	 * Changed back to null as soon as sending it out. 
	 */
	private transient SipServletRequest m_pendingCancelReq;

	/**
	 * indicates that Cancel message was sent out for this TU
	 * used to know if we need to create automatic Cancel or not
	 */
	private transient boolean m_cancelSent = false;

	/**
	 * This flag defines if the initial request that started the dialog
	 * was forwarded to the APP by the Websphere.
	 * If the request was rejected by the security level without
	 * forwarding to the Application = the flag will be false.
	 */
	private transient boolean m_forwardToApplication = true;

	/**
	 * Will be true only if it acts as UAS and response on the initial INVITE was send reliably
	 */
	private transient boolean m_wasAnsweredReliable = false;

	/**
	 *  Handle to the TransactionUserWrapper
	 */
	//Moti: OG: must set _tuWrapper to transient.
	private transient TransactionUserWrapper _tuWrapper = null;

	/**
	 * Use for not replicating when TU is invalidated
	 */
	private transient boolean _duringInvalidate = false;

	/**
	 * Indicates that open transactions are now being automatically terminated by the container when TU invalidation was initiated. We use this flag to avoid
	 * TU cleaning triggered from the transaction termination, and make sure that happens only on the end of the TU invalidation process.
	 */
	private transient boolean _underlyingTransactionsBeingTerminated  = false;

	/**
	 * Holds the reference for the timer that is responsible to retransmit the
	 * 2xx response in case this TU is UAS.
	 */
	private Invite2xxRetransmitTimer _2xxRetransmitTimer = null;

	/**
	 * When this flag is on, the TU will be replicated also for non-confirmed dialogs
	 */
	private transient boolean _replicateAllStates = false;

	/**
	 * This parameter will hold the method of Initial Request.
	 */
	private String _initialDialogMethod;

	/**
	 * When this flag is on, the TU will not creat an timeout response upon invalidation.
	 */
	private transient boolean _proxyReceivedFinalResponse = false;

	/**
	 * Holds the information about the destination where the INVITE (if it is INVITE)
	 * sent and provisional response was received on it. Will be used for next
	 * CANCEL request it it will coming.
	 * Not need to be replicated - only for early dialog.
	 */
	private transient SipURL _latestDestination;


	/**
	 * Holds the ID of the related Session. Meaning that this TU created when
	 * INVITE message received where "Join" or "Replaced" headers persists.
	 */
	private String _relatedSessionId = null;

	/**
	 * Holds the related session header that set the _relatedSessuinId.
	 */
	private String _relatedSessionHeader = null;

	/**
	 * The subscriber URI is set by the application router
	 * at application selection process. 
	 */
	private URI _subscriberURI = null;

	/**
	 * The region is set by the application router
	 * at application selection process. 
	 */
	private SipApplicationRoutingRegion _region = null;

	/**
	 * Holds pending messages for B2B session according to UA mode.
	 * After the SipSession is invalidate, we do not need those messages
	 * List of message objects sorted by message Cseq order.
	 */
	private transient LinkedList<SipServletMessage> _b2bUACPendingMessages = null;

	private transient LinkedList<SipServletMessage> _b2bUASPendingMessages = null;

	/**
	 * Flag that indicates true in case failed response sent to original request.
	 */
	private boolean _isFailedResponseSent = false;

	/**
	 * members to support JSR289 feature: Multi Homed host.
	 */
	private OutboundInterface _preferedOutBoundIface = new OutboundInterface();
	/*private int 	_preferedOutBoundIfaceIdxUDP = SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED; 
	private int 	_preferedOutBoundIfaceIdxTCP = SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED; 
	private int 	_preferedOutBoundIfaceIdxTLS = SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED;*/ 


	/**
	 * In proxy mode when the proxy acts as a Record-Route proxy, we want to keep the 
	 * inbound interface for the UAC side so we can use it for subsequent requests
	 */
	private OutboundInterface _originatorPreferedOutBoundIface = new OutboundInterface();

	/**
	 * Will be true only when a final response to a BYE will be received 
	 */
	private boolean _terminateConfirmed = false;
	/**
	 * Member that identifies that this TU was already added to the
	 * Transactions users table.
	 */
	private transient boolean _addedToTUTable = false;

	/**
	 * This variable represents ID of this TU. In case when this is
	 * DerivedSession = it will hold the ID which is insterted in the
	 * RR of original TU. 
	 * Relevant only for Proxy mode.
	 */
	private String _sharedIdForDS = null;

	/**
	 * Reference to TUKey object which is used as a key of this TU.
	 * Need it to return it to the pool
	 */
	private transient TUKey _key;

	/**
	 * Indicates whether an invite request was received 
	 * that has no final response yet.
	 */
	private transient boolean _noFinalResponseToReceivedInvite = false;

	/**
	 * Indicates whether an invite request was sent 
	 * that has no final response yet.
	 */
	private transient boolean _noFinalResponseToSentInvite = false;

	/**
	 * Represents the cseq of the invite request
	 * that has no final response yet.
	 */
	private transient long _pendingReceivedInviteCseq = -1;

	/**
	 * Represents the cseq of the invite request
	 * that has no final response yet.
	 */
	private transient long _pendingSentInviteCseq = -1;

	/**
	 * Holds the last 2xx response that came back from the application for a case where 
	 * we are in proxying mode which means Record-Route and Supervised mode. Used in case
	 * the 200 OK will be retransmitted for a Re-Invite event. In this case we will use this
	 * value for a direct transmission on transport. 
	 */
	private transient SipServletResponseImpl _lastOkResponse;

	/**
	 * Stores retransmit timer per 2xx CSeq so as to process all ACKs independently of the order the ACK received. 
	 * Then cancel the timer for each CSeq when ACK with the same CSeq received.
	 */
	private HashMap <Long, Invite2xxRetransmitTimer> _retransmitTimerPerCSeq = null;

	/**
	 * Sets a response code to return when an underlying transaction is invalidated
	 */
	private int _sessionInvalidatedResponse = 
			PropertiesStore.getInstance().getProperties().getInt(CoreProperties.WAS80_SESSION_INVALIDATE_RESPONSE);
    /**
	 * Stores client transactions in Proxy mode only PI17680.
	 * Table key is transactionId
	 */
	private HashMap <Long, ClientTransaction> _proxyClientTransactions = null;

	/**
    * When this flag is true - meaning that Proxy received reINVITE wich was not answered with final response yet.
    */

   private transient boolean _proxyHasOngoingReInvite = false;
	
	/**
	 * Default constructor
	 */
	public TransactionUserImpl(){
		super();
	}

	/**
	 * Construct a new Transaction User
	 * 
	 * @param sipMessage
	 *            The Sip Message that was associated with this session.
	 * @param isServerTransaction
	 *            Indicates whether the session is associated with a server or a
	 *            client transaction.
	 * @pre sipMessage != null
	 *  
	 */    
	void initialize(TransactionUserWrapper tuWrapper,
			SipServletRequestImpl sipMessage,
			boolean isServerTransaction,
			SipApplicationSessionImpl sipApp) {
		m_sipMessage = sipMessage;
		_tuWrapper = tuWrapper;

		_initialDialogMethod = m_sipMessage.getMethod();
		if (c_logger.isTraceDebugEnabled()) {
			StringBuffer buff = new StringBuffer();
			buff.append("TU ID = ");
			buff.append(_tuWrapper.getId());
			buff.append(_tuWrapper.getAppName());
			buff.append(" Initial request Method = ");
			buff.append(_initialDialogMethod);
			c_logger.traceDebug(this, "initialize", buff.toString());
		}
		m_isServerTransaction = isServerTransaction;
		_tuWrapper.logToContext(SipSessionSeqLog.IS_UAS, m_isServerTransaction ? "true"
				: "false");

		setParams();
		
		setSharedId( _tuWrapper.getSharedID());
	}

	/**
	 * Construct a new Derived Transaction User
	 * @param tuWrapper Related TransactionUserWrapper
	 * @param originalTU base TransactionUser 
	 * @param response reponse which caused for this Derived TU
	 * to be created.
	 */
	void initializeDerivedTU(TransactionUserWrapper tuWrapper,
			TransactionUserWrapper originalTU) {

		m_sipMessage = originalTU.getSipMessage();
		_tuWrapper = tuWrapper;
		SipTransaction st = m_sipMessage.getTransaction();
		if(st != null){
			st.addReferece(_tuWrapper);
		}

		_b2bUACPendingMessages = null;
		_b2bUASPendingMessages = null;


		_initialDialogMethod = m_sipMessage.getMethod();
		if (c_logger.isTraceDebugEnabled()) {
			StringBuffer buff = new StringBuffer();
			buff.append("TU ID = ");
			buff.append(_tuWrapper.getId());
			buff.append(_tuWrapper.getAppName());
			buff.append("Base ID = ");
			buff.append(originalTU.getId());
			buff.append(" Initial request Method = ");
			buff.append(_initialDialogMethod);
			c_logger.traceDebug(this, "initializeDerivedTU", buff.toString());
		}

		m_isServerTransaction = originalTU.isServerTransaction();
		_tuWrapper.logToContext(SipSessionSeqLog.IS_UAS, m_isServerTransaction ? "true"
				: "false");

		setParams();

		setAllVariablesFromOriginal(originalTU);

		if(_isB2B){		
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "initializeDerivedTU", 
				"Initialise TU for B2B");
			}
			List<SipServletMessage> pendingMessages = originalTU.getPendingMessages(UAMode.UAS);

			if(pendingMessages != null && pendingMessages.size()!= 0){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "initializeDerivedTU", 
							_tuWrapper.getAppName() + " Creating UAS_ based on UAS_1 when pending request in B2B list");
				}
				IncomingSipServletRequest origRequest = 
					(IncomingSipServletRequest)originalTU.getPendingMessages(UAMode.UAS).get(0);
				IncomingDeadSipRequest origRequestForThisTU = new 
				IncomingDeadSipRequest(origRequest,_tuWrapper);

				m_sipMessage = origRequestForThisTU;

				addB2BPendingMsg(origRequestForThisTU,UAMode.UAS);				
			}
			else if (isServerTransaction()){

				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "initializeDerivedTU", 
							_tuWrapper.getAppName() + " No Pending messages use initial m_sipMessage ");
				}	

				if(m_sipMessage != null){

					IncomingDeadSipRequest origRequestForThisTU = new 
					IncomingDeadSipRequest((IncomingSipServletRequest)m_sipMessage,_tuWrapper);

					addB2BPendingMsg(origRequestForThisTU,UAMode.UAS);
				}				
			}
		}				
		/*TODO Liberty initForReplication( _tuWrapper.getLogicalName(), _tuWrapper.getSharedID());*/
	}

	/**
	 * Helper method which sets the parameters of this TUImpl during initialization
	 *
	 */
	private void setParams() {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {  _tuWrapper.getAppName(), _tuWrapper.getId()};
			c_logger.traceEntry(this,"setParams", params);
		}

		if (m_isServerTransaction) {
			m_localParty = m_sipMessage.getTo();
			m_remoteParty = m_sipMessage.getFrom();
			// Only if ServerTransaction we already have the tag from the remote party.
			m_remoteTag = ((SipServletRequestImpl)m_sipMessage).getRequest().getFromHeader().getTag();
		} else {
			m_localParty = m_sipMessage.getFrom();
			m_remoteParty = m_sipMessage.getTo();
		}

		m_callId = m_sipMessage.getCallIdHeader().getCallId();
		
		m_sipProvider = m_sipMessage.getSipProvider();

		//	Determine what interface the original request was received on.
		//	This will be used to set the preferred outbound interface. This can
		//	be overwritten by the application. Note that we can only do this for
		//	a server transaction. Outbound interfaces for a client transaction either
		//	takes the default or comes from the application.
		if (m_isServerTransaction == true)
		{
			SipURI uri = SipProxyInfo.getInstance().extractReceivedOnInterface(m_sipMessage);

			if (uri != null)
			{
				if (c_logger.isTraceDebugEnabled()) {
					StringBuffer buff = new StringBuffer();
					buff.append("Extracted Received On Interface: ");
					buff.append("host = " + uri.getHost());
					buff.append(" port = " + uri.getPort());
					c_logger.traceDebug(this, "setParams", buff.toString());
				}

				InetSocketAddress address = InetAddressCache.getInetSocketAddress(uri.getHost(), uri.getPort());

				setOutboundInterface(address);
			}
			else
			{
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "setParams", "ERROR: no received on interface was found!!!");
				}
			}
		}
		// Don't set default for client transaction
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setParams: tu = " + this.hashCode(), null);
			c_logger.traceDebug(this, "setParams: _preferedOutBoundIfaceIdx = " + _preferedOutBoundIface, null);
/*
			c_logger.traceDebug(this, "setParams: _preferedOutBoundIfaceIdx = " + _preferedOutBoundIfaceIdxUDP, null);
			c_logger.traceDebug(this, "setParams: _preferedOutBoundIfaceIdxTCP = " + _preferedOutBoundIfaceIdxTCP, null);
			c_logger.traceDebug(this, "setParams: _preferedOutBoundIfaceIdxTLS = " + _preferedOutBoundIfaceIdxTLS, null);*/
		}

		
		// set the response code for terminating underlying transaction
		_sessionInvalidatedResponse = 
					PropertiesStore.getInstance().getProperties().getInt(CoreProperties.WAS80_SESSION_INVALIDATE_RESPONSE);

		/*TODO Liberty initForReplication( _tuWrapper.getLogicalName(), _tuWrapper.getSharedID());*/
	}
	/**
	 * Helper method that sets all variables to be like in original TU.
	 * @param originalTU
	 */
	private void setAllVariablesFromOriginal(TransactionUserWrapper originalTU) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {  _tuWrapper.getAppName(), _tuWrapper.getId()};
			c_logger.traceEntry(this,"setAllVariablesFromOriginal", params);
		}
		_isB2B = originalTU.isB2B();
		if(!m_isServerTransaction){
			m_localTag = originalTU.getLocalTag();
		}
		m_isRRProxy = originalTU.isRRProxy();
		m_inviteCseq = originalTU.getInviteCseq();
		m_wasAnsweredReliable = originalTU.wasAnsweredReliable();
		m_remoteCseq = originalTU.getRemoteCseq();
		m_localCSeq = originalTU.getLocalCSeq();
		_initialDialogMethod = m_sipMessage.getMethod();
		_latestDestination = originalTU.getUsedDestination();
		_preferedOutBoundIface = new OutboundInterface(originalTU.getPreferedOutboundIface("UDP"),
				originalTU.getPreferedOutboundIface("TCP"),
				originalTU.getPreferedOutboundIface("TLS")); 
		m_isProxying = originalTU.isProxying();
		m_routeHeaders = originalTU.getRouteHeaders();
		_sharedIdForDS = originalTU.getSharedIdForDS();
		m_remoteContact = originalTU.getContactHeader();

	}

	/**
	 * Returns the sharedIdForDS from TUImp
	 * @return
	 */
	String getSharedIdForDS() {
		if (_sharedIdForDS != null) {
			return _sharedIdForDS;
		}
		return _tuWrapper.getId();
	}



	/**
	 * Helper method that will clean all the members in the TU and prepare
	 * it for the future use
	 */
	void cleanTU(){
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {  _tuWrapper.getAppName(), _tuWrapper.getId()};
			c_logger.traceEntry(this,"cleanTU", params);
		}
		//    	This method prepare the TU to be reUsed.
		m_sipMessage = null;
		m_callId = null;
		m_sipProvider = null;
		m_localParty = null;
		m_remoteParty = null;
		m_isServerTransaction = false;
		m_finalResponseStatus = -1;
		m_localCSeq = 1;
		m_remoteCseq = -1;
		m_localTag = null;
		m_remoteTag =  null;
		m_isProxying = false;
		m_isRRProxy = false;
		m_isVirtualBranch = false;
		m_remoteContact = null;
		m_routeHeaders = null;
		m_reliableProcessor = null;
		m_AckFor2xxRef = null;
		m_inviteCseq = 0;
		m_pendingCancelReq = null;
		m_cancelSent = false;
		m_forwardToApplication = true;
		m_wasAnsweredReliable = false;
		_nextSipSessionId = 1;
		_isCombinedMode = false;
		_tuWrapper = null;
		_duringInvalidate = false;
		_underlyingTransactionsBeingTerminated  = false;
		_2xxRetransmitTimer = null;
		_initialDialogMethod = null;
		_relatedSessionId = null;
		_relatedSessionHeader = null;
		_latestDestination = null;
		_isB2B = false;
		_isUAS = false;

		if (_b2bUACPendingMessages != null) {
			_b2bUACPendingMessages.clear();
		}
		if (_b2bUASPendingMessages != null) {
			_b2bUASPendingMessages.clear();
		}
		_isFailedResponseSent = false;
		_addedToTUTable = false;
		_sharedIdForDS = null;
		//_preferedOutBoundIfaceIdxUDP = SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED;
		//_preferedOutBoundIfaceIdxTCP = SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED;
		//_preferedOutBoundIfaceIdxTLS = SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED;
		_preferedOutBoundIface = new OutboundInterface();
		_originatorPreferedOutBoundIface = new OutboundInterface();
		_destinationTagInProxy = null;
		_replicateAllStates = false;
		_subscriberURI = null;
		_region = null;
		_terminateConfirmed = false;
		_key = null;
		_proxyReceivedFinalResponse = false;
		_noFinalResponseToReceivedInvite = false;
		_pendingReceivedInviteCseq = -1;
		_noFinalResponseToSentInvite = false;
		_pendingSentInviteCseq = -1;
		_lastOkResponse = null;
		if (_retransmitTimerPerCSeq != null) {
			_retransmitTimerPerCSeq.clear();
		}
		if (_proxyClientTransactions != null) {
			_proxyClientTransactions.clear();
		}
		_proxyHasOngoingReInvite = false;
		// the response code might be replaced by timeout event, so need to set the configured response code again
		_sessionInvalidatedResponse = 
					PropertiesStore.getInstance().getProperties().getInt(CoreProperties.WAS80_SESSION_INVALIDATE_RESPONSE);
		
		setSharedId(null);
	}

	/**
	 * Get SipSession associate with this Transaction User 
	 * @param create
	 * @return
	 */
	SipSession getSipSession(boolean create) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {  _tuWrapper.getAppName(), _tuWrapper.getId()};
			c_logger.traceEntry(this,"getSipSession", params);
		}     	
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this,"getSipSession",
			"SipSession form the BaseTU will be returned");
		}

		return _tuWrapper.getSipSessionFromBase(create);
	}    


	/**
	 * Function that creates the new request according to the method
	 * @param method
	 * @return
	 */
	public synchronized SipServletRequest createRequest(String method) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, "createRequest", params);
		}     	

		//    	This method is called from the SipSesisonImple
		if(method.equals(Request.ACK) || method.equals(Request.CANCEL))
		{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createRequest", 
				"Can not create request for ACK or CANCEL using this method");
			}

			throw new IllegalArgumentException("Can not create a " + method + 
			" request by this method.");
		}

		if(isProxying())
		{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createRequest", 
				"Can not create request for a session in proxying mode");
			}

			throw new IllegalStateException("Can not create request while proxying request " + this);
		}

		SipSession.State state = _tuWrapper.getState();
		if (isTermitedState(state) &&
				!(method.equals("BYE") && !_terminateConfirmed)//allow for BYEs when termination is not yet confirmed with a 200
		) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createRequest",
						"Session is TERMINATED, Create Request not allowed, "
						+ this);
			}
			throw new IllegalStateException("Session is TERMINATED: " + this);
		}

		// The PRACK and UPDATE request can be created even if the state of the SipSession
		// is EARLY as the reason of received 1xx reliable response
		if (!SipUtil.canSendOnDialog(method,getWrapper())) {
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer b = new StringBuffer(64);
				b.append("Can not create Request while Session is in : ");
				b.append(_tuWrapper.getState());
				b.append(" State, Session");
				b.append(this);
				c_logger.traceDebug(this, "createRequest", b.toString());
			}
			throw new IllegalStateException("Invalid Session State: " + this);
		}

		boolean initial = isInitialState(state);
		OutgoingSipServletRequest req =
			new OutgoingSipServletRequest(_tuWrapper, method, getLocalParty(),
					getRemoteParty(), initial);
		req.setIsSubsequentRequest(!initial);

		if (initial) {
			//in case that this is not the first message but it it initial e.g. re-invite after 401 response on the first invite
			//use the same request URI as the first request.
			if (!m_isServerTransaction && m_sipMessage != null) {
				req.setRequestURI(m_sipMessage.getRequestURI());
			}

			m_isServerTransaction = false;
			m_remoteTag = null;
		}
		else {
			//Subsequent message on the same dialog need to use Contact Information
			//and Route headers associated with the dialog
			if(null != m_remoteContact) {
				req.setRequestURI(m_remoteContact.getURI());
			}

			if(null != m_routeHeaders)
			{
				Iterator<String> iter = m_routeHeaders.iterator(); 
				while(iter.hasNext())
				{
					req.addHeader(RouteHeader.name, iter.next(), false);
				}
			}
		}
		return req;
	}

	/**
	 * Set sipProvider
	 * @param sipProvider
	 */
	void setProvider(SipProvider provider)
	{
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setProvider", "provider=" + provider);
		}
		m_sipProvider = provider;
	}

	/**
	 * Returns the CallId as a String from the CallId header
	 * @return
	 */
	public String getCallId() {
		return m_callId;
	}

	/**
	 * Method that is responsible to initiate the m_reliableProcessor variable
	 *
	 */
	private synchronized void initiateReliableResponsesProcessor(){
		if(m_reliableProcessor == null){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "initiateReliableResponsesProcessor","creating a new reliableProcessor. tu="+this);
			}

			m_reliableProcessor = new ReliableResponsesProcessor(_tuWrapper);
		}else{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "initiateReliableResponsesProcessor","reliableProcessor was already created before");
			}
		}
	}

	/**
	 * Return the next RSeq for the new reliable response
	 * @return
	 */
	long getNextRSegNumber() {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "getNextRSegNumber");
		}
		if(m_reliableProcessor == null){
			initiateReliableResponsesProcessor();
		}
		long result =  m_reliableProcessor.getNextRseg();
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "getNextRSegNumber", new Long(result));
		}
		return result;
	}

	/**
	 * Returns the m_localParty
	 */
	Address getLocalParty() {
		return m_localParty;
	}

	/**
	 * Returns the m_remoteParty
	 */
	Address getRemoteParty() {
		return m_remoteParty;
	}

	/**
	 * Invalidate the Transaction User and Remove it from the TransactionUserTable
	 *
	 */
	protected void invalidateTU() {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, "invalidateTU", params);
		}

		// the only way to get here for the second time is when previous run did a partial
		// invalidation. 
		_duringInvalidate = true;

		SipServletRequest originalRequest = m_sipMessage;

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "invalidateTU" , "before");
		}
		
		if(shouldTerminateUnderlyingTransactions(originalRequest)){
        	terminateUnderlyingTransaction(originalRequest);
        }         	

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "invalidateTU" , "after");
		}
		
		if (m_pendingCancelReq == null) {
			// cannot fully invalidate TU if there is a pending cancel request.
			_tuWrapper.logToContext(SipSessionSeqLog.INVALIDATED);

			if(_2xxRetransmitTimer != null){
				_2xxRetransmitTimer.cancel();
			}        	
			_tuWrapper.setSessionState(SipSessionImplementation.State.TERMINATED,originalRequest);
			m_sipMessage = null;
			_latestDestination = null;
		}
		
		// removing from replication to avoid cases which failover occurs during wait period.
		removeFromStorage();
		
		if (PerformanceMgr.getInstance() != null) {
			PerformanceMgr.getInstance().decrementNotReplicatedSipSessionsCounter();
		}
	}

	/**
	 * Return true if there are underlying transactions that should be removed 
	 * during the invalidation.
	 * 
	 * @param request
	 * @return
	 */
	private boolean shouldTerminateUnderlyingTransactions(SipServletRequest request){
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "shouldTerminateUnderlyingTransactions",
					" has onGoingReinvite = " + _proxyHasOngoingReInvite + 
					" isEarlyState(_tuWrapper.getState()) = " + isEarlyState(_tuWrapper.getState()) +
					" isInitialState(_tuWrapper.getState()) " + isInitialState(_tuWrapper.getState()) + 
					" _tuWrapper.isAfterInitial() = " + _tuWrapper.isAfterInitial() +
					" _proxyReceivedFinalResponse = " + _proxyReceivedFinalResponse +
					" isProxying = " +  isProxying() +
					" originalRequest.isCommitted() = " + (request != null ? request.isCommitted() : "request is null"));
		}
		
			
		if (request != null && request.getMethod().equals(Request.INVITE)
				&& !(request instanceof IncomingDeadSipRequest)) {

			if (isProxying()) { // Proxy mode
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "shouldTerminateUnderlyingTransactions", "Proxy mode.");
				}
				if (!request.isCommitted()) {
					// In case when the request is not completed (INVITE and re-INVITE) we
					// should close all underlying transactions.
					// We will do this only when flag is true because of the performance
					// degradation caused by new HashTable for all
					// outgoing client transactions in Proxy mode.
					if (!proxyHasFinalResponse() || _proxyHasOngoingReInvite) {
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this, "shouldTerminateUnderlyingTransactions",
									"We have no final response for proxy or request is not commited.");
						}
						return true;
					}
				}
			}
				
				// UAS mode
			else if (isServerTransaction()) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "shouldTerminateUnderlyingTransactions", "UAS mode.");
				}
				if (!request.isCommitted()) { // No final response was sent
												// for this transaction
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "shouldTerminateUnderlyingTransactions",
								"We have to close the incoming INVITE transaction");
					}
					return true;
				}
			}

			// UAC mode
			else {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "shouldTerminateUnderlyingTransactions", "UAC mode.");
				}
				if (isEarlyState(_tuWrapper.getState())) { // Only 1xx was received on outgoing INVITE
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "shouldTerminateUnderlyingTransactions",
								"We have no final response for outgoing INVITE request");
					}
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return TRUE when proxy has at least one final response which > 199;
	 * @return
	 */
	private boolean proxyHasFinalResponse() {
		int lastProxyBranchResponseStatus = _tuWrapper.getLastProxyResponseStatus();
		if(lastProxyBranchResponseStatus > 199){
			return true;
		}
		return false;
	}

	/**
	 * called upon session invalidation, for INVITE dialog in "early" state,
	 * to clean the underlying transaction.
	 * this is necessary so the transaction does not remain open forever.
	 * @param invite the INVITE request of the transaction to be terminated
	 */
	private void terminateUnderlyingTransaction(SipServletRequest invite) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "terminateUnderlyingTransaction", invite);
		}
		
		if(!_tuWrapper.hasOngoingTransactions()) {
			return;
		}
		
		_underlyingTransactionsBeingTerminated = true;

		try{
			SipTransaction transaction = ((SipServletMessageImpl)invite).getTransaction();
			if(transaction != null && transaction.isTerminated()){
				//in case another derived session dialog caused the termination of this transaction
				//and this session was left in the early state, there is no underline transaction
				//to terminate
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "terminateUnderlyingTransaction",
							"TU [" + toString() + "]: The transaction was already terminated. Possibly due to a" +
					"downstream forking creating a derived session");
				}
				return;
			}
			
			if(isProxying()){
				terminateUnderlyingProxyTransactions(invite);
			}
			// UAS mode
			else if (m_isServerTransaction) {
				terminateUndelyingServerTransactions(invite);
			}
			// UAC mode
			else {
				terminateUndelyingClientTransactions(invite);
			}
		}
		finally{
			_underlyingTransactionsBeingTerminated = false;

			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(this, "terminateUnderlyingTransaction");
			}
		}
	}


	/**
	 * Terminates related client transactions
	 * @param invite
	 */
	private void terminateUndelyingClientTransactions(SipServletRequest invite) {
		// TODO Auto-generated method stub
		// terminate the outbound INVITE with a CANCEL request
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "terminateUndelyingClientTransactions",
					"TU [" + toString() + "] terminating stale INVITE client transaction");
		}
		
		//in case that a cancel request was not sent by the application (or is not waiting to be sent) but the dialog is
		//terminated in early state the container is responsible to send cancel
		if (! m_cancelSent && m_pendingCancelReq == null){
			if(((SipServletRequestImpl)invite).getTransaction() != null){
				m_pendingCancelReq = invite.createCancel();
			}
			else{
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "terminateUndelyingClientTransactions",
							"This TU has no outgoing transaction. Request was not sent.");
				}
			}
		}

		if (m_pendingCancelReq != null && m_finalResponseStatus > 1) {
			sendPendingCancelRequest();
		}
	}
	/**
	 * Terminates underlying Server transactions
	 * @param invite
	 */
	private void terminateUndelyingServerTransactions(SipServletRequest invite) {
		// terminate the inbound INVITE with a 408 response
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "terminateUndelyingServerTransactions",
					"TU [" + toString() + "] terminating stale INVITE server transaction");
		}
		SipServletResponse message = invite.createResponse(_sessionInvalidatedResponse);

		try {
			message.send();
		} catch (IOException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "terminateUndelyingServerTransactions",
						"failed sending message to terminate the call", e);
			}
		}		
	}
	/**
	 * In the Proxy mode we should close the server and the client related transactsion.
	 * ServerTransaction = incoming INVITE.
	 * ClientTransaction = outgoing proxied INVITE.
	 * @param invite
	 */
	private void terminateUnderlyingProxyTransactions(SipServletRequest invite) {
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceEntry(this, "terminateUnderlyingProxyTransactions");
		}
		
		// remove related server transactions.
		IncomingSipServletRequest originalInvite = (IncomingSipServletRequest)invite;
		OutgoingSipServletResponse response = (OutgoingSipServletResponse)originalInvite.createResponse(SipServletResponse.SC_REQUEST_TIMEOUT);
		response.markAsProxyResponse();
		try {
			response.send();
		} catch (IOException e1) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "terminateUnderlyingProxyTransactions",
						"Send response - IllegalStateException... " + e1.toString());
			}
		}
			
		//Terminate Client Transactions:
		
		// To keep the current performance ability the _proxyClientTransactions exists only when flag is true 			
		if(_proxyReceivedFinalResponse){
			//We are in re-INVITE mode
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "terminateUnderlyingProxyTransactions",
					"This INVITE was already answered with a final response. Remove Client Transaction ");
			}
			
			if(_proxyClientTransactions != null && _proxyClientTransactions.size()>0){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "terminateUnderlyingProxyTransactions",
							"Remove open INVITE client transaction in Proxy");
				}
				for (Iterator<ClientTransaction> iterator = _proxyClientTransactions.values().iterator(); iterator.hasNext();) {		
					ClientTransaction transaction = (ClientTransaction) iterator.next();
					try {
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this, "terminateUnderlyingProxyTransactions",
									"Send CANCEL for transaction = " + transaction);
						}
						
						OutgoingSipServletRequest cancel =
					            (OutgoingSipServletRequest) transaction.getOriginalRequest().createCancel();
						cancel.send(transaction.getListener());
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this, "terminateUnderlyingProxyTransactions",
									"IllegalStateException... " + e.toString());
						}
					}
					TransactionTable.getInstance().removeTransaction(transaction);
				}
			}
		}
		else{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "terminateUnderlyingProxyTransactions",
						"This INVITE was not completed yet, cancel the Proxy.");
			}
			try {
				invite.getProxy().cancel();
			} catch (IllegalStateException e) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "terminateUnderlyingProxyTransactions",
							"IllegalStateException... " + e.toString());
				}
			} catch (TooManyHopsException e) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "terminateUnderlyingProxyTransactions",
							"TooManyHopsException... " + e.toString());
				}
			}
		}
		c_logger.traceExit(this, "terminateUnderlyingProxyTransactions");
	}

	/**
	 * Checks if we have a pending cancel that was not sent for an initial dialog (invite without provisional response) 
	 * @return true is pending cancel exists that wasn't sent.
	 */
	public boolean isPendingCancelExists() {
		return m_pendingCancelReq != null;
	}



	/**
	 * @return if this TU during invalidation
	 */
	public boolean isInvalidating(){ 
		return _duringInvalidate;
	}
	
	public boolean isUnderlyingTransactionsBeingTerminated(){
		return _underlyingTransactionsBeingTerminated;
	}

	/**
	 * Gets the Jain Sip Provider associated with this message.
	 */
	SipProvider getSipProvider() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getSipProvider", "provider=" + m_sipProvider);
		}
		return m_sipProvider;
	}


	/**
	 * Return the Jain Sip Contact Header. convenient for internal use.
	 * 
	 * @return The first contact header if more then one available.
	 */
	Address getContactHeader() {
		return m_remoteContact;
	}

	/**
	 * Returns the remoteTag;
	 * @return
	 */
	String getRemoteTag(){
		return m_remoteTag;
	}

	/**
	 * sets remoteTag_2 useful only in Proxy mode;
	 * @param tag The tag to set
	 * @param replaceExistingTU Will replace the existing TU in the repository with the same one, but with 
	 * a new dialog key. This should happen if the remote tag used to be null (before any provisional response)
	 * and the TU was stored with a null remote tag in its key. Now there is a new tag and key is recreated.
	 */
	void setDestinationTagInProxy(String tag, boolean replaceExistingTU){
		_destinationTagInProxy = tag;
		if (_destinationTagInProxy == null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setDestinationTagInProxy", "set empty string");
			}
			_destinationTagInProxy = "";
		}
		if (_addedToTUTable && replaceExistingTU) {
			// TU was already added with another key, we need to remove the old
			// reference before adding it again.
			SessionRepository.getInstance().removeTuWrapper(_key,false);
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setDestinationTagInProxy",
						"Removed  key = " + _key.toString() + " sessionId = " + _tuWrapper.getId());
			}

			TUKeyPool.finishToUseKey(_key);
			_key = null;
			_addedToTUTable = false;
			addToSessionsTable();
		}
	}

	/**
	 * Helpful method which used to add the current TU to the SipSessionsTable
	 *
	 */
	private void addToSessionsTable(){
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, "addToSessionsTable", params);
		}

		if(_addedToTUTable){
			if( c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "addToSessionsTable", 
						"This TU was already added to the table and Should be replaces.  Id = " +  
						_tuWrapper.getId());
			}
			return;
		}   	

		_addedToTUTable = true;
		TUKey key = null;
		if(_key != null){
			key = _key;
		}
		else{
			key = new TUKey();
		}
		if(isProxying()){
			if(m_isVirtualBranch){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "addToSessionsTable",
							" Adding as VirtualBranch");
				}
				// In case of virtual branch this TU should behave as UAS for the future
				// incoming requests. so the it should be inserted to the table as 
				// locslTU + remoteTag + callId, but in this case localTag is actually _destinationTagInProxy
				// since this TU is created as virtual branch from existing ProxyBranch TU.
				key.setup(_destinationTagInProxy, m_remoteTag, getCallId(), false);
			}
			else{
				key.setup(m_remoteTag, _destinationTagInProxy, getSharedIdForDS(), true);
			}
		}
		else{
			key.setup(m_localTag, m_remoteTag, getCallId(), false);
		}
		_key = key;

		SessionRepository.getInstance().put(key, _tuWrapper, true);

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "addToSessionsTable",
					" key = " + key.toString() + " sessionId = " + _tuWrapper.getId());
		}
	}

	/**
	 * Returns the remoteTag_2 - useful only in Proxy mode;
	 * @return
	 */
	void setRemoteTag(String tag){
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() , " Tag = " + tag};
			c_logger.traceEntry(this, "setRemoteTag", params);
		}

		TUKey key = null;
		if(!_addedToTUTable && (m_remoteTag == null || tag != null)){
			m_remoteTag = tag;
			addToSessionsTable();
		}

		if(_addedToTUTable == true && m_remoteTag == null && tag != null){
			// this is relevan only for case when this TU acts as UAC for
			// request which is initating the dialog. In this case request is
			// added to the all TU table in moment it sent so KEY should be changed 
			// when remote tag received.
			m_remoteTag = tag;
			if(!isProxying() && !isServerTransaction()){
				key = ThreadLocalStorage.getTUKey();
				key.setup(m_localTag, null, getCallId(), false);
				TransactionUserWrapper tuw = SessionRepository.getInstance().removeTuWrapper(key,false);
				if(tuw == null){
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "setRemoteTag",
								"Trying to update the key of a transaction user that is not found in the table, key = " + key.toString() + " sessionId = " + _tuWrapper.getId());
					}
					if (c_logger.isTraceEntryExitEnabled()) {
						c_logger.traceExit(this, "setRemoteTag");
					}
					return;
				}
				_key = null;
				_addedToTUTable = false;
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "setRemoteTag",
							"Removed  key = " + key.toString() + " sessionId = " + _tuWrapper.getId());
				}
				addToSessionsTable();
			}
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "setRemoteTag");
		}
	}


	/**
	 * Returns the _destinationTagInProxy - useful only in Proxy mode;
	 * @return
	 */
	String getDestinationTagInProxy(){
		return _destinationTagInProxy;
	}

	/**
	 * Returns the remoteTag;
	 * @return
	 */
	String getLocalTag(){
		return m_localTag;
	}


	/**
	 * Method that will verify if CSeq in received request is higher that previous one
	 * @param request
	 * @return
	 */
	private boolean checkCSeq(SipServletRequestImpl request, 
			boolean isAck, boolean isCancel)
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId(), isAck , isCancel};
			c_logger.traceEntry(this, "checkCSeq", params);
		}
		boolean isOk = false;
		if (isCancel) {
			isOk = checkCSeqACK_Cancel(request);
		}
		else if (isAck) {
			isOk = true;
		}
		else {
			long receivedRemoteCseq = request.getRequest()
			.getCSeqHeader().getSequenceNumber();
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer b = new StringBuffer();
				b.append(" Expected remote CSeq: ");
				b.append(m_remoteCseq);
				b.append(" Received CSeq: ");
				b.append(receivedRemoteCseq);
				c_logger.traceDebug(this, "checkCSeq", b.toString());
			}
			if (receivedRemoteCseq > m_remoteCseq) {
				m_remoteCseq = receivedRemoteCseq;
				//We decide not to replicate here, to improve performances. The remoteCSeq is used for 
				//validity confirmation, if the server drops, the worst that can happen
				//is that all client messages will regarded as valid. Its a tradeoff. 
				//                replicate();
				isOk = true;                
			}
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "checkCSeq", " Is OK: " + isOk);
			}
		}

		if (!isOk) {
			int responseCode;
			String reasonPhrase;
			if (isCancel) {
				responseCode = SipServletResponse.SC_CALL_LEG_DONE;
				reasonPhrase = null;
			}
			else {
				responseCode = SipServletResponse.SC_SERVER_INTERNAL_ERROR;
				reasonPhrase = INCORRECT_CSEQ_REASON;
			}
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "checkCSeq", 
						"lower CSeq received. return error [" + responseCode + "] latest cseq [" +
						m_remoteCseq + ']');
			}
			sendResponse(request, responseCode, reasonPhrase);
		}
		return isOk;
	}

	/**
	 * @see com.ibm.ws.sip.container.transaction.ServerTransactionListener#processRequest(javax.servlet.sip.SipServletRequest)
	 */
	void processRequest(SipServletRequest request) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId()};
			c_logger.traceEntry(this, "processRequest", params);
		}

		if (c_logger.isTraceDebugEnabled()) {
			StringBuffer b = new StringBuffer(request.getMethod());
			b.append(' ');
			b.append(request.getCallId());
			c_logger.traceDebug(this, "processRequest", b.toString());
		}
		boolean isJSR289;
		if (_tuWrapper.getSipServletDesc() == null && isProxying()){
			//this is a situation where the container is acting as a proxy without an application
			//this can happen if the application router did not find matching application but the 
			//request includes preset route headers, in this case we will stay on the routing path
			//but we do not need to call any application. because there is no real application we are
			//choosing the same rules as 289 application
			isJSR289 = true;
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "processRequest", "This request is for proxy without a local application");
			}
		}else{
			isJSR289 = _tuWrapper.getSipServletDesc().getSipApp().isJSR289Application();
		}

		if(isJSR289){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "processRequest", "checkTopRouteDestination");
			}
			// Check if the top route is our own listening point - if so
			// remove it as we either reached our destination (proxy mode) or
			// undesired behavior in the application which will cause to
			// get into a loop
			boolean isIncomingRequest = true;
			((SipServletRequestImpl) request).checkTopRouteDestination(isIncomingRequest);
		}

		_tuWrapper.logToContext(SipSessionSeqLog.PROCESS_REQ, request.getMethod(), request);

		SipApplicationSessionImpl appSession = _tuWrapper.getAppSessionForInternalUse();
		if(appSession != null){
			appSession.setLastAccessedTime();
		}

		if (!isProxying()) {
			processRequestUASMode((SipServletRequestImpl) request);
		}

		else{
			if (isCombinedMode()) {
				String toTag = ((SipServletRequestImpl) request).getRequest()
				.getToHeader().getTag();
				if (toTag != null && toTag.equals(m_localTag)) {
					// will compare toTag to identify if this is a request that
					// relates to response that was created by the application
					// on the Original request when the original request itself
					// was proxied to UAS
					processRequestUASMode((SipServletRequestImpl) request);
					return;
				}
			}
			if (request.getMethod().equals(Request.CANCEL)) {
				// Pass the CANCEL to the proxy for handling.
				cancelProxyOperation(request);
			} 
			else if (request.getMethod().equals(Request.ACK)
					&& !SipUtil.is2xxResponse(m_finalResponseStatus)) {

				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "processRequest", "ACK dropped, finalResponse = " + m_finalResponseStatus + " proxy final Response: " + _proxyReceivedFinalResponse);
				}            
				// JSR 116 Section 8.2.7
				// ACKs for non-2xx final responses are just dropped.
			}
			else {
				processSubsequentProxyRequest(request);
			}
		}

		// Anat: This part of code is never call as wasForwardedToApplication()
		// always returns true as no one calls to setForwardToApplication() 
		// which is the only method that can set the m_forwardToApplication to false.

		//        final int response = m_finalResponseStatus;
		//		if ((response == 401 || response == 403 || response == 407)
		////				&& request.getMethod().equals(Request.ACK))
		//			&& !wasForwardedToApplication())
		//		{
		//			// The initial INVITE request was not forwarded to the Application as
		//			// it was rejected by the security level.
		//			// We can remove this SipSession from the related SipApplicationSession
		//			// If none had a reference to the related SipApplicationSession
		//			// we will invalidate it as well.
		//			getWrapper().invalidateTU(true);
		//		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "processRequest");
		}
	}

	/**
	 * Method that check if the received PRACK request is OK and should be 
	 * forwarded to the Application
	 * @param request
	 */
	private boolean checkPrackRequest(SipServletRequestImpl request){
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId()};
			c_logger.traceEntry(this, "checkPrackRequest", params);
		}
		int error = SipServletResponse.SC_BAD_REQUEST;
		boolean isPrackOk = false;
		if (m_wasAnsweredReliable == true) {
			if (m_reliableProcessor != null){
				error = m_reliableProcessor.isLegalPrack(request);
				if(error == SipServletResponse.SC_OK){
					isPrackOk = true;
				}                    
			}
		}
		if(!isPrackOk){
			SipRouter.sendErrorResponse(request, error);
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "checkPrackRequest", new Boolean(isPrackOk));
		}
		return isPrackOk;
	}

	/**
	 * Method that handle all and  requests
	 * @param request
	 */
	private boolean checkReferRequest(SipServletRequestImpl request){
		boolean isReferOk = false;
		//rfc 3515: 
		//REFER request MUST contain exactly one Refer-To header field value.
		ListIterator iter = request.getHeaders(SipUtil.REFER_HEADER_NAME);
		if(iter.hasNext()){
			iter.next();
			if (!iter.hasNext()){
				isReferOk = true;
			}
		}

		if(!isReferOk){
			SipRouter.sendErrorResponse(request, SipServletResponse.SC_BAD_REQUEST);
		}
		return isReferOk;
	}


	/**
	 * Helper method that is used to continue handling UAS request after
	 * was decided that it should be forwarded to the Application
	 * @param request
	 * @param isAck
	 * @param isCancel
	 * @param isInvite
	 */
	private void handleUASRequest(SipServletRequestImpl request, 
			boolean isAck,
			boolean isCancel,
			boolean isInvite){

		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId()};
			c_logger.traceEntry(this, "handleUASRequest", params);
		}
		OutboundProcessor outboundProcessor = OutboundProcessor.instance();
		outboundProcessor.processRequest(request);

		//Save the contact information for messages that can establish
		//a dialog.
		if(SipUtil.isTargetRefreshRequest(request.getMethod())) {
			saveContactHeader(request);
		}

		boolean isAckOnErrorResponse = ((IncomingSipServletRequest)request).isAckOnErrorResponse();
		boolean isFirstACKForThis2xx= false; 

		if (isAck) { 
			// Check if ACK for the particular CSeq was not handled yet
			// The only first ACK will be processed, its retransmit timer will be closed and removed from _retransmitTimerPerCSeq map to avoid retransmission
			long receivedCSeq =	request.getRequest().getCSeqHeader().getSequenceNumber();
			if(_retransmitTimerPerCSeq != null && _retransmitTimerPerCSeq.containsKey(receivedCSeq)) {
				Invite2xxRetransmitTimer retransmitTimer = _retransmitTimerPerCSeq.remove(receivedCSeq); 
				retransmitTimer.cancel();
				
				//subsequent ACKs that might got received due to retransmissions done by this timer while the first ACK was in process, 				
				//and were waiting in queue, should not cause the re-invocation of the application.
				isFirstACKForThis2xx= true; 
			}
			if(_2xxRetransmitTimer != null){
				_2xxRetransmitTimer.cancel();
				_2xxRetransmitTimer = null;
				//subsequent ACKs that might got received due to retransmissions done by this timer while the first ACK was in process, 				//and were waiting in queue, should not cause the re-invocation of the application.
				//and were waiting in queue, should not cause the re-invocation of the application.
				isFirstACKForThis2xx= true; 
			}
			m_sipMessage = null; 
			_latestDestination = null;
		}


		//Applications are not notified of incoming ACKs for non-2xx final
		//responses to INVITE. These ACKs are needed for reliability of
		//final responses but are not usually of interest to applications.
		// there is no need to check the ACKed response error code here,
		// because the transaction does not pass up ACK to error responses.
		if (isAckOnErrorResponse || (isAck && !isFirstACKForThis2xx)){           
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "processRequestUASMode", 
						"Not passing request to application, isAck:" + isAck + 
						" , isOnErrorResponse: " + isAckOnErrorResponse +
						", isFirstACKForThis2xx: " + isFirstACKForThis2xx);
			}
		}
		else{
			c_router.invokeSipServlet(request, null, _tuWrapper.getSipServletDesc(), null);

		}

		if(isInvite) {
			//Keep the INVITE and reInvites message as it is needed for handling 
			//ACKs and CANCEL
			m_sipMessage = request; 
		}
	}
	/**
	 * Process a request for a session in UAS mode.
	 * 
	 * @param request
	 */
	private void processRequestUASMode(SipServletRequestImpl request) {

		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId()};
			c_logger.traceEntry(this, "processRequestUASMode", params);
		}
		boolean isAck = request.getMethod().equals(Request.ACK);
		boolean isCancel = false;
		boolean isInvite = false;
		boolean forwardToApplication = true;

		if(!isAck) {
			isCancel = request.getMethod().equals(Request.CANCEL);

			if(!isCancel) {
				isInvite = request.getMethod().equals(Request.INVITE);
			}
		}

		if(isInvite){
			if(_noFinalResponseToSentInvite) {
				SipRouter.sendErrorResponse(request, SipServletResponse.SC_REQUEST_PENDING, NO_FINAL_RESP_PREV_INVITE);
				return;
			}
			m_inviteCseq = 
				request.getRequest().getCSeqHeader().getSequenceNumber();
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "processRequestUASMode", 
						"Updated local Invite CSeq: " + m_inviteCseq);
			}
		}

		if (checkCSeq(request, isAck, isCancel)) {
			if (isInvite) {
				//Got another invite before sent a final response on the previous one.
				if (_noFinalResponseToReceivedInvite) {
					//According to RFC 3261, section 14.2
					SipRouter.sendErrorResponse(request, SipServletResponse.SC_SERVER_INTERNAL_ERROR, NO_FINAL_RESP_PREV_INVITE);
					return;
				}
				//There is no final response to this invite yet, save an indication to it, and its cseq.
				_noFinalResponseToReceivedInvite = true;
				_pendingReceivedInviteCseq = m_inviteCseq;
			}
			// Sessions in UAS mode will automatically change to EARLY state
			// as the 1xx is automatically generated by the Stack.
			//			if (isInitialState(_tuWrapper.getState())) {
			//				_tuWrapper.setSessionState(SipSessionImplementation.State.EARLY,request);
			//			}

			if (isCancel) {
				// Yes, cancel our original request
				boolean isCanceled = cancelOriginalRequest(request);

				//if we do not need to send bad cancel to the application we will set the forwardToApplication flag
				//according to the result of the cancel request
				if (! PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.FORWARD_BAD_CANCEL_TO_APP)){
					forwardToApplication = isCanceled;
				}
			}		
			else if(request.getMethod().equals(RequestImpl.REFER)){
				forwardToApplication = checkReferRequest(request);
			}			
			else if (request.getMethod().equals(RequestImpl.PRACK)) {
				// Anat - PRACK
				// If it is a PRACK request - we should ask the m_reliableProcessor
				// if it is a legal PRACK that should be passed to the application
				forwardToApplication = checkPrackRequest(request);
			}

			if(forwardToApplication){

				// If custom property "pmi.count.all.massages" is not on 
				// count PMI in the old way - means only non proxy inbound requests that are forwarded to the application are counted
				PerformanceMgr perfMgr = PerformanceMgr.getInstance();
				if (perfMgr != null && !PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.PMI_COUNT_ALL_MESSAGES)) {					
					perfMgr.updatePmiInRequest(request.getMethod(), _tuWrapper, null);
				}
				handleUASRequest(request, isAck, isCancel, isInvite);
			}
		}            
	}    

	/**
	 * Helper method that will clean the m_reliableProcessor after the final
	 * response will be sent
	 */
	void cleanReliableObject(){    	
		if (m_reliableProcessor != null){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "cleanReliableObject", "removing reliableProcessor");
			}
			m_reliableProcessor = null;
		}
	}

	/**
	 * Determines if the incoming ACK or CANCEL request matches the pending 
	 * INVITE transaction. 
	 * @param request either a ACK or CANCEL request. 
	 * @return
	 */
	private boolean checkCSeqACK_Cancel(SipServletRequestImpl request) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId()};
			c_logger.traceEntry(this, "checkCSeqACK_Cancel", params);
		}
		boolean rc = false;

		//Original request MUST be an INVITE
		long receivedCSeq = 
			request.getRequest().getCSeqHeader().getSequenceNumber();
		if(m_inviteCseq == receivedCSeq)
		{
			rc = true;
		}

		if (c_logger.isTraceDebugEnabled()) {
			StringBuffer b = new StringBuffer();
			b.append(" Expected CSeq (INVITE):  ");
			b.append(m_inviteCseq);
			b.append(" Received CSeq: ");
			b.append(receivedCSeq);
			b.append(" Is OK: ");
			b.append(rc);
			c_logger.traceDebug(this, "checkCSeqACK_Cancel", b.toString());
		}

		return rc;
	}

	/**
	 * Save the Contact information for messages that establish a dialog.
	 * The contact information can be updated during the dialog with 
	 * Target Refresh Request. 
	 * @param msg
	 */
	private void saveContactHeader(SipServletMessage msg) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId()};
			c_logger.traceEntry(this, "saveContactHeader", params);
		}

		Address contact = null; 
		try {
			contact = msg.getAddressHeader(ContactHeader.name);
			if(null != contact) {
				m_remoteContact = contact; 
			}
		}
		catch (ServletParseException e) {
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.exception", Situation.SITUATION_CREATE,
						null, e);
			}
		}
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "processRequest", 
					"Saved Contact: " + m_remoteContact);
		}
	}

	/**
	 * We are in proxying mode and we got a CANCEL request for the pending proxy
	 * operation. Pass the message to the State Full Proxy for handling.
	 * 
	 * @param request
	 *            MUST be a CANCEL request.
	 */
	private void cancelProxyOperation(SipServletRequest request) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId()};
			c_logger.traceEntry(this, "cancelProxyOperation", params);
		}
		try {
			// Save the reference to the Proxy because in the
			// send() method the m_sipMessage will be deleted - performance
			// changes
			StatefullProxy p = (StatefullProxy)m_sipMessage.getProxy(false);

			sendResponse(request,SipServletResponse.SC_OK,null);
			
			boolean cancelProxy = true;
			
//			if (c_logger.isTraceEntryExitEnabled()) {
//				c_logger.traceDebug("isInitialState(_tuWrapper.getState() = " + isInitialState(_tuWrapper.getState()) +
//					" isEarlyState(_tuWrapper.getState()) = " + isEarlyState(_tuWrapper.getState()));
//			}
//			
//			if( !(isInitialState(_tuWrapper.getState()) || isEarlyState(_tuWrapper.getState()))){
//				if (c_logger.isTraceDebugEnabled()) {
//					c_logger.traceDebug("cancelProxyOperation will not cancel Proxies since dialog is not in inital/early state = " + _tuWrapper.getState());
//				}
//				cancelProxy = false;
//			}	
			
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("Custom Property - clean.client.transaction.proxy is set - working according to it");
			}

			if (_proxyHasOngoingReInvite) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug("This is reINVITE. Cannot be canceled.");
				}
				cancelProxy = false;
			}
			
			
			if (cancelProxy) {
				// Pass the CANCEL request to the proxy.
				ArrayList<String> reasons = new ArrayList<String>();
				ListIterator headerIter = request.getHeaders("Reason");
				while (headerIter.hasNext()) {
					reasons.add(headerIter.next().toString());
				}
				p.cancel(reasons.toArray(new String[reasons.size()]));
			}

			
//			if(PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.WAS80_CLEAN_CLIENT_TRANSACION_PROXY)){
//				if (c_logger.isTraceDebugEnabled()) {
//					c_logger.traceDebug("Custom Property - clean.client.transaction.proxy is set - working according to it");
//				}
//				if(_proxyHasOngoingReInvite){
//					if (c_logger.isTraceDebugEnabled()) {
//						c_logger.traceDebug("This is reINVITE. Cannot be canceled.");
//					}
//					cancelProxy = false;
//				}
//			}
//			
//			// This is problematic fix which will prevent cancel the ProxyBranch in sequential mode after several
//			// Error responses received, but there is still outgoing active ProxyBranch exists.
//			// better to upgrade and use the WAS80_CLEAN_CLIENT_TRANSACION_PROXY flag.
//			else{
//				if(PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.WAS80_CANCEL_PROXY_ON_INITIAL_ONLY) &&
//					!(isInitialState(_tuWrapper.getState()) || isEarlyState(_tuWrapper.getState()))){
//					if (c_logger.isTraceDebugEnabled()) {
//						c_logger.traceDebug("cancelProxyOperation will not cancel Proxies since dialog is not in inital/early state = " + _tuWrapper.getState());
//					}
//					cancelProxy = false;
//				}
//			}
//			
//			if (cancelProxy) {
//			
//				//if we need to add the reason header.
//				boolean addReasonHeader= PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.WAS80_ADD_REASON_HEADER);
//				if(addReasonHeader) {
//					//Pass the CANCEL request to the proxy.
//					ArrayList<String> reasons = new ArrayList<String>();
//					ListIterator headerIter = request.getHeaders("Reason");
//					while (headerIter.hasNext()) {
//						reasons.add(headerIter.next().toString());
//					}
//					p.cancel(reasons.toArray(new String [reasons.size()]));
//				}
//				else {
//					p.cancel();
//				}
//			}

			//Pass the notification to the application only if we have an application to invoke, 
			//if the tu does not have a servletDesc than the sip container is acting as a proxy without
			//application. this can happen if the application router found no application to process the initial request but 
			//the initial request included pre defined route set.
			if (_tuWrapper.getSipServletDesc() != null){
				c_router.invokeSipServlet(request, null, _tuWrapper.getSipServletDesc(), null);
			}else{
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "cancelProxyOperation", "This request is for proxy without a local application");
				}
			}

		} catch (TooManyHopsException e) {
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.exception", Situation.SITUATION_CREATE,
						null, e);
			}
		} 
	}

	/**
	 * Helper method that creates and sends a response.
	 *  
	 * @param request            the request to send the response to
	 * @param responseCode       the response code
	 * @param responseCodePhrase the response reason
	 */
	private void sendResponse(SipServletRequest request,
			int responseCode,
			String responseCodePhrase){
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId()};
			c_logger.traceEntry(this, " sendResponse", params);
		}
		if(request.getMethod().equals(Request.ACK))
		{
			//Can not send a response for ACKs
			return;
		}

		SipServletResponse resp = null;
		try{
			if (responseCodePhrase == null) {
				resp = request.createResponse(responseCode);
			}
			else{
				resp = request.createResponse(responseCode,responseCodePhrase);
			}
			//According to RFC 3261, section 14.2
			if ((responseCodePhrase != null) && (responseCodePhrase.equals(NO_FINAL_RESP_PREV_INVITE))) {
				//If the meaning of the RFC is a real random number, use this commented line below.
				//If not - use a 'random' value of 10 seconds.
				//            	resp.addHeader(SipConstants.RETRY_AFTER, String.valueOf(new Random().nextInt(11)));
				resp.addHeader(SipConstants.RETRY_AFTER, RETRY_AFTER_SECONDS);
			}

			resp.send();
		}
		catch (IOException e) {
			if (c_logger.isErrorEnabled()) {
				Object[] args = { resp };
				c_logger.error("error.sending.response", Situation.SITUATION_REQUEST, args, e);
			}
		}    
	}

	/**
	 * Determines whether the session is in proxying mode.
	 * 
	 * @return
	 */
	boolean isProxying() {

		return m_isProxying;
	}


	/**
	 * Called when CANCEL is received when the session is in UAS mode. 
	 * @param request
	 */
	private boolean cancelOriginalRequest(SipServletRequest request) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " cancelOriginalRequest", params);
		}	
		//7.2.3
		//When a CANCEL is received for a request which has been passed to an
		//application, and the application has not responded yet or proxied the 
		//original request, the container responds to the original request with a 
		//487 (Request Terminated) and to the CANCEL with a 200 OK final response, 
		//and it notifies the application by passing it a SipServletRequest object
		//representing the CANCEL request. The application should not attempt to 
		//respond to a request after receiving a CANCEL for it. Neither should it 
		//respond to the CANCEL notification.

		SipTransaction transaction = m_sipMessage.getTransaction();

		//Identifies if the cancel was accepted or rejected by the container. cancel is rejected if the original 
		//Invite request is already confirmed
		boolean cancelAccepted = true;

		//Must synchronize to avoid a race condition with the application, See 
		//JSR 116 7.2.3 for more information 
		synchronized (transaction) {
			//Check if a response has not already been generated for this
			// transaction.
			if (!transaction.isTerminated()) {

				//The original message that will be cancelled. Need to keep
				//reference as it is changed to null when transaction terminates
				SipServletRequestImpl originalMsg = m_sipMessage;
				SipServletResponse response487 = null;

				try {
					//                	defect 362481
					//                	Try to create response on initial request. If the initial response is already
					//                	Commited (Application already created a final response)the IllegalStateException
					//                	will be thrown. In this case we will catch the exception and reject the
					//                	incoming CANCEL request with 481.
					response487 = originalMsg.createResponse(SipServletResponse.SC_REQUEST_TERMINATED);
				}
				catch(IllegalStateException e) {
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "cancelOriginalRequest", 
								"Exception thrown " + e +
								"Is intial INVITE confirmed "  + originalMsg.isCommitted());
					}
					sendResponse(request,SipServletResponse.SC_CALL_LEG_DONE,null);
				}

				//            	If the response 487 on the original INVITE request successfully created
				//            	Send this response and also send the 200 OK on the incoming CANCEL request.
				if(response487 != null ){
					sendResponse(request,SipServletResponse.SC_OK,null);              

					//Send the 487 response
					try {
						response487.send();
					}
					catch (IOException e) {
						if (c_logger.isErrorEnabled()) {
							Object[] args = { m_sipMessage };
							c_logger.error("error.sending.487.response",
									Situation.SITUATION_REQUEST, args, e);
						}
					}
				}

				//Either way mark transaction as terminated.
				originalMsg.getTransaction().markAsTerminated();
			}
			else {
				//Transaction already terminated - reject CANCEL
				cancelAccepted = false;
				sendResponse(request,SipServletResponse.SC_CALL_LEG_DONE,null);
			}
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "cancelOriginalRequest", cancelAccepted);
		}
		return cancelAccepted;
	}

	/**
	 * Send a response to the application
	 * 
	 * @param response
	 *            The response to be sent
	 * @param listener
	 *            The listener which tell when the application was invoked
	 */
	void sendResponseToApplication(SipServletResponse response,
			SipServletInvokerListener listener) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] args = { _tuWrapper.getAppName(),
					_tuWrapper.getId(),
					response.getReasonPhrase(),
					Integer.toString(response.getStatus()),
					response.getCallId()};
			c_logger.traceEntry(this, _tuWrapper.getAppName() + "sendResponseToApplication", args);
		}

		// If application already invalidated this TU (by calling invalidate())
		// Response should be not forwarded to this application after it.
		if(_duringInvalidate){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "sendResponseToApplication", 
				"This response will be not forwarded to the Application as it already invalidated");
			}
		}

		else{
			// Invoke the
			c_router.invokeSipServlet(null, response, _tuWrapper.getSipServletDesc(), listener);
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "sendResponseToApplication");
		}
	}

	/**
	 * Send a subsequent request to the application, when the application is
	 * proxying in Record Route and Supervised mode. The request will passed to
	 * the application which can modify its content before it is being proxied
	 * down stream.
	 * 
	 * @param request
	 *            The request to be sent
	 */
	void processSubsequentProxyRequest(SipServletRequest request) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId(),request.getMethod() };
			c_logger.traceEntry(this, "processSubsequentProxyRequest", params);
		}


		// Invoke the siplet - siplet may modify request prior to sending
		//Siplets are invoked on all subsequent requests/responses regardless
		//of the original supervised mode.
		c_router.invokeSipServlet(request, null, _tuWrapper.getSipServletDesc(), _tuWrapper);

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "processSubsequentProxyRequest");
		}
		if (request.getMethod().equals(Request.INVITE)) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "processSubsequentProxyRequest",
						"Set new m_sipMessage = " + m_sipMessage);
			}
			// Keep the INVITE and reInvites message as it is needed for
			// handling ACKs and CANCEL
			m_sipMessage = (SipServletRequestImpl) request;
			_proxyHasOngoingReInvite = true;
		}
		
		// extract the inbound interface in case we are in RR proxy mode and the request comes from the 
		// UAC side 
		// save the incoming message inbound interface so we can use it for subsequent requests
		String originalFromHeaderTag = _tuWrapper.getBranch().getOriginalRequest().getFrom().getParameter(AddressImpl.TAG);
		String fromHeaderTag = request.getFrom().getParameter(AddressImpl.TAG);;

		if(originalFromHeaderTag!= null && originalFromHeaderTag.equals(fromHeaderTag)) {
			InetSocketAddress address = extractReceivedOnInterface(request);

			if (address != null) {
				setOriginatorOutboundInterface(address); 
			}
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "processSubsequentProxyRequest");
		}
	}

	/**
	 * @see com.ibm.ws.sip.container.transaction.ServerTransactionListener#onSendingResponse(javax.servlet.sip.SipServletResponse)
	 */
	boolean onSendingResponse(SipServletResponse response) {

		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(),
					_tuWrapper.getId(),
					response.getReasonPhrase(),
					Integer.toString(response.getStatus()),
					response.getCallId()};
			c_logger.traceEntry(this, "onSendingResponse", params);
		}

		_tuWrapper.logToContext(SipSessionSeqLog.SENDING_RES, response.getStatus(),
				response);

		String method = response.getMethod();
		boolean isInvite = method.equals(Request.INVITE);
		int status = response.getStatus();

		if(_initialDialogMethod!= null && _initialDialogMethod.equals(response.getMethod())
				&& !is1xxResponse(status) 
				&& !SipUtil.is2xxResponse(status)){
			_isFailedResponseSent = true;
		}

		// Close the response and the request
		// if the container is sending a 2xx which means that the container processed 
		// the request so for BYE and NOTIFY request we can close the TU. if the server
		// returned a different response code (4xx for authentication errors) we need 
		// to ignore this request.
		if (SipUtil.is2xxResponse(status)){
			_tuWrapper.checkIfTerminateRequest(response.getRequest());
		}

		PerformanceMgr perfMgr = PerformanceMgr.getInstance();
		if (perfMgr != null) {
			// Update PMI for Out response
			perfMgr.updatePmiOutResponse(status, _tuWrapper);
		}

		//a flag indicating whether we're done after setting the timer
		boolean exitAfterUpdatingState = false;

		// if state is CONFIRMED - no necessary check the error code of the
		// response.
		// The dialog is already established and error code can't be < 200
		// This case is not interesting for PRACK as it can be sent only for
		// the first INITIAL request with no "To" tag
		if (isConfirmedState(_tuWrapper.getState())) {
			if (isInvite && !m_isProxying && SipUtil.is2xxResponse(status)) {
				// We still need to setup timer for retransmission of 2xx
				// in case it is a re-invite. 2xx response for INVITEs are
				// retransmitted till ACK is received.

				// Reset the ACK flag as we need the ACK to be passed to the
				// application
				setRetransmitTimerFor2xxResponse(response);
			}
			if ((isInvite) && (status >= 200)) {
				//If it's a final response, clear the indication for last provisional status.
				clearPendingInviteStatusOnFinalResponse(response);
			}
			//if we got here we need to update the state and go out
			exitAfterUpdatingState = true;
		}

		//Update the dialog state in any case, since we might 
		//need to update the dialog usage (for initial requests)
		updateSessionState(response);

		//if the state has been set to true, we should leave
		if (exitAfterUpdatingState){
			return true;
		}

		updateTags(response);

		//		 Only invite and subscribe create a dialog in case 2xx or 1xx
		// with TO Tag response
		// The responses that sent reliably - are added to the table and its
		// Routing Info save in the method "updateSessionWithReliableResponse"
		if (SipUtil.is2xxResponse(status)
				|| !((OutgoingSipServletResponse) response).isSendingReliably()
				&& SipUtil.isDialogInitialRequest(method)) {
			// It is a 2xx final response or 1xx Invite with a TO tag.
			// Keep the session for future transaction in the same dialog.
			addToTransactionUsersTable();

			// Save the routing info as it will be needed for future
			// requests
			// it will be saved only for 2xx
			saveRoutingInfo(response);

			// UAS only - not for proxies
			// 2xx response for INVITEs are retransmitted till ACK is
			// received.
			if (isInvite && !m_isProxying && SipUtil.is2xxResponse(status)) {
				setRetransmitTimerFor2xxResponse(response);
			}
		}

		if (isEqualToInitalRequestMethod(response)) {
			// Update the final response status - Relates to response
			// on initiated method. 
			if (m_finalResponseStatus < 200) {
				m_finalResponseStatus = status;
				setDirty();
			}
			if ((isInvite) && (status >= 200)) {
				//If it's a final response, clear the indication for last provisional status.
				clearPendingInviteStatusOnFinalResponse(response);
			}
		}

		if ((status == 401 || status == 403 || status == 407)
				&& !wasForwardedToApplication()
				&& !SipUtil.isDialogInitialRequest(response.getMethod())) {
			// The initial non-INVITE request was not forwarded to the Application as
			// it was rejected by the security level. We can remove this SipSession 
			// from the related SipApplicationSession. If none had a reference to the 
			// related SipApplicationSession we will invalidate it as well.
			getWrapper().invalidateTU(true,true);
		}

		
		replicateForInitialRequest(response);

		checkIfTUShouldBeReused();

		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] args = { "Sip Session on sending response",
					response.getReasonPhrase(),
					Integer.toString(response.getStatus()) };
			c_logger.traceExit(this, "onSendingResponse", args);
		}

		return true;
	}

	/**
	 * Clears the indication that a pending Invite awaits a final response. According to JSR289 11.2.4.2
	 * 
	 * @param response the received response
	 * 
	 * @see SipServletResponse
	 */
	private void clearPendingInviteStatusOnFinalResponse(SipServletResponse response) {

		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(),
					_tuWrapper.getId(),
					response.getReasonPhrase(),
					Integer.toString(response.getStatus()),
					response.getCallId()};
			c_logger.traceEntry(this, "clearPendingInviteStatusOnFinalResponse", params);
		}

		long responseCseq = ((SipServletRequestImpl)response.getRequest()).getRequest().getCSeqHeader().getSequenceNumber();

		if (c_logger.isTraceDebugEnabled()) {
			StringBuilder sb = new StringBuilder(500);
			sb.append("Response status=").append(response.getStatus());
			sb.append(" Response cseq=").append(responseCseq);
			sb.append(" _noFinalResponseToReceivedInvite=").append(_noFinalResponseToReceivedInvite);
			sb.append(" _pendingReceivedInviteCseq=").append(_pendingReceivedInviteCseq);
			sb.append(" _noFinalResponseToSentInvite=").append(_noFinalResponseToSentInvite);
			sb.append(" _pendingSentInviteCseq=").append(_pendingSentInviteCseq);
			c_logger.traceDebug(this, "clearPendingInviteStatusOnFinalResponse", sb.toString());
		}

		//Check if we got a final response to the last provisional one 
		if (responseCseq == _pendingReceivedInviteCseq || responseCseq == _pendingSentInviteCseq) {
			_noFinalResponseToReceivedInvite = false;
			_pendingReceivedInviteCseq = -1;
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "clearPendingInviteStatusOnFinalResponse", "clearing status");
			}
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] args = { "clearPendingInviteStatusOnFinalResponse",
					response.getReasonPhrase(),
					Integer.toString(response.getStatus()) };
			c_logger.traceExit(this, "clearPendingInviteStatusOnFinalResponse", args);
		}
	}
	/**
	 * Helper method which updates the tags according to the state of the 
	 * @param response
	 */
	private void updateTags(SipServletResponse response) {

		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " updateTags", params);
		}

		SipSessionImplementation.State state = _tuWrapper.getState();
		if (isConfirmedState(state) || isTermitedState(state)) {
			return;
		}

		int respStatus = response.getStatus();
		// check if this a 2xx final response or a 1xx for an Dialog initial request
		// with the from tag set. Either one will create a dialog.
		String toTag = ((AddressImpl) response.getTo()).getTag();

		if (SipUtil.is2xxResponse(respStatus)
				|| (is1xxResponse(respStatus)
						&& SipUtil.isDialogInitialRequest(response.getMethod()) && toTag != null)) {

			if(isProxying()){
				if(_destinationTagInProxy == null){
					_destinationTagInProxy = ((SipServletResponseImpl) response)
					.getResponse().getToHeader().getTag();
					if(_destinationTagInProxy == null) {
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this, "updateTags",	"Both destinationTag and To-tag are null");
						}
						_destinationTagInProxy = "";
					}
				}
			}
			
			// Process response that should create a dialog..
			else if (m_isServerTransaction) {
				// Update the local party with the header after we have the To
				// tag set.
				m_localParty = response.getTo();

				if(!isProxying()){
					//UAS mode
					m_localTag = ((SipServletResponseImpl) response).getResponse()
					.getToHeader().getTag();
				}

				setDirty();
			} else {
				// How is it possible to get a response for client transaction ?
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "onSendingResponse",
					"SipSession OnSending Response Error 1");
				}
			}

		}

	}


	/**
	 * @see com.ibm.ws.sip.container.failover.ReplicatableImpl#shouldBeReplicated()
	 */
	public boolean shouldBeReplicated(boolean forBootstrap){
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "shouldBeReplicated", _tuWrapper.getAppName() + _tuWrapper.getId());
		}

		if(_tuWrapper.getSipServletDesc() == null){
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(this, "shouldBeReplicated", "servlet desc is null");
			}
			return false;
		}

		SipAppDesc appDescriptor = _tuWrapper.getSipServletDesc().getSipApp();

		boolean result = 
			appDescriptor != null && 
			appDescriptor.isDistributed() && 
			(isConfirmedState(_tuWrapper.getState()) || _replicateAllStates);

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this,"shouldBeReplicated" ," result is:"+result
					+ " appDesc="+appDescriptor+ " dialogConfirmed?"+isConfirmedState(_tuWrapper.getState()));
		}


		if (result && _duringInvalidate) {
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceEntry(this,
						"this TU is during invalidation ID = "
						+ _tuWrapper.getId());
			}
			return false;
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "shouldBeReplicated", _tuWrapper.getId()+"="+  result);
		}

		return result;
	}
	/**
	 * Helper method that takes care of sending reliable responses
	 * 
	 * @param response
	 */
	void onSendingReliableProvisionalResponse(SipServletResponse response) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this,"onSendingReliableProvisionalResponse", response);
		}
		// If it is a reliableResponse we should forward it to the
		// m_reliableProcessor object and it will decide if it should be
		// send or will be send later.
		// The first reliableResponse will be always return with rc "true"
		if (m_reliableProcessor == null) {
			initiateReliableResponsesProcessor();
		}

		// Can throw exception - this is the reason that it called
		// before this session will be added to the SipTransactionUserTable
		m_reliableProcessor.sendResponse((SipServletResponseImpl) response);
		updateSessionWithReliableResponse(response);
		addToTransactionUsersTable();  

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this,"onSendingReliableProvisionalResponse");
		}
	}

	/**
	 * Helper method which is update the state in case of the provisional response
	 * @param response
	 */
	void updateSessionWithReliableResponse(SipServletResponse response){
		//      Reliable response SHOULD create a dialog RFC 3262
		// and it is interesting only for the first reliable response
		// If the flag m_wasAnsweredReliable == false this means that
		// the INVITE was not respond by the Application in this case 
		// during the first response the SipSession should be added to the
		// SipTransactionUserTable.
		if(m_wasAnsweredReliable == false){
			m_wasAnsweredReliable = true;
		}

		if (response.getStatus()>= 180 && response.getStatus() < 190){
			boolean isInitial = isInitialState(_tuWrapper.getState());
			
			if(response instanceof IncomingSipServletResponse){
				// Save the routing info as it will be needed for future requests
				// it will be saved for 18x reliable responses RFC 3262
				updateTargetAndRoutingInfo(response, isInitial);
				// Save remote tag for this dialog. Should be save only for 
				// client transactions because in server transaction 
				// remoteTag sets when message arrived.
				setRemoteTag(((SipServletResponseImpl) response).
						getResponse().getToHeader().getTag());
			}else{
				//for outgoing responses we only need to update the route set for subsequent requests like Update
				//but only for initial state dialogs
				if (isInitial){
					saveRoutingInfo(response);
				}
			}
		}
	}

	/**
	 * Update the SipSession object with information received in the Reliable Response
	 * @param response
	 */
	void updateWithProxyReliableResponse(SipServletResponse response) {
		//        In the Proxy mechanism - when reliable response is proxying - the 
		//        routing info should be saved in the session
		//        and session should be saved in the sessionTable

		if (m_reliableProcessor == null) {
			initiateReliableResponsesProcessor();
		}

		updateSessionWithReliableResponse(response);
	}

	/**
	 * Helper method that will be called when final response is going to be sent
	 * after provisional responses on thins session.
	 * @param response
	 */
	void onSendingFinalResponseAfterProvisional(SipServletResponse response){
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " onSendingFinalResponseAfterProvisional", params);
		}
		if(m_wasAnsweredReliable == true && m_reliableProcessor != null){
			m_reliableProcessor.sendFinalResponse((SipServletResponseImpl)response);
		}
		else{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "onSendingFinalResponseAfterProvisional",
						_tuWrapper.getAppName() + "No Provisional response was sent before");
			}
		}
	}

	/**
	 * Set the retransmission timer for 2xx responses to INVITE. Retransmission
	 * will be stopped when ACK is received timer reaches 64*T1. See 
	 * RFC 3261 Section 13.3.1.4.  
	 *  
	 * @param response The response to be retransmitted. 
	 */
	private void setRetransmitTimerFor2xxResponse(SipServletResponse response) {

		// If this is a first retransmission for 2xx response, the map is null, so we use a single timer object 
		if (_2xxRetransmitTimer == null) {
			_2xxRetransmitTimer = new Invite2xxRetransmitTimer((SipServletResponseImpl) response);
			SipContainerComponent.getTimerService().schedule(_2xxRetransmitTimer, false, SIPTransactionConstants.T1);
		}
		else {
			// meaning we already have a retransmit timer, so need to start using a map for the timers				
			if (_retransmitTimerPerCSeq == null) {
				_retransmitTimerPerCSeq = new HashMap<Long, Invite2xxRetransmitTimer>();
				_retransmitTimerPerCSeq.put(_2xxRetransmitTimer.getRetransmittedResponseCSeq(), _2xxRetransmitTimer);
				_2xxRetransmitTimer = null;
			}
			
			// create a timer for the new retransmitted response and add it to the map
			Invite2xxRetransmitTimer retransmitTimer = new Invite2xxRetransmitTimer((SipServletResponseImpl) response);       
			_retransmitTimerPerCSeq.put(retransmitTimer.getRetransmittedResponseCSeq(), retransmitTimer);                
		
			SipContainerComponent.getTimerService().schedule(retransmitTimer, false, SIPTransactionConstants.T1);
		}
	}

	/**
	 * Store INVITE transactions
	 * @param trnsaction
	 */
	public void storeClientTransaction(ClientTransaction transaction){
		
		if(!(isProxying() && transaction.getOriginalRequest().getMethod().equals(Request.INVITE))){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "storeClientTransaction", "We store only INVITE transactions");
			}
			return;
		}
		// meaning we already have a retransmit timer, so need to start using a map for the timers				
		if (_proxyClientTransactions == null) {
			_proxyClientTransactions = new HashMap<Long, ClientTransaction>();
			
		}
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "storeClientTransaction", "Transaction Id  = " + transaction.getTransactionID());
		}
		_proxyClientTransactions.put(transaction.getTransactionID(), transaction);
	}
	/**
	 * Remove the client transaction from the TUImpl when it is terminated
	 * @param transaction
	 */
	public void removeClientTransaction(ClientTransaction transaction){
		if (_proxyClientTransactions != null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "removeClientTransaction", "Transaction Id  = " + transaction.getTransactionID());
			}
			_proxyClientTransactions.remove(transaction.getTransactionID());			
		}
	}

	/**
	 * Save the routing info associated with dialog. Will be used in the 
	 * creation of future requests in this dialog. 
	 * 
	 * @param response
	 */
	private void saveRoutingInfo(SipServletResponse response) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " saveRoutingInfo", params);
		}
		if(isProxying())
		{
			//No need to keep the routing path. only for UAC and UAS. 
			return; 
		}

		try {

			//Keep the list of Record-Route headers as we will use as Route
			//headers in future requests. 
			ListIterator iter = response.getAddressHeaders(RecordRouteHeader.name);

			if(iter.hasNext()) {

				//For now use a vector as we need an object that implements
				//serializable. Need to consider the option of using an array
				//instead of a vector to optimize memory usage on replication.
				m_routeHeaders = new Vector<String>(3);
				Object rroute;  
				while(iter.hasNext())
				{
					rroute = iter.next();
					if(m_isServerTransaction) {
						m_routeHeaders.add(rroute.toString());
					}
					else {
						//Reverse the order of the list of record routes 
						m_routeHeaders.add(0, rroute.toString());
					}

					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "saveRoutingInfo", 
								"Added Route: " + rroute);
					}
				}
				setDirty();
			}
		}
		catch (ServletParseException e) {
			if(c_logger.isErrorEnabled())
			{
				c_logger.error("error.exception", Situation.SITUATION_CREATE, 
						null, e);
			}
		}
	}

	/**
	 * Update the target and the routing info associated with dialog. Will be used upon receiving responses to
	 * target refresh methods.  
	 * @param response
	 */
    private void updateTargetAndRoutingInfo(SipServletResponse resp, boolean updateRoutingInfo) {
    	if (c_logger.isTraceEntryExitEnabled()) {
 			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
 			c_logger.traceEntry(this, "updateTargetAndRoutingInfo", params);
 		}
    	if(SipUtil.isTargetRefreshRequest(resp.getMethod())) {
    		if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "updateTargetAndRoutingInfo", 
                		"Target Refresh Request");
            }
    		saveContactHeader(resp);
    		if (updateRoutingInfo) {
    			saveRoutingInfo((SipServletResponseImpl) resp);
    		}
    	}
    	 if(c_logger.isTraceEntryExitEnabled()){
             c_logger.traceExit(this, "updateTargetAndRoutingInfo");
         }       
    }


	/**
	 * Checks whether the specified response is in the 1xx range.
	 * 
	 * @param status
	 */
	private final boolean is1xxResponse(int status) {
		if (status >= 100 && status < 200) {
			return true;
		}
		return false;
	}


	/**
	 * Gets the Sip Servlet Request associated with this session.
	 * 
	 * @return
	 */
	SipServletMessage getSipServletRequest() {
		return m_sipMessage;
	}


	/**
	 * Add this sipSession to the SipSessionsTable. Always add according to 
	 * session id and not dialog id. 
	 */
	void addToTransactionUsersTable() {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " addToTransactionUsersTable", params);
		}

		//In stress and load situations we might final response after the session
		//was already expired as we do let transaction to complete regardless
		//of the session's state. So in that case if we add the TU to the table
		//we will have a memory LEAK as the expiration timer was already fired 
		//and the the TU will never be cleaned from the table. 
		if(isTermitedState(_tuWrapper.getState())|| _duringInvalidate) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "addToTransactionUsersTable", 
						"Will not add TUimpl to Sessions table because session is already expired" +  
						_tuWrapper.getId());
			}
			return;
		}    	
		//    	if(isProxying() && ! isRRProxy()){
		//    		if (c_logger.isTraceDebugEnabled()) {
		//                StringBuffer buff = new StringBuffer(100);
		//                buff.append(_tuWrapper.getAppName());
		//                buff.append("Session id = [");
		//                buff.append(_tuWrapper.getId());
		//                buff.append("] was proxying without RR... will be not added to the Sessions table");
		//                c_logger.traceDebug(this, "addToSipSessionTable", buff
		//                        .toString());
		//            }
		//    	}
		//    	else{
		//    		addToSessionsTable();
		//    	}
		addToSessionsTable();
	}

	/**
	 * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#processResponse(javax.servlet.sip.SipServletResponse)
	 */
	void processResponse(SipServletResponseImpl response) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId(),response.getStatus(),response.getMethod() };
			c_logger.traceEntry(this, " processResponse", params);
		}
		boolean shouldBeForwardedToApp = true;
		if(isInitialState(_tuWrapper.getState()) && SipUtil.isDialogInitialRequest(response.getMethod())){
			//        	Meaning that the response that received was not 2xx response
			SipTransactionUserTable.getInstance().removeTransactionUserForOutgoingRequest((SipServletRequestImpl)response.getRequest());
		}

		// Update PMI for In response
		// this will happen only if custom property "" is disabled
		// means that in responses are counted as before, only for non proxy responses
		PerformanceMgr perfMgr = PerformanceMgr.getInstance();
		if (perfMgr != null && !PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.PMI_COUNT_ALL_MESSAGES)) {
			perfMgr.updatePmiInResponse(response.getStatus(), _tuWrapper);
		}

		SipApplicationSessionImpl sipApp = _tuWrapper.getAppSessionForInternalUse();

		if(sipApp != null){
			sipApp.setLastAccessedTime();
		}

		_tuWrapper.logToContext(SipSessionSeqLog.PROCESS_RESP, response.getStatus(), response);

		boolean shouldInvalidate = (m_pendingCancelReq != null && _duringInvalidate);

		//Check if we have pending cancel request that has been waiting for
		//a provisional response before it can be sent.
		if(m_pendingCancelReq != null && is1xxResponse(response.getStatus())) {
			sendPendingCancelRequest();
		}
		
		// In case we got a final response to the original INVITE - we have no
		// further use in the pending CANCEL.
		m_pendingCancelReq = null;

		if( response.isReliableResponse() ){
			if(m_reliableProcessor == null){
				initiateReliableResponsesProcessor();
			}
			updateSessionWithReliableResponse(response);
			shouldBeForwardedToApp = m_reliableProcessor.processReliableResponse(response);

		}

		if(!response.getMethod().equals(Request.CANCEL)){
			m_finalResponseStatus = response.getStatus();
			
			//The UAC application is invoked for all incoming responses except
			//100 responses (and retransmissions). The servlet invoked is the
			//current handler for the SipSession to which the request
			//belongs (see section 10.2.6).
			if (shouldBeForwardedToApp == true && response.getStatus() > 100 ) {

				if(isEqualToInitalRequestMethod(response)){
					// Update the final response status - Relates to response
					// on initiated method.
//					m_finalResponseStatus = response.getStatus(); 
					setDirty();
				}

				if (_initialDialogMethod != null) {
					if(!SipUtil.isDialogInitialRequest(_initialDialogMethod)){
						// If this is not dialog initial request - reference to m_sipMessage can be removed.
						// Otherwise the m_sipMessage can be used for DerivedSessions.
						m_sipMessage = null;
					}
				}

				sendResponseToApplication(response, null);
				checkIfTUShouldBeReused();           
			}
		}
		
		

		if(!is1xxResponse(response.getStatus()) && _latestDestination != null && isEqualToInitalRequestMethod(response)){
			//If we got final response (for the initial request)- the UAC cannot send CANCEL after it and we can remove
			//the _latestDestination;
			if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("final response received, remove latest destination");
			}        	
			_latestDestination = null;
		}

		if (shouldInvalidate) {
			if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug(this, "processResponse", "Sent cancel on a partial invalidated sessiom, forcing invalidation.");
			}        	
			_tuWrapper.invalidateTU(true, true);
		}


		if(response.getRequest().getMethod().equals("BYE")){ 
			_terminateConfirmed = true;
			setDirty();
		}
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "processResponse", "try to clear _noFinalResponseToSentInvite");
		}
		if (response.getRequest().getMethod().equals("INVITE") && (response.getStatus() >= 200)) {
			long responseCseq = ((SipServletRequestImpl)response.getRequest()).getRequest().getCSeqHeader().getSequenceNumber();
			//Check if we got a final response to the last provisional one 
			if (responseCseq == _pendingSentInviteCseq) {
				_noFinalResponseToSentInvite = false;
				_pendingSentInviteCseq = -1;
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "processResponse", "clearing _noFinalResponseToSentInvite" + _noFinalResponseToSentInvite);
				}
			}
		}

		if(isDirty()){
			if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("TransactionUSerImpl.processResponse(): replicate (status change on cancel)");
			}
			store();
		}
	}

	/**
	 * Check if this TU should be reused. If it is not in the middle of transaction
	 * It will immediately reused.
	 */
	private void checkIfTUShouldBeReused() {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " checkIfTUShouldBeReused", params);
		}
		if (isConfirmedState(_tuWrapper.getState()) && !_tuWrapper.isTUDialog()) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "checkIfTUShouldBeReused","This TU should be reUsed as it is not a dialog " + this);
			}
			_tuWrapper.setShouldBeReused();
		}
	}

	/**
	 * Update the session's internal state according to the response. Will be
	 * called prior to dispatching the request to the listener of the
	 * transaction which could be either the session itself or a proxy.
	 * 
	 * @param response
	 */
	void updateSession(SipServletResponse response) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " updateSession", params);
		}
		String method = response.getMethod();
		if(method.equals(RequestImpl.PRACK)){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "updateSession", 
				"The response on PRACK should not change the session state");
			}
			return;
		}

		int status = response.getStatus();

		//If we are already in confirmed state then we no longer 
		//update the state according to responses that are being sent
		if (!isConfirmedState(_tuWrapper.getState())) {

			updateSessionState(response);

			//check if this a 2xx final response or a 1xx for an Invite with
			// the
			//from tag set. Either one will create a dialog.
			String toTag = ((AddressImpl) response.getTo()).getTag();

			if (SipUtil.is2xxResponse(status)
					|| (is1xxResponse(status)
							&& method.equals(Request.INVITE) && toTag != null)) {

				if (m_isServerTransaction) {
					//How is it possible to get a response for server
					// transaction ?
					if (c_logger.isTraceDebugEnabled()) {
						c_logger
						.traceDebug(this, "updateSession",
						"Error Processing Response for a Server Transaction");

					}
				}
				else {
					//Update the local/remote party with the header after we
					// have the To/From tags set.
					m_remoteParty = response.getTo();
					m_localParty = response.getFrom();
				}
				//Only invite and subscribe create a dialog
				if ((SipUtil.is2xxResponse(status) || (toTag != null))) {
					// Save the routing info as it will be needed for future
					// requests
					updateTargetAndRoutingInfo(response, true);
					//Save remote tag for this dialog
					setRemoteTag(((SipServletResponseImpl) response).
							getResponse().getToHeader().getTag());
				}
			}
		}
		else if(SipUtil.is2xxResponse(status)){
        	updateTargetAndRoutingInfo(response, false);
		}
		//Need to update the dialog usage for some error codes
        else if (SipUtil.isUsageTerminatingResponse(status,response)) {
			_tuWrapper.setSessionState(SipSessionImplementation.State.TERMINATED, response);
		}

		setDirty();
		replicateForInitialRequest(response);
	}

	/**
	 * Perform replication only in case of a response to an initial request
	 * 
	 * @param response
	 */
	private void replicateForInitialRequest( SipServletResponse response){
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " replicateForInitialRequest", params);
		}
		if( SipUtil.isDialogInitialRequest(response.getMethod())) {
			store();
		}
	}


	/**
	 * @see com.ibm.ws.sip.container.transaction.ServerTransactionListener#onTransactionCompleted()
	 */
	void onTransactionCompleted(){
		if(isConfirmedState(_tuWrapper.getState())){

			if(m_sipMessage != null && 
					!m_sipMessage.getMethod().equals((Request.INVITE)))
			{
				m_sipMessage = null;
			}
		}
	}

	/**
	 * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#processTimeout(javax.servlet.sip.SipServletRequest)
	 */
	void processTimeout(SipServletRequestImpl req) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " processTimeout", params);
		}
		if( isInitialState(_tuWrapper.getState()) && SipUtil.isDialogInitialRequest(req.getMethod())){
			//        	If the request that was timedOut is on the dialog that is in initial 
			//        	state it should be removed from transactionUsers table.
			SipTransactionUserTable.getInstance().removeTransactionUserForOutgoingRequest(req);
		}

		req.setIsCommited(true);

		_tuWrapper.logToContext(SipSessionSeqLog.PROCESS_TIMEOUT, req.getCallId(), req);

		IncomingSipServletResponse response = null;
		try {
			response = SipUtil.createResponse(SipServletResponse.SC_REQUEST_TIMEOUT, req);
			updateSession(response);
			sendResponseToApplication(response, null);
		}
		catch (IllegalArgumentException e) {
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.failed.create.timeout.response",
						Situation.SITUATION_CREATE, null, e);
			}
		}
		catch (SipParseException e) {
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.failed.create.timeout.response",
						Situation.SITUATION_CREATE, null, e);
			}
		}
	}

	/**
	 * Generate 500 response upstream for appilcation router exception. 
	 * @param req
	 */
	void generateCompositionErrorResponse(SipServletRequestImpl req) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " generateCompositionErrorResponse", params);
		}
		if( isInitialState(_tuWrapper.getState()) && SipUtil.isDialogInitialRequest(req.getMethod())){
			SipTransactionUserTable.getInstance().removeTransactionUserForOutgoingRequest(req);
		}

		req.setIsCommited(true);

		_tuWrapper.logToContext(SipSessionSeqLog.SENDING_RES, req.getCallId(), req);

		Request jainReq = req.getRequest();
		MessageFactory msgFactory = StackProperties.getInstance()
		.getMessageFactory();
		try {
			//Create a dummy Jain SIP Response
			Response jainRes = msgFactory
			.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR,
					jainReq);

			//Wrap the Jain Response in Sip Servlet Response
			IncomingSipServletResponse response = new IncomingSipServletResponse(
					jainRes, req.getTransactionId(), req.getSipProvider());

			response.setRequest(req);
			response.setTransactionUser(req.getTransactionUser());
			updateSession(response);

			sendResponseToApplication(response, null);
		}
		catch (IllegalArgumentException e) {
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.failed.create.500.response",
						Situation.SITUATION_CREATE, null, e);
			}
		}
		catch (SipParseException e) {
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.failed.create.500.response",
						Situation.SITUATION_CREATE, null, e);
			}
		}
	}


	/**
	 * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#onSendingRequest(javax.servlet.sip.SipServletRequest)
	 */
	boolean onSendingRequest(SipServletRequestImpl request) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " onSendingRequest", params);
		}
		_tuWrapper.logToContext(SipSessionSeqLog.SENDING_REQ, 
				request.getMethod(), request);

		PerformanceMgr perfMgr = PerformanceMgr.getInstance();
		if (perfMgr != null) {
			// Update PMI for Out requests
			perfMgr.updatePmiOutRequest(request.getMethod(), _tuWrapper);
		}
		//		This method called only when TU acts as an UAC - so we can't get here
		//      when _hasDerivedSessions = true (which happen only in Proxy state)
		SipSessionImplementation sipSession = _tuWrapper.getSipSessionForInternalUse();
		SipApplicationSessionImpl sipAppSession = _tuWrapper.getAppSessionForInternalUse();
		if(sipSession != null){
			sipSession.setLastAccessedTime();
		}

		if(sipAppSession != null){
			sipAppSession.setLastAccessedTime();
		}

		boolean rc = true;
		if (request instanceof OutgoingSipServletAckRequest) {
			//Keep the ACK message around for replying with the same ACK
			//for retransmission of 2xx.
			if (m_AckFor2xxRef != null && m_AckFor2xxRef != request) {
				// sending ACK for a re-INVITE
				if (c_logger.isTraceDebugEnabled()) {
					String oldCSeq = m_AckFor2xxRef.getHeader("CSeq");
					String newCSeq = request.getHeader("CSeq");
					c_logger.traceDebug(this, "onSendingRequest",
							"replacing cached ACK [" + oldCSeq
							+ "] with [" + newCSeq + "] in TU [" + this + ']');
				}
			}
			m_AckFor2xxRef = (OutgoingSipServletAckRequest)request;
		}
		else if(request.getMethod().equals(RequestImpl.PRACK)){
			if(m_reliableProcessor == null){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "onSendingRequest", 
					"Cannot send the PRACk no reliable provisional response was received");
				}
				throw new 
				IllegalStateException("PRACK request should be sent only after the reliable response received "+ 
						this); 
			}
			m_reliableProcessor.checkPrack(request);
		}
		else if (_tuWrapper.isAfterInitial() && 
				request.getMethod().equals(Request.CANCEL))
		{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "onSendingRequest", 
				"Pending CANCEL request till a 1xx response is received");
			}
			m_pendingCancelReq = request;
			rc = false;
		}
		else{
			_tuWrapper.checkIfTerminateRequest(request);
		}

		if( isInitialState(_tuWrapper.getState()) && rc == true && SipUtil.isDialogInitialRequest(request.getMethod())){
			//        	Add this request to the SipTransactionUsersTable.
			//        	Will prevent race condition when received subsequent reqeust before
			//        	response on dialog that trying to be created. 
			//        	E.g.NOTIFY received before 200OK on	the dialog.
			_tuWrapper.setStateToAfterInitial();
			if(_initialDialogMethod == null){
				_initialDialogMethod = request.getMethod();
				if (c_logger.isTraceDebugEnabled()) {
					StringBuffer buff = new StringBuffer();
					buff.append("TU ID = ");
					buff.append(_tuWrapper.getId());
					buff.append(" Set new initial request Method = ");
					buff.append(_initialDialogMethod);
					c_logger.traceDebug(this, "onSendingRequest", buff.toString());
				}
			}
			addToTransactionUsersTable();
		}

		//According to JSR116 If the container receives an INVITE on a dialog while an INVITE 
		//it had sent on that dialog is still in progress, it MUST send a 491 (Request Pending) 
		// response to the received INVITE. 
		if(request.getMethod().equals(Request.INVITE) && !m_isProxying) {
			_noFinalResponseToSentInvite = true;
			_pendingSentInviteCseq = request.getRequest().getCSeqHeader().getSequenceNumber();
		}

		//if we are going to send out a cancel request we need to set the flag cancelSent flag
		if (rc == true && request.getMethod().equals(Request.CANCEL)){
			m_cancelSent = true;
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, " onSendingRequest", rc);
		}
		return rc; 
	}

	/**
	 * Send a pending CANCEL request that has been pended till a provisional 
	 * response is received. 
	 */
	private void sendPendingCancelRequest() {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " sendPendingCancelRequest", params);
		}
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "sendPendingCancelRequest", "Call Id: "
					+ m_pendingCancelReq.getCallId());
		}

		try {
			m_pendingCancelReq.send();
		} catch (IOException e) {
			logException(e);
		}
		m_pendingCancelReq = null;
	}

	/**
	 * Helper function for logging exceptions
	 * @param e
	 */
	private void logException(Exception e) {
		if (c_logger.isErrorEnabled()) {
			c_logger.error("error.exception", Situation.SITUATION_CREATE,
					null, e);
		}
	}

	/**
	 * Helper method that will verify if the received SipServletMessage
	 * has the same METHOD as initial dialog request.
	 * @param message
	 * @return
	 */
	private boolean isEqualToInitalRequestMethod(SipServletMessage message){
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " isEqualToInitalRequestMethod", params);
		}

		boolean isEqual = false;
		if (_initialDialogMethod != null) {
			if (message.getMethod().equals(_initialDialogMethod)) {
				isEqual = true;
			}
			else {
				if (c_logger.isTraceDebugEnabled()) {
					StringBuffer b = new StringBuffer(100);
					b.append("Different method in received response = ");
					b.append(message.getMethod());
					b.append(" Initial dialog method = ");
					b.append(_initialDialogMethod);
					c_logger.traceDebug(this, "isEqualToInitalRequestMethod", b
							.toString());
				}
			}
		} 
		else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "isEqualToInitalRequestMethod",
						"_initialDialogMethod is null. Is dialog ? " + 
						_tuWrapper.isTUDialog());
			}	
		}	

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "isEqualToInitalRequestMethod",
					isEqual ? "true" : "false");
		}	
		return isEqual;		
	}

	/** Update the session's state according to the response code. See JSR 116
	 * Section 10.2.1
	 * 
	 * @param response
	 */
	private void updateSessionState(SipServletResponse response) {
		//synchronization of this method was removed since all callers are in sync already

		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " updateSessionState", params);
		}
		SipSessionImplementation.State prevState = _tuWrapper.getState();

		// check if this response is relevant for this dialog, 
		// is the same method as initial method
		if (SipUtil.isDialogInitialRequest(response.getMethod())) { 
			// Galina: commented out b/c it caused a dialog termination when INFO inside the dialog is failed
			// there were many other cases where a dialog was terminated unexpectedly (defect 617266, 609083.5)
			// || SipUtil.isErrorResponse(response.getStatus())) {

			SipSessionImplementation.State newState = _tuWrapper.updateState(response);

			if(_tuWrapper.getState() == SipSessionImplementation.State.INITIAL
					&& prevState != newState){
				_initialDialogMethod = null;
				if (c_logger.isTraceDebugEnabled()) {
					StringBuffer buff = new StringBuffer();
					buff.append("TU ID = ");
					buff.append(_tuWrapper.getId());
					buff.append(" Remove initial request Method");
					c_logger.traceDebug(this, "updateSessionState", buff.toString());
				}

				// remove the remote tag, but don't touch the original message,
				// because the stack might need this tag for transaction
				// matching
				AddressImpl newRemoteParty = (AddressImpl) ((AddressImpl)m_remoteParty)
				.clone(true);
				newRemoteParty.removeTag();
				m_remoteParty = newRemoteParty;
			}
			if (newState != prevState) {
				if (c_logger.isTraceDebugEnabled()) {
					StringBuffer b = new StringBuffer(64);
					b.append("Previous State: ");
					b.append(prevState);
					b.append(" ,New State: ");
					b.append(newState + " Session: " + this);
					c_logger.traceDebug(this, "updateSessionState", b.toString());
				}
			}
		}   	
	}


	/**
	 * Gets the Transaction User Identifier which is actually the dialog
	 * identifier which is made up of the from tag, to tag and call Id
	 * associated with this session.
	 * 
	 * @return The Session Id
	 */
	SessionId getDialogId() {
		SessionId sessionId = null;
		String localTag = ((AddressImpl) m_localParty).getTag();
		String remoteTag = ((AddressImpl) m_remoteParty).getTag();

		// 3261-12.1.1
		// A UAS MUST be prepared to receive a
		// request without a tag in the From field, in which case the tag is
		// considered to have a value of null.
		// 3261-12.1.2
		// A UAC MUST be prepared to receive a response without a tag in the To
		// field, in which case the tag is considered to have a value of null.

		if (null == localTag)
			localTag = "";
		if (null == remoteTag)
			remoteTag = "";

		if (c_logger.isTraceDebugEnabled()) {
			StringBuffer b = new StringBuffer(64);
			b.append("Local tag: ");
			b.append(localTag);
			b.append(" , Remote tag: ");
			b.append(remoteTag);
			c_logger.traceDebug(this, "getSessionId", b.toString());
		}

		sessionId = new SessionId(getCallId(), localTag, remoteTag);

		return sessionId;
	}

	/**
	 * Indicates whether this session has been initially associated with a server
	 * or client transaction.
	 * 
	 * @return true for server transaction, false for client.
	 */
	boolean isServerTransaction() {
		return m_isServerTransaction;
	}

	/**
	 * Gets the next CSeq Number.
	 */
	long getNextCSeqNumber() {
		m_localCSeq++;
		return m_localCSeq;
	}

	/**
	 * Sets the base cSeq number that will be used instead of the default value
	 * of 1. Required for proxy operation that need to follow up using the same
	 * CSeq number
	 * 
	 * @param l
	 */
	void setcSeq(long l) {
		m_localCSeq = l;
		setDirty();

		//replicate the cseq change when the cseq is changed
		store();
	}

	/**
	 * Generated the from tag that is used for client transaction initiated by
	 * applications running on the container. We need to generate it once per
	 * dialog and keep it, in case we have multiple requests generated before
	 * the dialog is established. We need all of these requests use the same
	 * from tag.
	 * 
	 * @return
	 */
	synchronized String generateLocalTag() {
		if (null == m_localTag) {
			m_localTag = generateTag();

			if(m_isProxying){
				//            	Application has sent response after the original request had proxied
				setCombinedMode(true);
			}
		}

		return m_localTag;
	}

	/**
	 * Generate the local tag which encapsulates the session identifier. 
	 * @return 
	 */
	private String generateTag() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(Math.random());
		buffer.append(SESSION_ID_TAG_SEPARATOR);
		buffer.append(_tuWrapper.getId());

		//trim the 0. from the tag
		return buffer.substring(2);
	}

	/**
	 * Sets the session to be in Proxy mode. Which means that subsequent CANCEL
	 * request go through this session to the proxy and also subsequent requests
	 * when the proxy is in Record Route mode.
	 * 
	 * @param isProxying
	 */
	void setIsProxying(boolean isProxying) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " setIsProxying", params);
		}
		if(isProxying != m_isProxying)
		{
			_tuWrapper.logToContext(SipSessionSeqLog.IS_PROXY, isProxying);
		}
		if(m_localTag != null && isProxying == true){
			//        	Application had sent response before proxy the initial request
			//        	to UAS
			setCombinedMode(true);
		}
		m_isProxying = isProxying;
		
		store();
	}

	/**
	 * Sets whether the proxy associated with this session is in Record Route
	 * mod.
	 * 
	 * @param isRR
	 */
	void setIsRRProxying(boolean isRRProxy) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " setIsRRProxying", params);
		}
		m_isRRProxy = isRRProxy;
		if (m_isRRProxy) {
			// save the incoming message inbound interface so we can use it for subsequent requests
			// this method can be called multiple times
			// we need to extract the inbound interface only for an initial request 
			if (_originatorPreferedOutBoundIface == null ||
					!_originatorPreferedOutBoundIface.isSet()) {
				InetSocketAddress address = extractReceivedOnInterface(m_sipMessage);

				if (address != null) {
					setOriginatorOutboundInterface(address); 
				}
			}

		}
		setDirty();
		store();
	}
	
	private void setOriginatorOutboundInterface(InetSocketAddress address) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {  _tuWrapper.getAppName(), _tuWrapper.getId()};
			c_logger.traceEntry(this,"setOriginatorOutboundInterface", params);
		}

		_originatorPreferedOutBoundIface = getOutboundInterface(address);

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "setOriginatorOutboundInterface: " + _originatorPreferedOutBoundIface);
		}
	}

	private InetSocketAddress extractReceivedOnInterface(SipServletRequest sipRequest) {
		InetSocketAddress receivedIf = null;
		SipURI uri = SipProxyInfo.getInstance().extractReceivedOnInterface(sipRequest);

		if (uri != null) {
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer buff = new StringBuffer();
				buff.append("Extracted Received On Interface: ");
				buff.append("host = " + uri.getHost());
				buff.append(" port = " + uri.getPort());
				c_logger.traceDebug(this, "extractReceivedOnInterface", buff.toString());
			}

			receivedIf = InetAddressCache.getInetSocketAddress(uri.getHost(), uri.getPort());
		}
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "extractReceivedOnInterface", "" + receivedIf);
		}
		return receivedIf;
	}
	/**
	 * Sets whether the proxy associated with this session is in Record Route
	 * mode.
	 * 
	 * @param response
	 */
	void setIsVirtualBranch(Response response) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " setIsVirtualBranch", params);
		}
		// we don't need to replicate this flag. We need this flag only to add the TU to the 
		// sessions table in addToSessionsTable() method.
		// After this TU was added, it is acting as regualar UAS for incoming requests.
		
		m_isVirtualBranch = true;	
		
		// Replace the remote tag to one that is related to this TU
		setDestinationTagInProxy(response.getToHeader().getTag(), false);

	}
	

	/**
	 * Return if this TU is a virtual branch
	 * @return
	 */
	public boolean isVirtualBranch() {
		return m_isVirtualBranch;
	}

	/**
	 * Determines whether the session is in Record Route proxying mode.
	 * 
	 * @return
	 */
	boolean isRRProxy() {

		return m_isRRProxy;
	}

	/**
	 * @see com.ibm.ws.sip.container.router.SipServletInvokerListener#
	 *      servletInvoked(javax.servlet.sip.SipServletResponse)
	 */
	void servletInvoked(SipServletResponse response) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(),
					_tuWrapper.getId(),
					response.getMethod(),
					response.getReasonPhrase() };
			c_logger.traceEntry(this, "servletInvoked", params);
		}

		//We are only expected to get here when we are in proxying mode which
		//means Record-Route and Supervised mode.

		SipServletResponseImpl respImpl = (SipServletResponseImpl) response;

		updateTags(response);

		//save 200 OK resoponse from servlet for later retransmissions, will use this 
		// response and not let the retransmission go up to the application again
		if(response.getMethod().equals(Request.INVITE) && !response.getRequest().isInitial() && SipUtil.is2xxResponse(response.getStatus())) {
			_lastOkResponse = (SipServletResponseImpl) response;
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this,
						"servletInvoked",
				"Saving _lastOkResponse");
			}
		}

		if(respImpl.getTransactionId() == -1)
		{
			//Send the response directly to the transport layer as it does not 
			//belong to any transaction. We should only get for 2xx responses
			//for invites which could be either retransmission or multiple 
			//responses received for a single INVITE request due to forking
			//proxy down stream.
			SipRouter.sendResponseDirectlyToTransport(respImpl.getSipProvider(),
					respImpl.getResponse(),true);
		}
		else
		{
			sendResponseUpStream(response);
		}
	}

	/**
	 * @see com.ibm.ws.sip.container.router.SipServletInvokerListener#
	 *      servletInvoked(javax.servlet.sip.SipServletRequest)
	 */
	void servletInvoked(SipServletRequest request) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(),
					_tuWrapper.getId(),
					request.getMethod(), 
					request.getFrom(),
					request.getTo() };
			c_logger.traceEntry(this, "servletInvoked", params);
		}

		if (!request.isCommitted()) {
			RecordRouteProxy.proxyRequest(request, _tuWrapper);
		}
		else {
			//Applications are allowed to generate their own final responses
			//What should the Session's state be now, UAS ? Proxy ?
			if (c_logger.isTraceDebugEnabled()) {
				c_logger
				.traceDebug(this,
						"servletInvoked",
						"Session In RR Proxy mode but application"
						+ " generated final response for a new request in dialog");
			}
		}
	}

	/**
	 * Sends the specified response up stream.
	 * 
	 * @param response
	 */
	private void sendResponseUpStream(SipServletResponse response) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId(),response.getMethod() };
			c_logger.traceEntry(this, "sendResponseUpStream", params);
		}

		try {
			response.send();
		}
		catch (IOException e) {
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.exception", Situation.SITUATION_REQUEST,
						null, e);
			}
		}
	}

	/**
	 * Send a subsequent response to the application, when the application is
	 * proxying in Record Route and Supervised mode. The response will passed to
	 * the application which can modify its content before it is being proxied
	 * up stream.
	 * 
	 * @param response
	 *            The response to be sent
	 */
	void processSubsequentProxyResponse(SipServletResponse response) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId(), response.getMethod() };
			c_logger.traceEntry(this, "processSubsequentProxyResponse", params);
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntryExit(this,"setProxyReceivprocessSubsequentProxyResponseedFinalResponse", 
					"Response status = " + response.getStatus() + 
					" Proxy has open reINVITE ? = " + _proxyHasOngoingReInvite);
		}
		
		if(_proxyHasOngoingReInvite && response.getStatus() > 199 && response.getMethod().equals(Request.INVITE)){
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceEntryExit(this,"setProxyReceivprocessSubsequentProxyResponseedFinalResponse", 
						"ReInvite transaction terminated");
			}
			_proxyHasOngoingReInvite = false;
		}
			
		//Invoke the siplet - siplet may modify request prior to sending
		//Siplets are invoked on all subsequent requests/responses regardless
		//of the original supervised mode.
		c_router.invokeSipServlet(null, response, _tuWrapper.getSipServletDesc(), _tuWrapper);

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this,"processSubsequentProxyResponse");
		}
	}

	/**
	 * We got retransmission of response that can be only 2xx response on INVITE 
	 * or retransmission of reliable response 
	 * @param response
	 * @param provider
	 */
	void processStrayResponse(Response response, SipProvider provider){
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " processStrayResponse", params);
		}
		try {
			if(SipUtil.is2xxResponse(response.getStatusCode())){
				processStrayInvite2xx(response,provider);
			}
		} 

		catch (SipParseException e) {
			if(c_logger.isErrorEnabled())
			{
				c_logger.error("error.exception", "Parse Failure", null, e);
			}
		}
	}


	/**
	 * We got a another 2xx response for an INVITE request that was either proxied
	 * or generated locally. It could either retransmission of a prior 2xx that
	 * we received and is now passed to the session without a transaction as the
	 * stack removes the transaction once the first 2xx response is received.
	 * The second options that this is a 2xx response received due to forking
	 * proxy downstream.   
	 * @param response
	 */
	private void processStrayInvite2xx(Response response, SipProvider provider) 
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId(), response.getCallIdHeader().getCallId()};
			c_logger.traceEntry(this, "processStrayInvite2xx", params);
		}

		if(isProxying()) 
		{
			processProxyStrayInvite2xx(response, provider);
		}
		else
		{
			if(_tuWrapper.getState() == SipSessionImplementation.State.CONFIRMED || 
					_tuWrapper.getState() == SipSessionImplementation.State.TERMINATED){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "processStrayInvite2xx", 
							"This stray response is retransmission on CONFIRMED session.");
				}
				processUACInvite2xxRetransmission(response, provider);
			}
			else{
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "processStrayInvite2xx", 
					"This stray response received on derived non-CONFIRMED session and handled as regular 2xx response");
				}
				//        		Create a Sip Servlet Response
				SipServletResponseImpl sipResponse =
					new IncomingSipServletResponse(response, -1, provider);
				sipResponse.setRequest(m_sipMessage);
				sipResponse.setTransactionUser(_tuWrapper);
				updateSession(sipResponse);
				processResponse(sipResponse);
			}
		}
	}

	/**
	 * Process a retransmission of 2xx for INVITE when session is in UAC mode. 
	 * Currently we treat all 2xx as retransmission and we don't handle the
	 * forking scenario.  
	 * @param response
	 * @param provider
	 */
	private void processUACInvite2xxRetransmission(Response response, SipProvider provider) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " processUACInvite2xxRetransmission", params);
		}

		if(m_AckFor2xxRef == null)
		{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "processUACInvite2xxRetransmission", 
				"ACK Reference is unavailable, can not send reply to retransmission");
			}
			return;
		}

		OutgoingSipServletAckRequest ack = m_AckFor2xxRef;
		if (!ack.wasSent())
		{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "processUACInvite2xxRetransmission", 
				"ACK request not sent yet, can not send reply to retransmission");
			}
			return;
		}

		/*
		 * PM72839 - (sysroute PM71821)
		 * In case of restransmition by reinvite we should check whether the ACK
		 * is still relevant to current response.
		 * the cached ack request is for the previous final invite response  
		 */
		CSeqHeader ackCseqHeader = ack.getRequest().getCSeqHeader();

		if (response.getCSeqHeader() != null && ackCseqHeader != null) {
			if (ackCseqHeader.getSequenceNumber() != response.getCSeqHeader().getSequenceNumber()) {

				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "processUACInvite2xxRetransmission",
							"Response: " + response.getCSeqHeader().getSequenceNumber()
							+ " CSEQ != Ack " + ackCseqHeader.getSequenceNumber() + " CSEQ");
				}
				return;
			}
		} else {//should not get here
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "processUACInvite2xxRetransmission",
						"Response CSeq : " + response.getCSeqHeader() + " ackCSeqHeader : " + ackCseqHeader);
			}
		}

		Request request = ack.getRequest();

		try 
		{
			provider.sendRequest(request);
		}
		catch (IllegalArgumentException e) 
		{
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.exception", Situation.SITUATION_CREATE,
						null, e);
			}
		}
		catch (SipException e) 
		{
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.exception", Situation.SITUATION_CREATE,
						null, e);
			}
		}

	}

	/**
	 * Process a 2xx retransmission for INVITEs when the session is in proxying
	 * mode. At this point we ignore whether this is a retransmission or another
	 * 2xx response received due to forking. In either case we pass it to the
	 * application and later on forward it upstream. 
	 * @param response
	 * @param provider
	 */
	private void processProxyStrayInvite2xx(Response response, SipProvider provider) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId() };
			c_logger.traceEntry(this, " processProxyStrayInvite2xx", params);
		}
		//_lastOkResponse is not null only in case we are dealing with response to ReInvite
		if(_lastOkResponse == null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this,
						"processProxyStrayInvite2xx", 
				"Got a Stray 2xx Response for Initial Invite");
			}
			if(m_sipMessage != null)
			{
				StatefullProxy p = null;
				try {
					p = (StatefullProxy)m_sipMessage.getProxy(false);
				}
				catch (TooManyHopsException e) {
					logException(e);
				}

				if(null != p) {
					p.handleStrayInvite2xx(response, provider, getWrapper());
				}
				else {
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this,
								"processProxyStrayInvite2xx", 
								"Error, Got a stray response for a proxy when no matching proxy exists");
					}
				}
			}
			else {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "processProxyStrayInvite2xx", 
					"Error, Got a stray response for a proxy when no matching message exists");
				}
			}
		}
		else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this,
						"processProxyStrayInvite2xx", "Got a Stray 2xx Response for ReInvite");
			}
			Response lastResponse = (Response)_lastOkResponse.getMessage();
			SipRouter.sendResponseDirectlyToTransport(provider, lastResponse, false);
		}
	}

	/**
	 * @return Returns the m_forwardToApplication.
	 */
	boolean wasForwardedToApplication() {
		return m_forwardToApplication;
	}

	/**
	 * @param toApplication The m_forwardToApplication to set.
	 */
	void setForwardToApplication(boolean toApplication) {
		m_forwardToApplication = toApplication;
	}

	/**
	 * @see com.ibm.ws.sip.container.failover.Replicatable#store()
	 */
	public void store(){
		if(isInvalidating()){
			if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug(this, "store","replication denied, session was already invalidated");
			}
			return;
		}
		
		SessionRepository.getInstance().put(_tuWrapper.getId(), this);
	}

	/**
	 * Returns the transaction user wrapper
	 * @return
	 */
	public TransactionUserWrapper getWrapper(){
		return _tuWrapper;
	}

	/**
	 * @see com.ibm.ws.sip.container.failover.Replicatable#removeFromStorage()
	 */
	public void removeFromStorage() {
		SessionRepository.getInstance().removeTuImpl(this);
	}

	/**
	 * Returns if this dialog in the combined Mode
	 * @return
	 */
	public boolean isCombinedMode() {
		return _isCombinedMode;
	}

	/**
	 * Sets if the dialog is in the combined mode. Will be true when application
	 * will respond to incoming request and  in paralleled proxy it.
	 * @param isCombinedMode
	 */
	public void setCombinedMode(boolean isCombinedMode) {
		_isCombinedMode = isCombinedMode;
		setDirty();
	}

	/**
	 * Allocate the next session id.
	 */
	int getNexSipSessionId() {
		return _nextSipSessionId++;
	}


	/**
	 * 
	 * @return if this dialog was answered reliably
	 */
	public boolean wasAnsweredReliable() {
		return m_wasAnsweredReliable;
	}

	/**
	 * @see com.ibm.ws.sip.container.failover.Replicatable#notifyOnActivation()
	 */
	public void notifyOnActivation() {
		//this action makes the TU available to incoming requests.
		addToTransactionUsersTable();
	}

	/**
	 * Setting the TU wrapper
	 * @param wrapper
	 */
	public void setWrapper( TransactionUserWrapper wrapper){
		_tuWrapper = wrapper;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer myInformation = new StringBuffer("My Id = ");
		myInformation.append(getSharedId());
		myInformation.append(" Wrapper = ");
		myInformation.append(_tuWrapper.toString());
		myInformation.append(" My Info = ");
		myInformation.append(super.toString());
		return myInformation.toString();
	}
	/**
	 * Helper method that returns true if the state is TERMINATED
	 * @return
	 */
	private boolean isTermitedState(SipSessionImplementation.State state){
		if(state == SipSessionImplementation.State.TERMINATED){
			return true;
		}
		return false;
	}

	/**
	 * Helper method that returns true if the state is INITIAL
	 * @return
	 */
	private boolean isInitialState(SipSessionImplementation.State state){
		if(state == SipSessionImplementation.State.INITIAL){
			return true;
		}
		return false;
	}

	/**
	 * Helper method that returns true if the state is EARLY
	 * @return
	 */
	private boolean isEarlyState(SipSessionImplementation.State state){
		if(state == SipSessionImplementation.State.EARLY){
			return true;
		}
		return false;
	}


	/**
	 * Helper method that returns true if the state is CONFIRMED
	 * @return
	 */
	private boolean isConfirmedState(SipSessionImplementation.State state){
		if(state == SipSessionImplementation.State.CONFIRMED){
			return true;
		}
		return false;
	}
	/**
	 * Save the related sessionId. Used to connect new TU created for
	 * request which contain "Join" or "Replace" to the TU which is defined in 
	 * those methods.
	 * @param id
	 */
	public void setRelatedSessionId(String id) {
		_relatedSessionId = id;		
	}

	/**
	 * Returns Related Sip SessionId
	 * @return
	 */
	public String getRelatedSipSessionId() {
		return _relatedSessionId;		
	}

	/**
	 * Save the related session header. Used to connect new TU created for
	 * request which contain "Join" or "Replace" to the TU which is defined in 
	 * those methods.
	 * @param relatedSessionHeader
	 */
	public void setRelatedSessionHeader(String relatedSessionHeader) {
		_relatedSessionHeader = relatedSessionHeader;		
	}


	/**
	 * Returns Related session header
	 * @return
	 */
	public String getRelatedSipSessionHeader(){
		return _relatedSessionHeader;
	}

	/**
	 * Saves the latest destination where the INVITE request was sent. It can be
	 * used in the case when the UAC will send CANCEL request.
	 * 
	 * @param lastUsedDestination
	 */
	public void setUsedDestination(SipURL lastUsedDestination) {
		// We should saave the _latest destinatio in all dialog state for case
		// the UAC will send reInvite (on top of confirmed dialog).
		_latestDestination = lastUsedDestination;
	}

	/** 
	 * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#getUsedDestination()
	 */
	public SipURL getUsedDestination() {
		return _latestDestination;
	}
	
	/**
	 * Sets a response code to return when an inderlying session is invalidated
	 * @param code
	 */
	public void setSessionInvalidatedResponse(int code) {
		_sessionInvalidatedResponse = code;
	}

	/**
	 * Determines whether the session is in B2B mode.
	 * 
	 * @return
	 */
	public boolean isB2B() {

		return _isB2B;
	}

	/**
	 * Determines whether the session is in UAS mode.
	 * 
	 * @return
	 */
	public boolean isUAS() {

		return _isUAS;
	}

	/**
	 * Returns the B2buaHelperImpl associated with this request. 
	 * Invocation of this method also indicates to the container that the 
	 * application wishes to be a B2BUA, and any subsequent call to getProxy() 
	 * will result in IllegalStateException. 
	 * 
	 * @param create flag that indicate to set a b2b mode or not
	 * @param mode indicate the B2bua mode
	 *  
	 * @return the B2buaHelper for this request, or null in case create flag is
	 * 	set to false and we don't already in B2b mode 
	 * @throws IllegalStateException - if getProxy() had already been called
	 */
	public B2buaHelperImpl getB2buaHelper(boolean create, UAMode mode) throws IllegalStateException {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {_tuWrapper.getAppName(),_tuWrapper.getId(), create, mode };
			c_logger.traceEntry(TransactionUserImpl.class.getName(),
					"getB2buaHelper", params);
		}
		if(!create && !_isB2B){
			// this mean that we don't have b2bhelper and we don't want to 
			// create a new one.
			return null;
		}

		if(m_isProxying){
			throw new IllegalStateException("the application has already retrieved a proxy");
		}

		if(!_isB2B){
			_isB2B = true;

			if (m_sipMessage != null && ! m_sipMessage.isCommitted()){
				// insert the original sip message to the UAS un committed messages list
				addB2BPendingMsg(m_sipMessage, mode);
			}
		}

		return B2buaHelperImpl.getInstance();
	}

	/** 
	 * Returns a List of all uncommitted messages
	 * in the order of increasing CSeq number for the given mode of the session.
	 * 
	 * @param mode - the mode for which the pending messages are required, 
	 * 				one of UAC or UAS 
	 * 
	 * @return the list of SipServletMessage objects representing pending 
	 * 			messages for the session, or the empty list if none
	 */
	public List<SipServletMessage> getPendingMessages(UAMode mode){
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId(), mode };
			c_logger.traceEntry(TransactionUserImpl.class.getName(),
					"getPendingMessages", params);
		}

		LinkedList<SipServletMessage> pending;
		if(mode == UAMode.UAC){
			pending = _b2bUACPendingMessages;
			if (pending == null) {
				pending = _b2bUACPendingMessages = new LinkedList<SipServletMessage>();
			}
		}
		else {
			pending = _b2bUASPendingMessages;
			if (pending == null) {
				pending = _b2bUASPendingMessages = new LinkedList<SipServletMessage>();
			}
		}
		return pending;
	}

	/**
	 * Remove uncommitted message for the given mode of the session.
	 * 
	 * @param msg
	 * @param mode
	 */
	public void removeB2BPendingMsg(SipServletMessage msg, UAMode mode) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _tuWrapper.getAppName(), _tuWrapper.getId(), mode, _isB2B, 
					Integer.toHexString(msg.hashCode()) };
			c_logger.traceEntry(TransactionUserImpl.class.getName(),
					"removeB2BPendingMsg", params);
		}


		if(!isB2B()) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "removeB2BPendingMsg", "left with no update " + !isB2B());
			}			
			return;			
		}


		LinkedList<SipServletMessage> listToWork = null;

		if(mode == UAMode.UAC)
			listToWork = _b2bUACPendingMessages;
		else{
			listToWork = _b2bUASPendingMessages;
		}

		if(listToWork != null){	
			if(listToWork.remove(msg)){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "removeB2BPendingMsg",
							"removing message from list in mode " + mode + " list.size: " 
							+ listToWork.size());
				}
			}
			else{
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "removeB2BPendingMsg",
					"msg doesn't exist in the list");
				}
			}
		}

		else if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "removeB2BPendingMsg", 
			"Pending List is null");
		}
	}

	/**
	 * Add uncommitted message in the order of increasing 
	 * CSeq number for the given mode of the session.
	 * 
	 * @param msg
	 * @param mode
	 */
	public void addB2BPendingMsg(SipServletMessage msg, UAMode mode) {
		if (c_logger.isTraceEntryExitEnabled()) {
			String messageHex = "null";
			//check if the message is null
			if (msg != null){
				messageHex = Integer.toHexString(msg.hashCode());
			}
			Object[] params = {  _tuWrapper.getAppName(), _tuWrapper.getId(), mode, _isB2B, messageHex};
			c_logger.traceEntry(TransactionUserImpl.class.getName(),
					"addB2BPendingMsg", params);
		}
		if (!_isB2B) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "addB2BPendingMsg", "left with no update " + !isB2B());
			}			
			return;
		}

		if(msg == null){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "addB2BPendingMsg", "message is null");
			}
			return;
		}

		if(mode == UAMode.UAC){
			if(_b2bUACPendingMessages == null){
				_b2bUACPendingMessages = new LinkedList<SipServletMessage>();
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "addB2BPendingMsg", 
					"create new b2bUAC PendingMessages list");
				}
			}

			_b2bUACPendingMessages.add(msg);
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "addB2BPendingMsg", 
						"adding message to UAC list.size: " + _b2bUACPendingMessages.size());
			}
		} 
		else {
			if(_b2bUASPendingMessages == null){
				_b2bUASPendingMessages = new LinkedList<SipServletMessage>();
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "addB2BPendingMsg", 
					"create new b2bUAS PendingMessages list");
				}
			}
			_b2bUASPendingMessages.add(msg);
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "addB2BPendingMsg", 
						"adding message to UAS list.size: " + _b2bUASPendingMessages.size());
			}
		}
	}

	/**
	 * Retrieve is failed response sent on dialog initial request
	 * 
	 * @return true or false 
	 */
	public boolean isFailedResponseSent(){
		return _isFailedResponseSent;
	}

	/**
	 * Retrieve is session has been terminated
	 * @return true or false 
	 */
	public boolean isTerminated() {
		return isTermitedState(_tuWrapper.getState());
	}
	/**
	 * Returns method which intiated this dialog
	 * @return
	 */
	public String getInitialDialogMethod() {
		return _initialDialogMethod;
	}

	public void setIsB2bua(boolean mode) {
		_isB2B = mode;
	}

	/**
	 * Set transaction mode to be B2b mode 
	 */
	public void setB2buaMode() {		
		if (_isB2B == false) {
			_isB2B = true;
			if (isServerTransaction() && !m_sipMessage.isCommitted()) {
				// In this case we are UAS leg and m_sipMessage should be stored in
				// pending messages.
				addB2BPendingMsg(m_sipMessage, UAMode.UAS);
			}
		}
	}

	/**
	 * Set transaction mode to be UAS mode 
	 */
	public void setUASMode() {
		_isUAS = true;	  	
	}

	public Vector<String> getRouteHeaders() {
		return m_routeHeaders;
	}

	/**
	 * subscriberURI getter
	 * @return subscriberURI
	 */
	public URI getSubscriberURI() {
		return _subscriberURI;
	}

	/**
	 * subscriberURI setter
	 * @param subscriberURI
	 */
	public void setSubscriberURI(URI subscriberURI) {
		this._subscriberURI = subscriberURI;
	}

	/**
	 * getter for region
	 * @return
	 */
	public SipApplicationRoutingRegion getRegion() {
		return _region;
	}

	/**
	 * setter for region
	 * @param region
	 */
	public void setRegion(SipApplicationRoutingRegion region) {
		this._region = region;
	}

	/*
	 * @param address
	 */
	public void setOutboundInterface(InetSocketAddress address)
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {  _tuWrapper.getAppName(), _tuWrapper.getId()};
			c_logger.traceEntry(this,"setOutboundInterface", params);
		}

		_preferedOutBoundIface = getOutboundInterface(address);

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "setOutboundInterface: " + _preferedOutBoundIface);
		}
	}

	public OutboundInterface getOutboundInterface(InetSocketAddress address) {
		OutboundInterface outboundIf = null;
		
		if (address == null) {
			throw new IllegalArgumentException("Invalid address = null");
		}
			
		boolean isSet = false;
		int preferedOutBoundIfaceIdxUDP, preferedOutBoundIfaceIdxTCP, preferedOutBoundIfaceIdxTLS;
		
		//note that the same address can be matched to more then one transport....
		preferedOutBoundIfaceIdxUDP = SipProxyInfo.getInstance().getIndexOfIface(address, "udp");
		if (preferedOutBoundIfaceIdxUDP != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
			isSet = true;
		}
		
		preferedOutBoundIfaceIdxTCP = SipProxyInfo.getInstance().getIndexOfIface(address, "tcp");
		if (preferedOutBoundIfaceIdxTCP != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
			isSet = true;
		}
		
		preferedOutBoundIfaceIdxTLS = SipProxyInfo.getInstance().getIndexOfIface(address, "tls");
		if (preferedOutBoundIfaceIdxTLS != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
			isSet = true;
		}
		
		if (!isSet)
		{
			throw new IllegalArgumentException("address:" + address + " is not listed as allowed outbound interface.");
		}
		outboundIf = new OutboundInterface(preferedOutBoundIfaceIdxUDP, 
				preferedOutBoundIfaceIdxTCP, preferedOutBoundIfaceIdxTLS);
		
		return outboundIf;
	}

	/*
	 * @param address
	 */
	public void setOutboundInterface(InetAddress address)
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {  _tuWrapper.getAppName(), _tuWrapper.getId()};
			c_logger.traceEntry(this,"setOutboundInterface", params);
		}

		if (address != null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setOutboundInterface", "Attempting to set outbound interface to: " + address);
			}

			boolean isSet = false;

			int preferedOutBoundIfaceIdxUDP, preferedOutBoundIfaceIdxTCP, preferedOutBoundIfaceIdxTLS;
			
			preferedOutBoundIfaceIdxUDP = SipProxyInfo.getInstance().getIndexOfIface(address, "udp");
			if (preferedOutBoundIfaceIdxUDP != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
				isSet = true;
			}

			preferedOutBoundIfaceIdxTCP = SipProxyInfo.getInstance().getIndexOfIface(address, "tcp");
			if (preferedOutBoundIfaceIdxTCP != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
				isSet = true;
			}

			preferedOutBoundIfaceIdxTLS = SipProxyInfo.getInstance().getIndexOfIface(address, "tls");
			if (preferedOutBoundIfaceIdxTLS != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
				isSet = true;
			}
			
			if (!isSet)
			{
				throw new IllegalArgumentException("address:" + address + " is not listed as allowed outbound interface.");
			}
			_preferedOutBoundIface =  new OutboundInterface(preferedOutBoundIfaceIdxUDP, 
					preferedOutBoundIfaceIdxTCP, preferedOutBoundIfaceIdxTLS);
		}
		else
			throw new IllegalArgumentException("Invalid address = null");

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "setOutboundInterface");
		}
	}

	public int getOriginatorPreferedOutboundIface(String transport) {
		int outboundIfaceInd = SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED;
		if (_originatorPreferedOutBoundIface != null) {
			outboundIfaceInd = _originatorPreferedOutBoundIface.getOutboundInterface(transport);
		}
		return outboundIfaceInd;
	}
	
	
	public int getPreferedOutboundIface(String transport)
	{
		int returnValue;

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceEntry(this, "getPreferedOutboundIface", "transport = " + transport + " tu = " + this.hashCode());
		}
		
		if (SIPTransactionStack.instance().getConfiguration().getSentByHost() != null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getPreferedOutboundIface", "Return OUTBOUND_INTERFACE_NOT_DEFINED since the sentByHost property is set");
			}
			returnValue = SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED;
		}
		else {
			returnValue = _preferedOutBoundIface.getOutboundInterface(transport);
		}

		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceExit(this, "getPreferedOutboundIface", "transport = " + transport + " tu = " + this.hashCode());
		}
		return returnValue;
	}


	/**
	 Returns the SipServletRequest which caused in the
	 * ServerTransaction mode to create this TU.
	 * Can be null.
	 * @return
	 */
	public SipServletRequestImpl getSipMessage() {
		return m_sipMessage;
	}

	/**
	 * Returns CSeq of origianl INVITE
	 * @return
	 */
	public long getInviteCseq() {
		return m_inviteCseq;
	}

	/**
	 * Returns associated TUKey object
	 * @return
	 */
	public TUKey getTUkey() {
		return _key;
	}

	public boolean isProxyReceivedFinalResponse() {
		return _proxyReceivedFinalResponse;
	}

	public void setProxyReceivedFinalResponse(boolean receivedFinalResponse, int status) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntryExit(this,"setProxyReceivedFinalResponse", new Object[] { receivedFinalResponse, status});
		}

		_proxyReceivedFinalResponse = receivedFinalResponse;
		m_finalResponseStatus = status;
	}

	public long getLocalCSeq() {
		return m_localCSeq;
	}

	public long getRemoteCseq() {
		return m_remoteCseq;
	}

}
