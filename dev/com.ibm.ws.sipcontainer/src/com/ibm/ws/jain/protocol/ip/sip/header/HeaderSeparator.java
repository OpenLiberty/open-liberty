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
import java.util.HashMap;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.parser.HeaderCreator;
import com.ibm.ws.sip.parser.util.LRUStringCache;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;

/**
 * This class decides how the values of a header field are separated - either
 * line-separation (each value in its own line) or comma-separation (all values
 * are in the same line, separated by commas).
 * 
 * It affects the parsing of extension header fields, as well as the
 * serialization of both standard and extension header fields.
 * 
 * The default behavior is:
 * 1. Parse all extension header fields as if they are line-separated.
 * 2. Serialize all header fields as line separated, except for a specified set
 *    of header fields that are to be comma-separated.
 * 
 * There is a configuration property that allows customizing separation. If set,
 * it is processed on top of the default setting. Format:
 * 
 * HeaderSeparation[,HeaderSeparation,...]
 * HeaderSeparation = header[:separation]
 * header = (either header name or *)
 * separation = on/off/in/out (default is on)
 * 
 * The default string is:
 * "Accept,Accept-Encoding,Accept-Language,Allow,In-Reply-To,Proxy-Require,Require,Supported,Unsupported"
 * 
 * During initialization, the custom string, if set, is appended on top of the
 * default, before parsing the string. Duplicate entries are allowed, and the
 * latter wins. If a header field name of "*" is specified, it erases all
 * entries that come before it, and it specifies the default separation.
 * 
 * For example, setting the custom property to "*:off,My-Header-Field:on" produces:
 * "Accept,Accept-Encoding,Accept-Language,Allow,In-Reply-To,Proxy-Require,Require,Supported,Unsupported,*:off,My-Header-Field:on"
 * which means all header fields are line-separated (including the default)
 * except for My-Header-Field which is comma-separated.
 * 
 * @author ran
 */
public class HeaderSeparator
{
	/** Class logger */
	private static final LogMgr s_logger = Log.get(HeaderSeparator.class);

	/** A cache of extension header field names converted to lowercase */
	private static final LRUStringCache s_headerNameCache = new LRUStringCache(128);

	/** Temporary buffer needed for converting strings to lowercase */
	private static final ThreadLocal<char[]> s_lowercase =
		new ThreadLocal<char[]>() {
			protected char[] initialValue() {
				return new char[32];
			}
		};

	/** Options for comma-separation of a specific header field */
	static enum HeaderSeparation {
		OFF, // line-separated
		ON,  // comma-separated
		IN,  // inbound comma-separated, outbound line-separated
		OUT  // outbound comma-separated, inbound line-separated
	}

	/**
	 * Map describing the type of separation per header field type.
	 * The key to this map is the header field name.
	 * The value describes how the values of the header field are separated:
	 */
	private HashMap<String, HeaderSeparation> m_commaSeparatedHeaders;

	/** Cached value of the "*" setting, OFF if no "*" set */
	private HeaderSeparation m_default;

	/** The default header separation setting */
	private static final String[] DEFAULT_STRING =
		{"Accept","Accept-Encoding","Accept-Language","Allow","In-Reply-To","Proxy-Require","Require","Supported","Unsupported"};

	/** 
	 * A list of line separated headers to be always excluded from a header separation list 
	 *  *** DON'T REMOVE headers from the list, only add others if required
	 */ 
	private static final String[] EXCLUDE_HEADER_SEPARATION_HEADERS = 
		{"IBM-Heartbeat:off","OutboundIfList:off"};
	
	/** Singleton instance */
	private static final HeaderSeparator s_instance = new HeaderSeparator();

	/**
	 * @return The singleton instance
	 */
	public static HeaderSeparator instance() {
		return s_instance;
	}

	/**
	 * Private constructor.
	 */
	private HeaderSeparator() {
		String[] customCommaSeparatedHeaders =
			SIPTransactionStack.instance().getConfiguration().getCommaSeparatedHeaders();
		changeSetting(customCommaSeparatedHeaders);
	}

