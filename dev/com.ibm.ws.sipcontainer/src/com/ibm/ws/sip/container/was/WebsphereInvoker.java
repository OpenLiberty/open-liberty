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

import jain.protocol.ip.sip.SipProvider;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.router.SipServletInvokerListener;
import com.ibm.ws.sip.container.servlets.SipServletMessageImpl;
import com.ibm.ws.sip.container.servlets.ext.SipServletMessageExt;
import com.ibm.ws.sip.container.util.Queueable;
import com.ibm.ws.sip.container.util.wlm.QueueableTransformer;
import com.ibm.ws.sip.container.util.wlm.SipContainerWLMHooksFactory;
import com.ibm.ws.sip.container.util.wlm.SipDialogContext;
import com.ibm.ws.sip.container.was.message.SipMessage;

/**
 * Invoker for the sip container when it runs as an add-on in 
 * 			Websphere web container
 * 
 * @author yaronr 
 */
public class WebsphereInvoker
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(WebsphereInvoker.class);
    
    /**
     * Singelton
     */
    private static WebsphereInvoker s_instance = new WebsphereInvoker();
    
    
    /**
     * Key for the listener (in SipMessage)
     */
    public static final String INVOKER_LISTENER_KEY = "INVOKER_LISTENER_KEY";
    
    /**
     * private Ctor
     */
    private WebsphereInvoker() {
    	
    }
    
    public static WebsphereInvoker getInstance() {
    	return s_instance;
    }

    /**
     * 
     * @see com.ibm.ws.sip.container.router.WebsphereInvoker#invokeSipServlet(javax.servlet.sip.SipServletRequest, javax.servlet.sip.SipServletResponse, com.ibm.ws.sip.container.parser.SipServletDesc)
     */
    public void invokeSipServlet(
        SipServletRequest request,
        SipServletResponse response,
        SipServletDesc sipServletDesc,
        SipServletInvokerListener listener)
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(
                this,
                "invokeSipServlet",
                "name: " + sipServletDesc.getName());
        }

        // Create a SIP message 
        SipAppDesc appDesc = sipServletDesc.getSipApp(); 
        SipMessage msg = SipContainerComponent.getSipMessageFactory().createSipMessage(request, response,appDesc);
        SipProvider provider = null;
        
        if( request instanceof SipServletMessageImpl){
        	provider = ((SipServletMessageImpl)request).getSipProvider();
        }
        else if( response instanceof SipServletMessageImpl){
        	provider = ((SipServletMessageImpl)response).getSipProvider();
        }
        
        //Get any one of the port associated with this VH. Matching should 
        //already be according to VH so it should not matter to the Web Container
        //We don't use the provider's listening point because that will not work
        //in case of loop back e.g. multiple proxies on the same host
        int port = appDesc.getVHPort();
        String host = appDesc.getVHHost();
        if(port < 1) {
            //Virtual not available use the provider's address instead
        	port = provider.getListeningPoint().getPort();
            host = provider.getListeningPoint().getHost();
        }
        
    	msg.setServerPort(port);
    	msg.setHost(host);
	if(provider==null){
		msg.setSSLEnbaled(false); 
	} else {
	    	msg.setSSLEnbaled( ((ListeningPointImpl)provider.getListeningPoint()).isSecure()); 
	}
    	msg.setServletName(sipServletDesc.getName());
    	
    	
        // Put the listener in the msg too
        if (null != listener)
        {
            msg.setListener( listener);
        }
        
        String requestURI = sipServletDesc.getSipApp().getRootURI();
        
        // If context root is / we prevent
        // double slash in siplet path
        if (requestURI.equals("/")){
        	requestURI = "";
        }
        
        	requestURI +=   
                '/'  
                + sipServletDesc.getName();
        
        msg.setRequestURI( requestURI);
        
        Queueable queueable = wrapMessageForWLM(msg);
        
        queueable.run();
    }
    
    /**
    * wrap SipMessage for WLM hook.
    * @param msg
    * @return
    */ 
    private Queueable wrapMessageForWLM(SipMessage msg){
    	if(c_logger.isTraceEntryExitEnabled()){
    		c_logger.traceEntry(this,"wrapMessageForWLM");
    	}
    	Queueable queueable = msg;
    	QueueableTransformer transformer = SipContainerWLMHooksFactory.getSipContainerHooks().getQueueableTransformer();        
        if(transformer!=null){
        	if(c_logger.isTraceDebugEnabled()){
        		c_logger.traceDebug(this,"wrapMessageForWLM","calling WLMHook transformer");
        	}
        	SipServletMessageExt sipServletMsg = null;
        	if(msg.getRequest()!= null){
        		if(c_logger.isTraceDebugEnabled()){
            		c_logger.traceDebug(this,"wrapMessageForWLM","WLMHook - this is request");
            	}
        		sipServletMsg = (SipServletMessageExt) msg.getRequest();
        	}else{
        		if(c_logger.isTraceDebugEnabled()){
            		c_logger.traceDebug(this,"wrapMessageForWLM","WLMHook - this is response");
            	}
        		sipServletMsg = (SipServletMessageExt) msg.getResponse();
        	}
        	SipDialogContext ctx = ((SipServletMessageImpl)sipServletMsg).getTransactionUser();
        	queueable = transformer.wrap(ctx,msg,sipServletMsg);
        	if(c_logger.isTraceDebugEnabled()){
        		c_logger.traceDebug(this,"wrapMessageForWLM","WLMHook - wrapping done");
        	}
        }
        if(c_logger.isTraceEntryExitEnabled()){
    		c_logger.traceExit(this,"wrapMessageForWLM");
    	}
        return queueable;		
    }


    
    
    /**
     * @see com.ibm.ws.sip.container.router.WebsphereInvoker#stop()
     */
    public void stop()
    {
    	//do nothing
    }
    

}
