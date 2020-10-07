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

import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionListener;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSession.State;
import javax.servlet.sip.SipSessionListener;
import javax.servlet.sip.UAMode;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.sip.IBMSipSession;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.proxy.ProxyBranchImpl;
import com.ibm.ws.sip.container.router.SipRouter;
import com.ibm.ws.sip.container.router.SipServletInvokerListener;
import com.ibm.ws.sip.container.servlets.IncomingSipServletRequest;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;
import com.ibm.ws.sip.container.servlets.SipSessionImplementation;
import com.ibm.ws.sip.container.servlets.SipSessionSeqLog;
import com.ibm.ws.sip.container.timer.Invite2xxRetransmitTimer;
import com.ibm.ws.sip.container.transaction.ClientTransaction;
import com.ibm.ws.sip.container.transaction.ClientTransactionListener;
import com.ibm.ws.sip.container.transaction.ServerTransactionListener;
import com.ibm.ws.sip.container.transaction.SipTransaction;
import com.ibm.ws.sip.container.util.ContainerObjectPool;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.ws.sip.container.util.wlm.DialogAux;
import com.ibm.ws.sip.container.util.wlm.SipDialogContext;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;
import com.ibm.ws.sip.properties.CoreProperties;
/*TODO Liberty import com.ibm.wsspi.sip.hamanagment.logicalname.ILogicalName;

/**
 * @author Anat Fradin, Nov 23, 2005
 *
 * Interface that will be implemented by the TRansactionUser
 * This interface should be used to prevent the SipSession, SipApplicationSessio, Incoming/Outgoing Requests
 * to access the TransacionUser directly as it can be reused and deleted.
 * 
 * In case when some object will access the TransactionUser after it deleted - IllegalStateException will be thrown
 *  
 */
