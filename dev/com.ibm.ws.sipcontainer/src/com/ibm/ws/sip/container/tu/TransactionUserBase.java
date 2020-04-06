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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.sip.IBMSipSession;
import com.ibm.ws.sip.container.failover.Replicatable;
import com.ibm.ws.sip.container.failover.ReplicatableImpl;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.servlets.SIPSessionFactory;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipSessionImplementation;
import com.ibm.ws.sip.container.servlets.SipSessionSeqLog;
import com.ibm.ws.sip.container.servlets.WASXSipApplicationSessionFactory;
import com.ibm.ws.sip.container.timer.TransactionUserTimerListener;
import com.ibm.ws.sip.container.util.wlm.DialogAux;
import com.ibm.ws.sip.properties.CoreProperties;

/**
 * @author anat, Mar 16, 2006
 *
 * This TransactionUserBase will contain all the basic information 
 * that is common for all type of TU (TU for UAS,
 * TU for UAC and TU for Proxy).
 */
public class TransactionUserBase extends ReplicatableImpl { 

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(TransactionUserBase.class);

	/**
	 * The descriptor of the Sip Servlet that is associated to handle this
	 * request. Amir: Should be transient otherwise SipAppDesc will be
	 * replicated
	 * Moti: we don't really serialize this object we merely need the 
	 * servlet name and the app name from it.
	 */
	private transient SipServletDesc m_sipServletDesc;

	/**
	 * Application Session Ids counter.
	 */
	private static transient long c_nextAppSessionId = 1;

	/**
	 * Session's Expiration time. Use a default of 1 min. This setting should be
	 * override by setting from sip.xml configuration.
	 */
	private long m_expires = 0;


	/**
	 * Unique identifier of the Application Session - when / if  it will be created.
	 */
	private transient String m_applicationId;

	/**
	 * Holds reference major SipSession. When TU will act as UAS or UAC - only
	 * one SipSession will be created and used. In case it will act as Proxy
	 * or Proxy and UAS together this _sipSession will be the original SipSession
	 */
	private transient SipSessionImplementation _sipSession = null;


	/**
	 * Used as a prefix for session IDs so they will be unique across invocations
	 */
	/*TODO Liberty cluster mechanism TBD*/
	private static transient String serverIdentifier = "wlp";//"local." + System.currentTimeMillis();//TODO Liberty should we have that configurable or take host address(could have security concerns)

	/**
	 * Reference to the UserTransactionTimer
	 * Moti: OG: made transient
	 */
	private transient TransactionUserTimerListener _tUserTimer = null;

	/**
	 *  back pointer to the TransactionUserWrapper
	 */
	//Moti: OG: moved to be transient.
	private transient TransactionUserWrapper _tuWrapper = null;

	/**
	 * This SipSession state. Used only when the dialog state is NOT_DIALOG
	 */
	private boolean _isInvalidated = false;

	/**
	 * Context log associated with this session
	 */
	private transient SipSessionSeqLog m_contextLog;

	/**
	 * Object which holds and manages the state of the dialog.
	 */
	private DialogState _dialogState = new DialogState();

	/**
	 * A locking handle that is used to synchronize all API methods of objects related 
	 * to this TransactionUser. If an appicationSession is created first, then the synchronizer will be 
	 * taken from the AS, otherwise it will be create in the TUBase and will be assigned to the AS, in
	 * case it will be created later. 
	 */
	private Object _synchronizer;

	/**
	 * A locking handle that is used to synchronize application service methods that are related to 
	 * this TU's application session. If an appicationSession is created first, then the synchronizer will be 
	 * taken from the AS, otherwise it will be create in the TUBase and will be assigned to the AS, in
	 * case it will be created later. 
	 */
	private Object _serviceSynchronizer;

	/**
	 * Flag that used to identify if more derived sessions can be created
	 */
	private boolean _canCreateDerivedSessions = false;

	/**
	 * Session key based key, storing this value 
	 */
	private transient String _sessionKeyBase = null;

	/**
	 * 
	 */
	/*TODO Liberty cluster mechanism TBD*/
	private static Map<String, Counter> s_nextTransactionIdPerLogicalName = new HashMap<String, Counter>(5);

	/**
	 * Holds a reference to related ApplicationSession after it 
	 * invalidated. This is because application can always call
	 * to message.getApplicationSession() and it should be return.
	 * meaning that the SipApplicationSession was already removed from the session repository tables
	 * but we still need to get to him if user is calling to getApplicationSession 
	 */
	private transient SipApplicationSession m_sipAppSession = null;

	/**
	 * Default constructor
	 */
	public TransactionUserBase() {
		super();
	}

