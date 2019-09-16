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
package com.ibm.ws.sip.container.was.filters;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.servlet.event.ServletEvent;
import com.ibm.websphere.servlet.event.ServletListener;
import com.ibm.ws.sip.container.osgi.ServletInstanceHolderFactory;
import com.ibm.ws.sip.container.parser.ServletsInstanceHolder;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.router.SipAppDescManager;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipServletMessageImpl;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.was.message.SipMessage;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;

/**
 * This listener is registered once when the first application is loaded.
 * it will get notification for each servlet (in any application) event
 * we are using it to catch servlet init finished event and call to the appropriate sip listener
 * 
 * @author asafz
 *
 */
public class SipServletListener implements ServletListener {
	/* Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(SipServletListener.class);


	public SipServletListener() {
	}

	public void onServletAvailableForService(ServletEvent servletevent) {
	}

	public void onServletFinishDestroy(ServletEvent servletevent) {

	}

	public void onServletFinishInit(ServletEvent servletevent) {
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "onServletFinishInit", servletevent);
        }
		
		SipServletRequestImpl sipRequest = null;
    	SipServletResponseImpl sipResponse = null;
    	int index = -1;
    	
		 //Read back the servlet request\response which we stored locally for 
        // this thread before entering the web-container.
        SipMessage msg = ThreadLocalStorage.getSipMessage();
        
        //if the msg is null we cannot be sure if this is not a sip servlet (it can be web servlet since we are 
        //listening on all servlets event) or this is load on start up message. we will ignore this event 
        //load on start up evenets are handled in the SipletServletInitiatorImpl class where we can be sure that
        //we are in a siplet load on startup event
        if (msg != null){
	        sipResponse = (SipServletResponseImpl) msg.getResponse();
	        sipRequest = (SipServletRequestImpl) msg.getRequest();
	        
	        SipServletMessageImpl message = sipResponse != null ? sipResponse : sipRequest;
	        
			TransactionUserWrapper tu = message.getTransactionUser();
			SipApplicationSessionImpl appSess = tu.getAppSessionForInternalUse();
			if (appSess != null) {
				// Moti: I assume here that every TU has an SipAppSessionImpl. I'm not sure if thats
				// a true assumption.
				index = appSess.extractAppSessionCounter();
			}  else {
				index = SipApplicationSessionImpl.extractAppSessionCounter(tu.getApplicationId());
			}
			
			if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "onServletFinishInit", "found queue index: " + index);
    		}
			
			ServletInstanceHolderFactory.getInstanceHolder().triggerSipletInitServlet(index);
        }
        else {
        	
        	boolean isLoadOnStartServlet = false;
        	for(SipAppDesc appDesc : SipAppDescManager.getInstance().getSipAppDescs()){
        		if(appDesc.getSipServlet(servletevent.getServletName()) != null){
        			// No message is received, thus application queue index should be -1.
        				ServletInstanceHolderFactory.getInstanceHolder().saveOnStartupServlet();
        			isLoadOnStartServlet = true;
        			break;
        		}
        	}
        	
        	if (c_logger.isTraceDebugEnabled()) {
    			if(isLoadOnStartServlet){
    				c_logger.traceDebug(this, "onServletFinishInit", "Siplet is load on startup Servlet. Firing the event, servletName: " + servletevent.getServletName());
    			}
    			else{
            		c_logger.traceDebug(this, "onServletFinishInit", "Siplet isn't load on startup. Ignoring the event, servletName: " + servletevent.getServletName());
    			}
    		}
        }
        
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntryExit(this, "onServletFinishInit", null);
        }
	}

	public void onServletStartDestroy(ServletEvent servletevent) {
	}

	public void onServletStartInit(ServletEvent servletevent) {
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "onServletStartInit", servletevent);
        }
	}

	public void onServletUnavailableForService(ServletEvent servletevent) {
	}

	public void onServletUnloaded(ServletEvent servletevent) {
	}
}