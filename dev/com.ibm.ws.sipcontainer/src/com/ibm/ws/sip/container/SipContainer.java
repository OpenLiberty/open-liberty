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
package com.ibm.ws.sip.container;

import jain.protocol.ip.sip.SipPeerUnavailableException;

import java.util.Iterator;
import java.util.List;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.appqueue.MessageDispatcher;
import com.ibm.ws.sip.container.events.TasksInvoker;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.protocol.SipProtocolLayer;
import com.ibm.ws.sip.container.router.SipAppDescManager;
import com.ibm.ws.sip.container.router.SipRouter;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipSessionImplementation;
import com.ibm.ws.sip.container.servlets.WASXHttpSessionListener;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.was.WASHttpSessionListener;
import com.ibm.ws.sip.container.was.WASXTasksInvoker;
import com.ibm.ws.sip.container.was.WebsphereInvoker;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.sip.security.auth.SipSecurityManager;
//import com.ibm.wsspi.channel.framework.ChannelFrameworkFactory;
//import com.ibm.wsspi.channel.framework.exception.ChainException;
//import com.ibm.wsspi.channel.framework.exception.ChannelException;
//TODO Liberty the following import no longer exist: import com.ibm.ws.bootstrap.ExtClassLoader;
//TODO Liberty the following imports don't longer exist as we don't support HA in Liberty
//import com.ibm.ws.sip.container.failover.FailoverMgrLoader;

/**
 * @author Amir Perlman, Jul 2, 2003
 *
 * The Sip Container object itself, can be operated as a standalone application 
 * or as an add-on to Websphere Web Container. 
 * @update moti: Jan/2008: class will now start to access the SessionRepository  
 */
public class SipContainer
{
	
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipContainer.class);
    
    /**
     * Tcp outbound chain name - used to initialize the 
     * channel framework tcp thread during warmup.
     */
    private static final String TCP_OUTBOUND_CHAIN_NAME = "TcpOutboundChain_0";

    /**
     * The message receiver
     */
    private SipProtocolLayer m_msgReceiver;


    /**
     * The router
     */
    private SipRouter m_router = SipRouter.getInstance();

    /**
     * This object will perform dispatching of messages and tasks to the application threads pool
     */
    private MessageDispatcher _messageDispatcher = new MessageDispatcher();
    
    /**
     *  variable that defined if container should allow to application to add/set
     *  system headers
     */
    private boolean m_enableSysHeadersModify = CoreProperties.SYSTEM_HEADERS_MODIFY_DEFAULT;
   
    /**
     * Singleton instance of the Container. 
     */
    private static SipContainer c_sipContainer = new SipContainer();
    
    /**
     * The MBean for managing and receiving events from the container. 
     */
    private SipContainerMBean m_sipContainerMBean;

    /**
     * Flag indicating whether we an external router - a SLSP/Proxy in front of the 
     * container. 
     */
	private boolean m_isUsingExternalRouter = false;
    
	/**
	 * Invoker for SIP tasks
	 */
	private static TasksInvoker s_tasksInvoker = new WASXTasksInvoker();
	
	/**
	 * The listener for http sessions
	 */
	private static WASHttpSessionListener s_httpSessionListener = null;
	
	/**
	 * The security manager object.
	 */
	private SipSecurityManager m_securityManager = new SipSecurityManager();
	

	/**
	 * Being modified when administrator sets quiesce on/off
	 * State that the server will not recieve more calls 
	 */
	private boolean s_isInQuiesce = false;

	
	
	
	/**
	 * indicate if the SIP container was initialized 
	 */
	private boolean _isInitialized;
	
    /**
     * Constructor
     */
    private SipContainer()
    {
    	_isInitialized = false;
    }

    /**
     * Initializing the SIP Container. This method is synchronized to prevent concurrent initialization.
     * The container will only get initialized once.
     */
    public synchronized void init(){
    	if (_isInitialized) {
    		return;
    	}

		WebsphereInvoker invoker = WebsphereInvoker.getInstance();

		getRouter().setInvoker(invoker);

		try {		
			initialize();
			_isInitialized = true;
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("SipContainer init container");
			}
		} catch (IllegalStateException e) {
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.sip.container.initialization.failed", e.getLocalizedMessage());
				return;
			}
		} catch (SipPeerUnavailableException e) {
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.sip.container.initialization.failed", e.getLocalizedMessage());
				return;
			}
		}
    }
    
    /**
     * Initializing the SIP Container. At this point the container will
     * init the transport level and start listening for events. 
     * 
     * @param invoker - the siplet invoker for this container
     * @throws SipPeerUnavailableException
     */
    private void initialize() throws IllegalStateException, SipPeerUnavailableException
    {
        if (c_logger.isTraceEntryExitEnabled())
        {
   
            c_logger.traceEntry(this, "initialize");
        }

        // Create and start the protocol layer
  
        m_msgReceiver = SipProtocolLayer.getInstance();
        m_msgReceiver.init();
        
        s_httpSessionListener = new WASXHttpSessionListener();
      
        
         
        if(c_logger.isInfoEnabled())
        {
            
            c_logger.info(
                "info.container.initialized",
                Situation.SITUATION_START,
                null);
    
            String[] args = { getVersion() }; 
            
            c_logger.info(
                "info.container.version",
                Situation.SITUATION_START,
                args);
        }
        
        //Initialize the SIP Container MBean
        m_sipContainerMBean = new SipContainerMBean();
               
        //Set the properties read for this configuration
        m_enableSysHeadersModify = PropertiesStore.getInstance().getProperties()
        	.getBoolean(CoreProperties.SYSTEM_HEADERS_MODIFY);

        // Notify the security manager that security is going to be needed soon.
        m_securityManager.onContainerStarted();

        if (c_logger.isTraceDebugEnabled()) {
            c_logger
                .traceDebug(
                            this,
                            "initialize",
                            "System headers modification is enabled: "
                            + m_enableSysHeadersModify);
        }
        
        //Load all required classes prior to initialization to avoid hiccups 
		//when we start receiving requests. 
		
        if (c_logger.isTraceEntryExitEnabled())
        {
            c_logger.traceExit(this, "initialize");
        }
    }

  
    
    /**
     * Switches to the system class loader and loads some channel framework tcp thread.
     */
    private void loadChannelFrameworkTcp() {
    	
    	//Save the current class loader
    	ClassLoader currentThreadClassLoader = Thread.currentThread().getContextClassLoader();
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceDebug(this, "loadChannelFrameworkTcp", "switching to system class loader from current class loader: " + currentThreadClassLoader);
    	}