public class TransactionUserWrapper implements ServerTransactionListener,
ClientTransactionListener, Serializable, SipServletInvokerListener,SipDialogContext{

	/**
     * Class Logger.
     */
    private static final transient LogMgr c_logger = Log
            .get(TransactionUserWrapper.class);
    
	
    private final static String MAX_TU_POOL_SIZE = "max.tu.pool.size";
    
    //private final static String MAX_BASE_TU_POOL_SIZE = "max.base.tu.pool.size"; 
    
    /** pool of TransactionUsers objects */
    private static ContainerObjectPool s_tuPool = 
    		new ContainerObjectPool(TransactionUserImpl.class,MAX_TU_POOL_SIZE);

    /** pool TransactionUserBase objects */
    //Moti: this is never used
//    public static ContainerObjectPool s_baseTUPool = 
//    		new ContainerObjectPool(TransactionUserBase.class,MAX_BASE_TU_POOL_SIZE);
    
    /**
     * Handler for Transaction User
     */
    private transient TransactionUserImpl _transactionUser = null;
    
    /**
     * Handler for Transaction User
     */
    private transient TransactionUserBase _baseTU = null;
    
    /**
     * This is the counter that is increased when new transaction started
     * and decreased when it is ended.
     */
    protected transient int _transactionsCounter = 0;
    
    /**
     * This is the counter that is increased when new transaction started
     * and decreased when it is ended.
     */
    protected transient int _NewTransactionsCounter = 0;
    
    /**
     * This counter is only relevant for proxy forking mode.
     * It counts the transactions added to this TU due to new branches derived from it. When a new branch is created,
     * then before it gets the first response, the TU of the first branch is related to it.
     * When creating a branch, we will check if this TU has already a branch associated with it. 
     * If so this means that this TU will have an extra transaction added to it from the outgoing message of that new branch.
     * That transaction will need to be related to the new derived session that will be created when the 
     * first remote-tag response is received.   
     */
    private transient int _extraTransactionsCounter = 0;
    
    /**
     * This transaction was invalidated and waiting for the transaction to be
     * completed to reused by itself.
     */
    private transient boolean _invalidating = false;
    
    /**
     * This marks TU is partially invalidated when application tries to invalidate an initial dialog.
     */
    private transient boolean _partialInvalidate = false;
    
    /**
     * This flag will be true after the tu will be actually reused and
     * method tu.clean() called. Added to prevent doulbe clean
     */
    protected transient boolean _cleaned = false;
    
    /**
     * This Object is used to synchronize between reusing operation.
     */
    private transient final Object _syncObject = new Object();
    
    
    /**
     * This counter used to count number of method that are running in the same time.
     * At the beginning of each method this counter will be increased and decreased
     * at the end. When it will be a time to reuse transactionImpl this will be done 
     * ONLY  if this counter will be zero. 
     */
    protected transient int _methodInWorkCounter = 0;
    
    /**
     * Indicates that this TUWrapper was added to the ThreadLocalStrorage
     * as TU that should be invalidated in "endOfService" as
     * caused by of invalidateWhenReady mechanism.
     */
    boolean addedToInvalidateWhenReadyList = false;
    
    
    /**
     * Indicates that this is a TU generated but we did not process it yet.
     * Default is set to FALSE because we only want this behavior on special
     * cases.
     */
    protected transient boolean _pendingMessageExists = false;
    
    /**
     * The wrapper will keep this TUImpl state of final response, since this might be needed after invalidation
     */
    private boolean _receivedFinalResponse = false;
    
    /**
     * Will point to a related branch, if exists
     * TODO have to take care of replication and failover! 
     */
    private transient ProxyBranchImpl branch = null;
    
    /**
     * This flag will be true if the transaction is created as derived
     */
    protected transient boolean _derived = false;
    
    /**
     * The original TUWrapper in case the TU is derived.
     */
    private TransactionUserWrapper _origTUWrapper;
    
	/**
     * Default Ctor
     */
    public TransactionUserWrapper(){
    	super();
    }

    /**
     * Ctor
     * @param sipMessage
     * @param isServerTransaction
     */
    public TransactionUserWrapper(SipServletRequestImpl sipMessage,
            						boolean isServerTransaction,
            						SipApplicationSessionImpl appSession,
            						boolean pendingMessageExists){
    	_baseTU = new TransactionUserBase();
    	_baseTU.initialize(this,sipMessage,appSession);
    	_transactionUser = getTUImplObjectFromPool();
    	
    	_transactionUser.initialize(this,sipMessage,isServerTransaction, appSession);
    	_baseTU.attachToSipAppSession();
    	_pendingMessageExists = pendingMessageExists;
    }
    
    /**
     * Setting the branch related to this TU, for a proxy session 
     * @param branch
     */
    public void setBranch(ProxyBranchImpl newBranch) {
    	if(branch != null && !branch.equals(newBranch)){
    		branch.unrelateTU(this);
    	}
		branch = newBranch;
	}
    
    /**
     * @return branch
     */
    public ProxyBranchImpl getBranch() {
    	return branch;
    }
    
    /**
     * This method will create SS/SAS in case that the application has set sessions listeners, 
     * and enabled the custom property create.sessions.when.listeners.exist.
     * Sessions will be created for incoming initial requests, and for outgoing initial requests (other then ASYNWORK)
     */
    public void createSessionsWhenListenerExists(){
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this, "createSessionsWhenListenerExists");
    	}
    	if(!PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.CREATE_SESSIONS_WHEN_LISTENERS_EXIST))
		{
    		if (c_logger.isTraceEntryExitEnabled()) {
        		c_logger.traceExit(this, "createSessionsWhenListenerExists", "not creating, custom property is false");
        	}
			return;
		}
    	
    	SipApplicationSession sipApp = getApplicationSession(false);
    	if( sipApp == null){
    		Collection<SipApplicationSessionListener> sasListeners = null;
    		//if from some reason we can not find the sipApp object we have no way to know 
    		//if there are listeners or not, in this case the SAS will not get created
    		if (getSipServletDesc() != null && getSipServletDesc().getSipApp() != null){
    			sasListeners = getSipServletDesc().getSipApp().getAppSessionListeners();
    		}else{
    			if (c_logger.isTraceDebugEnabled()) {
    				c_logger.traceDebug(this, "createSessionsWhenListenerExists", "SipAppDesc was not found, SAS will not get created");

    			}
    		}
			 
			if(sasListeners != null && !sasListeners.isEmpty()){//creating the SAS so that listener methods will be called.
				getApplicationSession(true);
				if (c_logger.isTraceDebugEnabled()) {
    				c_logger.traceDebug(this, "createSessionsWhenListenerExists", "SAS listeners were found, SAS was created");

    			}
			}
		}
    	
    	SipSession ss = getSipSession(false);
    	if( ss == null){
    		Collection<SipSessionListener> ssListeners = null;
    		//if the SAS exits we can take the SipAppDesc object from it, in case of a outgoing request that is being
    		//created by the application using the SipFactory, the SAS will exist since should be used according to the API
    		//so we can use it to find the SipApp even if the servletDesc is not defined yet on the newly created Tu object
    		if (sipApp != null){
    			ssListeners = ((SipApplicationSessionImpl) sipApp).getAppDescriptor().getSessionListeners();
    		}else if (getSipServletDesc() != null && getSipServletDesc().getSipApp() != null){
    			ssListeners = getSipServletDesc().getSipApp().getSessionListeners();
    		}else{
    			if (c_logger.isTraceDebugEnabled()) {
    				c_logger.traceDebug(this, "createSessionsWhenListenerExists", "SipAppDesc was not found, SS will not get created");

    			}
    		}
    		
			if(ssListeners != null && !ssListeners.isEmpty()){//creating the SS so that listener methods will be called.
				getSipSession(true);
				if (c_logger.isTraceDebugEnabled()) {
    				c_logger.traceDebug(this, "createSessionsWhenListenerExists", "SS listeners were found, SS was created");

    			}
			}
		}
    	
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceExit(this, "createSessionsWhenListenerExists");
    	}
    }
    
    /**
     * Ctor used to create Derived TransactionUser when additional response arrived
     * @param sipMessage
     * @param SipServletResponse received response
     */
    public TransactionUserWrapper(TransactionUserWrapper originalTU){
    	//_baseTU = (TransactionUserBase)s_baseTUPool.get();
    	_baseTU = new TransactionUserBase();
    	_baseTU.initialize(this,originalTU);
    	
    	_transactionUser = getTUImplObjectFromPool();
    	
    	_transactionUser.initializeDerivedTU(this,originalTU);
    	
    	_baseTU.continueDerivedInitalization(originalTU);
    	
    	//we need to copy the counter, so we will close the tu only when it is not in use.
    	//_transactionsCounter = originalTU._transactionsCounter;
    	
    	//In case of proxy the new TU should count also the incoming transaction. This will be decremented later
    	//if it is not chosen as best response
    	if(isProxying()){
    		_transactionsCounter = 2;
    		//Anat: we need to decrease the counter in DerivedTU since it was created with 
			// counter 2 - represents Incoming and Outgoing transactions.
			// Now, when this Branch got a responsibility about this incoming request it causes the DerivedTU
			// counter to be 3 (instead 2).
			// When new DTU created - if counter will be 1 - when final response be received on outgoing leg
			// DTU counter will get 0 and DTU be allowed to invalidate.
    		
    		IncomingSipServletRequest origInRequest = (IncomingSipServletRequest) originalTU.getSipMessage();
    		if(origInRequest == null){
    			// This is in case when Incoming request was already answered with final request and ACK was received on it.
    			// we should not count it as an open transaction in DTU.
    			_NewTransactionsCounter = 1;
    			if (c_logger.isTraceDebugEnabled()) {
    				c_logger.traceDebug(this, "TransactionUserWrapper", "For this Derived - Origianl Incoming request was already acknowledged with ACK on original TU = " + originalTU.getId());
    			}
    		}
    		else {
    			SipTransaction st = origInRequest.getTransaction();
    			if(st.isTerminated()){
    				// Incoming Transaction was already answered with final response.
    				_NewTransactionsCounter = 1;
    				if (c_logger.isTraceDebugEnabled()) {
        				c_logger.traceDebug(this, "TransactionUserWrapper", "For this Derived - Origianl Incoming request was answered with final response for original TU = " + originalTU.getId());
        			}
    			}
    			else{
    				//Incoming Transaction is still active and should be counted in DTU.
    				_NewTransactionsCounter = 2;
    			}
    		}
    	}else{
    		//A derived session on a UAC should only start with 1 open transaction, since there is no scenario
    		//where there should be more then one.
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "TransactionUserWrapper", "Derived for UAC");
			}
    		_NewTransactionsCounter = 1;
    		_transactionsCounter = 1;
    	}
    	
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "TransactionUserWrapper", "New Derived with counter = " + _NewTransactionsCounter);
		}
    	// set the flag to indicate that this transaction is derived
    	_derived = true;
    	
    	// Save the original TUWrapper
    	_origTUWrapper = originalTU;
    }
	
    
    /**
     * Incrementing transaction counter
     */
    public void incrementTransactions(){ 
    	_transactionsCounter++;
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "incrementTransactions", "new transaction was added, tu=" + this + " ,counter=" + _transactionsCounter);

		}
    }
    
    /**
     * Decrementing transaction counter
     */
    public void decrementTransactions(){ 
    	_transactionsCounter--;
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "decrementTransactions", "transaction was removed, tu=" + this + " ,counter=" + _transactionsCounter);

		}
    }
    
    /**
     * Remove the extra transactions that should belong to the TU derived from this one on another branch, from
     * this TU transaction count. This should be called when the derived branch receives a response with remote tag
     * and can now relate a new session to it.
     */
    private void adjustTransactionsCounter(){
    	_transactionsCounter-=_extraTransactionsCounter;
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "adjustTransactionsCounter", "decrementing transactions by" + _extraTransactionsCounter + 
					" _transactionsCounter = " + _transactionsCounter);

		}
    	_extraTransactionsCounter = 0;
    }
    
    /**
     * A new branch is created with this TU, while its already assigned to an older one. This will cause for
     * an extra transaction to be added.
     */
    public void addExtraTransaction(){
    	_extraTransactionsCounter++;
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "addExtraTransaction", "extra transactions="+ _extraTransactionsCounter +
					" _transactionsCounter = " + _transactionsCounter);

		}
    }
    
    /**
     * New transaction was created
     */
    private void transactionInitated(){
    	if(isRelatedToBranch()){
    		branch.incrementTransactionCounters();
    	}else{
    		incrementTransactions();
    	}
    //	incrementTransactions();
    }
    
    /**
     * Close transaction, used to force transaction to close from proxy scenario
     */
    public void transactionCompleted(){
    	adjustTransactionsCounter();
    	if(isRelatedToBranch()){
    		branch.decrementTransactionCounters();
    	}else{
    		decrementTransactions();
    	}
    	
    	//decrementTransactions();
    }    
    
    /**
     * Check whether this TU has a proxy branch associated with it. 
     * @return
     */
    public boolean isRelatedToBranch(){
    	return branch != null;
    }
    /**
     * Helper method which returns true when there are ongoing transactions
     */
    public boolean hasOngoingTransactions()
    {
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceDebug(this, "hasOngoingTransactions", 
    				"hasOngoingTransactions: TU = " + getId() + " _NewTransactionsCounter = " + _NewTransactionsCounter + " _transactionsCounter = " + _transactionsCounter );
    	}
    	
    	if(PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.WAS855_TU_COUNTER_TRANSACTION_FIX) == true){
    		return _NewTransactionsCounter > 0 ? true:false;
    	}

    	return _transactionsCounter > 0 ? true:false;
    }
    
    /**
     * In proxy mode, in case the server transaction is being terminated when the non original TU
     * is the one that got the best response forwarded upstream, we will have to make sure that 
     * the original TU of the incoming request is getting cleaned up.
     * 
     * @param requestMethod the method of the current request
     */
    private void completeTransactionForOriginalRequest(String requestMethod) {
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this, "completeTransactionForOriginalRequest", "requestMethod=" + requestMethod);
    	}
    	boolean result = false;
    	try{
	    	if(branch == null){
	    		result = false;
	    		return;
	    	}
	    	
	    	SipServletRequestImpl origRequest = (SipServletRequestImpl)branch.getProxy().getOriginalRequest();
	    	TransactionUserWrapper origTU = origRequest.getTransactionUser();
	    	if (c_logger.isTraceDebugEnabled()) {
	    		c_logger.traceDebug(this, "completeTransactionForOriginalRequest", "origRequestTu=" +  origTU +
	    				", this TU=" + this);
	    	}
	    	String origMethod = origRequest.getMethod(); //Retrieve the method of the original request
	    	if((origTU.equals(this)) || (!requestMethod.equals(origMethod))) {
	    		//If this is the original request transaction user, it will be completed later
	    		//OR if the method of the request is different than the original request's method,
	    		//(could happen in derived sessions) the TU shouldn't be invalidated
	    		if (c_logger.isTraceDebugEnabled()) {
		    		c_logger.traceDebug(this, "completeTransactionForOriginalRequest", "origMethod=" + origMethod);
		    	}
	    		result = false; //this is the original request transaction user, it will be completed later
	    		return;
	    	}
	    	
	    	origTU.transactionCompleted();
	    	origTU.invalidateIfReady();
	    	result = true;
    	}finally{
    		if (c_logger.isTraceEntryExitEnabled()) {
        		c_logger.traceExit(this, "completeTransactionForOriginalRequest", " invalidated original:" + result);
        	}
    	}
    }
    
    /**
     * Transaction terminated
     *
     */
    protected void transactionTerminated(){
    	transactionTerminated(true, "");
    }
    
    /**
     * Terminates the transaction.
     * 
     * @param isClient
     * @param requestMethod the method of the current request
     */
    protected void transactionTerminated(boolean isClient, String requestMethod) {
    	//Moti: made protected for JDBC support
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this, "transactionTerminated");
    	}
    	
    	if(!isClient){
    		completeTransactionForOriginalRequest(requestMethod);
    	}
    	
		transactionCompleted();
		
		tryToInvalidate();
    	
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceExit(this, "transactionTerminated");
    	}
    }
    
    /**
     * Helper method which calls to invalidated or resume interruped invalidation
     */
    private void tryToInvalidate() {
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this, "tryToInvalidate","TU = " + getId());
    	}
    	if(_invalidating){
    		reuseTU();
    		//check if we need to invalidate the SAS for this SS
    		//The SS was already invalidated and now it is being cleaned so we 
    		//need to make sure that the SAS also passed the ready to invalidate mechanism
    		//there is no reason to run the invalidate when ready if the app session is not valid
    		SipApplicationSessionImpl appSession = (SipApplicationSessionImpl) getApplicationSession(false);
    		if (appSession != null) {
    			if (appSession.isValid() && appSession.getInvalidateWhenReady() && appSession.isReadyToInvalidate()) {
    				appSession.readyToInvalidate();
    			}					
    		}
    	} else {
    		invalidateIfReady();
    	}    	
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceExit(this, "tryToInvalidate");
    	}
    }
    
    /**
     * Checkes if TU is ready to invalidate and if so adds it to a Threadlocal variable which will
     * be invalidated only once the thread is finished.
     * This is public but should be used carefully.
     */
    public void invalidateIfReady() {
    	if (addedToInvalidateWhenReadyList) {
			if (c_logger.isTraceDebugEnabled()) {
	    		c_logger.traceDebug(this, "invalidateIfReady","Already added to the invalidateWhenReadList. " + this + 
	    				", sessionID=" + getId());
	    	}
			return;
    	}
    	
    	if(isInvalidating()){
    		if (c_logger.isTraceDebugEnabled()) {
	    		c_logger.traceDebug(this, "invalidateIfReady","Already invalidating. " + this);
	    	}
    		return;
    	}
    	
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceDebug(this, "invalidateIfReady","Adding to list: " + this + 
	    				", sessionID=" + getId());
    	}
		TransactionUserBase bTU = isBaseTUAlive();
		boolean shouldinvalidate = bTU
		.shouldInvokeInvalidateWhenReady();
		if (shouldinvalidate) {
			ThreadLocalStorage.setTuForInvalidate(this);
			addedToInvalidateWhenReadyList = true;
		}
    	
    }
    

    /**
     * Setting base
     */
    public void setBase( TransactionUserBase bTU){
    	_baseTU = bTU;
    }
    
    /**
     * Setting TU
     */
    public void setTU(TransactionUserImpl tu){
    	_transactionUser = tu;
    }
	/**
	 * Create Request according to the method parameter
	 * @param method
	 * @return
	 */
    public SipServletRequest createRequest(String method){
    	TransactionUserImpl impl = startToUseTU();
		SipServletRequest req = impl.createRequest(method);
		
		finishToUseTU(impl);
    	
		return req;
	}
	
    /**
     * Add the transactionUserImpl to the SipTransactionUsersTable
     */
    public void addToTransactionUsersTable(){
 	   TransactionUserImpl impl = startToUseTU();
 	   impl.addToTransactionUsersTable();
 	   finishToUseTU(impl);
 	 }
    
    /**
     * Checks if the Transaction User is active (not terminated).
     * @throws IllegalStateException if TUBase is null or invalidated
     */
    public void ensureTUActive() throws IllegalStateException{
    	TransactionUserBase bTU = isBaseTUAlive();
    	bTU.isBaseTUActive();
    }
    
  /**
   * Forward invalidated sipApplicationSession to the TUBase.
   * @param appSession
   */
    public void applicationSessionIvalidated(SipApplicationSession appSession){
	  TransactionUserBase bTU = isBaseTUAlive();
	  bTU.applicationSessionIvalidated(appSession);
    }
  
    /**
     * Checks if the TransactionUser was not invalidated and still alive
     * 
     * @throws IllegalStateException
     *             In case the session is already invalidated.
     */
    protected TransactionUserImpl isTransactionUserAlive() throws IllegalStateException {
		
    	TransactionUserImpl impl = getTuImpl();
    	if(impl == null ){

            if (c_logger.isTraceDebugEnabled()) {
                c_logger
                    .traceDebug(
                                this,
                                "isTransactionUserAlive",
                                "This Transaction User was ENDED,  "
                                 + this);

            }

            StringBuffer buff = new StringBuffer();
            buff.append("This TransactionUser was ENDED and REUSED. Transaction Id = ");
            buff.append(getTuBase().toString());
            buff.append(this);
            throw new IllegalStateException(buff.toString());
		}
    	
    	return impl; 
	}
    
    /**
     * Checks if the TransactionUser was not invalidated and still alive
     * 
     * @return true if the Transaction user was invalidated.
     */
    public boolean isTransactionUserInvalidated(){
    	boolean isTrasactionInvalidated = false;
    	TransactionUserImpl impl = _transactionUser;
    	if(impl == null ){
            if (c_logger.isTraceDebugEnabled()) {
                c_logger
                    .traceDebug(
                                this,
                                "isTransactionUserInvalidated",
                                "This Transaction User was ENDED,  "
                                 + this);

            }

            isTrasactionInvalidated = true;
		}
    	
    	return isTrasactionInvalidated; 
	}
	
    /**
     * Checks if the Base TransactionUser was not ended (by the timer)
     * or Invalidated and still alive
     * 
     * @throws IllegalStateException
     *             In case it is already invalidated.
     */
    protected TransactionUserBase isBaseTUAlive() throws IllegalStateException {
		
    	TransactionUserBase bTU = getTuBase();
    	if(bTU  == null){

            if (c_logger.isTraceDebugEnabled()) {
                c_logger
                    .traceDebug(
                                this,
                                "isBaseTUAlive",
                                "This Base TU was ENDED,  "
                                 + this);

            }

            throw new IllegalStateException("This Base TU was ENDED and REUSED"
                                            + this);

		}
    	
    	return bTU; 
	}
    
    public String getInternalCallId() {
    	TransactionUserImpl impl = startToUseTU(false);
    	String callId = null;
    	if (impl != null) {
        	callId = impl.getCallId();
        	finishToUseTU(impl);
    	}
    	
    	return callId;
    }    
    
    /**
     * Gets CallId from the Transaction User
     * @return
     */
    public String getCallId() {
    	TransactionUserImpl impl = startToUseTU();
    	String callId = impl.getCallId();
    	
    	finishToUseTU(impl);
    	
    	return callId;
    }
    /**
     * Gets invites CSeq. Used only in UAS mode and only
     * when initial request was INVITE.
     * @return
     */
    public long getInviteCseq() {
    	TransactionUserImpl impl = startToUseTU();
    	long cseq = impl.getInviteCseq();
    	
    	finishToUseTU(impl);
    	
    	return cseq;
    }
    
    public long getLocalCSeq() {
    	TransactionUserImpl impl = startToUseTU();
    	long cseq = impl.getLocalCSeq();
    	
    	finishToUseTU(impl);
    	
    	return cseq;
	}

	public long getRemoteCseq() {
		TransactionUserImpl impl = startToUseTU();
    	long cseq = impl.getRemoteCseq();
    	
    	finishToUseTU(impl);
    	
    	return cseq;
	}	
    
    /**
     * Returns the SipServletRequest which caused in the
     * ServerTransaction mode to create this TU.
     * Can be null.
     * @return
     */
    public SipServletRequestImpl getSipMessage(){
    	TransactionUserImpl impl = startToUseTU();
    	SipServletRequestImpl request = impl.getSipMessage();
    	
    	finishToUseTU(impl);
    	
    	return request;
    }
    
    
    /**
     * Gets ID from the TransactionUser
     * @return
     */
    public String getId() {
    	TransactionUserBase bTU = isBaseTUAlive();
    	return bTU.getSharedId();
    }
    
    /**
     * Gets ID from the TransactionUser
     * @return
     */
    public void invalidateWhenReady() {
    	TransactionUserBase bTU = isBaseTUAlive();
    	bTU.callInvalidateWhenReady();
    }
    
    
    
    /**
     * Gets Local Party from the TransactionUser
     */
    public Address getLocalParty() {
    	TransactionUserImpl impl = startToUseTU();
    	Address localParty = impl.getLocalParty();    	
    	finishToUseTU(impl);
    	return localParty;
    }

    /**
     * Gets Remote Party from the TransactionUser
     */
    public Address getRemoteParty() {
    	TransactionUserImpl impl = startToUseTU();
		Address remoteParty = impl.getRemoteParty();		
		finishToUseTU(impl);
		
    	return remoteParty;
    }
    
	/**
     * @see com.ibm.ws.sip.container.transaction.ServerTransactionListener#processRequest(javax.servlet.sip.SipServletRequest)
     */
    public void processRequest(SipServletRequest request) {
    	//This one gets the app service method to be in a sync block 
    	//and eventually can cause deadlocks
    	//synchronized (getSynchronizer()) {


    	TransactionUserImpl impl = startToUseTU();

    	if(impl == null){
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "processRequest",
    			"TransactionUserImpl is null, sending 481 error");
    		}
    		SipServletRequestImpl requestImpl = (SipServletRequestImpl)request;
    		SipRouter.sendErrorResponse(requestImpl, SipServletResponse.SC_CALL_LEG_DONE);
    	}
    	
    	else{
    	if(!request.getMethod().equals(Request.ACK)){
//    		ACK will not counted as a new transaction as it not added to transactionTable
    		transactionInitated();
    	}
    	impl.processRequest(request);
    	}
    	
    	finishToUseTU(impl);
