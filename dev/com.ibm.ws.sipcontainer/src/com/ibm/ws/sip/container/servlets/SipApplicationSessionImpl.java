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
package com.ibm.ws.sip.container.servlets;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionAttributeListener;
import javax.servlet.sip.SipApplicationSessionBindingEvent;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipApplicationSessionListener;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.websphere.sip.IBMSipSession;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.events.ContextEstablisher;
import com.ibm.ws.sip.container.events.EventsDispatcher;
import com.ibm.ws.sip.container.failover.Replicatable;
import com.ibm.ws.sip.container.failover.ReplicatableImpl;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.container.timer.AppSessionTimerListener;
import com.ibm.ws.sip.container.timer.BaseTimer;
import com.ibm.ws.sip.container.timer.ExpirationTimer;
import com.ibm.ws.sip.container.timer.ServletTimerImpl;
import com.ibm.ws.sip.container.tu.TUKey;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

/**
 * @author Amir Perlman, Mar 20, 2003
 *
 * Implementation of Sip Application Session API.
 *
 */
public class SipApplicationSessionImpl extends ReplicatableImpl
implements SipApplicationSession {
	//Moti: no need to impl Serializable as ReplicatableImpl already does so.

	/** Serialization UID (do not change) */
	static final long serialVersionUID = 7746526996100284291L;

	/**
	 * The constant representing infinite expiration
	 */
	private static final int NO_EXPIRATION = -1;
	/**
	 * Class Logger.
	 */
	private static final transient LogMgr c_logger = Log
	.get(SipApplicationSessionImpl.class);
	
	/**
	 * Session's Expiration time in milliseconds. Use a default of 3 min per JSR 289 6.1.2.1. This setting should be
	 * override by setting from sip.xml configuration.
	 */
	private long m_expires = 3 * 1000 * 60;

	/**
	 * timer for upcoming expiration according to the m_expires member
	 */
	private transient AppSessionTimerListener m_expiresTimer;

	/**
	 * Last time this session was accessed.
	 */
	private long m_lastAccessedTime;

	/**
	 * Creation time.
	 */
	private long m_creationTime;

	/**
	 * The list of Transaction User Wrappers associated with this Application Session.
	 */
	//Moti: OG: correct me if I'm wrong but this variable can be set transient. 
	// it is really not needed for failover
	private transient List<TransactionUserWrapper> m_transactionUsers = null;

	/**
	 * The list of Timers associated with this Application Session
	 * moti: it  was called m_timers before.
	 */
	private Vector<Integer> m_timersIds;

	/**
	 * Moti: OG:
	 * old: Map of attributes associated with this application session.
	 * new: A list of attributes names
	 */
	private LinkedHashSet<String> m_attributes;

	/**
	 * Application's descriptor configuration from sip.xml.
	 */
	private transient SipAppDesc m_appDescriptor;

	/**
	 * ApplicationId that used in failover to receive SipAppDesc from Container
	 */
	private String _applicationName;

	/**
	 * Session Ids counter.
	 */
	private int m_nextTimerId = 1;


	/**
	 * Indicates whether the session is valid. Once a session is invalidated
	 * some operations will no longer be supported.
	 */
	private boolean m_isValid = true;

	/**
	 * Flag that indicates if the application is alive. Meaning that
	 * sendAppSessionCreatedNotification() called but no listeners were defined
	 * for this application
	 */
	private boolean m_applicationIsAlive = false;


	/**
	 * Table of all active SIP Application Sessions.
	 * @deprecated - Moti: use repository
	 */
	//private static transient Map c_appSessions = new Hashtable();

	/**
	 * Constant used in encoded URI to identify a specific application session.
	 * The information will be added as a parameter to the URI and its value
	 * will be mapped to a SessionID of a specific SIP Application Session instance
	 */
	public final static String ENCODED_APP_SESSION_ID = "ibmappid";

	/**
	 * Indicates whether the session in the middle of the invalidation
	 * process.
	 */
	private transient boolean m_duringInvalidate = false;

	/**
	 * Each SIP application session ID contains also internal sequential counter.
	 * We use this counter for queuing a message into SIP container's queues.
	 * look at Queueable#getQueueIndex() 
	 */
	private transient int  m_extractedAppSessionSeqCounter = -1;

	/**
	 * Member that Developer can set to define that this ApplicationSession
	 * should be invalidated by the Container when state of all related sessions 
	 * will become TERMINATED or INITIAL (if not a dialog).
	 */
	private boolean m_invalidateWhenReady = false;


	/**
	 * This member will contain session key base value taken from the TU object when 
	 * this object is created. This value will be used to remove this value from the
	 * repository.
	 * 
	 */
	private String m_sessionKeyBaseKey = null;

	/**
	 * A locking handle that is used to synchronize all API methods of objects related to this ApplicationSession 
	 */
	private Object _synchronizer;

	/**
	 * A locking handle that is used to synchronize the application service methods
	 * in case we use a direct thread execution model (not hashing tasks to queues per appsessions)
	 */
	private Object _serviceSynchronizer;

	/**
	 * Default constructor for activation after failover
	 */
	SipApplicationSessionImpl(){
		super();
	}
	/**
	 * Construct a new Sip Application Session that is not associated with any
	 * Sip Session
	 *
	 * @param sipSession
	 */
	SipApplicationSessionImpl(String sessionID) {
		m_creationTime = System.currentTimeMillis();
		m_lastAccessedTime = m_creationTime;
		/*TODO Liberty cluster mechanism TBD*/
		String srvrid = sessionID.substring( 0,sessionID.indexOf( ID_INTERNAL_SEPERATOR));

		setSharedId(sessionID);
		setServerID(srvrid);
		PerformanceMgr.getInstance().incrementNotReplicatedSipAppSessionsCounter();
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "SipApplicationSessionImpl",
					"New ApplicationSession id = " + getId());
		}
	}

	/**
	 * Add this ApplicationSession to the Application Sessions table
	 *
	 */
	public void addToApplicationSessionsTable(){
		SessionRepository.getInstance().put(this);
	}


	/**
	 * Create and schedule SipApplicationSession timer
	 *
	 * @param delay
	 */
	public void createSipAppTimer(long delay,ExpirationTimer expTimer) {
		m_expiresTimer = new AppSessionTimerListener(this,expTimer);
		if(expTimer == null){
			//			Meaning that this timer wasn't forwarded from the TransactionUser because in the
			//			Transaction User timer was never set
			m_expiresTimer.schedule(false,delay);
		}
	}

	/**
	 * @see javax.servlet.sip.SipApplicationSession#getCreationTime()
	 */
	public long getCreationTime() {
		checkIsSessionValid();
		return m_creationTime;
	}

	/**
	 * @see javax.servlet.sip.SipApplicationSession#getLastAccessedTime()
	 */
	public long getLastAccessedTime() {
		return m_lastAccessedTime;
	}

	/**
	 * This method will be invoked every time that this SipApplication
	 * Session accessed by any of it's contained SipSessions.
	 *
	 */
	public void setLastAccessedTime(){
		if(c_logger.isTraceDebugEnabled()){
			c_logger.traceEntry(this, "setLastAccessedTime");
		}
		m_lastAccessedTime = System.currentTimeMillis();

		store();
	}

	/**
	 * Get next ID for new timer
	 *
	 * @return
	 */
	public synchronized int getNextTimerId() {
		return m_nextTimerId++;
	}

	/**
	 * Sets expiration for this ApplicationSession as it was set at the beginning by the R
	 * TransactionUser
	 * @param deltaMinutes
	 */
	public void setExpirationFromTU(long expirationTime){
		//As TransactionUser was created first also Timer was created for this TransactionUser
		// when SipApplicationSession created time moved to be SipApplication's timer
		// the m_expires should be set as well
		m_expires = expirationTime;
		//Note we don't replicate yet because this method will be followed by an addTransactionUser method,
		//where we might replicate
	}

	/**
	 * @see javax.servlet.sip.SipApplicationSession#setExpires(int)
	 */
	public int setExpires(int deltaMinutes) {
		synchronized (getSynchronizer()) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setExpires", "deltaMinutes = " + deltaMinutes);
			}

			//check if the session is valid
			checkIsSessionValid();

			if (deltaMinutes < 1) {
				//this means the session will never expire, previously created timer is cancelled.
				if(m_expiresTimer != null){
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "setExpires",
								"cancel the existing epiration timer - session will not expired" + this);
					}	
					m_expiresTimer.cancel();
					m_expires = NO_EXPIRATION;
					m_expiresTimer = null;
					deltaMinutes = Integer.MAX_VALUE;
				}
			}else{
				//the timer should be created only for a session with an expiration time that is above 1 (inclusive).
				long deltaMilis = deltaMinutes * 1000 * 60;
				long expires = System.currentTimeMillis() + deltaMilis;

				//if a timer doesn't exist, create one
				if(m_expiresTimer == null){
					//If m_expiresTimer is null means that application timer was never set
					if (c_logger.isTraceDebugEnabled()) {
						StringBuffer buffer = new StringBuffer(96);
						buffer.append("AppSessionId: ");
						buffer.append(getId());
						buffer.append(" Prev m_expires was = ");
						buffer.append(m_expires);
						buffer.append(" Timer was set by application for = ");
						buffer.append(deltaMinutes);
						buffer.append(" minutes...");
						c_logger.traceDebug(this, "setExpires", buffer.toString());
					}
					m_expires = expires;
					createSipAppTimer(deltaMilis,null);
				}
				//if a timer exists, reschedule it
				else{
					boolean reschedule = expires < m_expires;
					m_expires = expires;
					if (reschedule) {
						// if expiration has decreased, it must be rescheduled explicitly.
						m_expiresTimer.rescheduleAppSessionTimer();
					}
				}
			}
			if(c_logger.isTraceEntryExitEnabled() ){
				c_logger.traceExit(this,"setExpires",deltaMinutes);
			}


			store();

			return deltaMinutes;
		}
	}

	/**
	 * Destroy all related timers
	 * @throws SIPReplicationException
	 */
	private void destroyAllTimers()
	{
		if (m_timersIds == null || m_timersIds.isEmpty()) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this,"destroyAllTimers",
						"No timers were found for SipApplicationSession Id = "
						+ getId());
			}
			return;
		}

		//Moti: OG:
		Vector<Integer> timersClone = (Vector<Integer>)m_timersIds.clone();
		for (int i = 0 ; i < timersClone.size() ; i++) {
			Integer timerId = (Integer)timersClone.get(i);
			ServletTimerImpl timer = (ServletTimerImpl)SessionRepository.getInstance().getTimer(getId(), timerId);
			if (timer != null)
			{
				timer.cancel();
			}
		}

		if (!(m_timersIds.size() == 0))
		{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "destroyAllTimers",
				"SipApplicationSession Timer queue wasn't fully cleaned");
			}
		}

	}

	/**
	 * invalidates all sessions belonging to this application session
	 * destroys all related SipSessions and TransactionUser objects
	 */
	private void destroyAllSessions() {
		if(c_logger.isTraceEntryExitEnabled() ){
			c_logger.traceEntry(this,"destroyAllSessions");
		}

		if (m_transactionUsers == null) {
			return;
		}
		// 1. make a copy of m_transactionUsers, and delete the original
		TransactionUserWrapper[] transactionUsers;
		synchronized (m_transactionUsers) {
			transactionUsers = copyTransactionUsers();
			m_transactionUsers.clear();
		}
		if (transactionUsers == null) {
			return;
		}

		// 2. work on the copy to invalidate all sessions
		int nTransactionUsers = transactionUsers.length;
		//Moti: defect 571056 always added the same key, since its source in thread local storage.
		List<TUKey> tuKeyList = new ArrayList<TUKey>(nTransactionUsers);
		List<String> tuIdList = new LinkedList<String>();
		try {
			for (int iTransactionUsers = 0; iTransactionUsers < nTransactionUsers; iTransactionUsers++) {
				TransactionUserWrapper tu = transactionUsers[iTransactionUsers];
				if (tu.isWaitingForPendingMessage()) {
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "destroyAllSessions", "Session belongs to an Encode URI/Key base targeting message that wasn't processed, skipping invalidation till we process the message.");
					}
					continue;
				}

				if (tu.isTransactionUserInvalidated()){
					//tuImpl does not exist on the tuwrapper, this can happen after replication if only the tuBase was replicated
					//in this case we need to remove the wrapper from the tables according to the id and not the key
					//since it is only exist in the session repository tables according to the id
					tuIdList.add(tu.getId());
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "destroyAllSessions", "TranscationUser did not have tuImpl object, will be removed accroding to id, id=" + tu.getId());
					}
				}else{
					TUKey key = tu.resetTempTUKeyValues();
					if (nTransactionUsers > 1) {
						tuKeyList.add((TUKey)key.clone()); // we must not rely on thread local storage TUKey since
						//we must not rely on thread local storage TUKey since
					} else {
						tuKeyList.add(key); 
					}
				}

				// its being changed constantly by other methods.
				tu.invalidateTU(false,true);
				tu.applicationSessionIvalidated(this);
				// we shall not remove the TuWrapper from repository now. read defect 513136.1
			}
		} catch (CloneNotSupportedException e) {
			//Catch all exception, alert the user and continue normal processing.
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.exception", Situation.SITUATION_CREATE,
						null, e);
			}
		}

		//Moti: defect 513136.1 trying to delay the TuWrapper removal from repository
		// for cases where two SIP session's attributes reference each other.
		for (TUKey key: tuKeyList) {
			SessionRepository.getInstance().removeTuWrapper(key,true);
		}

		//go over the tu objects that does not have tuImpl and remove them
		for (String id : tuIdList) {
			SessionRepository.getInstance().removeTuWrapper(id);
		}

		if(c_logger.isTraceEntryExitEnabled() ){
			c_logger.traceExit(this,"destroyAllSessions");
		}
	}

	/**
	 * @return a copy of the transaction user list, null if no transaction users
	 */
	private TransactionUserWrapper[] copyTransactionUsers() {
		if (m_transactionUsers == null) {
			return null;
		}
		int nTransactionUsers = m_transactionUsers.size();
		if (nTransactionUsers == 0) {
			return null;
		}
		// make a copy, and work on it outside the sync block
		// todo pool this array?
		TransactionUserWrapper[] transactionUsers = new TransactionUserWrapper[nTransactionUsers];
		for (int iTransactionUsers = 0; iTransactionUsers < nTransactionUsers; iTransactionUsers++) {
			TransactionUserWrapper tu = m_transactionUsers.get(iTransactionUsers);
			transactionUsers[iTransactionUsers] = tu;
		}
		return transactionUsers;
	}

	/**
	 * @see javax.servlet.sip.SipApplicationSession#invalidate()
	 */
	public void invalidate() {
		synchronized (getSynchronizer()) {
			if(m_duringInvalidate) {
				checkIsSessionValid();
				return;
			}

			m_duringInvalidate = true;

			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "invalidate", "AppSessionId: " + getId());
			}

			if(m_expiresTimer!= null){
				m_expiresTimer.cancel();
			}

			//	Notify performance manager about destroyed SipAppSession
			PerformanceMgr perfMgr = PerformanceMgr.getInstance();
			if (perfMgr != null) {
				perfMgr.sipAppSessionDestroyed(m_appDescriptor.getApplicationName(),
					m_appDescriptor.getAppIndexForPmi());
				perfMgr.decrementNotReplicatedSipAppSessionsCounter();
			}
			//Notify listeners that App Session has been destroyed.
			m_applicationIsAlive = false;
			try{
				sendAppSessionNotification(LstNotificationType.APPLICATION_DESTROYED);
			} catch (Throwable t) {
				if (c_logger.isErrorEnabled()) {
					c_logger.error("invalidate", Situation.SITUATION_REMOVE,
							"Exception while calling application", t);
				}			
			}

			//Get the list of sessions and timers before invalidating, otherwise the call
			//will generate an illegal state exception.
			try {	
				destroyAllTimers();
			} catch (Throwable t) {
				if (c_logger.isErrorEnabled()) {
					c_logger.error("invalidate", Situation.SITUATION_REMOVE,
							"Exception while removing timers", t);
				}			
			}		

			try {
				destroyAllSessions();
			} catch (Throwable t) {
				if (c_logger.isErrorEnabled()) {
					c_logger.error("invalidate", Situation.SITUATION_REMOVE,
							"Exception while invalidating sessions", t);
				}			
			}

			// removing reference to the session key base data
			if (m_sessionKeyBaseKey != null) {
				SessionRepository.getInstance().removeKeyBaseAppSession(m_sessionKeyBaseKey);
			}

			removeFromStorage(); //will also remove all attributes.

			m_attributes = null;
			m_isValid = false;

			if(SipContainer.getInstance().isInQuiesce()){
				List<SipApplicationSessionImpl> totalAppSessions = SessionRepository.getInstance().getAllAppSessions();
				if (totalAppSessions.isEmpty()) {
					if(c_logger.isInfoEnabled()){
						c_logger.info("info.sip.container.quiesce.ended", null);
					}
				}

				if (c_logger.isTraceDebugEnabled()) {
					StringBuffer b = new StringBuffer(64);
					b.append("Sip Application Session Invalidated, Id: ");
					b.append(getId());
					b.append(" , ");
					b.append(this);

					c_logger.traceDebug(this, "invalidate", b.toString());

				}
			}

			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(this, "invalidate", "AppSessionId: " + getId());
			}
		}
	}

	/**
	 * Removing all attributes from replication domain, if exists
	 */
	private void removeAttributesFromStorage(){

		Set<String> attr = m_attributes;

		if(attr == null || attr.isEmpty()){
			return;
		}

		synchronized(attr){
			Object[] attrArr = attr.toArray();
			for( int i = 0; i < attrArr.length; i++)
			{
				String name = (String)attrArr[i];
				removeAttribute(name);
			}
		}
	}

	/**
	 * @see javax.servlet.sip.SipApplicationSession#getSessions()
	 */
	public Iterator<IBMSipSession> getSessions() {
		synchronized (getSynchronizer()) {
			checkIsSessionValid();

			// As a fix to SPR #MGIX6DUHDC we will clone the  sipSessions iterator
			// for external use of it.
			List<IBMSipSession> allSessions = getAllSIPSessions();
			//some code here was removed by Moti. 10/June/2007. code moved to getAllSIPSessions()

			return allSessions.iterator();
		}
	}

	/**
	 * Return all of the sip sessions stored inside the current active
	 * user transactions. If those objects doesn't exist the container will
	 * allocate new Sip sessions.
	 *  
	 * @return
	 */
	protected List<IBMSipSession> getAllSIPSessions() {
		return getAllSIPSessions(true);
	}


	/**
	 * all the SIP session that are stored inside the current active
	 * user transactions.
	 * 
	 * @param create create sip sessions of those sessions doesn't exists.
	 */
	protected List<IBMSipSession> getAllSIPSessions(boolean create)
	{
		synchronized (getSynchronizer()) {
			// a special method added by moti to fix defect 443590
			if( m_transactionUsers == null || m_transactionUsers.size() == 0){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getAllSIPSessions", "no SIP session found. return empty list.");
				}
				return Collections.EMPTY_LIST;
			}
			int size = m_transactionUsers.size();

			//Moti: we create new ArrayList to prevent later any modification
			// to the iterator on that list.
			List<IBMSipSession> result = new ArrayList<IBMSipSession>(size); //approximation only.
			synchronized (m_transactionUsers) {
				TransactionUserWrapper tu = null;
				for (int i = 0 ; i < size ; i++) {
					tu = m_transactionUsers.get(i);
					result.addAll(tu.getAllSipSessions(create));
				}
			}
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getAllSIPSessions", "found SIP sessions. count:"+result.size());
			}

			return result;
		}
	}


	/**
	 * @see javax.servlet.sip.SipApplicationSession#getSessions(java.lang.String)
	 */
	public Iterator<IBMSipSession> getSessions(String protocol) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry("SipApplicationSessionImpl", "getSessions("+protocol);
		}
		checkIsSessionValid();
		if (protocol.equalsIgnoreCase("SIP")) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("SipApplicationSessionImpl","getSessions", "getSessions(SIP) detected");
			}
			//Moti 24/Sep: here is a bug in PMR 46156,033,00 : should call getAllSipSessions
			// if we call  return getSessions() here we might accidently activate derived class' method
			// defect PK54754
			return getAllSIPSessions().iterator();
		}
		else if (protocol.equalsIgnoreCase("HTTP")){
			return Collections.EMPTY_MAP.keySet().iterator();
		}
		else{

			SipAppDesc sipAppDesc = getAppDescriptor();
			if(sipAppDesc != null && !sipAppDesc.isJSR289Application()){
				return Collections.EMPTY_MAP.keySet().iterator();				
			}

			throw new IllegalArgumentException("Unsupported protocol type " + protocol);
		}

	}

	/**
	 * @see javax.servlet.sip.SipApplicationSession#getSipSession(java.lang.String)
	 * @throws NullPointerException - Throw a null pointer exception when id is null
	 */
	public SipSession getSipSession(String id){
		checkIsSessionValid();

		// defect 484586 - Per the JSR 289 API Doc, should throw a NPE when 
		// passed in a null argument 
		if(id == null){
			throw new NullPointerException("The SipSession ID is null");
		}

		SipSession result = null;
		if( m_transactionUsers == null || m_transactionUsers.size() == 0){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getAllSIPSessions", 
				"no SIP session found.");
			}
			new NullPointerException("No Session found for ID = " + id);
		}
		else{
			int size = m_transactionUsers.size();
			synchronized (m_transactionUsers) {
				TransactionUserWrapper tu = null;
				List<IBMSipSession> allSessions = null;
				for (int i = 0 ; i < size && result == null ; i++) {
					// go over each TransactionUser and get all related SipSessions
					tu = m_transactionUsers.get(i);
					allSessions = tu.getAllSipSessions();
					// Check each SipSession.
					if(allSessions != null && allSessions.size() != 0){
						for (Iterator<IBMSipSession> iter = allSessions.iterator(); iter.hasNext();) {
							IBMSipSession session = iter.next();
							if(session.getId().equals(id)){
								result = session;
							}
						}						
					}
				}
			}
		}
		return result;
	}

	/**
	 * @see javax.servlet.sip.SipApplicationSession#encodeURI(javax.servlet.sip.URI)
	 */
	public void encodeURI(URI uri) {
		checkIsSessionValid();

		if (uri.isSipURI()) {
			((SipURI) uri).setParameter(ENCODED_APP_SESSION_ID, getId());
			setDirty();
		}
		else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "encodeURI",
						"Can not encode URI, Not a SIP URI " + uri);

			}
			throw new IllegalArgumentException(
			"Not a SIP a URI, Can not encode session information");
		}
		
		store();
	}

	/**
	 * @see javax.servlet.sip.SipApplicationSession#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String name) {
		checkIsSessionValid();
		LinkedHashSet attr = m_attributes;
		if(attr == null){
			return null;
		}
		if (!attr.contains(name)) {
			return null;
		}
		return SessionRepository.getInstance().getSASAttr(this, name);
	}

	/**
	 * @see javax.servlet.sip.SipApplicationSession#getAttributeNames()
	 */
	public Iterator getAttributeNames() {
		checkIsSessionValid();

		LinkedHashSet attr = m_attributes;

		if(attr == null){
			return Collections.EMPTY_MAP.keySet().iterator();
		}

		return attr.iterator();
	}

	/**
	 * @see javax.servlet.sip.SipApplicationSession#setAttribute(java.lang.String,
	 *      java.lang.Object)
	 */
	public void setAttribute(String name, Object attribute) {
		synchronized (getSynchronizer()) {
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceEntry(this, "setAttribute",name);
			}
			checkIsSessionValid();
			if( attribute == null || name == null){
				throw new NullPointerException("SipApplicationSession#setAttribute: attribute name or value cannot be null");
			}
			
			if (m_appDescriptor.isDistributed()
					&& attribute instanceof Serializable == false) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "setAttribute",
							"Attribute must implement Serializable ... attrName = " + name + this);
				}
				throw new IllegalArgumentException(
						"Attribute not Serializable: Attribute name=" + name + ", Attribute=" + attribute);
			}

			Object prevValue = null;
			//Moti: OG:
			LinkedHashSet<String> attr =  m_attributes;
			if(attr == null){
				if(attr == null && !m_duringInvalidate){
					m_attributes = attr = new LinkedHashSet<String>();
				}
				else if(m_duringInvalidate){
					throw new IllegalStateException("Invalid Session: " + this );
				}
			}

			attr.add(name);
			setDirty();
			prevValue = SessionRepository.getInstance().putSASAttr(this, name, attribute);
			if(prevValue!=null){
				sendAttributeNotification(name,LstNotificationType.APP_ATTRIBUTE_REPLACED);
			}
			else{
				sendAttributeNotification(name,LstNotificationType.APP_ATTRIBUTE_ADDED);
			}
			EventsDispatcher.AppSessionAttributeBounding(this, name, m_appDescriptor, true);
		}

	}

	/**
	 * Send event to Applications listener. This is a common method used  to
	 * notify listeners.
	 * @param listeners
	 * @param evt
	 * @param attrNotifyType
	 */
	private <T extends EventListener>  void sendEvent( Collection<T> listeners,
			Object evt,
			LstNotificationType attrNotifyType) {
		Iterator iter = listeners.iterator();

		ContextEstablisher contextEstablisher = 
			m_appDescriptor.getContextEstablisher();

		ClassLoader currentThreaClassLoader = null;

		try {
			if (contextEstablisher != null) {
				currentThreaClassLoader = contextEstablisher
				.getThreadCurrentClassLoader();
				contextEstablisher.establishContext();
			}
			while (iter.hasNext()) {
				switch (attrNotifyType) {
				case APP_ATTRIBUTE_ADDED:
					((SipApplicationSessionAttributeListener) iter.next())
					.attributeAdded((SipApplicationSessionBindingEvent)evt);
					break;	
				case APP_ATTRIBUTE_REPLACED:
					((SipApplicationSessionAttributeListener) iter.next())
					.attributeReplaced((SipApplicationSessionBindingEvent)evt);
					break;
				case APP_ATTRIBUTE_REMOVED:
					((SipApplicationSessionAttributeListener) iter.next())
					.attributeRemoved((SipApplicationSessionBindingEvent)evt);
					break;
				case APPLICATION_CREATED:
					((SipApplicationSessionListener) iter.next())
					.sessionCreated((SipApplicationSessionEvent)evt);
					break;

				case APPLICATION_DESTROYED:
					((SipApplicationSessionListener) iter.next())
					.sessionDestroyed((SipApplicationSessionEvent)evt);
					break;

				case APPLICATION_EXPIRED:
					((SipApplicationSessionListener) iter.next())
					.sessionExpired((SipApplicationSessionEvent)evt);
					break;
				default:
					break;
				}
			}
		} 
		finally {
			if (contextEstablisher != null) {
				contextEstablisher.removeContext(currentThreaClassLoader);
			}
		}
	}

	/**
	 * Send a attribute Added notification to Attributes Listeners.
	 */
	public void sendAttributeNotification(String attrName,
			LstNotificationType attrNotifyType) {

		if (c_logger.isTraceEntryExitEnabled()) {
			StringBuffer buff = new StringBuffer(attrName);
			buff.append(attrNotifyType);
			c_logger.traceEntryExit(this, "sendAttributeAddedNotify", buff
					.toString());
		}

		if (null == m_appDescriptor) {
			//Sip Application Sessions created with the factory will not
			//have an app description associated with them.
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer buff = new StringBuffer(100);
				buff.append("Unable to send Attribute Added notification, ");
				buff.append("sessionAppId = ");
				buff.append(getId());
				buff.append(" No Application Descriptor");
				c_logger.traceDebug(this, "sendAttributeRemovedNotify",
						buff.toString());
			}
			return;
		}   
		Collection<SipApplicationSessionAttributeListener> listeners = m_appDescriptor.getAppSessionAttributesListeners();

		if (!listeners.isEmpty()) {
			SipApplicationSessionBindingEvent evt = 
				new SipApplicationSessionBindingEvent(this, attrName);
			sendEvent(listeners,evt,attrNotifyType);
		}
	}


	/**
	 * Send a session destroyed notification to App Session Listeners.
	 *
	 */
	public void sendAppSessionNotification(LstNotificationType type) {
		if (null == m_appDescriptor) {
			//Sip Application Sessions created with the factory will not
			//have an app description associated with them.
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer buff = new StringBuffer(100);
				buff.append("Unable to send sendAppSessionNotification ");
				buff.append("sessionAppId = ");
				buff.append("Notification type = ");
				buff.append(type);
				buff.append(getId());
				buff.append(" No Application Descriptor");
				c_logger.traceDebug(this, "sendApplicationSessionCreatedNotification",
						buff.toString());
			}
			return;
		}

		Collection<SipApplicationSessionListener> listeners = m_appDescriptor.getAppSessionListeners();

		if (!listeners.isEmpty()) {
			SipApplicationSessionEvent evt = 
				new SipApplicationSessionEvent(this);

			sendEvent(listeners, evt,type);
		}
	}

	/**
	 * @see javax.servlet.sip.SipApplicationSession#removeAttribute(java.lang.String)
	 */
	public void removeAttribute(String name) {
		synchronized (getSynchronizer()) {
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceEntry(this, "removeAttribute",name);
			}
			if(m_appDescriptor != null && m_appDescriptor.isJSR289Application()){
				//TODO this is temp if just to pass regression until it is fixed
				checkIsSessionValid();
			}

			Set<String> attrMap = m_attributes;
			if (attrMap == null || !attrMap.remove(name)) {
				if (c_logger.isTraceEntryExitEnabled()) {
					c_logger.traceExit(this,"removeAttribute",name+" wasn't found. request ignored. " + this);
				}
				return;
			}

			EventsDispatcher.AppSessionAttributeBounding(this, name, m_appDescriptor, false);

			Object attr = SessionRepository.getInstance().removeAttribute(this , name);
			if( attr == null){
				if (c_logger.isTraceEntryExitEnabled()) {
					c_logger.traceExit(this,"removeAttribute",name+" not removed, not found in SessionRepository " + this);
				}
				return;
			}

			sendAttributeNotification(name,LstNotificationType.APP_ATTRIBUTE_REMOVED);
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this,"removeAttribute",name+" calling to unbound, if attribute is a listener. " +
						attr);
			}

			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(this, "removeAttribute",name);
			}
		}

	}

	/**
	 * Gets the application descriptor obtained from the sip.xml file associated
	 * with this app.
	 *
	 * @return
	 */
	public SipAppDesc getAppDescriptor() {
		return m_appDescriptor;
	}

	/**
	 * Add a Sip session to the list of session associated with this application
	 * session.
	 *
	 * @param session
	 */
	public void addSipSession(SipSession session) {
		setLastAccessedTime();
	}

	/**
	 * Add a Sip session to the list of session associated with this application
	 * session.
	 *
	 * @param tu TransactionUserWrapper
	 * @param tuState The state is needed because this app session
	 * must be replicated if a confirmed dialog was assigned to it
	 *
	 */
	public void addTransctionUser(TransactionUserWrapper tu) {
		if( m_transactionUsers == null){
			m_transactionUsers = new ArrayList<TransactionUserWrapper>(2);
		}
		synchronized (m_transactionUsers){
			m_transactionUsers.add(tu);
		}

		// move session key base key from TU and store it localy
		// within the SipApplicationSession object. This is needed
		// to allow this connection to be removed on invalidate()
		String sessionKey = tu.getSessionKeyBaseKey();
		if (sessionKey != null) {
			if (m_sessionKeyBaseKey != null && !m_sessionKeyBaseKey.equals(sessionKey)) {
				if(c_logger.isErrorEnabled()){
					c_logger.error("Multiple session key based key old key = '" + m_sessionKeyBaseKey + "' new key = '" + sessionKey + "'. new value ignored");
				}
			} else {
				m_sessionKeyBaseKey = sessionKey;
				tu.setSessionKeyBase(null);				
			}
		}
	}

	/**
	 * Removes a Sip session from the list of session associated with this
	 * application session.
	 *
	 * @param session
	 */
	public void removeTransactionUser(TransactionUserWrapper tu) {
		if(m_transactionUsers == null){
			if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("SipApplicationSessionImp.removeTransactionUser: trying to remove nonexisting TU: " + tu);
			}
			return;
		}

		synchronized (m_transactionUsers) {
			m_transactionUsers.remove(tu);
		}
	}

	/**
	 * Set the application descriptor of the SIP Application associated with
	 * this SIP Session.
	 *
	 * @param desc
	 * @param setExpiration false if this ApplicationSession got its expiration timer from the
	 * Transaction user (i.e. it was created using the getApplicationSession method).
	 * this is true otherwise (i.e. created from factory)
	 */
	public void setSipApp(SipAppDesc desc, boolean setExpiration) {
		_applicationName = desc.getApplicationName().toString();
		if(desc.isJSR289Application()){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setSipApp",
				"this is 289 app - m_invalidateWhenReady is true");
			}
			m_invalidateWhenReady = true;
		}

		/*
		 * Meaning that this SipApplicationSession just created
		 * and we need to send ApplicationSessionCreatedNotification
		 * But first we should set the AppDescriptor
		 */
		boolean isSASJustNowCreated = (m_appDescriptor == null);
		m_appDescriptor = desc;
		notifyPerfMgrOnNewSAS();

		if (isSASJustNowCreated == true) {
			//a new app session has been created. notify listeners.
			sendAppSessionNotification(LstNotificationType.APPLICATION_CREATED);
		}

		if( setExpiration){
			// Now that we have the app descriptor we can set the session's
			// expiration
			// time according the app's configuration
			int sessionTimeout = m_appDescriptor.getAppSessionTTL();
			if (sessionTimeout < 1) {
				// Meaning by sip.xml that this SipAppliationSessin
				// will be never time-out
				m_expires = NO_EXPIRATION;

				if (c_logger.isTraceDebugEnabled()) {
					StringBuffer buff = new StringBuffer();
					buff.append("SipApplicationId = [");
					buff.append(getId());
					buff.append("]");
					buff.append("WARNING: According to sip.xml settings, this ApplicationSession will never expire. The session must be explicitly invalidated when dialog finished, or else it will stay in memory forever!");
					c_logger.traceDebug(this, "setSipApp", buff.toString());
				}
			}else{
				setExpires(sessionTimeout);
			}
		}


		if (c_logger.isTraceDebugEnabled()) {
			StringBuffer buff = new StringBuffer();
			buff.append("SipApplicationId = [");
			buff.append(getId());
			buff.append("]");
			buff.append("isDistributed = [");
			buff.append(desc.isDistributed());
			buff.append("]");
			c_logger.traceDebug(this, "setSipApp", buff.toString());
		}

		store();

		if(c_logger.isTraceEntryExitEnabled() ){
			c_logger.traceExit(this,"setSipApp");
		}
	}

	/**
	 * Send a session created notification to App Session Listeners.
	 *
	 */
	public void notifyPerfMgrOnNewSAS() {

		if (null == m_appDescriptor) {
			//Sip Application Sessions created with the factory will not
			//have an app description associated with them.
			return;
		}
		PerformanceMgr perfMgr = PerformanceMgr.getInstance();
		if (perfMgr != null) {
			perfMgr.sipAppSessionCreated(m_appDescriptor.getApplicationName(),
				m_appDescriptor.getAppIndexForPmi());
		}

	}

	/**
	 * Gets the Session's Expiration time.
	 *
	 * @return
	 */
	public long getExpires() {
		return m_expires;
	}

	/**
	 * @see javax.servlet.sip.SipApplicationSession#isValid()
	 */
	public boolean isValid() {
		return m_isValid;
	}

	/**
	 * @see javax.servlet.sip.SipApplicationSession#getTimers()
	 */
	public Collection getTimers() {
		ArrayList<BaseTimer> timersCollection = null;

		synchronized (getSynchronizer()) {
			checkIsSessionValid();
			if(m_timersIds == null)
			{
				return Collections.EMPTY_LIST;
			}
			//Moti: OG:
				int size = m_timersIds.size();
				timersCollection = new ArrayList();
				for(int i = 0 ; i < size ; i++)
				{
					Integer timerId = m_timersIds.get(i);
					BaseTimer currentTimer = SessionRepository.getInstance().getTimer(getId(),timerId);
					if (currentTimer != null) {
						timersCollection.add(currentTimer);
					} else {
						if(c_logger.isTraceDebugEnabled()){
							c_logger.traceDebug(this,"getTimers"," check flow. Timer value was missing in repository.");
						}
					}
				}
		}

		return timersCollection;
	}
	/**
	 * Looks up a SIP Application Session associated with the given Id.
	 *
	 * @param id
	 * @return The Sip Application Session if available otherwise null.
	 */
	public static SipApplicationSessionImpl getAppSession(String id) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry("SipApplicationSessionImpl", "getAppSession",id);
		}
		SipApplicationSessionImpl appSession = (SipApplicationSessionImpl)
		SessionRepository.getInstance().getAppSession(id);

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit("SipApplicationSessionImpl", "getAppSession: "
					+ appSession);
		}

		return appSession;
	}


	/**
	 * Checks if the Session is valid.
	 *
	 * @throws IllegalStateException
	 *             In case the session is already invalidated.
	 */
	private void checkIsSessionValid() throws IllegalStateException {
		if (!m_isValid) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger
				.traceDebug(
						this,
						"checkIsSessionValid",
						"Session is invalid, Operation not allowed,  "
						+ this);

			}

			throw new IllegalStateException("Invalid Session: " + this );
		}
	}

	/**
	 * Add a timer to the list of timers associated with this application
	 * session.
	 *
	 * @param timer
	 */
	public void addTimer(ServletTimerImpl timer) {
		synchronized (getSynchronizer()) {

			if(c_logger.isTraceEntryExitEnabled()){
				c_logger.traceEntry( this, "addTimer", timer);
			}

			if(m_timersIds == null)
			{
				m_timersIds = new Vector<Integer>(2);
			}

			m_timersIds.addElement(timer.getTimerId());
			setDirty();
			SessionRepository.getInstance().put(this, timer);

			store();
			
			timer.store();

			if(c_logger.isTraceEntryExitEnabled()){
				c_logger.traceExit( this, "addTimer");
			}
		}
	}

	/**
	 * Remove timer in case it was Cancelled()
	 * @param timer
	 */
	public void removeTimer(ServletTimerImpl timer) {
		synchronized (getSynchronizer()) {

			if(c_logger.isTraceEntryExitEnabled()){
				c_logger.traceEntry( null, "removeTimer", timer);
			}
			if(m_timersIds == null || m_timersIds.isEmpty())
			{
				if(c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug(this,"removeTimer"," empty timer list. check flow.");
				}
				return;
			}

			SessionRepository.getInstance().removeTimer(getId(), timer);
			m_timersIds.removeElement(timer.getTimerId());
			store();

			if(getInvalidateWhenReady() && isReadyToInvalidate()){
				readyToInvalidate();
			}

			if(c_logger.isTraceEntryExitEnabled()){
				c_logger.traceExit( null, "removeTimer", timer);
			}
		}

	}

	/**
	 * @return Returns the m_applicationId.
	 */
	public String getApplicationId() {
		return _applicationName;
	}

	/**
	 * @return Returns the m_applicationIsAlive.
	 */
	public boolean isApplicationAlive() {
		return m_applicationIsAlive;
	}

	/**
	 * Notify listeners that AppSession activated or passivated
	 * @param activation if true notification is for activation otherwise for passivation
	 */
	public void notifyOnActivationOrPassivation( boolean activation){
		if (null != m_appDescriptor) {
			if( activation){
				EventsDispatcher.AppSessionActivated(this);
			}else{
				EventsDispatcher.AppSessionWillPassivate(this);
			}
		} else {
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer buff = new StringBuffer(100);
				buff.append("Unable to send Application Session");
				if(activation){
					buff.append(" activation");
				}
				else{
					buff.append(" passivation");
				}
				buff.append(" notification, SIP app descriptor not available");
				buff.append("sessionId = ");
				buff.append(getId());
				c_logger.traceDebug(this, "sendSessionActivateNotification",
						buff.toString());
			}
		}
	}

	/**
	 * @see com.ibm.ws.sip.container.failover.Replicatable#store()
	 */
	public void store() {
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this,"store");
		}

		SessionRepository.getInstance().put(this);
	}

	/**
	 * @see javax.servlet.sip.SipApplicationSession#getId()
	 */
	public String getId() {
		return getSharedId();
	}


	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer myInformation = new StringBuffer();
		myInformation.append("Id = ");
		myInformation.append(getSharedId());
		myInformation.append(" Info = ");
		myInformation.append(super.toString());
		return myInformation.toString();
	}

	/**
	 * @see com.ibm.ws.sip.container.failover.Replicatable#removeFromStorage()
	 */
	public void removeFromStorage() {
		removeAttributesFromStorage();
		SessionRepository.getInstance().remove(this);
	}


	/**
	 * Assuming the app-session is invalid when expired then the requirement to return 
	 * Long.MIN_VALUE for expired sessions is redundant, since the exception will be thrown....
	 * @see javax.servlet.sip.SipApplicationSession#getExpirationTime()
	 */
	public long getExpirationTime() throws IllegalStateException{
		checkIsSessionValid();
		if(m_expires == NO_EXPIRATION){
			return 0;
		}

		return m_expires; 
	}

	/**
	 *  @see javax.servlet.sip.SipApplicationSession#getApplicationName()
	 */
	public String getApplicationName(){
		if( m_appDescriptor == null){
			if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug(this, "getApplicationName", "m_appDescriptor was null");
			}
			return null;
		}
		return m_appDescriptor.getApplicationName();
	}

	/**
	 *  @see javax.servlet.sip.SipApplicationSession#getTimer(java.lang.String)
	 */
	public ServletTimer getTimer(String id){
		checkIsSessionValid();
		try{
			int timerId = Integer.parseInt(id);

			return
			(ServletTimerImpl)SessionRepository.getInstance().getTimer(getId(), timerId);
		}catch( NumberFormatException e){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this,"getTimer", "Error - id is not numeric "+ id);
			}
			return null;
		}
	}

	/**
	 * @see javax.servlet.sip.SipApplicatioSession#encodeURL(java.net.URL)
	 */
	public URL encodeURL(URL url) throws IllegalStateException{
		checkIsSessionValid();
		return null; // return value is implemented on WASXSipApplicationSessionImpl
	}
	
	public boolean isDuringInvalidate()
	{
		return m_duringInvalidate;
	}

	/**
	 * @param AppSessionId - full sip application session ID
	 * @return - the last two digits of the SipApplicationSession ID.
	 */
	public int extractAppSessionCounter()
	{
		if (m_extractedAppSessionSeqCounter < 0) {
			m_extractedAppSessionSeqCounter = extractAppSessionCounter(getSharedId());
		}
		return m_extractedAppSessionSeqCounter;
	}

	/**
	 * @param AppSessionId - full sip application session ID
	 * We assume the Sip Session ID is similar to "member1.1213603453712.0_1_3_6"
	 * where member1 is the node name
	 * 1213603453712 is a unique timestamp
	 * 0 is the ???
	 * 1 is the the SIP App session counter
	 * 3 is the internal Sip Session ID 
	 * 6 is the internal Transaction user ID
	 * @return - the last two digits of the SAS ID.
	 */
	public static int extractAppSessionCounter(String AppSessionId)
	{
		StringTokenizer tokenizer = new StringTokenizer(AppSessionId,Replicatable.ID_INTERNAL_SEPERATOR);
		tokenizer.nextToken(); // skip the serverid
		String appCounterAsString = tokenizer.nextToken(); //get the SAS counter
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug("SipApplicationSessionImpl", "extractAppSessionCounter","found App session counter:"+appCounterAsString);
		}
		int result = Integer.parseInt(appCounterAsString);
		//Moti: fix for defect 487485: apprantly getAppSession().getID().hashcode()
		// is not univormly distributed (prefix is always the same:logical name).
		// so we will extract the only thing that actually changes: 
		// the AppSession number. which is the last digit and also
		// the global app session internal counter
		return result;
	}

	/**
	 * Getting the session API synchronizing object
	 * @return
	 */
	public Object getSynchronizer() {
		return _synchronizer;
	}

	/**
	 * Setting the session API synchronizing object
	 * @return
	 */
	public void setSynchronizer(Object synchronizer) {
		this._synchronizer = synchronizer;
	}

	/**
	 * Getting the session service synchronizing object
	 * @return
	 */
	public Object getServiceSynchronizer() {
		return _serviceSynchronizer;
	}

	/**
	 * Setting the session service synchronizing object
	 * @return
	 */
	public void setServiceSynchronizer(Object serviceSynchronizer) {
		this._serviceSynchronizer = serviceSynchronizer;
	}

	/**
	 *  @see javax.servlet.sip.SipApplicationSession#getSession(java.lang.String, javax.servlet.sip.SipApplicationSession.Protocol)
	 */
	public Object getSession(String id, Protocol protocol) throws NullPointerException, IllegalStateException {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { id, protocol };
			c_logger.traceEntry(this, " getSession", params);
		}

		SipSession sipSession = getSipSession(id);

		if(c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug(this,"getSession", "Requested SIP session found = " + sipSession);
		}
		return sipSession;
	}

	/** 
	 * @see javax.servlet.sip.SipApplicationSession#getInvalidateWhenReady()
	 */
	public boolean getInvalidateWhenReady() throws IllegalStateException {
		checkIsSessionValid();
		return m_invalidateWhenReady;
	}

	/**
	 *  @see javax.servlet.sip.SipApplicationSession#isReadyToInvalidate()
	 */
	public boolean isReadyToInvalidate() throws IllegalStateException {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, " isReadyToInvalidate", getId());
		}
		synchronized (getSynchronizer()) {
			checkIsSessionValid();
			boolean isRedyToInvalidate = true;
			if (m_transactionUsers != null) {
				for (TransactionUserWrapper transactionUserWrapper : m_transactionUsers) {
					if(!transactionUserWrapper.isReadyToInvalidate()){
						isRedyToInvalidate = false;
						if(c_logger.isTraceDebugEnabled()){
							c_logger.traceDebug(this,"isReadyToInvalidate", 
									"SipSession is not ready yet = " + transactionUserWrapper.getId());
						}
						break;
					}				
				}			
			} else {
				if(c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug(this,"isReadyToInvalidate", 
					"Empty sessions list, sip applications can be invalidated.");
				}				
			}

			if(isRedyToInvalidate && (m_timersIds != null && !m_timersIds.isEmpty())){
				isRedyToInvalidate = false;
				if(c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug(this,"isReadyToInvalidate", 
					"There are still active timers associated with this APplicationSession");
				}
			}

			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(this, " isReadyToInvalidate", getId() + " result: " + isRedyToInvalidate);
			}
			return isRedyToInvalidate;
		}
	}

	/**
	 * @see javax.servlet.sip.SipApplicationSession#setInvalidateWhenReady(boolean)
	 */
	public void setInvalidateWhenReady(boolean invalidateWhenReady) throws IllegalStateException {
		checkIsSessionValid();
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { getId(),invalidateWhenReady};
			c_logger.traceEntry(this, " setInvalidateWhenReady", params);
		}
		m_invalidateWhenReady = invalidateWhenReady;

	}



	/**
	 * This method might be executed multi-threaded or single threaded,
	 * depends on the TasksInvoker definition 
	 * @see com.ibm.ws.sip.container.events.TasksInvoker
	 * @see com.ibm.ws.sip.container.SipContainer#setTimerInvoker()
	 * @see java.lang.Runnable#run()
	 */
	public void readyToInvalidate() {
		if (isValid())
		{
			// First check if this flag is still true.
			if (getInvalidateWhenReady() == true) {

				// Notify listeners that the session expired.
				sendSessionReadyToInvalidateEvt();

				// Check if session life time has been extended by the
				// listeners.
				if (getInvalidateWhenReady() == true) {
					// Session expired - Invalidate.
					try {
						invalidate();
					} 
					catch (IllegalStateException e) {
						if (c_logger.isTraceDebugEnabled()) {
							c_logger
							.traceDebug(this, "readyToInvalidate",
							"SipApplication session was already invalidated");
						}
					}
				} else {
					if (c_logger.isTraceDebugEnabled()) {
						c_logger
						.traceDebug(this, "readyToInvalidate",
						"getInvalidateWhenReady() is false - invalidate later");
					}
				}
			}
			else if (c_logger.isTraceDebugEnabled()) {
				c_logger
				.traceDebug(this, "readyToInvalidate",
				"getInvalidateWhenReady() is false - invalidate later");
			}
		}
		else if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "readyToInvalidate",
			"AppSession has already been invalidated. Just ignore.");
		}
	}


	/**
	 * Method that send notification to listerens when Session is ready to
	 * be invalidated.
	 */
	private void sendSessionReadyToInvalidateEvt() {

		// Get the Application Session Listener from the application's
		// descriptor
		SipAppDesc desc = getAppDescriptor();

		// We will not have a Sip Add Descriptor in case the app session
		//was created from the factory. Seems like a hole in the Sip Servlets
		//API. 
		if (null != desc) {
			Iterator iter = desc.getAppSessionListeners().iterator();
			if (!iter.hasNext()) {
				return;
			}
			SipApplicationSessionEvent evt = new SipApplicationSessionEvent(
					this);
			//Invoke listeners - a notification is sent to allow listeners
			//to extend the session's expiration time. 
			ContextEstablisher contextEstablisher = desc
			.getContextEstablisher();
			ClassLoader currentThreadClassLoader = null;
			try {
				if (contextEstablisher != null) {
					currentThreadClassLoader = contextEstablisher
					.getThreadCurrentClassLoader();
					contextEstablisher.establishContext();
				}

				while (iter.hasNext()) {
					try {
						((SipApplicationSessionListener) iter.next())
						.sessionReadyToInvalidate(evt);
					} catch (Exception e) {
						if (c_logger.isTraceDebugEnabled()) {
							c_logger
							.traceDebug(this, "sendSessionReadyToInvalidateEvt",
									"Exception  = " + e.getMessage());
						}
					}
				}
			} 
			finally {
				if (contextEstablisher != null) {
					contextEstablisher.removeContext(currentThreadClassLoader);
				}
			}
		}
	}

	/**
	 * Needed mainly for ObjectGrid DB operations (do distinguish between
	 * INSERT vs. UPDATE of an attribute).
	 * @param attrName
	 * @return true if the attribute key exists for this applicaion session
	 * @author mordechai
	 */
	public boolean isContainsAttr(String attrName) {
		return m_attributes.contains(attrName);
	}

	/**
	 * 
	 * @return
	 */
	public String getSessionKeyBaseTargetingKey(){
		return m_sessionKeyBaseKey;
	}

	/**
	 * 
	 * @param skbt
	 */
	public void setSessionKeyBaseTargeting(String skbt){
		if (m_sessionKeyBaseKey != null && !m_sessionKeyBaseKey.equals(skbt)) {
			if(c_logger.isErrorEnabled()){
				c_logger.error("Multiple session key based key old key = '" + m_sessionKeyBaseKey + "' new key = '" + skbt + "'. new value ignored");
			}
		}
		m_sessionKeyBaseKey = skbt;
	}
}
