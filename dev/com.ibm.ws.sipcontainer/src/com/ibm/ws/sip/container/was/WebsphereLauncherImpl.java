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
package com.ibm.ws.sip.container.was;

import javax.xml.parsers.ParserConfigurationException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.appqueue.MessageDispatcher;
import com.ibm.ws.sip.container.appqueue.MessageDispatchingHandler;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.properties.CoreProperties;
/*TODO Liberty import com.ibm.ws.sip.container.servlets.WASXHttpSessionListener;*/
//TODO Liberty 
//import com.ibm.ws.webcontainer.WebContainerService;
//import com.ibm.wsspi.runtime.config.ConfigObject;
//import com.ibm.wsspi.runtime.service.WsServiceRegistry;

public class WebsphereLauncherImpl{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(WebsphereLauncherImpl.class);
    
    /**
     *  Indicates whether to use an extension processor for locating siplets
     *  or not. The extension processor cannot be used in version 6 and lower 
     *  since some of the functions we use it for (like overriding the ServletConfig)
     *  required certain code modifications from the WebContainer side, that only exist
     *  in ver 6.1
     */
    protected static boolean s_useExtensionProcesor = CoreProperties.USE_EXTENSION_PROCESSOR_DEFAULT;
    
    /**
     * this string appears 4 times in this file so to keep the PMD happy 
     */
    protected final static String STRING_INITIALIZE = "initialize";

    /**
     * The SIP container
     */
    protected SipContainer m_sipContainer;

    
    /**
     * @throws ParserConfigurationException
     */
    public void init() throws ParserConfigurationException {
    	if(c_logger.isTraceDebugEnabled()){
        	c_logger.traceDebug(this,STRING_INITIALIZE,"Starting Sip Container Websphere mode");
        }
        // Register @Resource injection object factories 
        /*TODO Liberty Annotation - Anat, remove the following code after testing the new one with SipFactory as DS...
         *  - register after annotation dev is done
         *  SipSessionsUtilObjectFactory.registerSelf();
        SipFactoryObjectFactory.registerSelf();
        TimerServiceObjectFactory.registerSelf();
        DomainResolverObjectFactory.registerSelf();*/
    	
    	SipContainer.setTasksInvoker( new WASXTasksInvoker());
        /*TODO Liberty SipContainer.setHttpSessionListener(new WASXHttpSessionListener());*/
        // initiate threading dispatcher 
        MessageDispatchingHandler handler = getMessageDispatchingHandler();
        MessageDispatcher.setMessageDispatchingHandler( handler);
        // Creating the SIP Container
        m_sipContainer = SipContainer.getInstance();
    }
    
    /**
     * Returns the desired message dispatcher handler
     * @return
     */
    private MessageDispatchingHandler getMessageDispatchingHandler(){
    	//return  the MessageDispatching handler being injected by SipContainerComponent
    	return SipContainerComponent.getMessageDispatchingHandlerSvc();
    }
    
    /**
     * Override the stop() method in ComponentImpl. This is called when the server
     * is stopping.
     */
    public void stop()
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "stop", "Stopping the SIP container");
        }

        // Let the container know that we are being stopped
        m_sipContainer.stop();
    }

    /**
     * Returns value of s_useExtensionProcesor
     * @return
     */
	public static boolean useExtensionProcesor() {
		return s_useExtensionProcesor;
	}

}