//    }
    }

    /**
     * @see com.ibm.ws.sip.container.transaction.ServerTransactionListener#onSendingResponse(javax.servlet.sip.SipServletResponse)
     */
    public boolean onSendingResponse(SipServletResponse response) {
    	synchronized (getSynchronizer()) {

    	TransactionUserImpl impl = startToUseTU();
    	
    	boolean rc = impl.onSendingResponse(response);

    	finishToUseTU(impl);
    	
    	return rc;
    }
    }
    

    /**
     * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#processResponse(javax.servlet.sip.SipServletResponse)
     */
    public void processResponse(SipServletResponseImpl response) {
    	//This one gets the app service method to be in a sync block 
    	//and eventually can cause deadlocks
    	//synchronized (getSynchronizer()) {


	    	TransactionUserImpl impl = startToUseTU();
	    	impl.processResponse(response);
	    	
	    	finishToUseTU(impl);
//    	}
    }
    
    /**
     * @return FromTag from the TransactionUserWrapper
     */
    public String getLocalTag(){
    	TransactionUserImpl impl = startToUseTU();
    	String fromTag = impl.getLocalTag();
    	finishToUseTU(impl);
    	return fromTag;
    }
    
    /**
     * @see com.ibm.ws.sip.container.transaction.ServerTransactionListener#onTransactionCompleted()
     */
    public void onTransactionCompleted(){
    	synchronized (getSynchronizer()) {

    	TransactionUserImpl impl = startToUseTU();
		
    	impl.onTransactionCompleted();
    	
    	finishToUseTU(impl);
    }
    }
    
    /**
     * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#processTimeout(javax.servlet.sip.SipServletRequest)
     */
    public void processTimeout(SipServletRequestImpl req) {
    	//This one gets the app service method to be in a sync block 
    	//and eventually can cause deadlocks
    	//synchronized (getSynchronizer()) {

    		TransactionUserImpl impl = startToUseTU();
        	
        	impl.processTimeout(req);
        	
        	finishToUseTU(impl);
//    	}	
   }
    
    

    /**
     * Application router exception caused 500 response downstream
     */
	public void processCompositionError(SipServletRequestImpl request) {
    	//This one gets the app service method to be in a sync block 
    	//and eventually can cause deadlocks
    	//synchronized (getSynchronizer()) {

        if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "processCompositionError");
		}
		
    	TransactionUserImpl impl = startToUseTU();
    	
    	impl.generateCompositionErrorResponse(request);

    	finishToUseTU(impl);
		
        if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "processCompositionError");
		}
