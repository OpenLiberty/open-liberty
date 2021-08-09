/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jain.protocol.ip.sip.header;

import java.util.Arrays;
import java.util.HashSet;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.parser.Coder;
import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;
import com.ibm.ws.sip.stack.util.AddressUtils;

/**
 * This class is responsible to decide which header parameters should be
 * quoted, and which should not. It is needed when the application calls
 * ParametersHeader.setParameter(String, String) passing the parameter name
 * and value. The application has no control over parameter quoting through
 * this API, and this class helps in adding the quotes where needed.
 * The class is singleton.
 * 
 * The list of parameters to quote is configurable by the custom property
 * javax.sip.quoted.parameters
 * 
 * Note: Contact feature parameters, are always quoted, and there is no
 * way around that in configuration. A feature parameter is any parameter
 * that comes in the Contact header, and begins with a "+". See RFC 3840.
 * 
 * @author ran
 */
public class ParameterQuoter
{
	/** class Logger */
	private static final LogMgr s_logger = Log.get(ParameterQuoter.class);

	/**
	 * HashSet of parameters that need quoting
	 */
	private final HashSet<String> m_parameters;

	/** a default, hard-coded list of parameters to be quoted */
	private static final String[] DEFAULT_LIST = {
		"uri" // 3261 (Authorization,Proxy-Authorization)
		, "response" // 3261 (Authorization,Proxy-Authorization)
		, "username" // 3261 (Authorization,Proxy-Authorization)
		, "realm" // 3261 (Authorization,Proxy-Authorization,WWW-Authenticate,Proxy-Authenticate)
		, "nonce" // 3261 (Authorization,Proxy-Authorization)
		, "cnonce" // 3261 (Authorization,Proxy-Authorization,Authentication-Info)
		, "opaque" // 3261 (Authorization,Proxy-Authorization,WWW-Authenticate,Proxy-Authenticate)
		, "domain" // 3261 (WWW-Authenticate,Proxy-Authenticate)
		, "qop" // 3261 (WWW-Authenticate,Proxy-Authenticate)
		, "rspauth" // 3261 (Authentication-Info)
		, "nextnonce" // 3261 (Authentication-Info)
		, "text" // 3326 (Reason)
		, "audio","automata" // 3840 (Contact)
		, "class","duplex","data" // 3840 (Contact)
		, "control","mobility","description" // 3840 (Contact)
		, "events","priority","methods" // 3840 (Contact)
		, "schemes","application","video" // 3840 (Contact)
		, "language","type","isfocus" // 3840 (Contact)
		, "actor","text","extensions" // 3840 (Contact)
	};

	/** singleton instance */
	private static final ParameterQuoter s_instance = new ParameterQuoter();

	/**
	 * @return the singleton instance
	 */
	public static ParameterQuoter instance() {
		return s_instance;
	}

	/**
	 * private constructor
	 */
	private ParameterQuoter() {
		m_parameters = new HashSet<String>(32);
		parseList(DEFAULT_LIST);
		if (ApplicationProperties.getProperties().getObject(StackProperties.QUOTED_PARAMETERS) != null) {
			String[] configList = (String[])(ApplicationProperties.getProperties().
					getObject(StackProperties.QUOTED_PARAMETERS));
			if (configList.length > 0)
			parseList(configList);
		}
	}

	/**
	 * parses the list of parameters. format:
	 * item,item,...
	 * 
	 * @param list raw list of parameters
	 */
	private final void parseList(String[] list) {
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "parseList", "parsing list "+Arrays.toString(list));
		}
		int items = 0;
		int errors = 0;

		if (list != null) {
			items++;
			for (String item : list) {
				if (!parseItem(item)) {
					if (s_logger.isTraceFailureEnabled()) {
						s_logger.traceFailure(this, "parseList", "bad item ["
								+ item + ']');
					}
					errors++;
				}
			}
		}
		if (errors > 0) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "parseList", "complete with ["
						+ errors + "] errors out of ["
						+ items + "] items in list " + Arrays.toString(list));
			}
		}
		else if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "parseList", "complete with ["
					+ items + "] items");
		}
	}

	/**
	 * parses a single item in the list. format:
	 * parameter[=off]
	 * 
	 * @param item a single item extracted from the list
	 * @return true on success, false on failure
	 */
	private final boolean parseItem(String item) {
		int length = item.length();
		int equal = item.indexOf('=');

		String parameter;
		boolean add;

		if (equal == -1) {
			add = true;
			parameter = item;
		}
		else {
			if (equal >= length) {
				return false;
			}
			parameter = item.substring(0, equal);
			String equalValue = item.substring(equal+1, length);
			add = !equalValue.equalsIgnoreCase("off");
		}
		if (add) {
			addParameter(parameter);
		}
		else {
			removeParameter(parameter);
		}
		return true;
	}

	/**
	 * adds another parameter that needs quoting
	 * @param parameter the name of the parameter that needs quoting
	 * @return true if it was already contained before, false if added now
	 */
	private final boolean addParameter(String parameter) {
		return m_parameters.add(parameter);
	}

	/**
	 * removes a parameter that needs no quoting
	 * @param parameter the name of the parameter that needs no quoting
	 * @return true if removed, false if not found
	 */
	private final boolean removeParameter(String parameter) {
		return m_parameters.remove(parameter);
	}

	/**
	 * answers whether the specified parameter should be quoted or not
	 * 
	 * @param parameter the parameter name
	 * @param value the parameter value
	 * @param escapeParameters is escaping turned on
	 * @return true if should be quoted, false otherwise
	 */
	public boolean quote(String parameter, String value, boolean escapeParameters) {
		if (isFeatureParameter(parameter)) {
			return true;
		} 
		if(m_parameters.contains(parameter)) {
			return true;
		}
		if (escapeParameters) {
			return false;
		}
		// parameters of this header are not escaped by definition, so,
		// if there are any special characters in the value, need to quote.
		// this follows 3261-25.1:
		// gen-value = token / host / quoted-string
		return !Coder.isToken(value) && !AddressUtils.isIpAddress(value);
	}

	/**
	 * determines if the given parameter is a feature parameter
	 * per RFC 3840-9:
	 * 
	 * other-tags = "+" ftag-name
	 * ftag-name = ALPHA *( ALPHA / DIGIT / "!" / "'" / "." / "-" / "%" )
	 * 
	 * @param parameter the parameter name
	 * @return true if a feature parameter, false otherwise
	 */
	private boolean isFeatureParameter(String parameter) {
		int length = parameter.length();
		if (length < 2 || parameter.charAt(0) != '+') {
			return false;
		}
		char c = parameter.charAt(1);
		if (!Coder.isAlpha(c)) {
			return false;
		}
		for (int i = 2; i < length; i++) {
			c = parameter.charAt(i);
			if (Coder.isAlphanum(c)) {
				continue;
			}
			switch (c) {
			case '!': case '\'': case '.': case '-': case '%':
				continue;
			default:
				return false;
			}
		}
		return true;
	}
}
