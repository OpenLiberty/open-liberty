/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.failover.repository;

import java.util.List;
import java.util.Map;

import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.failover.repository.ctor.SASAttrRepositoryFactoryImpl;
import com.ibm.ws.sip.container.failover.repository.ctor.SASRepositoryFactoryImpl;
import com.ibm.ws.sip.container.failover.repository.ctor.SKBTRepositoryFactory;
import com.ibm.ws.sip.container.failover.repository.ctor.SSAttrRepositoryFactoryImpl;
import com.ibm.ws.sip.container.failover.repository.ctor.SSRepositoryFactoryImpl;
import com.ibm.ws.sip.container.failover.repository.ctor.TimersRepositoryFactoryImpl;
import com.ibm.ws.sip.container.failover.repository.ctor.TuBaseRepoFactoryImpl;
import com.ibm.ws.sip.container.failover.repository.ctor.TuImplRepoFactoryImpl;
import com.ibm.ws.sip.container.failover.repository.ctor.TuWrapperRepositoryFactory;
import com.ibm.ws.sip.container.failover.repository.ctor.TuWrapperRfc3261RepositoryFactory;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipSessionImplementation;
import com.ibm.ws.sip.container.timer.BaseTimer;
import com.ibm.ws.sip.container.tu.SessionKeyBase;
import com.ibm.ws.sip.container.tu.TUKey;
import com.ibm.ws.sip.container.tu.TransactionUserBase;
import com.ibm.ws.sip.container.tu.TransactionUserImpl;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

/**
 * A utility class which manages a repository of SIP objects.
 * It is implemented as a singleton
 * @author mordechai
 *
 */
public class SessionRepository 
{
	/**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(SessionRepository.class);

    
	private TimerRepository m_timersRepository;
	private SASRepository m_SASrepo;
	private SASAttrRepository m_SASAttrrepo;
	private SSRepository m_SSrepo;
	private SSAttrRepository m_SSAttrrepo;
	private TuWrapperRepository m_tuWrappperRepo;
	private TuImplRepository m_tuImplRepo;
	private TuBaseRepository m_tuBaseRepo;
	private SKBTRepository m_SKBTrepo;
	
	/**
	 * Repository which as a key contains TUKey (defined by rfc 3621: FormTag + ToTag + CallId)
	 */
	private TUWrapperRepositoryRfc3261 m_tuWrappperRepoRfc3261;
	
	/**
	 * This repository singleton instance
	 */
	private static SessionRepository s_singleton = new SessionRepository();
	
	/**
	 * REturns singleton instance of the repository
	 * @return
	 */
	public static SessionRepository getInstance() 
	{
		return s_singleton;
	}
	