//    	TODO Liberty - Switch to Anat's implementation 
//		try {
//	        //Switch to the system (WAS) class loader
//			//TODO replace ExtClassLoader.getInstance() with the correlated Liberty class loader. the current classLoader is just for the build to pass
//	        Thread.currentThread().setContextClassLoader(new com.ibm.xtq.bcel.util.ClassLoader()/*ExtClassLoader.getInstance()*/);
//	        if (c_logger.isTraceDebugEnabled()) {
//	            c_logger.traceDebug(this, "loadChannelFrameworkTcp", "class loader after switch: " + Thread.currentThread().getContextClassLoader());
//	        }
//	        
//	        //PM78132: load the channel framework tcp thread here, in order to avoid
//	        //a case where this thread is spawned by the web container class loader,
//	        //and then when the application is undeployed, ConnectChannelSelector class
//	        //still holds reference to the application class loader (which is not good). 
//			ChannelFrameworkFactory.getChannelFramework().getOutboundVCFactory(TCP_OUTBOUND_CHAIN_NAME);
//			
//		} catch (ChannelException e) {
//			logException(e);
//		} catch (ChainException e) {
//			logException(e);
//		} finally {
//			//Go back to the original class loader
//			Thread.currentThread().setContextClassLoader(currentThreadClassLoader);
//			if (c_logger.isTraceDebugEnabled()) {
//	            c_logger.traceDebug(this, "loadChannelFrameworkTcp", "class loader after switch back: " + Thread.currentThread().getContextClassLoader());
//	        }
//		}
    }

    /**
	 * Extract the version info from the manifest.mf included in the jar file.
	 */
	private String getVersion() 
	{
		String version = "Version: " + VersionInfo.VERSION_NUMBER;
		
		return version; 
	}

	/**
     * Stop the sip container
     */
    public void stop() {
    	if(c_logger.isTraceDebugEnabled()){
			c_logger.traceEntry(this, "stop");
		}

		//we need to notify sip sessions and sip application sessions only if the container was started 
		if (_isInitialized){
			if( c_logger.isTraceDebugEnabled()){	
		           c_logger.traceDebug( this, "stop", "Sip container was initialized, going to notify SipSession on passivation");
			}
			notifyOnSessionsPassivation();
		}
		
        if (null != _messageDispatcher){
        	_messageDispatcher.stop();
        }
        

        if ( null != m_router) {
        	m_router.stop();
        }
        
        // Clean up receiver
        if (null != m_msgReceiver){
            m_msgReceiver.stop();
            m_msgReceiver = null;
        }
        
        if(c_logger.isTraceDebugEnabled()){
			c_logger.traceExit(this, "stop");
		}
    }
    
    /**
     * Notifying the sessions state listeners that sessions are about to be 
     * passivated.
     */
    private void notifyOnSessionsPassivation(){
    	if(c_logger.isTraceDebugEnabled()){
    		c_logger.traceEntry(this, "notifyOnSessionsPassivation");
    	}
    	//Moti: OG:
    	List tempList = SessionRepository.getInstance().getAllTuWrappers();
        for(int i = 0 ; i < tempList.size() ; i++){
    		List sipSessions = ((TransactionUserWrapper)tempList.get(i)).getAllSipSessions();
    		for(int j = 0 ; j< sipSessions.size() ; j++ ){
    			SipSessionImplementation sipSession = (SipSessionImplementation)sipSessions.get(j);
    			if( sipSession != null){
        			sipSession.notifyOnActivationOrPassivation(false);
        		}
    		}
    	}
        
        //notify sip application Sessions on passivation
        List<SipApplicationSessionImpl> appSessions = SessionRepository.getInstance().getAllAppSessions();
        for (SipApplicationSessionImpl sipApplicationSession: appSessions) {
        	sipApplicationSession.notifyOnActivationOrPassivation(false);
		}
    	
    	if(c_logger.isTraceDebugEnabled()){
    		c_logger.traceExit(this, "notifyOnSessionsPassivation");
    	}
    }

    /**
     * Unload the specified application from the list of application currently
     * running in the container. 
     * @param appName
     */
    public void unloadAppConfiguration(String appName)
    {
        m_router.unloadAppConfiguration(appName);
    }

    /**
     * Returns number of running SIP applications
     * @return
     */
    public int getNumOfRunningApplications(){
    	return SipAppDescManager.getInstance().getSipAppDescs().size();
    }
   
	/**
	 * Gets the SIP App descriptor for the given application name. 
	 * @param name The name of the SIP Application. 
	 * @return The SIP App Descriptor if available, otherwise null
	 */
	public SipAppDesc getSipApp(String name)
	{
		return m_router.getSipApp(name);	
	}
	
	
	/**
	 * Gets the single instance of the SIP Container. It is the responsibility 
	 * of the launcher to initialize the container only ONCE.  
	 */
	public static SipContainer getInstance()
	{
		return c_sipContainer;
	}
	
	/**
     * @return Returns the m_router.
     */
    public MessageDispatcher getMessageDispatcher() {
        return _messageDispatcher;
    }
    
    /**
     * @return Returns the m_router.
     */
    public SipRouter getRouter() {
        return m_router;
    }
    
    /**
     * Gets the MBean associated with this SIP Container. 
     * @return
     */
    public SipContainerMBean getMBean()
    {
        return m_sipContainerMBean;
    }
    
    /**
     * Get Listening points
     * @return
     */
    public Iterator getListeningPoints(){
        return m_msgReceiver.getListeningPoints();
    }
    
    /**
	 * Utility function for logging exceptions. 
     * @param e
     */
    private static void logException(Exception e) 
    {
        if(c_logger.isErrorEnabled())
        {
			c_logger.error("error.exception", Situation.SITUATION_REQUEST,
			               	null, e);
        }
    }
    
    /**
     * @return Returns the m_enable_sys_headers_modify.
     */
    public boolean canModifySysHeaders() {
        return m_enableSysHeadersModify;
    }
	
	/**
	 * Flag indicating whether an external router is used - in other words, do we have
	 * a SLSP in front of the container. 
	 * @return
	 */
	public boolean isUsingExternalRouter()
	{
		return m_isUsingExternalRouter; 
	}

	
	
	/**
	 * Sets the flag indicating whether the container is running behind proxy. 
	 * @param usingExternalRouter
	 */
	public void setUsingExternalRouter(boolean usingExternalRouter) {
		m_isUsingExternalRouter = usingExternalRouter;
		if (c_logger.isTraceDebugEnabled()) {
            c_logger
                .traceDebug(this, "setUsingExternalRouter", usingExternalRouter ? "true" : "false");
        }
	}
	
	/**
	 * Sets the tasks invoker
	 * @param tasksInvoker
	 */
	public static void setTasksInvoker(TasksInvoker tasksInvoker) {
		SipContainer.s_tasksInvoker = tasksInvoker;
	}
	
	/**
	 * returns the tasks invoker
	 */
	public static TasksInvoker getTasksInvoker() {
		return SipContainer.s_tasksInvoker;
	}
	
	/**
	 * Sets http session listener
	 * @param listener
	 */
	public static void setHttpSessionListener(WASHttpSessionListener listener) {
		SipContainer.s_httpSessionListener = listener;
	}
	
	/**
	 * returns http session listener
	 */
	public static WASHttpSessionListener getHttpSessionListener() {
		return SipContainer.s_httpSessionListener;
	}

	/**
	 * Get the security manager.
	 */
	public SipSecurityManager getSecurityManager() {
		return m_securityManager;
	}



	/**
	 * Setter for container quience indicator
	 * @return
	 */
	public boolean isInQuiesce() {
		return s_isInQuiesce;
	}

	/**
	 * Getter for container quience indicator
	 * @param inQuiesce
	 */
	public void setQuiesceAttribute(boolean inQuiesce) {
		s_isInQuiesce = inQuiesce;
		if (c_logger.isInfoEnabled()) {
            if (inQuiesce) {
            	// quiesce
            	c_logger.info("info.sip.container.quiesce.on", Situation.SITUATION_STOP_INITIATED);
            }
            else {
            	// de-quiesce
            	c_logger.info("info.sip.container.quiesce.off", Situation.SITUATION_START);
            }
        }
		/*TODO Liberty FailoverMgrLoader.getMgrInstance().setQuiesceAttribute(s_isInQuiesce);*/
	}
	
	
}