//	}
	}

	/**
     * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#onSendingRequest(javax.servlet.sip.SipServletRequest)
     */
    public boolean onSendingRequest(SipServletRequestImpl request) {
    	synchronized (getSynchronizer()) {
	    	TransactionUserImpl impl = startToUseTU();
			
	    	if(!request.getMethod().equals(Request.ACK)){
	    		//ACK will not counted as a new transaction
	    		transactionInitated();
	    	}
	    	boolean rc = impl.onSendingRequest(request);
	    	
	    	finishToUseTU(impl);
	    	
	    	return rc;
    	}
    }
    
        
    /**
     * @see com.ibm.ws.sip.container.router.SipServletInvokerListener#
     *      servletInvoked(javax.servlet.sip.SipServletResponse)
     */
    public void servletInvoked(SipServletResponse response) {
    	synchronized (getSynchronizer()) {

	    	TransactionUserImpl impl = startToUseTU();
	    	impl.servletInvoked(response);
	    	
	    	finishToUseTU(impl);
    	}
    }

    /**
     * @see com.ibm.ws.sip.container.router.SipServletInvokerListener#
     *      servletInvoked(javax.servlet.sip.SipServletRequest)
     */
    public void servletInvoked(SipServletRequest request) {
    	synchronized (getSynchronizer()) {
    	
    	TransactionUserImpl impl = startToUseTU();
    	
    	impl.servletInvoked(request);
    	
    	finishToUseTU(impl);
    }
    }
    
    /**
     * We got retransmission of response that cen be only 2xx response on INVITE 
     * or retransmission of reliable response 
     * @param response
     * @param provider
     */
    public void processStrayResponse(Response response, SipProvider provider){
    	//This one gets the app service method to be in a sync block 
    	//and eventually can cause deadlocks
    	//synchronized (getSynchronizer()) {


	    	TransactionUserImpl impl = startToUseTU();
	    	impl.processStrayResponse(response,provider);
	    	
	    	finishToUseTU(impl);
//    	}
    }
    
    
    /**
	 * Helper method that takes care of sending reliable responses
	 * 
	 * @param response
	 */
    public void onSendingReliableProvisionalResponse(SipServletResponse response) {
    	synchronized (getSynchronizer()) {

	    	TransactionUserImpl impl = startToUseTU();
	    	impl.onSendingReliableProvisionalResponse(response);
	    	finishToUseTU(impl);
    	}
    }
    
    /**
     * Helper method that will be called when final response is going to be sent
     * after provisional responses on thins session.
     * @param response
     */
    public void onSendingFinalResponseAfterProvisional(SipServletResponse response){
    	synchronized (getSynchronizer()) {

	    	TransactionUserImpl impl = startToUseTU();
	    	impl.onSendingFinalResponseAfterProvisional(response);
	    	
	    	finishToUseTU(impl);
    	}
    }
    
    /**
     * Update the session's internal state according to the response. Will be
     * called prior to dispatching the request to the listener of the
     * transaction which could be either the session itself or a proxy.
     * 
     * @param response
     */
    public void updateSession(SipServletResponse response) {
    	synchronized (getSynchronizer()) {
	
	    	TransactionUserImpl impl = startToUseTU();
	    	impl.updateSession(response);
	    	finishToUseTU(impl);
    	}
    }
    
    /**
     * Generate the to tag that is used for client transaction initiated by
     * applications running on the container. The tag will contain the session
     * which will be used to match subsequent message in the same dialog
     * 
     * @return
     */
    public String generateLocalTag() {
    	//synchronized (getSynchronizer()) {

	    	TransactionUserImpl impl = startToUseTU();
	    	
	    	String toTag = impl.generateLocalTag();
	    	
	    	finishToUseTU(impl);
	    	return toTag;
    	//}
    }
    
    /**
     * Return the next RSeq for the new reliable response
     * @return
     */
    public long getNextRSegNumber() {
    	synchronized (getSynchronizer()) {

	    	TransactionUserImpl impl = startToUseTU();
			long rseq = impl.getNextRSegNumber();
			
			finishToUseTU(impl);
			
	    	return rseq;
    	}
    }
    
    /**
     * Helper method that will clean the m_reliableProcessor
     * after the final response will be sent
     */
    public void cleanReliableObject(){
    	synchronized (getSynchronizer()) {

			TransactionUserImpl impl = startToUseTU();
			impl.cleanReliableObject();
			finishToUseTU(impl);
    	}
    }


    /**
     * Gets the next CSeq Number.
     */
    public long getNextCSeqNumber() {
    	synchronized (getSynchronizer()) {

	    	TransactionUserImpl impl = startToUseTU();
			long nextCSeq = impl.getNextCSeqNumber();
	
			finishToUseTU(impl);
			return nextCSeq;
    	}
    }
    
    /**
     * @see javax.servlet.sip.SipSession#setHandler(String)
     */
    public void setHandler(String name) throws ServletException{
    	TransactionUserBase bTU = isBaseTUAlive();
    	bTU.setHandler(name);
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
    public void processSubsequentProxyResponse(SipServletResponse response) {
    	//This one gets the app service method to be in a sync block 
    	//and eventually can cause deadlocks
    	//synchronized (getSynchronizer()) {

    	TransactionUserImpl impl = startToUseTU();
    	
    	impl.processSubsequentProxyResponse(response);
    	
    	finishToUseTU(impl);
//    	}
    }
    
    /**
	 * Returns method which intiated this dialog
	 * @return
	 */
    public String getInitialDialogMethod() {
    	TransactionUserImpl impl = startToUseTU();
    	
    	String initialMethod = impl.getInitialDialogMethod();
    	
    	finishToUseTU(impl);
    	
		return initialMethod;
	}
    
    /**
	 * @return if this dialog was answered reliably
	 */
	public boolean wasAnsweredReliable() {
		TransactionUserImpl impl = startToUseTU();
		boolean rc = impl.wasAnsweredReliable();
		
		finishToUseTU(impl);
		
		return rc;
	}
        
    
    /** Determines whether the session is in proxying mode.
    * 
    * @return
    */
   public boolean isProxying() {
//	   TransactionUserBase bTU = isBaseTUAlive();
//		if (isBaseTUDialog(bTU)) {
			TransactionUserImpl impl = startToUseTU();
			boolean rc = impl.isProxying();
			finishToUseTU(impl);
			return rc;
//		}

//		return false;
   }	
   
   /** Determines whether the session is in RecordRoute proxying mode.
    * 
    * @return
    */
   public boolean isRRProxy() {
	   TransactionUserBase bTU = isBaseTUAlive();
		if (isBaseTUDialog(bTU)) {
			TransactionUserImpl impl = startToUseTU();
			boolean rc = impl.isRRProxy();
			finishToUseTU(impl);
			return rc;
		}
		return false;
   }	
   /**
    * Send a response to the application
    * 
    * @param response
    *            The response to be sent
    * @param listener
    *            The listener which tell when the application was invoked
    */
   public void sendResponseToApplication(SipServletResponse response,
                                         SipServletInvokerListener listener) {
   	//This one gets the app service method to be in a sync block 
   	//and eventually can cause deadlocks
   	//synchronized (getSynchronizer()) {

		    TransactionUserImpl impl = startToUseTU();
	
			impl.sendResponseToApplication(response, listener);
			finishToUseTU(impl);
//	   }
	}


   /**
    * Sets the session to be in Proxy mode. Which means that subsequent CANCEL
    * request go through this session to the proxy and also subsequent requests
    * when the proxy is in Record Route mode.
    * 
    * @param isProxying
    */
   public void setIsProxying(boolean isProxying) {
	   synchronized (getSynchronizer()) {
		    TransactionUserImpl impl = startToUseTU();
			impl.setIsProxying(isProxying);
			
			finishToUseTU(impl);
	   }
	}

   /**
    * Sets whether the proxy associated with this session is in Record Route
    * mod.
    * 
    * @param isRR
    */
   public void setIsRRProxying(boolean isRRProxy) {
	   synchronized (getSynchronizer()) {
		    TransactionUserImpl impl = startToUseTU();
			
			impl.setIsRRProxying(isRRProxy);
			
			finishToUseTU(impl);
	   }
	}
   
   /**
    * Sets whether this TU represents virtual branch.
    * 
    * @param isRR
    */
   public void setIsVirtualBranch(Response response) {
	   synchronized (getSynchronizer()) {
		    TransactionUserImpl impl = startToUseTU();
			
			impl.setIsVirtualBranch(response);
			// For virtual branch, we should not count the transactions counter.
			removeTransaction(null);
			
			finishToUseTU(impl);
	   }
	}
   
   /**
    * Update the SipSession obj with information received in the Reliable Response
    * @param response
    */
   public void updateWithProxyReliableResponse(SipServletResponse response) {
	   synchronized (getSynchronizer()) {
		   TransactionUserImpl impl = startToUseTU();
			impl.updateWithProxyReliableResponse(response);
			
			finishToUseTU(impl);
	   }
   }
   

    
    /**
     * Gets the Jain Sip Provider associated with this message.
     */
    public SipProvider getSipProvider() {
    	TransactionUserImpl impl = startToUseTU();
    	SipProvider provider = impl.getSipProvider();
    	
    	finishToUseTU(impl);
    	
    	return provider;
    }

 
    /**
     * Get SipSession associate with this Transaction User
     * @param create
     * @return
     */
    public SipSession getSipSession(boolean create){
    	synchronized (getSynchronizer()) {
	    	return getSipSessionFromBase(create);
    	}
    }
    
    /**
     * Get SipSession from the TransactionUserBase
     * @param create
     * @return
     */
    public SipSession getSipSessionFromBase(boolean create){
    	TransactionUserBase bTU = isBaseTUAlive();		
    	return bTU.getSipSession(create);
    }
    
    /**
     * Returns the all SipSession that are relate to this TU.
     * NOTE: sip session are created on demand, this will force it's creation. 
     * verify that you can't retrieve the needed info from TU objects.
     * 
     * @param sessionsListToFill This parameter is a list that is handled (created and cleared) 
     * by the calling Object so that a reuse of it will be possible.
     * @return
     */
    public List<IBMSipSession> getAllSipSessions(){
    	return getAllSipSessions(true);
    }
    
    /**
     * Returns the all SipSession that are relate to this TU.
     * @param sessionsListToFill This parameter is a list that is handled (created and cleared) 
     * by the calling Object so that a reuse of it will be possible.
     * @param create create sip session when those objects doesn't exist
     * @return
     */
    public List<IBMSipSession> getAllSipSessions(boolean create){
    	
    	List<IBMSipSession> sessions = null;
    	TransactionUserBase bTU = isBaseTUAlive();
    	sessions = new ArrayList<IBMSipSession>(1);
    	bTU.getAllSipSessions(sessions, create);
    	return sessions;
    }
    
    /**
     * Helper method that will test if the mode of the TU is a SIP DIALOG or not
     * @return
     */
    public boolean isTUDialog(){
    	TransactionUserBase bTU = isBaseTUAlive();
    	return isBaseTUDialog(bTU);
    }
    
    
    /**
     * Helper method that will test if the mode of the TU is a SIP DIALOG or not
     * @return
     */
    boolean isBaseTUDialog(TransactionUserBase bTU){
    	return  (bTU.isDialog());
    }
    
    /**
     * Get Application Sip Session associate with this Transaction User
     * @param create
     * @return
     */
    public SipApplicationSession getApplicationSession(boolean create){
    	synchronized (getSynchronizer()) {

	    	TransactionUserBase bTU = isBaseTUAlive();
	    	return bTU.getApplicationSession(create);
    	}
    }
    
       
    /**
     * Get Application Sip Session associate with this Transaction User
     * @param create
     * @return
     */
    public SipApplicationSessionImpl getAppSessionForInternalUse(){
    	TransactionUserBase bTU = isBaseTUAlive();
    	return (SipApplicationSessionImpl)bTU.getApplicationSession(false);
    }
    
    /**
     * Get the SipSession for internal use
     * @return
     */
    SipSessionImplementation getSipSessionForInternalUse() {
    	TransactionUserBase bTU = isBaseTUAlive();
    	return (SipSessionImplementation)bTU.getSipSession(false);
	}
    
    /**
     * Gets the Sip Servlet descriptor associated with this session.
     * 
     * @return
     */
    public SipServletDesc getSipServletDesc() {
    	TransactionUserBase bTU = isBaseTUAlive();
    	SipServletDesc desc = bTU.getSipServletDesc(); 
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this, "getSipServletDesc",desc+ "this="+this);
    	}
    	return desc;
    }
    
    
    /**
    * Gets the Sip Servlet Request associated with this session.
    * 
    * @return
    */
   public SipServletMessage getSipServletRequest() {
	   TransactionUserImpl impl = startToUseTU();
		SipServletMessage msg = impl.getSipServletRequest();

		finishToUseTU(impl);

		return msg; 
	}
    
   /**
    * Sets the Sip Servelt descriptor associated with this session.
    * 
    * @param desc
    */
   public void setSipServletDesc(SipServletDesc desc) {
	   if (c_logger.isTraceEntryExitEnabled()) {
		   c_logger.traceEntry(this, "setSipServletDesc",desc+ " this="+this);
	   }
		TransactionUserBase bTU = isBaseTUAlive();
		bTU.setSipServletDesc(desc);
	}
   
   /**
    * Sets the base cSeq number that will be used instead of the default value
    * of 1. Required for proxy operation that need to follow up using the same
    * CSeq number
    * 
    * @param l
    */
   public void setcSeq(long l) {
	   synchronized (getSynchronizer()) {
	
		   	TransactionUserImpl impl = startToUseTU();
	   		impl.setcSeq(l);
	   		
	   		finishToUseTU(impl);
	   }
	}
   
   /**
    * Saves the latest destination where the INVITE request was sent. It can be used 
    * in the case when the UAC will send CANCEL request.
    * @param lastUsedDestination
    */
   public void setUsedDestination(SipURL lastUsedDestination) {
	   synchronized (getSynchronizer()) {

		   TransactionUserImpl impl = startToUseTU();
	  		impl.setUsedDestination(lastUsedDestination);
	  		finishToUseTU(impl);
	   }
	}
	
	/** 
	 * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#getUsedDestination()
	 */
   public SipURL getUsedDestination() {
		TransactionUserImpl impl = startToUseTU();
		SipURL destination = impl.getUsedDestination();
		finishToUseTU(impl);
		return destination;
	}

   
   /**
    * Helper method that checks if related session was created
    * @return
    */
   public boolean hasSipSession() {
		TransactionUserBase bTU = isBaseTUAlive();
		return bTU.hasSipSession();
	}
   
	/**
	 * Returns the state of the Dialog.
	 * In the non dialog requests will return INITIAL  
	 * @return
	 */
	public SipSessionImplementation.State getState() {
		TransactionUserBase bTU = isBaseTUAlive();
		return bTU.getState();
	}

	/**
	 * Change the state to AFTER_INITIAL state
	 */
	public void setStateToAfterInitial() {
	   synchronized (getSynchronizer()) {
			TransactionUserBase bTU = isBaseTUAlive();
			bTU.setStateToAfterInitial();
	   }
	}
	
	/**
	 * Change the state to AFTER_INITIAL state
	 */
	public boolean isAfterInitial() {
		TransactionUserBase bTU = isBaseTUAlive();
		return bTU.isAfterInitialState();
	}
	
	 /**
     * Helper method decides if dialog in the current state can be invalidated
     * @return
     */
	public boolean canBeInvalidated() {
		TransactionUserBase bTU = isBaseTUAlive();
		return bTU.canBeInvalidated();
	}
	
	/**
	 * HelperMethod that changed the state to TERMINATED
	 */
	public void setSessionState(SipSessionImplementation.State state, SipServletMessage message) {		
		TransactionUserBase bTU = isBaseTUAlive();
		bTU.setSessionState(state,message);
	}
	
	/**
	 * Update SessionState according to 
	 * received response.
	 */
	public SipSessionImplementation.State updateState(SipServletResponse response) {
		TransactionUserBase bTU = isBaseTUAlive();
		return bTU.updateState(response);
	}
	
	/**
     * Method which runs on Requests and checkes if dialog state should
     * be changed to TERMINATED (in case of dialog requests).
     * Works for Incoming and Outging requests.
     * @param msg
     */
	public void checkIfTerminateRequest(SipServletRequest request){
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this,"checkIfTerminateRequest",request.getMethod());
		}
		TransactionUserBase bTU = isBaseTUAlive();
		bTU.checkIfTerminateRequest(request);
	}
   /**
    * Returns the Id of the relate ApplicationSession
    * @return
    */
   public String getApplicationId() {
	   TransactionUserBase bTU = isBaseTUAlive();
		return bTU.getApplicationId();
   }

   /**
    * Returns the Id of the relate ApplicationSession
    * @return
    */
   public String getSipSessionId() {
	   TransactionUserBase bTU = isBaseTUAlive();
		return bTU.getSipSessionId();
   }

   
     
   /**
	 * Method that called when TransactonUser expired by timer
	 */
   public void transactionUserExpired() {
	   logToContext(SipSessionSeqLog.EXPIRED);
	   
	   // fixing IllegalStateException thrown by startToUseTU if TUImpl is null
	   // the exception occurred in failover when an expired session was not committed
	   // we don't need to send the timeout response in this case 
	   if(getTuImpl() != null){
		   setTimeoutResponseCode();
	   }

	   invalidateTU(true,true);
	}
   

   /**
    * This method called from the Invite2xxRetransmitTimer to prevent the
    * situation when TU deleted when Invite2xxRetransmitTimer use it.
    * @param timer2xxRetransmit
    */
   public void handle2xxRetransmittion(Invite2xxRetransmitTimer timer2xxRetransmit){
	   synchronized (getSynchronizer()) {

		   TransactionUserImpl impl = startToUseTU(false);
		   if(impl == null){
			   return;
		   }
		   timer2xxRetransmit.rescheduleNextTimer(this);
		   
		   finishToUseTU(impl);
	   }
	   
   }
   
   /**
    * Invalidate the Transaction User
    *
    */
   public void invalidateTU(boolean removeFromAppSession, boolean removeFromSessionsTbl){
	   	synchronized (getSynchronizer()) {
	   		if (c_logger.isTraceEntryExitEnabled()) {
	   			c_logger.traceEntry(this, "invalidateTU", new Object[] {removeFromAppSession});
	   		}

	   		if(_invalidating && !_partialInvalidate) {
	   			//log some message
	   			return;
	   		}
	   		
	   		boolean partialInvalidate = _partialInvalidate;
	   		
	   		_invalidating = true;

	   		TransactionUserBase bTU = isBaseTUAlive();
	   		TUKey key = null;

	   		if(getTuImpl() != null){
	   			TransactionUserImpl impl = startToUseTU();
	   			
	   			impl.invalidateTU();
	   			
	   			// don't move the resetKey above, it fixes a memory leak
	   			// in some cases we got the getTransactionUserForOutboundRequest with setParams on the key,
	   			// then when removeTuWrapper invoked, the key was not found in the repository
	   			key = resetTempTUKeyValues();
	   			
	   			//This means there is a pending CANCEL on this session that wasn't change. TODO the isUnderlyingTransactionExists method name is misleading and hsould be changed.
	   			partialInvalidate = impl.isPendingCancelExists();

	   			finishToUseTU(impl);
	   			reuseTU();
	   			if(_transactionUser == null){
	   				//if _transactionUser == null this means that the invalidation was complete since transactions count was 0. In this case
	   		   		//it shouldn't matter if there was a pending CANCEL
	   				partialInvalidate = false;
	   			}
	   		}
	   		if (partialInvalidate && !_partialInvalidate) {
	   			if(c_logger.isTraceDebugEnabled()){
	   				c_logger.traceDebug(this,"invalidateTU", "TU with underlying transaction was detected, delaying the invalidate till a provisional response is received.");
	   			}
	   			bTU.rescheduleExpTimer();
	   			_partialInvalidate = true;
	   			return;
	   		}
	   		
	   		_partialInvalidate = false;
	   		
	   		bTU.invalidateBase(removeFromAppSession);

	   		//Moti: defect 513136.1 fix : don't invalidate TuWrapper too soon...
	   		// TODO: I don't know, maybe its too early to remove
	   		//System.out.println("Moti:TuWrapper#invaliteTU+remove("+getId());
	   		if (removeFromAppSession) {
	   			if (key != null){
	   				SessionRepository.getInstance().removeTuWrapper(key,removeFromSessionsTbl);
	   			}else{
	   				//this situation can only happen after replication when the tuImpl was not replicated 
	   				//since it is not in confirm state, but all other objects were already replicated
	   				//and now we are trying to invalidate the SAS
	   				//PAY ATTENTAION: this is a temporary solution until we change the replication implementation
	   				//to prevent this from happen
	   				if (c_logger.isTraceDebugEnabled()) {
	   					c_logger.traceDebug(this, "invalidateTU", "key is null, not able to remove from maps");
	   				}
	   			}

	   			ThreadLocalStorage.setTUKey(null);
	   		}
	   	}
   }
   
   
   
   //here was a duplicated method createKey() . use createKeyObject() instead.
   
   
   /**
    * Helper method which reuses a temporary TU Key Object. (which we store
    * on thread local)
    * Beware ! not to insert/save this method result in lists
    * this java reference is temporary TUKey and being overriden by many methods and
    * classes.
    * If you need to save the result for later usage :use TUKey.clone() 
    * Used in destroyAllSessions.
    * @return a TUKey with the current dialog attributes.
    */
   public TUKey resetTempTUKeyValues(){
	   TUKey key = ThreadLocalStorage.getTUKey();
	   if(isProxying()){
		   TransactionUserImpl impl = getTuImpl();
		   if(impl.isVirtualBranch()){
				if (c_logger.isTraceDebugEnabled()) {
   					c_logger.traceDebug(this, "resetTempTUKeyValues", "Virtual Branch");
   				}
			   // if this is a virtual branch - we should remove according to the CallId since it was inserted as
			   // local = getRemoteTag_2() + remote = getRemoteTag() + Callid
			   key.setup(getRemoteTag_2(), getRemoteTag(), impl.getCallId(), false);
		   }
		   else{
			   //we should use the original tu id, since this is the key that 
			   //was used to insert the tuWrapper object to the session repository maps
			   String localId = getSharedIdForDS();
			   key.setup(getRemoteTag(), getRemoteTag_2(), localId, true);
		   }
	   }
	   else{
		   //Anat+Moti: fixing memory leak in defect 571056
		   TransactionUserImpl impl = getTuImpl();
		   key.setup(getLocalTag(), getRemoteTag(), impl.getCallId(), false);
	   }
	   return key;
   }
   
   /**
    * This method used to Safe delete when needed.
    *
    */
   private void deletetIfNeeded(TransactionUserImpl tu){
//	   If _transactionUser == null meaning that it was already deleted from the
//	   deleteTU method. And now we should check the _methodInWorkCounter. If
//	   it is 0 - we can reuse this TransactionUserImpl
	   if(_transactionUser == null)
	   {
		   synchronized (_syncObject) {
			   if (c_logger.isTraceDebugEnabled()) {
				   c_logger.traceDebug(this, "deletetIfNeeded",
						   "_methodInWorkCounter = " + _methodInWorkCounter + " _cleaned = " + _cleaned);
			   }
			   if(_methodInWorkCounter == 0 && !_cleaned){
				   _cleaned = true;
//				   If there is no methods that are holding it - remove and reuse.
				   tu.cleanTU();
				   returnTUImplObjectToPool(tu);
			   }
		   }		   
	   }		
   }
   
   /**
    * Delete reference to the TransactionUser
    */
   protected void deleteTU() {
	   if (c_logger.isTraceEntryExitEnabled()) {
		   c_logger.traceEntry(this, "deleteTU");
	   }
	   TransactionUserImpl impl = isTransactionUserAlive();
	   _transactionUser = null; //Moti: its very important here not to comment this line
	   if (c_logger.isTraceDebugEnabled()) {
		   c_logger.traceDebug(this, "deleteTU",
				   "The TU Impl going to be Deleted and Reused " + impl);
	   }
	   deletetIfNeeded(impl);
	   if (c_logger.isTraceEntryExitEnabled()) {
		   c_logger.traceExit(this, "deleteTU");
	   }
   }
   
     
   /**
    * Set sipProvider
    * @param sipProvider
    */
   public void setProvider(SipProvider provider)
   {
   		synchronized (getSynchronizer()) {

		   TransactionUserImpl impl = startToUseTU();
		   impl.setProvider(provider);
		   finishToUseTU(impl);
   		}
	}
      
   /**
    * Indicates whether this session has been initialy associated with a server
    * or client transaction.
    * 
    * @return true for server transaction, false for client.
    */
   public boolean isServerTransaction() {
	   TransactionUserImpl impl = startToUseTU();
		boolean isServerTransaction = impl.isServerTransaction();
		finishToUseTU(impl);
		return isServerTransaction;
	}
   
   
   /**
    * Return the Jain Sip Contact Header. convenient for internal use.
    * 
    * @return The first contact header if more then one available.
    */
   public Address getContactHeader() {
	   TransactionUserImpl impl = startToUseTU();
		Address contact = impl.getContactHeader();
	   
		finishToUseTU(impl);
		
		return contact;
   }
   
    /**
     * Log to the sequence log associated with this SIP Session
     * @param state
     * @param info
     */
    public void logToContext(int state, Object info, Object extendedInfo)
    {
    	TransactionUserBase bTU = isBaseTUAlive();
    	bTU.logToContext(state, info, extendedInfo);
    }
    
    /**
     * Log to the sequence log associated with this SIP Session
     * @param state
     * @param info
     */
    public void logToContext(int state, int info, Object extendedInfo)
    {
    	TransactionUserBase bTU = isBaseTUAlive();
    	bTU.logToContext(state, info, extendedInfo);
    }
    
    /**
     * Log to the sequence log associated with this SIP Session
     * @param state
     * @param info
     */
    public void logToContext(int state, Object info)
    {
    	TransactionUserBase bTU = isBaseTUAlive();
    	bTU.logToContext(state, info);
    }
    
    /**
     * Log to the sequence log associated with this SIP Session
     * @param state
     * @param info
     */
    public void logToContext(int state)
    {
    	TransactionUserBase bTU = isBaseTUAlive();
    	bTU.logToContext(state);
    }
    
    /**
     * Log to the sequence log associated with this SIP Session
     * @param state
     * @param info
     */
    public void logToContext(int state, int info)
    {
    	TransactionUserBase bTU = isBaseTUAlive();
    	bTU.logToContext(state, info);
    }
    
    /**
     * Log to the sequence log associated with this SIP Session
     * @param state
     * @param info
     */
    public void logToContext(int state, boolean info)
    {
    	TransactionUserBase bTU = isBaseTUAlive();
    	bTU.logToContext(state, info);
    }
		
    /**
     * Returns TU shared ID
     * @return
     */
    public String getSharedID(){
    	TransactionUserBase bTU = isBaseTUAlive();
        return bTU.getSharedId();
    }
    
    /**
     * Returns TU shared ID
     * @return
     */
    public boolean isValid(){
    	TransactionUserBase bTU = isBaseTUAlive();
        return bTU.isValid();
    }
    
    /**
     * Checks if TU is alive before executing 
     * @see TransactionUserImpl#shouldBeReplicated()
     * @return
     */
    public boolean shouldBeReplicated(boolean forBootstrap){
    	
		TransactionUserImpl impl = startToUseTU(!forBootstrap);
		if (impl == null) {
			// TU is no longer alive, and we are replicating for bootstrap.
			// (non-bootstrap would throw an exception in this case)
			return false;
		}

		boolean rc = impl.shouldBeReplicated(forBootstrap);

		finishToUseTU(impl);
		
        return rc;
    }
    
    /**
     * Checks if TU is alive before executing 
     * @see TransactionUserImpl#setForwardToApplication(boolean)
     */
    public void setForwardToApplication(boolean b){
    	synchronized (getSynchronizer()) {

			TransactionUserImpl impl = startToUseTU();
			impl.setForwardToApplication(b);
			finishToUseTU(impl);
    	}
    }
    
    
    /**
     * Helper method which called every time when some method that access
     * TransactionUserImpl. Needed to prevent invalidate of TU in time
     * when some method use it.
     * @param mayThrow ok to throw an exception if already invalidated
     * @return the TransactionUserImpl, null if invalidated and mayThrow is false
     */
    private TransactionUserImpl startToUseTU(boolean mayThrow) {
    	TransactionUserImpl impl = null;
    	synchronized (_syncObject) {
    		if (mayThrow) {
    			impl = isTransactionUserAlive();
    		}
    		else {
    			impl = getTuImpl();
        		if (impl == null) {
        			return null;
        		}
    		}
    		_methodInWorkCounter ++;
		}
    	return impl;
	}

    /**
     * @see #startToUseTU(boolean)
     */
    protected TransactionUserImpl startToUseTU() {
    	return startToUseTU(true);
    }
	
    /**
	 * Helper method that called at the end of each method that working with
	 * transactionUserImpl. It will decrease _methodInWorkCounter counter and
	 * will call to deletetIfNeeded() method.
	 * 
	 * @param impl
	 */
    protected void finishToUseTU( TransactionUserImpl impl){
    	synchronized (_syncObject) {
        	_methodInWorkCounter--;
		}
        deletetIfNeeded(impl);
    }
    
    /**
     * Returns the next sipSessionId
     * @return
     */
    public int getNexSipSessionId(){
    	synchronized (getSynchronizer()) {

	    	TransactionUserImpl impl = startToUseTU();
	    	
	    	int sessionId = impl.getNexSipSessionId();
	    	
	    	finishToUseTU(impl);
	    	
	        return sessionId;
    	}
    }

    
    /**
     * Save the related sessionId and header name. Used to connect new TU created for
     * request which contain "Join" or "Replace" to the TU which is defined in 
     * those methods.
     * @param id
     */
    public void setRelatedSessionData(String sessionId, String header) {
    	TransactionUserImpl impl = startToUseTU();
    	
    	impl.setRelatedSessionId(sessionId);
    	impl.setRelatedSessionHeader(header);
    	
    	finishToUseTU(impl);
	}
    
    /**
	 * Returns Related SipSession id
	 * @return
	 */
	public String getRelatedSipSessionId() {
		TransactionUserImpl impl = startToUseTU();

		String relatedId = impl.getRelatedSipSessionId();

		finishToUseTU(impl);
		
		return relatedId;
	}

    /**
	 * Returns Related SipSession header (Join/Replaces)
	 * @return
	 */
	public String getRelatedSipSessionHeader() {
		TransactionUserImpl impl = startToUseTU();

		String relatedId = impl.getRelatedSipSessionHeader();

		finishToUseTU(impl);
		
		return relatedId;
	}
	
	/**
	 * Returns related SipSession.
	 * @return
	 */
	public SipSession getRelatedSipSession() {
    	TransactionUserBase bTU = isBaseTUAlive();
        return bTU.getRelatedSipSession();
	}
	
	
    /**
     * @return if this TU during invalidation
     */
    public boolean isInvalidating(){
    	if(_invalidating){
    		return true;
    	}
    	TransactionUserImpl impl = startToUseTU();
    	
    	boolean invalidating = impl.isInvalidating();
    	
    	finishToUseTU(impl);
    	
    	return invalidating;
    	
    }

    /**
	 * Remove the client transaction from the TUImpl when it is terminated
	 * @param trnsaction
	 */
	public void removeClientTransaction(ClientTransaction trnsaction){
		TransactionUserImpl impl = startToUseTU(false);
		if(impl != null){
			impl.removeClientTransaction(trnsaction);
			finishToUseTU(impl);
		}
		else{
			if (c_logger.isTraceDebugEnabled()) {
				   c_logger.traceDebug(this, "removeClientTransaction",
						   "Related TU was already invalidated.");
			   }
		}
		
	}
	/**
	 * Store INVITE transactions
	 * @param trnsaction
	 */
	public void storeClientTransaction(ClientTransaction transaction){
		TransactionUserImpl impl = startToUseTU();
		impl.storeClientTransaction(transaction);
		finishToUseTU(impl);
	}
	
	/**
	 * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#clientTransactionTerminated()
	 */
	public void clientTransactionTerminated(SipServletRequestImpl request) {
    	synchronized (getSynchronizer()) {

			if(!request.getMethod().equals(Request.ACK)){
				transactionTerminated();
			}
    	}
	}

	/**
	 * @see com.ibm.ws.sip.container.transaction.ServerTransactionListener#serverTransactionTerminated()
	 */
	public void serverTransactionTerminated(SipServletRequestImpl request) {
    	synchronized (getSynchronizer()) {

			if(!request.getMethod().equals(Request.ACK)){
				transactionTerminated(false, request.getMethod());
			}
    	}
	}
	
	/**
	 * callback method called by server transaction when this is a DTU and 
	 * its "original transaction" was terminated. And this DTU should
	 * invalidate itself if needed.
	 * This DTU is not a m_listener to the  ServerTransaction, it was created as result
	 * of provisional / error response with different tag but was not selected to
	 * be an active listener to this original ServerTransaction object.
	 */
	public void originalServerTransactionTerminated(SipServletRequestImpl request) {
    	synchronized (getSynchronizer()) {

			if(!request.getMethod().equals(Request.ACK)){
				tryToInvalidate();
			}
    	}
	}

	/**
	 * This code will delete the TU Impl and reuse it.
	 */
	protected synchronized void reuseTU() {
		
		if(PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.WAS855_TU_COUNTER_TRANSACTION_FIX) == true){
			if(_transactionsCounter == 0 && _NewTransactionsCounter != 0){
				if(PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.WAS855_TU_COUNTER_TRANSACTION_FIX_SYS_OUT) == true){
					System.out.println("AvayayLeakFix: ERROR !!!! LEAK !!! " + " TU = " + getId() +" _transactionsCounter = " + _transactionsCounter + " _NewTransactionsCounter = " + _NewTransactionsCounter);
				}
			}
			if(_NewTransactionsCounter < 0 ){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "reuseTU","_NewTransactionsCounter is less than 0 ");
				}
	    	}
			else if (_NewTransactionsCounter == 0 && _transactionUser != null && !_transactionUser.isUnderlyingTransactionsBeingTerminated()) {
					// If _transactionUser is null - this is not a first time when
					// deleteTU() called - prevent it
				if(PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.WAS855_TU_COUNTER_TRANSACTION_FIX_SYS_OUT) == true){
					System.out.println("AvayayLeakFix: deleting " + " TU = " + getId() +" _transactionsCounter = " + _transactionsCounter + " _NewTransactionsCounter = " + _NewTransactionsCounter);
				}
				deleteTU();
				} 
			else{
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "reuseTU",
							"This TU will be reused later as it still has open transactions "
									+ getTuImpl());
				}
			}
			return;
		}
    	
		
		// original behavior before WAS855_TU_COUNTER_TRANSACTION_FIX
		
		if(_transactionsCounter < 0 ){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "reuseTU","_transctionsCounter is less than 0 ");
			}
    	}
		else if (_transactionsCounter == 0 && _transactionUser != null && !_transactionUser.isUnderlyingTransactionsBeingTerminated()) {
				// If _transactionUser is null - this is not a first time when
				// deleteTU() called - prevent it
				deleteTU();
			} 
		else{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "reuseTU",
						"This TU will be reused later as it still has open transactions "
								+ getTuImpl());
			}
		}
	}
	
	/**
	 * Notify all listeners about activation
	 */
	protected void notifyTUOnActivation(){
		TransactionUserImpl transactionUser = getTuImpl();
		if( transactionUser != null){
			transactionUser.notifyOnActivation();
		}
	}
    
    /**
	 * @see java.lang.Object#toString()
	 * this is printed only upon some SIP exceptions
	 */
	public String toString() {
		TransactionUserBase _baseTU = getTuBase();
    	if (_baseTU != null ) {
    		isBaseTUAlive();
			 StringBuffer _myInformation = new StringBuffer(_baseTU.toString());
			_myInformation.append(" Wrapper Info = ");
			_myInformation.append(super.toString());
			return _myInformation.toString();
    	}
    	return super.toString();
	}

	/**
	 * @see com.ibm.ws.sip.container.util.wlm.SipDialogContext#setDialogAux(com.ibm.ws.sip.container.util.wlm.DialogAux)
	 */
	public void setDialogAux(DialogAux da){
		TransactionUserBase bTU = isBaseTUAlive();
		bTU.setDialogAux(da);
	}

	/**
	 * @see com.ibm.ws.sip.container.util.wlm.SipDialogContext#getDialogAux()
	 */
	public DialogAux getDialogAux(){
		TransactionUserBase bTU = isBaseTUAlive();
		return bTU.getDialogAux();
	}

	/**
	 * @see com.ibm.ws.sip.container.util.wlm.SipDialogContext#canHaveDialog()
	 */
	public boolean canHaveDialog() {
		return isTUDialog();		
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.wlm.SipDialogContext#getDialogState()
	 */
	public int getDialogState(){
		TransactionUserBase bTU = isBaseTUAlive();
		return bTU.getWLMDialogState();
	}

	 /**
     * Save uri for MultiHost support.
     * @param uri
     */
    public void setOutboundInterface(InetSocketAddress address) {
    	TransactionUserImpl impl = startToUseTU();
    	
    	impl.setOutboundInterface(address);
    	
    	finishToUseTU(impl);
    }
	
	 /**
     * Save uri for MultiHost support.
     * @param uri
     */
    public void setOutboundInterface(InetAddress address) {
    	TransactionUserImpl impl = startToUseTU();
    	
    	impl.setOutboundInterface(address);
    	
    	finishToUseTU(impl);
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
    public B2buaHelper getB2buaHelper(boolean create, UAMode mode) throws IllegalStateException {
    	// TODO should not manage creation logic and throw exception
    	// create B2buaHelper in the same way as Proxy created
    	TransactionUserImpl impl = startToUseTU();
    	
    	B2buaHelper helper = getTuImpl().getB2buaHelper(create,mode);
    	
    	finishToUseTU(impl);
		
    	return helper;
	}
    
    /**
     * Determines whether the session is in B2B mode.
     * 
     * @return
     */
    public boolean isB2B() {

    	TransactionUserImpl impl = startToUseTU();
    	
    	boolean isB2B = getTuImpl().isB2B();
    	
    	finishToUseTU(impl);
		
    	return isB2B;
    }
    
    
    /**
     * Determines whether the session is in UAS mode.
     * 
     * @return
     */
    public boolean isUAS() {

    	TransactionUserImpl impl = startToUseTU();
    	
    	boolean isUAS = _transactionUser.isUAS();
    	
    	finishToUseTU(impl);
		
    	return isUAS;
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
    	TransactionUserImpl impl = startToUseTU();
    	
    	List<SipServletMessage> list = getTuImpl().getPendingMessages(mode);
    	
    	finishToUseTU(impl);
		
    	return list;
    }
    
    /**
	 * Remove uncommitted message for the given mode of the session.
	 * 
	 * @param msg
	 * @param mode
	 */
	public void removeB2BPendingMsg(SipServletMessage msg, UAMode mode) {
		TransactionUserImpl impl = startToUseTU();
		
		getTuImpl().removeB2BPendingMsg(msg, mode);
		
		finishToUseTU(impl);
	}
	
    /**
	 * Add uncommitted message in the order of increasing 
	 * CSeq number for the given mode of the session.
	 * 
	 * @param msg
	 * @param mode
	 */
    public void addB2BPendingMsg(SipServletMessage msg,UAMode mode){
    	TransactionUserImpl impl = startToUseTU();
    	
    	getTuImpl().addB2BPendingMsg(msg, mode);
    	finishToUseTU(impl);
    }
    
    /**
     * Retrieve is failed response sent on dialog initial request
     * 
     * @return true or false 
     */
    public boolean isFailedResponseSent(){  
 	   
 	  TransactionUserImpl impl = startToUseTU();
  	
 	  boolean isFailedResponseSent = getTuImpl().isFailedResponseSent();
  	
 	  finishToUseTU(impl);
		
 	  return isFailedResponseSent;
    }
    
    /**
     * Retrieve is session has been terminated
     * @return true or false 
     */
    public boolean isTerminated() {
		
    	TransactionUserImpl impl = startToUseTU();
	  	
	 	boolean isTerminated = getTuImpl().isTerminated();
	  	
	 	finishToUseTU(impl);
			
	 	return isTerminated;
	}
    
    /**
     * Sets a response code to timeout
     */
    public void setTimeoutResponseCode() {
    	TransactionUserImpl impl = startToUseTU();
    	
    	impl.setSessionInvalidatedResponse(SipServletResponse.SC_REQUEST_TIMEOUT);
    	
    	finishToUseTU(impl);
    }

    /**
     * Set transaction mode to be B2b mode 
     */
    public void setB2buaMode() {
		
    	TransactionUserImpl impl = startToUseTU();
	  	
	 	getTuImpl().setB2buaMode();
	  	
	 	finishToUseTU(impl);
	}
    
    
    public void setIsB2bua(boolean bool) {
    	TransactionUserImpl impl = startToUseTU();
    	impl.setIsB2bua(bool);
	 	finishToUseTU(impl);
	}
    
    
    /**
     * Set transaction mode to be UAS mode 
     */
    public void setUASMode() {
    	TransactionUserImpl impl = startToUseTU();
	 	_transactionUser.setUASMode();
	 	finishToUseTU(impl);
	}
    
    public Vector<String> getRouteHeaders() {
    	TransactionUserImpl impl = startToUseTU();
    	Vector<String> routeHeaders  = impl.getRouteHeaders();
		return routeHeaders;
    }
    
    
    /**
     * Gets subscriber URI form the TransactionUser
     */
    public URI getSubscriberUri() {
    	TransactionUserImpl impl = startToUseTU();
    	URI subscriberURI= impl.getSubscriberURI();    	
    	finishToUseTU(impl);
    	return subscriberURI;
    }
    
    /**
     * Sets subscriber URI through application selection process 
     */
    public void setSubscriberUri(URI subscriberURI) {
    	TransactionUserImpl impl = startToUseTU();
    	impl.setSubscriberURI(subscriberURI);    	
    	finishToUseTU(impl);
    }
    
    /**
     * Getter for region saved in transaction wrapper
     * @return
     */
    public SipApplicationRoutingRegion getRegion(){
    	TransactionUserImpl impl = startToUseTU();
    	SipApplicationRoutingRegion region = impl.getRegion();    	
    	finishToUseTU(impl);
    	return region;
    }
    
    public void setRegion(SipApplicationRoutingRegion region){
    	TransactionUserImpl impl = startToUseTU();
    	impl.setRegion(region);    	
    	finishToUseTU(impl);
    }
    
    /**
     * Getter for outbound interface index for this TU.
     * @return Index of outbound interface to be used by this TU
     */
	public int getPreferedOutboundIface(String transport)
	{
    	TransactionUserImpl impl = startToUseTU();
    	int preferedOutBoundIfaceIdx = impl.getPreferedOutboundIface(transport);    	
    	finishToUseTU(impl);
		return preferedOutBoundIfaceIdx;
	}
	
	public int getOriginatorPreferedOutboundIface(String transport)
	{
    	TransactionUserImpl impl = startToUseTU();
    	int preferedOutBoundIfaceIdx = impl.getOriginatorPreferedOutboundIface(transport);    	
    	finishToUseTU(impl);
		return preferedOutBoundIfaceIdx;
	}
	/**
	 * Helper method which creates derived session based on this TU
	 * @param response
	 * @return
	 */
	public TransactionUserWrapper createDerivedTU(Response response, String reason){	
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "createDerivedTU",
					"New derived for proxy created. Reason - " + reason);
		}
		
		TransactionUserImpl impl = startToUseTU();

		TransactionUserWrapper derivedTU = null;
    			
		derivedTU = new TransactionUserWrapper(this);

		if (derivedTU.isProxying()) {
			//Pass false to replaceExistingTU since this is a new session.
			derivedTU.setRemoteTag_2(response.getToHeader().getTag(), false);
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createDerivedTU",
						"New derived for proxy created");
			}
		}

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "createDerivedTU",
					"New derived " + derivedTU);
		}
		
		finishToUseTU(impl);
		return derivedTU;
	}

	public TransactionUserImpl getTuImpl()
	{
		return _transactionUser;
	}
	
	public void setRemoteTag_2(String tag){
		setRemoteTag_2(tag, true);
	}
	/**
     * Set the remoteTag_2 - useful only in Proxy mode;
     * @return
     */
    public void setRemoteTag_2(String tag, boolean replaceExistingTU){
    	TransactionUserImpl impl = startToUseTU();
		impl.setDestinationTagInProxy(tag, replaceExistingTU);
		finishToUseTU(impl);
    }
    
    /**
     * Set the remoteTag - useful only in Proxy mode;
     * @return
     */
    void setRemoteTag(String tag){
    	TransactionUserImpl impl = startToUseTU();
		impl.setRemoteTag(tag);
		finishToUseTU(impl);
    }
	
	 /**
     * Returns the remoteTag_2 - useful only in Proxy mode;
     * @return
     */
	public String getRemoteTag_2(){
    	TransactionUserImpl impl = startToUseTU();
		String rt = impl.getDestinationTagInProxy();
		finishToUseTU(impl);
		return rt;
    }
	
	 /**
     * Returns the remoteTag - useful only in Proxy mode;
     * @return
     */
	public String getRemoteTag(){
    	TransactionUserImpl impl = startToUseTU();
		String rt = impl.getRemoteTag();
		finishToUseTU(impl);
		return rt;
    }

	/**
	 * Returns the localRR from TUImp
	 * @return
	 */
	public String getSharedIdForDS() {
		TransactionUserImpl impl = startToUseTU();
		String rt = impl.getSharedIdForDS();
		finishToUseTU(impl);
		return rt;
	}
	
	/**
	 * Returns true if transaction is a proxy transaction with error response
	 * @return
	 */
	public boolean isProxingErrorResponse(){
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "isProxingErrorResponse");
		}
		boolean result = false;
		try{
			SipServletResponse res;
			if(branch != null && ( res = branch.getResponse()) !=null){
				result = SipUtil.isErrorResponse(res.getStatus());
			}
			return result;
		}
		finally{
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(this, "isProxingErrorResponse", result);
			}
		}
	}
	
	/**
	 *  @see com.ibm.ws.sip.container.tu.TUInterface#canCreateDS()
	 */
	public boolean canCreateDS() {
		TransactionUserBase bTU = isBaseTUAlive();
		return bTU.canCreateDS();
	}

	/**
	 * Returns if represented SipSession is a dialog.
	 * @return
	 */
	public boolean isDialog(){
		TransactionUserBase bTU = isBaseTUAlive();
		return bTU.isDialog();
	}
	
	/**
	 *  @see com.ibm.ws.sip.container.tu.TUInterface#setCanCreateDS(boolean)
	 */
	public void setCanCreateDS(boolean flag) {
		TransactionUserBase bTU = isBaseTUAlive();
		bTU.setCanCreateDS(flag);
	}
	   /**
	    * Indicates whether the proxy of this session has received
	    * a final response
	    * 
	    * @return true if the proxy of this session has received
	    */
	   public boolean isProxyReceivedFinalResponse() {
		   if(getTuImpl() == null){
			   return _receivedFinalResponse;
		   }
		   TransactionUserImpl impl = startToUseTU();
		   boolean receivedFinalResponse = impl.isProxyReceivedFinalResponse();
		   finishToUseTU(impl);
		   return receivedFinalResponse;
	  }

	/**
	 * Returns reference to basic transaction user handler.
	 *    
	 * @return reference to basic transaction user handler.
	 * 
	 * @see TransactionUserBase
	 */
	public TransactionUserBase getTuBase() {
		   return _baseTU;
	}
	
	/**
	* sets the flag indicating whether the proxy of this session has received
	* a final response
	*/
	public void setProxyReceivedFinalResponse(boolean receivedFinalResponse, int status) {
		_receivedFinalResponse = receivedFinalResponse; 
		TransactionUserImpl impl = startToUseTU();
		impl.setProxyReceivedFinalResponse(receivedFinalResponse, status);
		finishToUseTU(impl);
	}

	/**
	 * If this TU has a proxy branch with related derived sessions, it will check if any of them got a final
	 * response. That will indicate what is the state of the underline common transaction.
	 * @return
	 */
	public boolean hasAnyRelatedTUGotFinalResponse(){
		if(branch == null) return false;
		
		return branch.hasAnyTUGotFinalResponse();
	}
	/**
	 * If this TU has a proxy branch it will return the last final response status received by this branch.
	 * If no final response was received, -1 will be returned.
	 * @return
	 */
	public int getLastProxyResponseStatus(){
		if(branch == null) return -1;
		
		return branch.getLatestFinalResponseStatus();
	}
	/**
	 * 
	 * @returns application name from BaseTU
	 */
	public String getAppName(){
		TransactionUserBase bTU = isBaseTUAlive();
		return bTU.getAppName();
	}
	
    /**
	 * Getting the session API synchronizing object
	 * @return
	 */
	public Object getSynchronizer() {
	    isBaseTUAlive();
		return _baseTU.getSynchronizer();
	}

	/**
	 * Getting the session service synchronizing object
	 * @return
	 */
	public Object getServiceSynchronizer() {
	    isBaseTUAlive();//TODO what is the correct behavior is this thing throws an exception
	    //is it even possible?
		return _baseTU.getServiceSynchronizer();
	}
	
	public void setShouldBeReused() {
		//Moti: this is stub method. it should be deleted ASAP
		// it is here only because Nitzan is holding tuImpl
		// we never really care fot the reuse flag...
	}

	/**
	 * Set session key base attribute to be used when sip app session is created.
	 * 
	 * @param keyBase
	 */
	public void setSessionKeyBase(String keyBase) {
	    isBaseTUAlive().setSessionKeyBase(keyBase);
	    
		if (keyBase != null) {
			String sipAppID = getApplicationId();
		    SessionRepository.getInstance().setSessionKeyBase(keyBase, sipAppID);			
		}
	}
	

	/**
	 * Getter for session key based key
	 *  
	 * @return
	 */
	public String getSessionKeyBaseKey() {
		return isBaseTUAlive().getSessionKeyBase();
	}

	