	/**
	 * Create sub repositories
	 */
	private void init() {
		try {
			m_SASrepo = new SASRepositoryFactoryImpl().createRepository();
			m_timersRepository = new TimersRepositoryFactoryImpl().createRepository();
			m_SASAttrrepo = new SASAttrRepositoryFactoryImpl().createRepository();
			m_SSrepo = new SSRepositoryFactoryImpl().createRepository();
			m_SSAttrrepo = new SSAttrRepositoryFactoryImpl().createRepository();
			m_tuImplRepo = new TuImplRepoFactoryImpl().createRepository();
			m_tuWrappperRepo = TuWrapperRepositoryFactory.createRepository();
			m_tuWrappperRepoRfc3261=  TuWrapperRfc3261RepositoryFactory.createRepository();
			m_tuBaseRepo = new TuBaseRepoFactoryImpl().createRepository();
			m_SKBTrepo = new SKBTRepositoryFactory().createRepository();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Ctor
	 */
	private SessionRepository()
	{
		init();
	}
	
	/**
	 * Put a timer in the repository
	 * @param sipAppSession - the owner SAS object
	 * @param timer - The timer
	 * @return previous timer that has the same ID (if exists)
	 */
	public BaseTimer put(SipApplicationSession sipAppSession , BaseTimer timer)
	{
		if (c_logger.isTraceDebugEnabled() && m_SASrepo.get(sipAppSession.getId()) == null) {
			c_logger.traceDebug("SessionRepository#put(BaseTimer) adding attribute to Appsession which is not in repository."); 
		}

		BaseTimer result ;
		result= m_timersRepository.put(sipAppSession, timer);
		if (c_logger.isTraceDebugEnabled() && result != null && result != timer) {
			c_logger.traceDebug("SessionRepository#put(timer) overriding value"); 
		}

		return result;
	}
	/**
	 * Get a timer from repository
	 * @param sipAppSessionId - the owner SAS ID
	 * @param timerId - the timer ID
	 * @return the timer
	 */
	public BaseTimer getTimer( String sipAppSessionId, Integer timerId) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "getTimer", new Object[]{sipAppSessionId, timerId}); 
		}
		BaseTimer timer = m_timersRepository.get(sipAppSessionId,timerId);
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(null, "getTimer", timer); 
		}
		return timer;
	}
	
	/**
	 * remove a timer from repository 
	 * @param appSessionId
	 * @param timer
	 * @return
	 */
	public BaseTimer removeTimer(String appSessionId, BaseTimer timer) {
		return m_timersRepository.remove(appSessionId, timer.getTimerId());
		
	}

	/**
	 * Put a SipApplicationSession in repository
	 * @param sipAppSession
	 * @return
	 */
	public SipApplicationSession put(SipApplicationSession sipAppSession) 
	{
		SipApplicationSession oldVal = m_SASrepo.put(sipAppSession.getId(), sipAppSession);
		return oldVal;
	}

	/**
	 * GEt SipApplicationSession from repository
	 * @param appSessionId
	 * @return
	 */
	public SipApplicationSession getAppSession(String appSessionId) {
		SipApplicationSession sas = m_SASrepo.get(appSessionId);
		return sas;
	}


	/**
	 * remove SIP App session and all its attribute.
	 * @param impl
	 */
	public SipApplicationSession remove(SipApplicationSession sipAppSession) {
		// TODO What about its sessions/timers ?
		SipApplicationSession result = m_SASrepo.remove(sipAppSession);
		if (result == null) {
			if (c_logger.isTraceDebugEnabled()) {
	    		c_logger.traceDebug("SAS was already removed. check flow");
	    	}
		}
		return result;
		
		
	}


	public Object putSASAttr(SipApplicationSession appSession, String name, Object value) {
		if (c_logger.isTraceDebugEnabled() && m_SASrepo.get(appSession.getId()) == null) {
			c_logger.traceDebug("SessionRepository#putSASAttr() adding attribute to Appsession which is not in repository."); 
		}
		 Object previousVal = m_SASAttrrepo.put(appSession, name, value);
		 if (c_logger.isTraceDebugEnabled() && previousVal != null && previousVal.equals(value)) {
			 c_logger.traceDebug("putSASAttr put an attribute which was already there. check flow");
		 }
		 return previousVal;
	}

	public Object getSASAttr(SipApplicationSession appSession, String name) {
		return m_SASAttrrepo.get(appSession, name);
	}


	public Object getSipSessAttr(SipSession session, String name) {
		return m_SSAttrrepo.get(session, name);
	}
	
	public Map getAttributes(String session) {
		return m_SSAttrrepo.getAttributes(session);
	}
	
	public Map getAttributes(SipApplicationSession appSession) {
		return m_SASAttrrepo.getAttributes(appSession.getId());
	}
	


	public Object putSSAttr(SipSession session , String attrName, Object value) {
		//Moti: 23/Jan/2008
		if (c_logger.isTraceDebugEnabled()) {
			if (session.getId() != null && m_SSrepo.get(session.getId()) == null)
			{
				c_logger.traceDebug(this ,"putSSAttr"," adding attribute to session "+
						session.getId()+ " which is not in repository.");
			}
		}
		return m_SSAttrrepo.put(session, attrName ,value);

	}

	/**
	 * Transaction user wrappers are not really replicated , they are
	 * presented here for API uniformity.
	 * @param sessionId
	 * @param wrapper
	 * @return
	 */
	public Object put(String sessionId, TransactionUserWrapper wrapper)
	{	
		if (c_logger.isTraceEntryExitEnabled()) {
  			Object[] params = { sessionId};
  			c_logger.traceEntry(this, "SessionRepository: put by sessionId", params);
  		}
		if (sessionId == null) {
			throw new NullPointerException("session ID is null");
		}
		if (wrapper == null) {
			throw new NullPointerException("wrapper object is null");
		}
		TransactionUserWrapper result =  m_tuWrappperRepo.put(sessionId, wrapper);
		if (c_logger.isTraceDebugEnabled() && result != null && result != wrapper) {
			c_logger.traceDebug("SessionRepository#put(wrapper) overriding value. check flow."); 
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
  			c_logger.traceExit(this, "SessionRepository: put by sessionId", result);
  		}
		return result;
	}
	
	/**
	 * Transaction user wrappers are not really replicated , they are
	 * presented here for API uniformity.
	 * @param sessionId
	 * @param wrapper
	 * @return
	 */
	public Object put(TUKey key, TransactionUserWrapper wrapper, boolean addToSessionsTbl)
	{
		if (c_logger.isTraceEntryExitEnabled()) {
  			Object[] params = { key, wrapper.getId() ,addToSessionsTbl};
  			c_logger.traceEntry(this, "SessionRepository: put by key", params);
  		}
		if (key == null) {
			throw new NullPointerException("session ID is null");
		}
		if (wrapper == null) {
			throw new NullPointerException("wrapper object is null");
		}
		TransactionUserWrapper result =  m_tuWrappperRepoRfc3261.put(key, wrapper);
		if (c_logger.isTraceDebugEnabled() && result != null && result != wrapper) {
			c_logger.traceDebug("SessionRepository#put(wrapper) overriding value. check flow."); 
		}
		if(addToSessionsTbl){
			put(wrapper.getId(), wrapper);
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
  			Object[] params = { key, wrapper.getId() ,addToSessionsTbl};
  			c_logger.traceExit(this, "SessionRepository", result);
  		}
		return result;
	}

	/**
	 * 
	 * @param tuId - Tu ID (which is different than Sip Session ID and may need 
	 * refinment if we use derived sessions...)
	 * @return the matching TuWrapper object
	 */
	public TransactionUserWrapper getTuWrapper(TUKey key) {
		if (c_logger.isTraceEntryExitEnabled()) {
  			Object[] params = { key};
  			c_logger.traceEntry(this, "SessionRepository: getTuWrapper by Key", params);
  		}
		TransactionUserWrapper result = m_tuWrappperRepoRfc3261.get(key);
		return result;
	}
	
	/**
	 * when removing TuWrapper we also remove its matching TuImpl,Base, SIP sessions and its attribuye
	 * @param id
	 */
	public TransactionUserWrapper removeTuWrapper(TUKey key, boolean removeFromSessionTbl) 
	{
		if (c_logger.isTraceEntryExitEnabled()) {
  			Object[] params = { key,removeFromSessionTbl};
  			c_logger.traceEntry(this, "SessionRepository: removeTuWrapper by Key", params);
  		}
		
		TransactionUserWrapper result = m_tuWrappperRepoRfc3261.remove(key);
		if (c_logger.isTraceDebugEnabled() && result == null) {
			c_logger.traceDebug(this, "removeTuWrapper", " TuWrapper was already removed:"+key);
		}
		if (removeFromSessionTbl && result != null){
			removeTuWrapper(result.getId());
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param tuId - Tu ID (which is different than Sip Session ID and may need 
	 * refinment if we use derived sessions...)
	 * @return the matching TuWrapper object
	 */
	public TransactionUserWrapper getTuWrapper(String tuId) {
		TransactionUserWrapper result = m_tuWrappperRepo.get(tuId);
		return result;
	}

	/**
	 * when removing TuWrapper we also remove its matching TuImpl,Base, SIP sessions and its attribuye
	 * @param id
	 */
	public TransactionUserWrapper removeTuWrapper(String sessionId) 
	{
		TransactionUserWrapper result = m_tuWrappperRepo.remove(sessionId);
		if (c_logger.isTraceDebugEnabled() && result == null) {
			c_logger.traceDebug(this, "removeTuWrapper", " TuWrapper was already removed:"+sessionId);
		}
		return result;
	}

	/**
	 * Needed to support SipTransactionUSerTable#getSnapshotView 
	 * @return
	 */
	public List getAllTuWrappers() {
		List list  = m_tuWrappperRepoRfc3261.getAll();
		return list;
	}

	public SipSession put(SipSession session) 
	{
		SipSession result = null;
		result = m_SSrepo.put(session.getId() , session);
		if (c_logger.isTraceDebugEnabled() && result != null && result != session) {
			c_logger.traceDebug("SessionRepository#put(sipsession) overriding value.check flow"); 
		}
		return result;
	}

	public SipSession getSipSession(String sessionId) {
		return m_SSrepo.get(sessionId);
	}
		

	public void commitAll() {
		if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this,"commitAll");
    	}
		m_SASrepo.commitTx(null);
		m_SASAttrrepo.commitTx(null);
		m_SSAttrrepo.commitTx(null);
		m_SSrepo.commitTx(null);
		m_timersRepository.commitTx(null);
		m_tuBaseRepo.commitTx(null);
		m_tuImplRepo.commitTx(null);
		m_tuWrappperRepo.commitTx(null);
		m_tuWrappperRepoRfc3261.commitTx(null);
		if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceExit(this,"commitAll");
    	}
	}

	/**
	 * will commit (usually in context of ObjectGrid) only updates of specific type.
	 * This method is most useful when working with databases in the background 
	 * (in which commit of single table has a meaning.
	 * @param typeToCommit
	 */
	public void commitSingleType(Object typeToCommit) {
		commitSingleType(typeToCommit.getClass());
	}

	
	/**
	 * 
	 * @param typeToCommit
	 */
	public void commitSingleType(Class typeToCommit) {
		if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this,"commitSingleType",typeToCommit.getName());
    	}
		if (SipApplicationSession.class.isAssignableFrom(typeToCommit)) {
			m_SASrepo.commitTx(null);
		} else if (TransactionUserBase.class.isAssignableFrom(typeToCommit)) {
			m_tuBaseRepo.commitTx(null);
		} else if (TransactionUserImpl.class.isAssignableFrom(typeToCommit)) {
			m_tuImplRepo.commitTx(null);
		} else {
			throw new IllegalStateException(" unable to commit type:"+typeToCommit.getClass().getName());
		}
	}

	/**
	 * removes all attributes related to a specific Sip Application session
	 * @param impl
	 * @param name
	 */
	public Object removeAttribute(SipApplicationSessionImpl impl, String name) {
		return m_SASAttrrepo.remove(impl, name);
		
	}

	public void removeTuImpl(TransactionUserImpl impl) {
		m_tuImplRepo.remove(impl);
	}

	public void removeTuBase(TransactionUserBase base) {
		m_tuBaseRepo.remove(base);
	}

	public Object removeAttribute(SipSessionImplementation sipSession, String attrName) {
		Object value = m_SSAttrrepo.remove(sipSession, attrName); 
		return value;

	}

	public void removeSipSession(SipSessionImplementation session) {
		session.removeAllAttributes();
		m_SSrepo.remove(session);
	}

	public void put(String sessionId, TransactionUserBase base) {
		TransactionUserBase result  = m_tuBaseRepo.put(sessionId,base);
		if (c_logger.isTraceDebugEnabled() && result != null && result != base) {
			c_logger.traceDebug("SessionRepository#put(tubase) overriding value.check flow"); 
		}
		
	}

	public void put(String sessionId, TransactionUserImpl tuimpl) {
		TransactionUserImpl result = m_tuImplRepo.put(sessionId,tuimpl);
		if (c_logger.isTraceDebugEnabled() && result != null && result != tuimpl) {
			c_logger.traceDebug("SessionRepository#put(tuimpl) overriding value.check flow"); 
		}
	}

	/**
	 * This method is required for SIP container shutdown sequence.
	 * We need a snapshot of current/active Sip Application Sessions.
	 * @return list -  a list to be filled with active sip application sessions
	 */
	public List<SipApplicationSessionImpl> getAllAppSessions() {
		List<SipApplicationSessionImpl> list = m_SASrepo.getAll();
		return list;
	}

	public TransactionUserImpl getTuImpl(String sessionId) {
		return  m_tuImplRepo.get(sessionId);
	}
	
	public TransactionUserBase getTuBase(String sessionId) {
		return  m_tuBaseRepo.get(sessionId);
	}

	
	
	/**
	 * set session key based key
	 *   
	 * @param keyBaseTargeting session key based
	 * @param sessionId sip application session id
	 */
	public void setSessionKeyBase(String keyBaseTargeting, String sessionId) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setSessionKeyBase", "Session key based adding key:"+keyBaseTargeting + " value: " + sessionId);
		}		
		
		SessionKeyBase result = m_SKBTrepo.put(keyBaseTargeting, new SessionKeyBase(keyBaseTargeting, sessionId));
		if (c_logger.isTraceDebugEnabled() && result != null && !result.getSipApplicationSessionID().equals(sessionId)) {
			c_logger.traceDebug("SessionRepository#setSessionKeyBase(keyBaseTargeting) overriding value.check flow"); 
		}
	}
	
	/**
	 * Retrieve sip application session id from session key based key
	 * 
	 * @param keyBaseTargeting
	 * @return
	 */
	public String getKeyBaseAppSession(String keyBaseTargeting) {
		SessionKeyBase sas = m_SKBTrepo.get(keyBaseTargeting);
		String ret = null;
		if (sas != null) {
			ret = sas.getSipApplicationSessionID(); 
		}
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getKeyBaseAppSession", "retrieving key:"+keyBaseTargeting + " value: " + ret);
		}		
		return ret; 
	}
	
	/**
	 * Remove reference to a session key based key
	 */
	public void removeKeyBaseAppSession(String keyBaseTargeting) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "removeKeyBaseAppSession", "removing key:"+keyBaseTargeting);
		}		
		m_SKBTrepo.remove(keyBaseTargeting);
	}	
	

}
