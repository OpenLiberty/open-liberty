/*******************************************************************************
 * Copyright (c) 2003,2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.session;

import java.util.StringTokenizer;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.servlet.session.IBMApplicationSession;
import com.ibm.websphere.servlet.session.IBMSession;
import com.ibm.ws.sip.container.converged.session.ApplicationSessionCreator;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipServletsFactoryImpl;
import com.ibm.ws.sip.container.servlets.WASXSipApplicationSessionImpl;

/**
 * SipApplicationSessionCreator implements an extention point to HttpSession manager. that allows
 * HttpSession manager to create application session. and associate sip and http.
 * a good example is use of web serivce which want to create sip requests.
 * @author dror
 *
 */
public class SipApplicationSessionCreator implements ApplicationSessionCreator {
	/**
     * Class Logger.
     */
    private static final transient LogMgr c_logger = Log.get(SipApplicationSessionCreator.class);
    
	private static final String DELIM = ";";

	/**
	 * Attribute name the will store sip application session id for on the fly
	 * activation
	 */
	private static final String ATTRIBUTE_APP_SESSION_ID = "com.ws.ibm.sip.application.sessionID";

	/**
	 * 
	 * @param httpSession
	 * @param appName
	 * @return
	 */
	public IBMApplicationSession createApplicationSession(IBMSession httpSession, String appName) {
		return createApplicationSession(httpSession,appName,null);		
	}
	
		
	/**
	 * Create ApplicationSession, used by httpSessionManager, if requesting ApplicationSession from
	 * HttpSessionManager, http session might already point to an existing ApplicationSession using encodedURI
	 * logical name indicates which logical name this application belongs to. 
	 *
	 * @param httpSession
	 * @param appName
	 * @param pathInfo
	 * @param logicalName
	 * @return
	 */
	public IBMApplicationSession createApplicationSession(
			IBMSession httpSession, String appName, String pathInfo) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "createApplicationSession",
					httpSession.toString() + " " + appName + " " + pathInfo);
		}

		WASXSipApplicationSessionImpl appSession = null;

		String ibmAppId = null;

		ibmAppId = (String) httpSession.getAttribute(ATTRIBUTE_APP_SESSION_ID);

		boolean isAttributeExists = (ibmAppId != null && ibmAppId.length() > 0);

		if (!isAttributeExists && pathInfo != null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createApplicationSession",
						"pathInfo[" + pathInfo + "]");
			}
			// look if the http request came with an encodedURI
			StringTokenizer tokenizer = new StringTokenizer(pathInfo, DELIM);
			String token;
			while (tokenizer.hasMoreTokens()) {
				token = tokenizer.nextToken();
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "createApplicationSession",
							"token[" + token + "]");
				}
				if (token
						.startsWith(SipApplicationSessionImpl.ENCODED_APP_SESSION_ID)) {
					ibmAppId = token
							.substring(SipApplicationSessionImpl.ENCODED_APP_SESSION_ID
									.length() + 1);
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "createApplicationSession",
								"ibmAppId[" + ibmAppId + "]");
					}
					break;
				}
			}
		}

		if (ibmAppId != null) {
			// ApplicationSession was encoded in request URI
			appSession = (WASXSipApplicationSessionImpl) WASXSipApplicationSessionImpl
					.getAppSession(ibmAppId);
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createApplicationSession",
						"found AppSessionID[" + ibmAppId + "], AppSession["
								+ appSession + "]");
			}
		}

		if (appSession == null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createApplicationSession",
						"creating new AppSession");
			}

			appSession = (WASXSipApplicationSessionImpl) ((SipServletsFactoryImpl) SipServletsFactoryImpl
					.getInstance(appName))
					.createApplicationSession();
			appSession.addToApplicationSessionsTable();
		}

		// associate the http session
		appSession.addHttpSession(httpSession);
		if (!isAttributeExists) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createApplicationSession",
						"Setting new attribute " + appSession.getId());
			}
			httpSession.setAttribute(ATTRIBUTE_APP_SESSION_ID,
					appSession.getId());
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "createApplicationSession",
					"returning application session=" + appSession);
		}
		return appSession;
	}
	
	/**
	 * @see com.ibm.ws.webcontainer.httpsession.ApplicationSessionCreator#encodeURL(java.lang.String)
	 */
	public String encodeURL(String url){
		return "";
	}
	
	/**
	 * @see com.ibm.ws.webcontainer.httpsession.ApplicationSessionCreator#encodeURL(java.lang.String, java.lang.String)
	 */
	public String encodeURL(String relativePath, String scheme){
		return "";
	}

}