	/**
	 * Parses the configuration property and sets the internal map.
	 * Called from the constructor, but declared public to make available
	 * for testing code.
	 * @param customCommaSeparatedHeaders The value of the custom property.
	 */
	public final void changeSetting(String[] customCommaSeparatedHeaders) {
		// 1. parse the default string
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "changeSetting", "Parsing default " + Arrays.toString(DEFAULT_STRING));
		}
		HashMap<String, HeaderSeparation> map =
			new HashMap<String, HeaderSeparation>();
		try {
			parse(DEFAULT_STRING, map);
		}
		catch (Exception e) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(this, "changeSetting",
					"Failed parsing default comma separation", e);
			}
		}
		// 2. parse the custom setting on top of the default
		if (customCommaSeparatedHeaders != null &&
			customCommaSeparatedHeaders.length > 0)
		{
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "changeSetting",
					"Parsing custom " + Arrays.toString(customCommaSeparatedHeaders));
			}
			try {
				parse(customCommaSeparatedHeaders, map);
				parse(EXCLUDE_HEADER_SEPARATION_HEADERS, map);
			}
			catch (Exception e) {
				if (s_logger.isTraceFailureEnabled()) {
					s_logger.traceFailure(this, "changeSetting",
						"Failed parsing custom comma separation", e);
				}
				if (s_logger.isWarnEnabled()) {
					s_logger.warn("warn.sip.invalid.comma.separated.headers", null);
				}
			}
		}
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "changeSetting", "Map result " + map);
		}
		m_commaSeparatedHeaders = map;

		HeaderSeparation star = map.remove("*");
		m_default = star == null ? HeaderSeparation.OFF : star;
	}

	/**
	 * Parses the configuration property that defines comma separation.
	 * Input format:
	 * HeaderSeparation[,HeaderSeparation,...]
	 * HeaderSeparation = header[:separation]
	 * header = (either header name or *)
	 * separation = on/off/in/out
	 * 
	 * @param commaSeparatedHeaders The configuration string.
	 * @param map Destination map.
	 * @throws Exception on syntax error.
	 */
	private static void parse(String[] commaSeparatedHeaders,
		HashMap<String, HeaderSeparation> map)
	{
		for (String headerSeparation : commaSeparatedHeaders) {
			headerSeparation = headerSeparation.trim();
			int length = headerSeparation.length();
			int colon = headerSeparation.indexOf(':');
			String key;
			HeaderSeparation value;
			if (colon == -1) {
				key = headerSeparation;
				value = HeaderSeparation.ON;
			}
			else {
				key = headerSeparation.substring(0, colon).trim();
				String separation = headerSeparation.substring(colon+1, length).trim();
				if (separation.equalsIgnoreCase("on")) {
					value = HeaderSeparation.ON;
				}
				else if (separation.equalsIgnoreCase("off")) {
					value = HeaderSeparation.OFF;
				}
				else if (separation.equalsIgnoreCase("in")) {
					value = HeaderSeparation.IN;
				}
				else if (separation.equalsIgnoreCase("out")) {
					value = HeaderSeparation.OUT;
				}
				else {
					throw new RuntimeException("Invalid comma separation ["
						+ separation + ']');
				}
			}
			if (key.length() == 0) {
				throw new RuntimeException("Empty header field name");
			}
			if (key.equals("*")) {
				// a star removes all entries that come before it. this allows
				// a custom filter to erase the default string with "*:off,..".
				map.clear();
			}
			// If comma.seperated.header property is defined to contain headers 
			// which mustn't be nested - a warning is printed
			HeaderImpl hdr = HeaderCreator.createHeader(key);
			// Extended headers can be nested/not nested no need to check isNested.
			// isNested for ExtendedHeader creates a recursive call for the HeaderSeparator
			// constructor which cause a null pointer exception.
			if (!(hdr instanceof ExtendedHeader)) {
				if ( !hdr.isNested() && value != HeaderSeparation.OFF) {
					//TODO replace the traceDebug print with a warning and update messages.nlsprops
					if (s_logger.isTraceFailureEnabled()) {
						s_logger.traceFailure(HeaderSeparator.class, "parse", "Invalid header: " + hdr.getName() + " in the custom property comma.separated.headers");
					}
					continue;
				}
			}
			key = toLowerCase(key);
			map.put(key, value);
		}
	}

	/**
	 * Determines whether the given header field should have its values
	 * comma-separated or line-separated.
	 * This method is never called for a standard header in an incoming
	 * message, because standard headers are always parsed according to the
	 * syntax defined in the RFC.
	 * It may be called to:
	 * 1. Determine how to parse an extension header field in incoming messages.
	 * 2. Determine how to serialize any header field in outgoing messages.
	 * 
	 * @param headerName The header field name.
	 * @param in true for inbound (parse) false for outbound (write).
	 * @return true if comma-separated, false if line-separated.
	 */
	public boolean isCommaSeparated(CharSequence headerName, boolean in) {
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "isCommaSeperated", "checking header: "+headerName+" .");
		}
		headerName = toLowerCase(headerName);
		HeaderSeparation separation = m_commaSeparatedHeaders.get(headerName);
		if (separation == null) {
			separation = m_default;
		}
		switch (separation) {
		case OFF:
			return false;
		case ON:
			return true;
		case IN:
			return in;
		case OUT:
			return !in;
		default:
			throw new RuntimeException("Unknown comma-separation ["
				+ separation + ']');
		}
	}

	/**
	 * Converts a header field name to lower case.
	 * @param headerName The header field name to convert.
	 * @return The header field name in lowercase.
	 */
	private static String toLowerCase(CharSequence headerName) {
		int length = headerName.length();
		char[] lowercase = s_lowercase.get();
		if (lowercase.length < length) {
			lowercase = new char[length];
			s_lowercase.set(lowercase);
		}
		for (int i = 0; i < length; i++) {
			char c = headerName.charAt(i);
			c = Character.toLowerCase(c);
			lowercase[i] = c;
		}
		String s = s_headerNameCache.get(lowercase, 0, length);
		return s;
	}
}
