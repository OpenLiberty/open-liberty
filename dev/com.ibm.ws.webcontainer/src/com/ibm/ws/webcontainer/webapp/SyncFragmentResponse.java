/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.webapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.ibm.websphere.webcontainer.async.FragmentResponse;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

public class SyncFragmentResponse implements FragmentResponse,com.ibm.wsspi.ard.JspFragmentResponse {
    protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.webapp");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.webapp.SyncFragmentResponse";
	
	private WebAppRequestDispatcher dispatcher;

	public SyncFragmentResponse (WebAppRequestDispatcher dispatcher) {
		this.dispatcher=dispatcher;
	}

	public void insertFragment(ServletRequest req, ServletResponse resp)
			throws ServletException, IOException {
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){ 
            logger.logp(Level.FINE, CLASS_NAME,"insertFragment","about to execute synchronous include");
        }
		dispatcher.include(req, resp);
	}

	public void insertFragmentBlocking(ServletRequest req, ServletResponse resp)
			throws ServletException, IOException {
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){ 
            logger.logp(Level.FINE, CLASS_NAME,"insertFragmentBlocking","about to execute synchronous include");
        }
		dispatcher.include(req, resp);
	}

	public void insertFragmentFromJsp(ServletRequest req, ServletResponse resp, PrintWriter pw) throws ServletException, IOException {
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){ 
            logger.logp(Level.FINE, CLASS_NAME,"insertFragmentFromJsp","about to execute synchronous include");
        }
		SyncResponseWrapper sw = new SyncResponseWrapper((HttpServletResponse)resp,pw);
		dispatcher.include(req, sw);
	}
	
	public void insertFragmentBlockingFromJsp(ServletRequest req, ServletResponse resp, PrintWriter pw) throws ServletException, IOException {
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){ 
            logger.logp(Level.FINE, CLASS_NAME,"insertFragmentBlockingFromJsp","calling insertFragmentFromJsp");
        }
		insertFragmentFromJsp(req, resp,pw);
	}



	protected class SyncResponseWrapper extends HttpServletResponseWrapper{

		private PrintWriter pw;

		public SyncResponseWrapper(HttpServletResponse resp, PrintWriter pw) {
			super(resp);
			this.pw=pw;
		}
		
		public PrintWriter getWriter(){
			return pw;
		}
		
	}
}
