/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.probe.servlet;

import javax.servlet.GenericServlet;
import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.request.probe.bci.internal.RequestProbeConstants;
import com.ibm.wsspi.requestContext.ContextInfoArray;

public final class ServletContextInfoHelper extends com.ibm.wsspi.request.probe.bci.ContextInfoHelper implements ContextInfoArray {

	private String[] contextInfoArray = null;
	
	ServletContextInfoHelper(Object instanceOfClass, Object methodArgs) {
		super(instanceOfClass, methodArgs);
	}
	
	@Override
	public String[] getContextInfoArray() {
		if (contextInfoArray == null) {
			final Object instanceOfThisClass = getInstanceOfThisClass();
			final Object methodArgs = getMethodArgs();
		
			String appName = "";
			String servletName = "";
			String pathInfo = "";
			String queryString = "";
			// Do something here with "this" object and get the right meta data String and return

			if (instanceOfThisClass != null) {

				GenericServlet passedInstance =  (GenericServlet) instanceOfThisClass;
				appName = passedInstance.getServletContext().getServletContextName();
				servletName = passedInstance.getServletName();
			}

			if(methodArgs != null ) {

				Object[] obj = (Object[]) methodArgs;
				HttpServletRequest servletRequest = (HttpServletRequest)obj[0];
				pathInfo = servletRequest.getPathInfo();
				queryString = servletRequest.getQueryString();

			}
			
			contextInfoArray = new String[] { appName, servletName, pathInfo, queryString };
		}
		
		return contextInfoArray;
	}

	@Override
	public String toString() {
		String[] array = getContextInfoArray();
		String appName = array[0];
		String servletName = array[1];
		String pathInfo = array[2];
		String queryString = array[3];
		StringBuilder eventContextInfo = new StringBuilder();

		if(appName  != null && appName.length() > 0) {
			eventContextInfo.append(appName);
		}
		if(servletName != null && servletName.length() > 0) {
			eventContextInfo.append(RequestProbeConstants.EVENT_CONTEXT_INFO_SEPARATOR).append(servletName);
		}
		if(pathInfo != null && pathInfo.length() > 0) {
			eventContextInfo.append(RequestProbeConstants.EVENT_CONTEXT_INFO_SEPARATOR).append(pathInfo);        	
		}
		if(queryString != null && queryString.length() > 0) {
			eventContextInfo.append("?").append(queryString);  
		}
		return eventContextInfo.toString();
	}

}