//	/**
//	 *  @see javax.servlet.sip.SipSession#isReadyToInvalidate()
//	 */	
//	public boolean isReadyToInvalidate() throws IllegalStateException {
//		if(c_logger.isTraceEntryExitEnabled()) {
//			c_logger.traceEntry(this, "isReadyToInvalidate");
//		}
//		boolean readyToInvalidate = false;
//		SipSessionImplementation.State state = getState();
//		
//
//		if (hasOngoingTransactions()) {
//			readyToInvalidate = false;
//		} else {
//			if (isDialog() && state == SipSession.State.TERMINATED) {
//				readyToInvalidate = true;
//			}
//			if(isProxying() && !isRRProxy() && state == SipSession.State.CONFIRMED) {
//				readyToInvalidate = true;
//			}
//			
//			if (!isUAS() && state == SipSession.State.INITIAL && 
//					_transactionUser.getFinalResponseStatus() > 0 && !SipUtil.is2xxResponse(_transactionUser.getFinalResponseStatus())) {
//				readyToInvalidate = true;
//			}
//		}
//
//		if(c_logger.isTraceEntryExitEnabled())
//			c_logger.traceExit(this, " isReadyToInvalidate", Boolean.valueOf(readyToInvalidate));
//		return readyToInvalidate;
//	}	
	
	
	
	public boolean isReadyToInvalidate() throws IllegalStateException {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "isReadyToInvalidate");
		}

		boolean readyToInvalidate = true;
		SipSessionImplementation.State state = getState();

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "isReadyToInvalidate",
							"state: " + state + " , ongoing transactions: "+_transactionsCounter);
		}
		
		if(hasOngoingTransactions()){
			readyToInvalidate = false;
		} else if (isDialog()){
			if(isProxying() && !isRRProxy()){
				if (state != State.CONFIRMED && state != State.TERMINATED){
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "isReadyToInvalidate",
										"none record route proxy is not ready to invalidate, state: " + state);
					}
					readyToInvalidate = false;
				}
			}
			else if (state != State.INITIAL && state != State.TERMINATED) {
				readyToInvalidate = false;
			}
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "isReadyToInvalidate", readyToInvalidate);
		}		
		return readyToInvalidate;
	}

	/**
	 * Check whether the related application is JSR 289 or older.
	 * @return
	 */
	public boolean isJSR289Application(){
		SipServletDesc sd = getSipServletDesc();
		if( sd != null){
			return sd.getSipApp().isJSR289Application();
		}
		return false;
    }

	/**
	 * Did this session processed it's incoming message.
	 * @return
	 */
	public boolean isWaitingForPendingMessage() {
		return _pendingMessageExists;
	}
	
	/**
	 * Set's flag which indicates that the session has processed it's incoming message.
	 * If in the meantime the app session was invalidated we are resetting the appSession
	 * pointers to detach the session from the app session. 
	 */
	public void setInitialRequestProcessed() {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "setInitialRequestProcessed", _pendingMessageExists);
		}
		
		if (!_pendingMessageExists) {
			return;
		}
		
		_pendingMessageExists = false;
		
		SipApplicationSessionImpl appSession = getAppSessionForInternalUse();
		// application session is invalidated
		if (appSession == null || (appSession != null && !appSession.isValid())) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setInitialRequestProcessed", "App session invalidated, reinitialize synchronizer.");
			}
			
			getTuBase().reinitilize();
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "setInitialRequestProcessed");
		}
	}
	
	/**
	 * Get TransactionUserImpl object from the pool
	 */
	public static TransactionUserImpl getTUImplObjectFromPool() {
		
		//new Sip Session created - update not replicated sessions counter
		if (PerformanceMgr.getInstance() != null) {
			PerformanceMgr.getInstance().incrementNotReplicatedSipSessionsCounter();
		}
		return (TransactionUserImpl)s_tuPool.get();
	}
	
	/** 
	 * Return TransactionUserImpl object to the pool
	 */
	public static void returnTUImplObjectToPool(TransactionUserImpl tu) {
		s_tuPool.putBack(tu);
	}
	
	/**
	 * Indicates whether the transaction is derived
	 * @return true if derived
	 */
	public boolean isDerived() {
		return _derived;
	}
	
	/**
	 * Gets the original TUWrapper.
	 * @return the original TUWrapper
	 */
	public TransactionUserWrapper getOrigTUWrapper() {
		return _origTUWrapper;
	}
	
	/**
	 * Remove the pending messages in UAS mode from original TU.
	 * Add the pending messages in UAS mode from derived TU to original TU.
	 * 
	 * @param derivedTUWrapper the derived TUWrapper
	 */
	public void overridePendingMessagesByDerived(TransactionUserWrapper derivedTUWrapper) {
		for (SipServletMessage msg: getPendingMessages(UAMode.UAS)) {
			removeB2BPendingMsg(msg, UAMode.UAS);
		}
		for (SipServletMessage msg: derivedTUWrapper.getPendingMessages(UAMode.UAS)) {
			addB2BPendingMsg(msg, UAMode.UAS);
		}
	}


	@Override
	public void removeTransaction(String method) {
		_NewTransactionsCounter --;
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "removeTransaction",
					"removeTransaction. TU = " + getId() + " _NewTransactionsCounter = " + _NewTransactionsCounter);
		}
	}

	@Override
	public void addTransaction(String method) {
		_NewTransactionsCounter ++;
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "addTransaction",
					"addTransaction. TU = " + getId() + " _NewTransactionsCounter = " + _NewTransactionsCounter);
		}
	}
}

