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
//package com.ibm.ws.sip.container.was;
//
//import com.ibm.sip.util.log.Log;
//import com.ibm.sip.util.log.LogMgr;
//import com.ibm.sip.util.log.Situation;
//import com.ibm.ws.exception.ComponentDisabledException;
//import com.ibm.ws.exception.RuntimeError;
//import com.ibm.ws.exception.RuntimeWarning;
////TODO Liberty import com.ibm.wsspi.runtime.component.WsComponentImpl;
////TODO Liberty import com.ibm.wsspi.runtime.config.ConfigObject;
///**
// * Used as the entry point for the sip container 
// * when operating as an add-on to Websphere web container
// * The class use the RuntimeExtensibility mechanism of Websphere
// * @see https://cbs6.rchland.ibm.com/WebSpherePlatform/RuntimeExtensibility/RuntimeExtensibility.html 
// * 
// *   
// * @author yaronr
// */
//public class WebsphereLauncher /*TODO Liberty extends WsComponentImpl*/
//{
//    /**
//     * Class Logger. 
//     */
//    private static final LogMgr c_logger = Log.get(WebsphereLauncher.class);
//
//    private WebsphereLauncherImpl wsl = new WebsphereLauncherImpl();
////    private WsComponent[] _siphaComponents = new WsComponent[5];
//    
//    /**
//     * Override the initialize() method in ComponentImpl.
//     * This method is called by Websphere during initialization 
//     */
//    public void initialize(Object config) throws ComponentDisabledException 
//    {
//        if( c_logger.isTraceEntryExitEnabled()){
//            c_logger.traceEntry( this, "initialize");
//        }
//        
//        try{
//        	//If we need to set the cluster member to be not available at server startup
//        	//so HTTP requests will not get in until the SIP container is ready 
//        	//we can remove the comment. this solution is not working for zos and still
//        	//leave a small window of time when HTTP requests will be received 
//        	//the code that set the member to available again is found in SipletServletInitiatorImpl
//        	
//        	/*//if we in a cluster environment we will register a discovery listener,
//        	//we are using it to set the cluster member as unavailable so requests from
//        	//the proxy will not arrive until (http and sip) until it is ready to receive 
//        	//sip requests. this will prevent converged application from sending sip requests
//        	//before the sip proxy is ready.
//        	if(SipClusterUtil.isZServerInCluster()){
//        		SipClusterUtil.getInstance().registerDiscoveryListener();
//        	}*/
//        	
//        	/*TODO Liberty wsl.init( (ConfigObject)config);*/
//        }
//        catch (Throwable e)
//        {
//            if(c_logger.isErrorEnabled())
//            {
//                c_logger.error(
//                    "error.initialize.sip.container",
//                    Situation.SITUATION_START,
//                    null,
//                    e);
//            }
//            throw new ComponentDisabledException(e);
//        }
//               
//        /*TODO Liberty wsl.doInitialize( config);*/
//        
//        if( c_logger.isTraceEntryExitEnabled()){
//            c_logger.traceExit( this, "initialize");
//        }
//    }
//
//
//    /**
//     * Override the start() method in ComponentImpl. 
//     * Since Websphere has finished initialization, we can at this stage
//     *  	get the	AppServerDispatcher
//     */
//    public void start() throws RuntimeWarning, RuntimeError
//    {
//        wsl.doStart();
//    }
//
//    /**
//     * Override the destroy() method in ComponentImpl.
//     */
//    public void destroy()
//    {
//        wsl.doDestroy();
//    }
//
//    /**
//     * Override the stop() method in ComponentImpl. This is called when the server
//     * is stopping.
//     */
//    public void stop()
//    {
//        wsl.doStop();
//        SipSessionManagerLauncher.stop();
//    }
//}