	/**
	 * Construct a new Base Transaction User
	 * 
	 */    
	void initialize(TransactionUserWrapper tuWrapper,
			SipServletRequestImpl sipMessage,
			SipApplicationSessionImpl sipApp) {

		if (c_logger.isTraceEntryExitEnabled()) {
			if(sipMessage != null && sipApp != null){
				Object[] params = {tuWrapper, sipMessage.getMethod(), sipApp.getId()};
				c_logger.traceEntry(this, "initialize", params);
			}else{
				c_logger.traceEntry(this, "initialize");
			}
		}

		_tuWrapper = tuWrapper;

		if(sipApp != null){
			//			This Transaction User relates to Application Session that is already exist
			synchronized (sipApp.getSynchronizer()) {
				if (sipApp.isValid()) {
					m_applicationId = sipApp.getSharedId();
					setSynchronizer(sipApp.getSynchronizer());
					setServiceSynchronizer(sipApp.getServiceSynchronizer());
				} else {
					sipApp = null;
				}
			}
		} 

		if (_synchronizer == null) {
			setSynchronizer(new Object());
			setServiceSynchronizer(new Object());
		}
		createTUId(sipApp);	

		if (SipSessionSeqLog.isEnabled()) {
			m_contextLog = SipSessionSeqLog.getInstance();
			m_contextLog.setId(getSharedId());
		}

		String method = sipMessage.getMethod();
		_dialogState.reset();
		_dialogState.setDialogState(method);

		if(isDialog()){
			if(method.equals("INVITE") || method.equals("SUBSCRIBE")){
				setCanCreateDS(true);
			}
		}
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "initialize", "New BaseTU was created. ID = " + getSharedId());
		}

		logToContext(SipSessionSeqLog.INIT, getSharedId());

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "initialize");
		}

	}

	public void reinitilize() {
		_synchronizer = new Object();
		_serviceSynchronizer = new Object();
	}

	/**
	 * add this TU to SipAppSession lists.
	 */
	public void attachToSipAppSession() {
		// attaching tu after base TU is initiated to avoid scenario's which uninitialized tu are accessed
		// via app session
		synchronized (getSynchronizer()) {
			SipApplicationSessionImpl sipApp = (SipApplicationSessionImpl)getSipApplicationSession();
			if (sipApp != null && sipApp.isValid()) {
				sipApp.addTransctionUser(_tuWrapper);
			}
		}    	
	}

	/**
	 * This method used when initalizing BaseTransactionUser for derivedSession
	 * @param wrapper
	 * @param originalTU
	 */
	public void initialize(TransactionUserWrapper wrapper, TransactionUserWrapper originalTU) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {wrapper, originalTU};
			c_logger.traceEntry(this, "initialize", params);
		}

		TransactionUserBase originalBaseTU = originalTU.getTuBase();
		_tuWrapper = wrapper;
		_synchronizer = originalBaseTU.getSynchronizer();
		_serviceSynchronizer = originalBaseTU.getServiceSynchronizer();
		SipApplicationSessionImpl originalApp = (SipApplicationSessionImpl) originalTU
		.getApplicationSession(true);

		// This Transaction User relates to Application Session that is
		// already exists
		m_applicationId = originalApp.getSharedId();
		//_sipAppSession = originalApp;
		//_sipAppSession.addTransctionUser(_tuWrapper);
		((SipApplicationSessionImpl)getSipApplicationSession()).addTransctionUser(_tuWrapper);


		createTUId(originalApp);	

		if (SipSessionSeqLog.isEnabled()) {
			m_contextLog = SipSessionSeqLog.getInstance();
			m_contextLog.setId(getSharedId());
		}

		_dialogState.reset();
		_dialogState.setDialogState(originalTU.getInitialDialogMethod());

		setCanCreateDS(true);

		m_sipServletDesc = originalBaseTU.getSipServletDesc();

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "initialize", "New BaseTU for Derived was created. ID = " + getSharedId());
		}

		logToContext(SipSessionSeqLog.INIT, getSharedId());

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "initialize");
		}
	}


	/**
	 * Method that tries to invoke "invalidateWhenReady" mechanism
	 *
	 */
	boolean shouldInvokeInvalidateWhenReady(){

		boolean tryToInvalidateLater = false;
		SipApplicationSession as = null;
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {getAppName(), getSharedId()};
			c_logger.traceEntry(this, "shouldInvokeInvalidateWhenReady", params);
		}

		if(_sipSession != null){
			if (_sipSession.getInvalidateWhenReady()){
				if(_sipSession.isReadyToInvalidate()){
					tryToInvalidateLater = true;
				}
			}

			// Maybe the flag is ON on SipApplicationSession only
			else if(m_applicationId != null && 
					(( as = getSipApplicationSession()) != null) &&
					as.getInvalidateWhenReady() == true){
				tryToInvalidateLater = true;
			}
		} else if (getWrapper().isJSR289Application() && getWrapper().isReadyToInvalidate()) {
				tryToInvalidateLater = true;
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "shouldInvokeInvalidateWhenReady", tryToInvalidateLater);
		}

		return tryToInvalidateLater;
	}

	/**
	 * Method that invalidates all the derived sessions on the same branch,
	 * when proxing an error response
	 *
	 */
	private void invalidateProxyDerivedSessions() {

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "invalidateProxyDerivedSessions");
		}

		if( getWrapper().isProxingErrorResponse()){
			Iterator<TransactionUserWrapper> iter = getWrapper().getBranch().getRelatedTUs();
			TransactionUserWrapper tuw;
			SipSession session;
			while ( iter != null && iter.hasNext() ) {
				tuw = iter.next();
				if ( tuw.isValid() && !tuw.isInvalidating() && !tuw.getId().equals(getWrapper().getId())) {
					session = tuw.getSipSession(false);
					if ( session == null ) {
						tuw.invalidateTU(true, true);
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(null, "invalidateProxyDerivedSessions", "Derived session invalidated");
						}
					}
					else {
						if ( session.getInvalidateWhenReady() == true ) {
							session.invalidate();
							if (c_logger.isTraceDebugEnabled()) {
								c_logger.traceDebug(null, "invalidateProxyDerivedSessions", "Derived session invalidated");
							}
						}
					}
				}
			}
		}	
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "invalidateProxyDerivedSessions");
		}
	}

	/**
	 * Helper method which calls to InvalidateWhenReady
	 * method in SipSession.
	 */
	void callInvalidateWhenReady(){
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {getAppName(), getSharedId(),};
			c_logger.traceEntry(this, "callInvalidateWhenReady", params);
		}

		SipApplicationSession applicationSession = getSipApplicationSession();
		invalidateProxyDerivedSessions();

		if(_sipSession != null ){
			if(_sipSession.getInvalidateWhenReady() == true){
				_sipSession.readyToInvalidate();
			}
			else {
				// We can get here only if flag "isReadyToInvalidate" 
				// was "FALSE" on SipSession
				// but "TRUE" on SipApplicationSession.
				if (applicationSession != null && applicationSession.isReadyToInvalidate()){
					((SipApplicationSessionImpl)applicationSession).readyToInvalidate();
				}
			}
		} else {
			
			//invalidate the TU
			readyToInvalidate();
		}
	}

	/**
	 * This method might be executed multi-threaded or single threaded,
	 * depends on the TasksInvoker definition 
	 * @see com.ibm.ws.sip.container.events.TasksInvoker
	 * @see com.ibm.ws.sip.container.SipContainer#setTimerInvoker()
	 * @see java.lang.Runnable#run()
	 */
	private void readyToInvalidate() {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { getSharedId() , isValid()};
			c_logger.traceEntry(this, "readyToInvalidate", params);
		}

		if (isValid()) {

			// keep reference to the sip app session before invalidating.
			// once SipSession is invalidate we cannot retrieve this reference
			SipApplicationSessionImpl appSession = (SipApplicationSessionImpl) getSipApplicationSession();

			// Session expired - Invalidate.
			try {
				getWrapper().invalidateTU(true, true);
			} 
			catch (IllegalStateException e) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "readyToInvalidate", "SipApplication session was already invalidated");
				}
			}

			// first verifying that the sip app session wasn't already validated.
			if (appSession != null) {
				if (appSession.getInvalidateWhenReady() && appSession.isReadyToInvalidate()) {
					appSession.readyToInvalidate();
				}					
			}
		} else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "readyToInvalidate", "TU has already been invalidated. Just ignore.");
			} 
		}
	}

	/**
	 * Continue initialization after TUImpl was fully initalized
	 * @param wrapper
	 * @param originalTU
	 */
	void continueDerivedInitalization(TransactionUserWrapper originalTU){

		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {getAppName(), getSharedId()};
			c_logger.traceEntry(this,
					"continueDerivedInitalization", params);
		}

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "continueDerivedInitalization", 
					"Base TU = " + originalTU);
		}

		SipSessionImplementation baseSS = 
			(SipSessionImplementation)originalTU.getTuBase().getSipSession(false);

		if(baseSS != null){
			// Create derived SipSession
			_sipSession = SIPSessionFactory.
			createDerivedSIPSession(_tuWrapper,baseSS);
		}
		else if (_tuWrapper.isB2B()){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "initialize", 
				"Original UAS not linked so Application MUST explicitly link original and derived sessions");
			}
		}

	}

	/**
	 * This method will be called when no more Derived Sessions
	 * will be able to created.
	 * 32 sec after dialog established by INVITE message
	 * no more stray responses will be accepted.
	 */
	public void setCanCreateDS(boolean flag){
		_canCreateDerivedSessions = flag;
	}

	/**
	 * Returns if this TUInterface can create more DerivedSessions.
	 * @return
	 */
	public boolean canCreateDS(){
		return _canCreateDerivedSessions;
	}	

	/**
	 * Rescheduling timer to 31 seconds, this is used when session is invalidated explicitly without
	 * terminating the dialog properly.
	 */
	public void rescheduleExpTimer() {
		cancelExpirationTimer();

		_tUserTimer = new TransactionUserTimerListener(_tuWrapper);
		_tUserTimer.schedule(false, 31 * 1000);    	 
	}

	/**
	 * Create timer for this TransactionUser
	 */
	private void createTranscationUserTimer(){
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {getAppName(), getSharedId()};
			c_logger.traceEntry(this,
					"createTranscationUserTimer", params);
		}
		if(getSipApplicationSession() != null){
			//moti: basically we must have a (expiration) timer for each SIP Application Session (SAS)
			// but sometimes SAS is not created (on initial incoming call).
			// However , we must monitor the transaction and kill it if not used . Therefore
			// we create the timer first on this transaction and later, when a SAS is created
			// we attach that same timer to the SAS.
			// now: if m_applicationId is not null, it means we have a SAS and it will manage the 
			// expiration and all...(common in b2b UA applications)
			return;
		}

		int sessionTimeout = 1;
		if (m_sipServletDesc != null) {
			sessionTimeout = m_sipServletDesc.getSipApp().getAppSessionTTL();
		}


		// 			The default timeout will be 1 min. In case that in the sip.xml file
		// 			expiration will be defined as - "Never expired" (-1) a timer will not be 
		//created, but the application composers must be aware that they must
		//invalidate the dialog through the SipAppSession, or else it will stay in memory forever!
		long deltaMilis;

		if (sessionTimeout >= 1) {
			deltaMilis = sessionTimeout * 1000 * 60;
		}else{
			m_expires = -1;
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createTranscationUserTimer",
				"WARNING: According to sip.xml settings, this dialog will never expire. ApplicationSession must be explicitly invalidated when dialog finished, or else it will stay in memory forever!");
			}
			return; //Timer will not be created
		}

		if (c_logger.isTraceDebugEnabled()) {
			StringBuffer buff = new StringBuffer();
			buff.append("TransactionUser = " + getSharedId());
			buff.append(" will die in = ");
			buff.append(deltaMilis);
			buff.append("MS");

			c_logger.traceDebug(this, "createTranscationUserTimer",buff.toString());
		}

		m_expires = System.currentTimeMillis() + deltaMilis;
		_tUserTimer = new TransactionUserTimerListener(_tuWrapper);
		_tUserTimer.schedule(false, deltaMilis);
	}


	/**
	 * Allocate the next session id.
	 */
	private static synchronized long getNextApplicationSessionId() {

		return c_nextAppSessionId++;
	}


	/**
	 * Helper method that creates Id for the FUTURE Application Session
	 * 
	 * @return
	 */
	private static String createApplicationId( String srvrid){
		StringBuffer buff = new StringBuffer( srvrid);
		buff.append(Replicatable.ID_INTERNAL_SEPERATOR);
		buff.append(getNextApplicationSessionId());
		return buff.toString();
	}

	/**
	 * Method that returns all the SipSessins relates to this TU
	 * @return
	 */
	List<IBMSipSession> getAllSipSessions(List<IBMSipSession> sessions, boolean create){
		if(_sipSession == null){
			getSipSession(create);
		}
		if(_sipSession != null){
			sessions.add(_sipSession);	
		}
		return sessions;
	}

	/**
	 * Invalidate the Transaction User and Remove it from the TransactionUserTable
	 *
	 */
	protected void invalidateBase(boolean removeFromAppSession) {

		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {getAppName(), getSharedId()};
			c_logger.traceEntry(this,
					"invalidateBase", params);
		}
		// Method for internal use that allow to remove this session from the
		// SipTransactionUserTable without remove it from SipApplicationSession.
		// This ability used in SipApplicationSession.destroyAllSesssion() to
		// avoid java.util.ConcurrentModificationException.

		if(_isInvalidated){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "invalidate",
						"Base TU is invalid, " + this);

			}
			logToContext(SipSessionSeqLog.ERROR_SESSION_TERMINATED);

			throw new IllegalStateException("Invalid Session: " + this);

		}
		try {

			_tuWrapper.logToContext(SipSessionSeqLog.INVALIDATED);

			cancelExpirationTimer();

			if(_sipSession == null){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "invalidate",
							"SipSession doesn't exist  "  + this);

				}
			} else {
				_sipSession.invalidateSipSession();
			}

			if (_sessionKeyBase != null) {
				SessionRepository.getInstance().removeKeyBaseAppSession(_sessionKeyBase);
			}

			_isInvalidated = true;

			if(removeFromAppSession){
				//Remove Sip Session from Application session
				if(m_applicationId != null){
					SipApplicationSessionImpl as = (SipApplicationSessionImpl)getSipApplicationSession();
					if( as != null){
						as.removeTransactionUser(_tuWrapper);
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this, "invalidate",
									"removed TUWrapper " + _tuWrapper + " from SAS " + as);

						}
					} else {
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this, "invalidate",
									"SAS is null, was not created by application. asID="+m_applicationId);

						}
					}
				}        
			}

			if(null != m_contextLog) {
				SipSessionSeqLog tmp = m_contextLog;
				m_contextLog = null;
				tmp.returnToPool();
			}
		} finally {
			removeFromStorage();
		}
	}

	/**
	 * This method should be called by ApplicationSessionImpl 
	 * during invalidation.
	 * @param appSession
	 */
	public void applicationSessionIvalidated(SipApplicationSession appSession){
		m_sipAppSession = appSession;
	}

	/**
	 * Checks if the Transaction User is active (not terminated).
	 * 
	 * @throws IllegalStateException
	 *             In case the it is already invalidated.
	 */
	void isBaseTUActive() throws IllegalStateException {
		if (_isInvalidated == true) 
		{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger
				.traceDebug(
						this,
						"isSessionValid",
						"Session is invalid, Operation not allowed,  "
						+ this);

			}

			throw new IllegalStateException("This SipSession was invalidated: "
					+ this);
		}
	}

	/**
	 * Get SipSession associate with this Transaction User 
	 * @param create
	 * @return
	 */
	SipApplicationSession getApplicationSession(boolean create){
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {getAppName(), getSharedId()};
			c_logger.traceEntry(this,
					"getApplicationSession", params);
		}

		if(m_sipAppSession != null){
			// If this ApplicationSession was invalidated - return 
			// a local reference to this ApplicationSession.
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this,"getApplicationSession","SipApplicationSession was invalidated, returning the local reference");
			}
			return m_sipAppSession;
		}

		//Moti: changed method a bit for sip session offloading.
		SipApplicationSession result = getSipApplicationSession();

		if(_isInvalidated){
			return result;
		}

		if(result == null && create == true){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this,"getApplicationSession","creating new App Session");
			}
			SipApplicationSessionImpl _sipAppSession = (SipApplicationSessionImpl)WASXSipApplicationSessionFactory.getInstance().createSipApplicationSession( getApplicationId());
			_sipAppSession.setSynchronizer(getSynchronizer());
			_sipAppSession.setServiceSynchronizer(getServiceSynchronizer());
			if(_tUserTimer != null){
				_sipAppSession.createSipAppTimer(0,_tUserTimer.getTimer());
			}

			_sipAppSession.setExpirationFromTU(m_expires);
			_sipAppSession.addToApplicationSessionsTable();
			_sipAppSession.addTransctionUser(_tuWrapper);
			_sipAppSession.setSipApp(m_sipServletDesc.getSipApp(), false);
			result = _sipAppSession ;

		} 
		return result;
	}


	/**
	 * Returns the Id of the relate ApplicationSession
	 * @return
	 */
	String getApplicationId() {
		//Moti: added for ObectGrid... for some reason m_applicationId was null; 
		// maybe I should serialize it...
		if (m_applicationId == null && getSharedId() != null ) {
			int appIDEndIndex = getSharedId().lastIndexOf( Replicatable.ID_INTERNAL_SEPERATOR);
			m_applicationId = getSharedId().substring( 0, appIDEndIndex);
		}
		return m_applicationId;
	}


	/**
	 * Helper method that creates Id for this TransactionUser
	 * @param sipApp 
	 * @return
	 */
	/*TODO Liberty cluster mechanism TBD*/
	private void createTUId(SipApplicationSessionImpl sipApp){
		String srvrid;

		if (sipApp ==  null){
			//in case we are trying to create derived session the
			// m_applicationId will already exists
			srvrid = serverIdentifier;
			m_applicationId = TransactionUserBase.createNextApplicationSessionId(srvrid);
		} else {
			srvrid = sipApp.getServerID();
		}

		StringBuilder buff = new StringBuilder(m_applicationId)
		.append(ID_INTERNAL_SEPERATOR)
		.append(getNextTransactionUseID( srvrid));

		setSharedId( buff.toString());
	}

	/**
	 * Allocate the next session id.
	 */
	public static String createNextApplicationSessionId() {
		return createNextApplicationSessionId(null);        
	}

	/**
	 * Allocate next session id for a specific logical name
	 * @param logicalNameStr logical name, if null then ignored.
	 * @return
	 */
	public static String createNextApplicationSessionId(String receivedServerID) {
		String srvrid;

		if (receivedServerID != null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("creating application session for logical name  " + receivedServerID);

			}    		
			srvrid = receivedServerID;
		} else {
			srvrid = serverIdentifier;
		}


		return TransactionUserBase.createApplicationId(srvrid);
	}

	/**
	 * Helper method that checks if related session was created
	 * @return
	 */
	boolean hasSipSession() {
		//		We can check only _sipSession because it will exist also when 
		//		derived Sessions had created
		if (_sipSession != null ) {
			return true;
		}
		return false;
	}

	/**
	 * Gets the Sip Servlet descriptor associated with this session.
	 * 
	 * @return
	 */
	SipServletDesc getSipServletDesc() {
		return m_sipServletDesc;
	}

	/**
	 * Sets the Sip Servlet descriptor associated with this session.
	 * 
	 * @param desc
	 */
	void setSipServletDesc(SipServletDesc desc) {
		if(c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug("TransactionUserBase.setSipServletDesc():"+desc);
		}
		m_sipServletDesc = desc;
		setDirty();
		createTranscationUserTimer();
		
		store();
		logToContext(SipSessionSeqLog.SIPLET_DESC, desc);
	}

	/**
	 * @see javax.servlet.sip.SipSession#setHandler(String)
	 */
	synchronized void setHandler(String name) throws ServletException {

		//Call on the router to search through the installed apps for a
		// matching siplet. We need to convert the name to a sip servlet descriptor.
		SipAppDesc appDesc = m_sipServletDesc.getSipApp();
		SipServletDesc desc = appDesc == null ? null : appDesc.getSipServlet(name);

		if (null != desc) {
			m_sipServletDesc = desc;
			setDirty();

			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setHandler for  " + this, "" + desc);

			}
		}
		else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setHandler for " + this, "Handler: " + name
						+ " Not available");

			}
			throw new ServletException(
					"No servlet with the specified name exists in the application" + this);
		}

	}

	/**
	 * Get SipSession associate with this Transaction User 
	 * @param create
	 * @return
	 */
	SipSession getSipSession(boolean create) {

		if (_sipSession == null && create == true) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getSipSession",
						"Create a new SipSession for TU = " + getSharedId());
			}
			_sipSession = SIPSessionFactory.createSIPSession(_tuWrapper);
		}

		return _sipSession;
	}

	/**
	 * Log to the sequence log associated with this SIP Session
	 * 
	 * @param state
	 * @param info
	 */
	void logToContext(int state, Object info, Object extendedInfo)
	{
		if(null != m_contextLog)
		{
			m_contextLog.log(state, info, extendedInfo);
		}
	}

	/**
	 * Log to the sequence log associated with this SIP Session
	 * @param state
	 * @param info
	 */
	void logToContext(int state, int info, Object extendedInfo)
	{
		if(null != m_contextLog)
		{
			m_contextLog.log(state, info, extendedInfo);
		}
	}

	/**
	 * Log to the sequence log associated with this SIP Session
	 * @param state
	 * @param info
	 */
	void logToContext(int state, Object info)
	{
		if(null != m_contextLog)
		{
			m_contextLog.log(state, info);
		}
	}

	/**
	 * Log to the sequence log associated with this SIP Session
	 * @param state
	 * @param info
	 */
	void logToContext(int state)
	{
		if(null != m_contextLog)
		{
			m_contextLog.log(state);
		}
	}

	/**
	 * Log to the sequence log associated with this SIP Session
	 * @param state
	 * @param info
	 */
	void logToContext(int state, int info)
	{
		if(null != m_contextLog)
		{
			m_contextLog.log(state, info);
		}
	}

	/**
	 * Log to the sequence log associated with this SIP Session
	 * @param state
	 * @param info
	 */
	void logToContext(int state, boolean info)
	{
		if(null != m_contextLog)
		{
			m_contextLog.log(state, info);
		}
	}


	/**
	 * Returns if represented SipSession is a dialog.
	 * @return
	 */
	boolean isDialog(){
		return _dialogState.isDialog();
	}

	/** 
	 * @see com.ibm.ws.sip.container.failover.Replicatable#store()
	 */
	public void store() {
		if( _sipSession != null){
			_sipSession.store();//TODO Liberty check if necessary
		}
		SessionRepository.getInstance().put(getSharedId(), this); 
	}

	/** 
	 * @see com.ibm.ws.sip.container.failover.Replicatable#removeFromStorage()
	 */
	public void removeFromStorage() {
		SessionRepository.getInstance().removeTuBase(this);
	}

	/** 
	 * @see com.ibm.ws.sip.container.failover.Replicatable#notifyOnActivation()
	 */
	public void notifyOnActivation() {
		if(c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug("TransactionUserBase.notifyOnActivation");
		}
		if( _sipSession != null){
			_sipSession.notifyOnActivation();	
		}
		_tuWrapper.notifyTUOnActivation();
	}

	/**
	 * Setting the TU wrapper
	 * @param wrapper
	 */
	public void setWrapper( TransactionUserWrapper wrapper){
		_tuWrapper = wrapper;
	}

	/**
	 * @see com.ibm.ws.sip.container.failover.Replicatable#shouldBeReplicated()
	 */
	public boolean shouldBeReplicated(boolean forBootstrap){
		return m_sipServletDesc != null && m_sipServletDesc.getSipApp().isDistributed();
	}

	/**
	 * Cancel the expiration timer, if the timer was not passed on to the SipApplicationSession
	 * This method is called on TU invalidation. 
	 */
	public void cancelExpirationTimer(){
		if( getSipApplicationSession() == null && _tUserTimer != null){
			_tUserTimer.cancel();
		}
	}

	/**
	 * @return if this TU already invalidated
	 */
	public boolean isValid(){ 
		return !_isInvalidated;
	}

	/**
	 * Returning next sequential transaction id per logical name. We have the transaction ID per logical name
	 * so that in case of failover, if the failed-over application session is assigned with a new session
	 * we will not have an id overlap. 
	 * @param logicalName
	 * @return
	 */
	/*TODO Liberty cluster mechanism TBD*/
	private static long getNextTransactionUseID( String logicalName){
		Counter counter = getCounter(logicalName);
		return counter.getNextCount();
	}

	/**
	 * Synchronously creates a counter for a logical name if none exists 
	 * @param counter
	 * @param logicalName
	 */
	/*TODO Liberty cluster mechanism TBD*/
	private static Counter getCounter(String logicalName){
		Counter counter = s_nextTransactionIdPerLogicalName.get( logicalName);
		if(counter == null){
			synchronized (s_nextTransactionIdPerLogicalName) {
				counter = new Counter();
				s_nextTransactionIdPerLogicalName.put( logicalName, counter);
			}
		}
		return counter;
	}

	/**
	 * use this after failover when a logicalName moved to a another server
	 * @param logicalName
	 * @param id
	 */
	/*TODO Liberty cluster mechanism TBD*/
	public static void setNextMaxTransactionId( String logicalName, long id){
		Counter counter = getCounter(logicalName);
		counter.setHigherCount( id);
	}

	/**
	 * Holds a sequential count state  
	 * @author Nitzan Nissim
	 */
	private static class Counter{
		private long _count = 0;

		/**
		 * Returns next sequential number
		 * @return
		 */
		public long getNextCount(){
			return _count++;
		}

		/**
		 * Setting new count only if new count is higher 
		 * @param count
		 */
		public void setHigherCount( long count){
			if( count > _count){
				_count = count;
			}
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		// Moti: 6/MAr/2008: removed lazy initialization of a private
		// member. it is way too expensive to save session to string in
		// a member.
		// it has been found a very expensive in terms of memory.
		// Over 100MB just in regular RTC GW dump with 100000 Sessions.

		StringBuffer myInformation = new StringBuffer(50);
		myInformation.append("Id = ");
		myInformation.append(getSharedId());
		myInformation.append(" Info = ");
		myInformation.append(super.toString());
		return myInformation.toString();
	}

	/**
	 * Returns the state of the Dialog.
	 * In the non dialog requests will return INITIAL  
	 * @return
	 */
	SipSessionImplementation.State getState() {
		return _dialogState.getState();
	}

	/**
	 * Sets the state of the relate dialog
	 * In the non dialog requests the state will be not set.
	 * @param _state
	 */
	public void setSessionState(SipSessionImplementation.State state, SipServletMessage sipMessage) {
		_dialogState.setSessionState(state, sipMessage);
	}

	/**
	 * Change the state to AFTER_INITIAL state
	 */
	void setStateToAfterInitial() {
		_dialogState.setStateToAfterInitial();
	}

	/**
	 * Returns true if state is AFTER_INITIAL
	 */
	boolean isAfterInitialState() {
		return _dialogState.isAfterInitialState();
	}

	/**
	 * Update SessionState according to 
	 * received response.
	 * Return state of the session.
	 * 
	 */
	SipSessionImplementation.State updateState(SipServletResponse response) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {getAppName(), getSharedId()};
			c_logger.traceEntry(this,
					"updateState", params);
		}
		//Moti: fixing defect 517772. when session state is changed by converged
		// application we should also trigger replication.
		// the big question here is how this will affect the overall sip 
		// container performance.
		SipSessionImplementation.State oldState = _dialogState.getState();
		SipSessionImplementation.State newState = _dialogState.updateState(response);
		if (oldState != newState ) {
			setDirty();
			store();
		}
		return newState;
	}

	/**
	 * Method which runs on Requests and checks if dialog state should
	 * be changed to TERMINATED (in case of dialog requests).
	 * Works for Incoming and Outgoing requests.
	 * @param msg
	 */
	void checkIfTerminateRequest(SipServletRequest request){
		_dialogState.checkIfTerminateRequest(request);
	}


	/**
	 * Helper method decides if dialog in the current state can be invalidated
	 * @return
	 */
	boolean canBeInvalidated() {
		return _dialogState.canBeInvalidated();
	}

	/**
	 * Returns Related SipSession
	 * @return
	 */
	public SipSession getRelatedSipSession() {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "getRelatedSipSession");
		}
		String relatedId = _tuWrapper.getRelatedSipSessionId();

		if(getApplicationId() != null){
			SipApplicationSessionImpl as = (SipApplicationSessionImpl)getSipApplicationSession();
			if( as == null){
				if (c_logger.isTraceEntryExitEnabled()) {
					c_logger.traceExit(this, "getRelatedSipSession - non found");
				}
				return null;
			}
			Iterator iter = getSipApplicationSession().getSessions();
			for (;iter.hasNext();) {
				SipSession session = (SipSession) iter.next();
				if(session.getId().equals(relatedId)){
					if (c_logger.isTraceEntryExitEnabled()) {
						c_logger.traceExit(this, "getRelatedSipSession - found: " + session);
					}
					return session;
				}
			}
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "getRelatedSipSession - non found");
		}
		return null;
	}

	/**
	 * Returns ID of related sipSession.
	 * @return
	 */
	public String getSipSessionId() {
		if (_sipSession != null) {
			return _sipSession.getId();
		}
		return null;
	}

	/**
	 * @return the current TuWrapper object
	 * @see DB_TuBaseMgr#remove
	 * @author mordechai
	 */
	public TransactionUserWrapper getWrapper()
	{
		return _tuWrapper;
	}



	/**
	 * Getting the session station synchronizing object
	 * @return
	 */
	public Object getSynchronizer() {
		return _synchronizer;
	}

	/**
	 * Getting the session station synchronizing object
	 * @return
	 */
	public Object getServiceSynchronizer() {
		return _serviceSynchronizer;
	}

	private SipApplicationSession getSipApplicationSession()
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "getSipApplicationSession",m_applicationId);
		}
		return SessionRepository.getInstance().getAppSession(getApplicationId());
	}


	/**
	 * Setting the session API synchronizing object
	 * @return
	 */
	private void setSynchronizer(Object synchronizer) {
		this._synchronizer = synchronizer;
	}

	/**
	 * Setting the session service synchronizing object
	 * @return
	 */
	private void setServiceSynchronizer(Object serviceSynchronizer) {
		this._serviceSynchronizer = serviceSynchronizer;
	}

	/**
	 * Helper method that returns Application name
	 * @return
	 */
	public String getAppName(){
		String appName = null;
		SipServletDesc desc = getSipServletDesc();
		if(desc != null){
			appName = getSipServletDesc().getName();
		}
		return " AppName = " + appName + " ";
	}

	/**
	 * Method for internal usage only to create a link between a TuBase and its SipSession.
	 * It is used mainly inside SIP - Objectgrid activation process.
	 * @param sess
	 */
	public void  setSipSession(SipSessionImplementation sess) {
		if (_sipSession != null && c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setSipSession",m_applicationId);
		}
		_sipSession = sess;
	}

	/**
	 * Set session key base attribute to be used when sip app session is created.
	 * @param keyBase
	 */
	public void setSessionKeyBase(String keyBase) {
		_sessionKeyBase = keyBase;
	}

	/**
	 * Set session key base attribute to be used when sip app session is created.
	 * @param keyBase
	 */
	public String getSessionKeyBase() {
		return _sessionKeyBase;
	}

	/**
	 * @see com.ibm.ws.sip.container.util.wlm.SipDialogContext#setDialogAux(com.ibm.ws.sip.container.util.wlm.DialogAux)
	 */
	protected void setDialogAux(DialogAux da){
		_dialogState.setDialogAux(da);
	}

	/**
	 * @see com.ibm.ws.sip.container.util.wlm.SipDialogContext#getDialogAux()
	 */
	protected DialogAux getDialogAux(){
		return _dialogState.getDialogAux();
	}

	/**
	 * @see com.ibm.ws.sip.container.util.wlm.SipDialogContext#getDialogState()
	 */
	protected int getWLMDialogState() {
		return _dialogState.getWLMDialogState();
	}
}
