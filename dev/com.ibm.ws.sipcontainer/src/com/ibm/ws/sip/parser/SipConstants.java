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
package com.ibm.ws.sip.parser;

/**
 * Various constants used by the sip parser.
 * 
 * @author Assaf Azaria
 */
public interface SipConstants
{
    // header keywords
	public static final String ERROR_INFO       ="Error-Info"; 
	public static final String MIME_VERSION     ="Mime-Version";
	public static final String IN_REPLY_TO      ="In-Reply-To";
	public static final String ALLOW		      ="Allow";
	public static final String CONTENT_LANGUAGE ="Content-Language";
	public static final String CALL_INFO		  ="Call-Info";
	public static final String CSEQ			  ="CSeq";
	public static final String ALERT_INFO	  ="Alert-Info";
	public static final String ACCEPT_ENCODING  ="Accept-Encoding";
	public static final String ACCEPT		  ="Accept";
	public static final String ENCRYPTION	  ="Encryption";
	public static final String ACCEPT_LANGUAGE  ="Accept-Language";
	public static final String RECORD_ROUTE	  ="Record-Route";
	public static final String TIMESTAMP		  ="Timestamp";
	public static final String TO			  ="To";
	public static final String VIA			  ="Via";
	public static final String FROM			  ="From";
	public static final String CALL_ID		  ="Call-ID";
	public static final String AUTHORIZATION	  ="Authorization";
	public static final String SERVER		  ="Server";
	public static final String UNSUPPORTED	  ="Unsupported";
	public static final String RETRY_AFTER	  ="Retry-After";
	public static final String CONTENT_TYPE	  ="Content-Type";
	public static final String CONTENT_ENCODING ="Content-Encoding";
	public static final String CONTENT_LENGTH   ="Content-Length";
	public static final String HIDE			  ="Hide";
	public static final String ROUTE			  ="Route";
	public static final String CONTACT		  ="Contact";
	public static final String WWW_AUTHENTICATE ="WWW-Authenticate";
	public static final String MAX_FORWARDS     ="Max-Forwards";
	public static final String ORGANIZATION     ="Organization";
	public static final String PROXY_AUTHOR     ="Proxy-Authorization";
	public static final String PROXY_AUTHENT    ="Proxy-Authenticate";
	public static final String PROXY_REQUIRE    ="Proxy-Require";
	public static final String REQUIRE          ="Require";
	public static final String CONTENT_DISP     ="Content-Disposition";
	public static final String SUBJECT          ="Subject";
	public static final String USER_AGENT       ="User-Agent";
	public static final String WARNING          ="Warning";
	public static final String PRIORITY         ="Priority";
	public static final String DATE             ="Date";
	public static final String EXPIRES          ="Expires";
	public static final String RESPONSE_KEY     ="Response-Key";
	public static final String WARN_AGENT       ="Warn-Agent";
	public static final String SUPPORTED        ="Supported";
	public static final String AUTH_INFO        ="Authentication-Info";
	public static final String EVENT            ="Event";
	public static final String ALLOW_EVENTS     ="Allow-Events";
	public static final String SUBSCR_STATE     ="Subscription-State";
	public static final String REFER_TO         ="Refer-To";
	public static final String CHARSET 			="charset";
	public static final String UTF8 			="UTF-8";
	

	public static final char SUPPORTED_SHORT = 'k';
	public static final char CONTENT_TYPE_SHORT = 'c';
	public static final char CONTENT_ENCODING_SHORT = 'e';
	public static final char FROM_SHORT = 'f';
	public static final char CALL_ID_SHORT = 'i';
	public static final char CONTACT_SHORT = 'm';
	public static final char CONTENT_LENGTH_SHORT = 'l';
	public static final char SUBJECT_SHORT = 's';
	public static final char TO_SHORT = 't';
	public static final char VIA_SHORT = 'v';
	public static final char EVENT_SHORT = 'o';
	public static final char ALLOW_EVENTS_SHORT = 'u';
	public static final char REFER_TO_SHORT = 'r';
	
	public static final char SUPPORTED_SHORT_CAP = 'K';
	public static final char CONTENT_TYPE_SHORT_CAP = 'C';
	public static final char CONTENT_ENCODING_SHORT_CAP = 'E';
	public static final char FROM_SHORT_CAP = 'F';
	public static final char CALL_ID_SHORT_CAP = 'I';
	public static final char CONTACT_SHORT_CAP = 'M';
	public static final char CONTENT_LENGTH_SHORT_CAP = 'L';
	public static final char SUBJECT_SHORT_CAP = 'S';
	public static final char TO_SHORT_CAP = 'T';
	public static final char VIA_SHORT_CAP = 'V';
	public static final char EVENT_SHORT_CAP = 'O';
	public static final char ALLOW_EVENTS_SHORT_CAP = 'U';
	public static final char REFER_TO_SHORT_CAP = 'R';
	

	public static final int MILLISECONDS_IN_SECOND = 1000;
	public static final int SECONDS_IN_MINUTE = 60;
	public static final int MINUTES_IN_HOUR = 60;
	public static final int HOURS_IN_DAY = 24;

	public static final int DEFAULT_PROXY_TIMEOUT_MINUTES = 3;
	public static final int DEFAULT_PROXY_TIMEOUT_SECONDS = DEFAULT_PROXY_TIMEOUT_MINUTES*SECONDS_IN_MINUTE;
	
	/**
	 * Represents undefined outbound interfaces
	 */
	public static final int OUTBOUND_INTERFACE_NOT_DEFINED = -1;
}
