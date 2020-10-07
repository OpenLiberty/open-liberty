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

import com.ibm.ws.jain.protocol.ip.sip.extensions.AlertInfoHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.CallInfoHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.ContentDispositionHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.ContentLanguageHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.ErrorInfoHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.IbmRetransmissionIntervalHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.IbmRetransmissionMaxIntervalHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.IbmTransactionTimeoutHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.InReplyToHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.JoinHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.JoinHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.MimeVersionHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.PAssertedIdentityHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.PAssertedIdentityHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.PPreferredIdentityHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.PPreferredIdentityHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.PathHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.PathHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.RAckHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.RSeqHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.ReferToHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.ReplacesHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.ReplacesHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.SupportedHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.simple.AllowEventsHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.simple.EventHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.simple.SubscriptionStateHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.AcceptEncodingHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.AcceptHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.AcceptLanguageHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.AllowHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.AuthorizationHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.CSeqHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.CallIdHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ContactHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ContentEncodingHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ContentLengthHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ContentTypeHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.DateHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.EncryptionHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ExpiresHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ExtendedHeader;
import com.ibm.ws.jain.protocol.ip.sip.header.FromHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.HideHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.MaxForwardsHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.OrganizationHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.PriorityHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ProxyAuthenticateHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ProxyAuthorizationHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ProxyRequireHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.RecordRouteHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.RequireHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ResponseKeyHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.RetryAfterHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.RouteHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ServerHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.SubjectHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.TimeStampHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ToHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.UnsupportedHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.UserAgentHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ViaHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.WWWAuthenticateHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.WarningHeaderImpl;

/**
 * stateless class that creates a header object given a header name
 * 
 * @author ran
 */
public class HeaderCreator
{
	/**
	 * creates a header object by its name
	 */
	public static HeaderImpl createHeader(String name) {
		if (name.length() == 1) {
			return createShortNameHeader(name);
		}
		char firstChar = name.charAt(0);
		switch (firstChar) {
		case 'C':
		case 'c':
			return createCHeader(name);
		case 'E':
		case 'e':
			return createEHeader(name);
		case 'F':
		case 'f':
			if (name.equalsIgnoreCase(SipConstants.FROM)) {
				return new FromHeaderImpl();
			}
			break;
		case 'J':
		case 'j':
			if (name.equalsIgnoreCase(JoinHeader.name)) {
				return new JoinHeaderImpl();
			}
			break;
		case 'S':
		case 's':
			return createSHeader(name);
		case 'T':
		case 't':
			if (name.equalsIgnoreCase(SipConstants.TO)) {
				return new ToHeaderImpl();
			}
			else if (name.equalsIgnoreCase(SipConstants.TIMESTAMP)) {
				return new TimeStampHeaderImpl();
			}
			break;
		case 'V':
		case 'v':
			if (name.equalsIgnoreCase(SipConstants.VIA)) {
				return new ViaHeaderImpl();
			}
			break;
		case 'P':
		case 'p':
			return createPHeader(name);
		case 'U':
		case 'u':
			if (name.equalsIgnoreCase(SipConstants.USER_AGENT)) {
				return new UserAgentHeaderImpl();
			}
			else if (name.equalsIgnoreCase(SipConstants.UNSUPPORTED)) {
				return new UnsupportedHeaderImpl();
			}
			break;
		case 'A':
		case 'a':
			return createAHeader(name);
		case 'R':
		case 'r':
			return createRHeader(name);
		case 'W':
		case 'w':
			if (name.equalsIgnoreCase(SipConstants.WWW_AUTHENTICATE)) {
				return new WWWAuthenticateHeaderImpl();
			}
			else if (name.equalsIgnoreCase(SipConstants.WARNING)) {
				return new WarningHeaderImpl();
			}
			break;
		case 'I':
		case 'i':
			return createIHeader(name);
		case 'O':
		case 'o':
			if (name.equalsIgnoreCase(SipConstants.ORGANIZATION)) {
				return new OrganizationHeaderImpl();
			}
			break;
		case 'H':
		case 'h':
			if (name.equalsIgnoreCase(SipConstants.HIDE)) {
				return new HideHeaderImpl();
			}
			break;
		case 'M':
		case 'm':
			if (name.equalsIgnoreCase(SipConstants.MAX_FORWARDS)) {
				return new MaxForwardsHeaderImpl();
			}
			else if (name.equalsIgnoreCase(SipConstants.MIME_VERSION)) {
				return new MimeVersionHeaderImpl();
			}
			break;
		case 'D':
		case 'd':
			if (name.equalsIgnoreCase(SipConstants.DATE)) {
				return new DateHeaderImpl();
			}
			break;
		}

		// an extension header
		return new ExtendedHeader(name);
	}

	/**
	 * creates a header that starts with 'C'
	 */
	private static HeaderImpl createCHeader(String name) {
		switch (name.charAt(1)) {
		case 'o':
		case 'O': // Co...
			if (name.equalsIgnoreCase(SipConstants.CONTACT)) {
				return new ContactHeaderImpl();
			}
			if (name.equalsIgnoreCase(SipConstants.CONTENT_TYPE)) {
				return new ContentTypeHeaderImpl();
			}
			if (name.equalsIgnoreCase(SipConstants.CONTENT_DISP)) {
				return new ContentDispositionHeaderImpl();
			}
			if (name.equalsIgnoreCase(SipConstants.CONTENT_LANGUAGE)) {
				return new ContentLanguageHeaderImpl();
			}
			if (name.equalsIgnoreCase(SipConstants.CONTENT_ENCODING)) {
				return new ContentEncodingHeaderImpl();
			}
			if (name.equalsIgnoreCase(SipConstants.CONTENT_LENGTH)) {
				return new ContentLengthHeaderImpl();
			}
			break;
		case 'A':
		case 'a': // Ca...
			if (name.equalsIgnoreCase(SipConstants.CALL_INFO)) {
				return new CallInfoHeaderImpl();
			}
			if (name.equalsIgnoreCase(SipConstants.CALL_ID)) {
				return new CallIdHeaderImpl();
			}
			break;
		case 'S':
		case 's': // Cs...
			if (name.equalsIgnoreCase(SipConstants.CSEQ)) {
				return new CSeqHeaderImpl();
			}
			break;
		}

		// an extension
		return new ExtendedHeader(name);
	}

	/**
	 * creates a header that starts with 'A'
	 */
	private static HeaderImpl createAHeader(String name) {
		switch (name.charAt(1)) {
		case 'L':
		case 'l': // Al...
			if (name.equalsIgnoreCase(SipConstants.ALLOW_EVENTS)) {
				return new AllowEventsHeaderImpl();
			}
			if (name.equalsIgnoreCase(SipConstants.ALLOW)) {
				return new AllowHeaderImpl();
			}
			if (name.equalsIgnoreCase(SipConstants.ALERT_INFO)) {
				return new AlertInfoHeaderImpl();
			}
			break;
		case 'C':
		case 'c': // Ac...
			if (name.equalsIgnoreCase(SipConstants.ACCEPT)) {
				return new AcceptHeaderImpl();
			}
			if (name.equalsIgnoreCase(SipConstants.ACCEPT_ENCODING)) {
				return new AcceptEncodingHeaderImpl();
			}
			if (name.equalsIgnoreCase(SipConstants.ACCEPT_LANGUAGE)) {
				return new AcceptLanguageHeaderImpl();
			}
			break;
		case 'U':
		case 'u': // Au...
			if (name.equalsIgnoreCase(SipConstants.AUTHORIZATION)) {
				return new AuthorizationHeaderImpl();
			}
			break;
		}

		// an extension
		return new ExtendedHeader(name);
	}

	/**
	 * creates a header that starts with 'S'
	 */
	private static HeaderImpl createSHeader(String name) {
		switch (name.charAt(1)) {
		case 'U':
		case 'u': // Su...
			if (name.equalsIgnoreCase(SipConstants.SUPPORTED)) {
				return new SupportedHeaderImpl();
			}
			if (name.equalsIgnoreCase(SipConstants.SUBJECT)) {
				return new SubjectHeaderImpl();
			}
			if (name.equalsIgnoreCase(SipConstants.SUBSCR_STATE)) {
				return new SubscriptionStateHeaderImpl();
			}
			break;
		case 'E':
		case 'e': // Se...
			if (name.equalsIgnoreCase(SipConstants.SERVER)) {
				return new ServerHeaderImpl();
			}
			break;
		}

		// an extension
		return new ExtendedHeader(name);
	}

	/**
	 * creates a header that starts with 'R'
	 */
	private static HeaderImpl createRHeader(String name) {
		switch (name.charAt(1)) {
		case 'E':
		case 'e': // Re...
			if (name.equalsIgnoreCase(SipConstants.RESPONSE_KEY)) {
				return new ResponseKeyHeaderImpl();
			}
			if (name.equalsIgnoreCase(SipConstants.RECORD_ROUTE)) {
				return new RecordRouteHeaderImpl();
			}
			if (name.equalsIgnoreCase(SipConstants.REQUIRE)) {
				return new RequireHeaderImpl();
			}
			if (name.equalsIgnoreCase(SipConstants.RETRY_AFTER)) {
				return new RetryAfterHeaderImpl();
			}
			if (name.equalsIgnoreCase(ReplacesHeader.name)) {
				return new ReplacesHeaderImpl();
			}
			if (name.equalsIgnoreCase(SipConstants.REFER_TO)) {
				return new ReferToHeaderImpl();
			}
			break;
		case 'O':
		case 'o': // Ro...
			if (name.equalsIgnoreCase(SipConstants.ROUTE)) {
				return new RouteHeaderImpl();
			}
			break;
		case 'A':
		case 'a': // RA..
			if (name.equalsIgnoreCase(RAckHeaderImpl.name)) {
				return new RAckHeaderImpl();
			}
			break;
		case 'S':
		case 's': // RS..
			if (name.equalsIgnoreCase(RSeqHeaderImpl.name)) {
				return new RSeqHeaderImpl();
			}
			break;
		}

		// an extension
		return new ExtendedHeader(name);
	}

	/**
	 * creates a header that starts with 'E'
	 */
	private static HeaderImpl createEHeader(String name) {
		switch (name.charAt(1)) {
		case 'X':
		case 'x': // Ex...
			if (name.equalsIgnoreCase(SipConstants.EXPIRES)) {
				return new ExpiresHeaderImpl();
			}
			break;
		case 'V':
		case 'v': // Ev...
			if (name.equalsIgnoreCase(SipConstants.EVENT)) {
				return new EventHeaderImpl();
			}
			break;
		case 'N':
		case 'n': // En...
			if (name.equalsIgnoreCase(SipConstants.ENCRYPTION)) {
				return new EncryptionHeaderImpl();
			}
			break;
		case 'R':
		case 'r': // Er...
			if (name.equalsIgnoreCase(SipConstants.ERROR_INFO)) {
				return new ErrorInfoHeaderImpl();
			}
			break;
		}

		// an extension
		return new ExtendedHeader(name);
	}

	/**
	 * creates a header that starts with 'P'
	 */
	private static HeaderImpl createPHeader(String name) {
		switch (name.charAt(1)) {
		case 'A':
		case 'a':
			if (name.equalsIgnoreCase(PathHeader.name)) {
				return new PathHeaderImpl();
			}
			break;
		case 'R':
		case 'r': // Pr...
			if (name.length() < 3) {
				break;
			}
			switch (name.charAt(2)) {
			case 'O':
			case 'o': // Pro...
				if (name.equalsIgnoreCase(SipConstants.PROXY_AUTHOR)) {
					return new ProxyAuthorizationHeaderImpl();
				}
				if (name.equalsIgnoreCase(SipConstants.PROXY_AUTHENT)) {
					return new ProxyAuthenticateHeaderImpl();
				}
				if (name.equalsIgnoreCase(SipConstants.PROXY_REQUIRE)) {
					return new ProxyRequireHeaderImpl();
				}
				break;
			case 'I':
			case 'i': // Pri...
				if (name.equalsIgnoreCase(SipConstants.PRIORITY)) {
					return new PriorityHeaderImpl();
				}
				break;
			}
			break;
		case '-': // P-...
			if (name.equalsIgnoreCase(PAssertedIdentityHeader.name)) {
				return new PAssertedIdentityHeaderImpl();
			}
			if (name.equalsIgnoreCase(PPreferredIdentityHeader.name)) {
				return new PPreferredIdentityHeaderImpl();
			}
			break;
		}

		// an extension
		return new ExtendedHeader(name);
	}

	/**
	 * creates a header that starts with 'I'
	 */
	private static HeaderImpl createIHeader(String name) {
		if (name.equalsIgnoreCase(SipConstants.IN_REPLY_TO)) {
			return new InReplyToHeaderImpl();
		}
		// see if the name matches a proprietary IBM-* header.
		// if it does, create it with a flag that specifies
		// application-created header.
		if (name.equalsIgnoreCase(IbmTransactionTimeoutHeader.name)) {
			return new IbmTransactionTimeoutHeader(true);
		}
		if (name.equalsIgnoreCase(IbmRetransmissionIntervalHeader.name)) {
			return new IbmRetransmissionIntervalHeader(true);
		}
		if (name.equalsIgnoreCase(IbmRetransmissionMaxIntervalHeader.name)) {
			return new IbmRetransmissionMaxIntervalHeader(true);
		}
		// an extension
		return new ExtendedHeader(name);
	}
	
	/**
	 * creates a short named header (compact form)
	 */
	private static HeaderImpl createShortNameHeader(String name) {
		// only one char
		char first = name.charAt(0);
		switch (first) {
		case SipConstants.TO_SHORT:
		case SipConstants.TO_SHORT_CAP:
			return new ToHeaderImpl(true);
		case SipConstants.FROM_SHORT:
		case SipConstants.FROM_SHORT_CAP:
			return new FromHeaderImpl(true);
		case SipConstants.CALL_ID_SHORT:
		case SipConstants.CALL_ID_SHORT_CAP:
			return new CallIdHeaderImpl(true);
		case SipConstants.CONTACT_SHORT:
		case SipConstants.CONTACT_SHORT_CAP:
			return new ContactHeaderImpl(true);
		case SipConstants.CONTENT_LENGTH_SHORT:
		case SipConstants.CONTENT_LENGTH_SHORT_CAP:
			return new ContentLengthHeaderImpl(true);
		case SipConstants.VIA_SHORT:
		case SipConstants.VIA_SHORT_CAP:
			return new ViaHeaderImpl(true);
		case SipConstants.CONTENT_ENCODING_SHORT:
		case SipConstants.CONTENT_ENCODING_SHORT_CAP:
			return new ContentEncodingHeaderImpl(true);
		case SipConstants.CONTENT_TYPE_SHORT:
		case SipConstants.CONTENT_TYPE_SHORT_CAP:
			return new ContentTypeHeaderImpl(true);
		case SipConstants.SUPPORTED_SHORT:
		case SipConstants.SUPPORTED_SHORT_CAP:
			return new SupportedHeaderImpl(true);
		case SipConstants.SUBJECT_SHORT:
		case SipConstants.SUBJECT_SHORT_CAP:
			return new SubjectHeaderImpl(true);
		case SipConstants.EVENT_SHORT:
		case SipConstants.EVENT_SHORT_CAP:
			return new EventHeaderImpl(true);
		case SipConstants.ALLOW_EVENTS_SHORT:
		case SipConstants.ALLOW_EVENTS_SHORT_CAP:
			return new AllowEventsHeaderImpl(true);
		case SipConstants.REFER_TO_SHORT:
		case SipConstants.REFER_TO_SHORT_CAP:
			return new ReferToHeaderImpl(true);
		}

		// an extension (with one letter? that's sick)
		return new ExtendedHeader(name);
	}

	/**
	 * creates a header object by its name
	 */
	public static HeaderImpl createHeader(char[] name, int length) {
		if (length == 1) {
			return createShortNameHeader(name, length);
		}
		char firstChar = name[0];
		switch (firstChar) {
		case 'C':
		case 'c':
			return createCHeader(name, length);
		case 'E':
		case 'e':
			return createEHeader(name, length);
		case 'F':
		case 'f':
			if (length == 4 &&
				(name[1] == 'R' || name[1] == 'r') &&
				(name[2] == 'O' || name[2] == 'o') &&
				(name[3] == 'M' || name[3] == 'm'))
			{
				return new FromHeaderImpl();
			}
			break;
		case 'S':
		case 's':
			return createSHeader(name, length);
		case 'T':
		case 't':
			if (length == 2 &&
				(name[1] == 'O' || name[1] == 'o'))
			{
				return new ToHeaderImpl();
			}
			else if (length == 9 &&
				(name[1] == 'I' || name[1] == 'i') &&
				(name[2] == 'M' || name[2] == 'm') &&
				(name[3] == 'E' || name[3] == 'e') &&
				(name[4] == 'S' || name[4] == 's') &&
				(name[5] == 'T' || name[5] == 't') &&
				(name[6] == 'A' || name[6] == 'a') &&
				(name[7] == 'M' || name[7] == 'm') &&
				(name[8] == 'P' || name[8] == 'p'))
			{
				return new TimeStampHeaderImpl();
			}
			break;
		case 'V':
		case 'v':
			if (length == 3 &&
				(name[1] == 'I' || name[1] == 'i') &&
				(name[2] == 'A' || name[2] == 'a'))
			{
				return new ViaHeaderImpl();
			}
			break;
		case 'P':
		case 'p':
			return createPHeader(name, length);
		case 'U':
		case 'u':
			if (length == 10 &&
				(name[1] == 'S' || name[1] == 's') &&
				(name[2] == 'E' || name[2] == 'e') &&
				(name[3] == 'R' || name[3] == 'r') &&
				 name[4] == '-' &&
				(name[5] == 'A' || name[5] == 'a') &&
				(name[6] == 'G' || name[6] == 'g') &&
				(name[7] == 'E' || name[7] == 'e') &&
				(name[8] == 'N' || name[8] == 'n') &&
				(name[9] == 'T' || name[9] == 't'))
			{
				return new UserAgentHeaderImpl();
			}
			else if (length == 11 &&
				(name[1] == 'N' || name[1] == 'n') &&
				(name[2] == 'S' || name[2] == 's') &&
				(name[3] == 'U' || name[3] == 'u') &&
				(name[4] == 'P' || name[4] == 'p') &&
				(name[5] == 'P' || name[5] == 'p') &&
				(name[6] == 'O' || name[6] == 'o') &&
				(name[7] == 'R' || name[7] == 'r') &&
				(name[8] == 'T' || name[8] == 't') &&
				(name[9] == 'E' || name[9] == 'e') &&
				(name[10] == 'D' || name[10] == 'd'))
			{
				return new UnsupportedHeaderImpl();
			}
			break;
		case 'A':
		case 'a':
			return createAHeader(name, length);
		case 'R':
		case 'r':
			return createRHeader(name, length);
		case 'W':
		case 'w':
			if (length == 16 &&
				(name[1] == 'W' || name[1] == 'w') &&
				(name[2] == 'W' || name[2] == 'w') &&
				 name[3] == '-' &&
				(name[4] == 'A' || name[4] == 'a') &&
				(name[5] == 'U' || name[5] == 'u') &&
				(name[6] == 'T' || name[6] == 't') &&
				(name[7] == 'H' || name[7] == 'h') &&
				(name[8] == 'E' || name[8] == 'e') &&
				(name[9] == 'N' || name[9] == 'n') &&
				(name[10] == 'T' || name[10] == 't') &&
				(name[11] == 'I' || name[11] == 'i') &&
				(name[12] == 'C' || name[12] == 'c') &&
				(name[13] == 'A' || name[13] == 'a') &&
				(name[14] == 'T' || name[14] == 't') &&
				(name[15] == 'E' || name[15] == 'e'))
			{
				return new WWWAuthenticateHeaderImpl();
			}
			else if (length == 7 &&
				(name[1] == 'A' || name[1] == 'a') &&
				(name[2] == 'R' || name[2] == 'r') &&
				(name[3] == 'N' || name[3] == 'n') &&
				(name[4] == 'I' || name[4] == 'i') &&
				(name[5] == 'N' || name[5] == 'n') &&
				(name[6] == 'G' || name[6] == 'g'))
			{
				return new WarningHeaderImpl();
			}
			break;
		case 'I':
		case 'i':
			return createIHeader(name, length);
		case 'J':
		case 'j':
			if (length == 4 &&
				(name[1] == 'O' || name[1] == 'o') &&
				(name[2] == 'I' || name[2] == 'i') &&
				(name[3] == 'N' || name[3] == 'n'))
			{
				return new JoinHeaderImpl();
			}
		case 'O':
		case 'o':
			if (length == 12 &&
				(name[1] == 'R' || name[1] == 'r') &&
				(name[2] == 'G' || name[2] == 'g') &&
				(name[3] == 'A' || name[3] == 'a') &&
				(name[4] == 'N' || name[4] == 'n') &&
				(name[5] == 'I' || name[5] == 'i') &&
				(name[6] == 'Z' || name[6] == 'z') &&
				(name[7] == 'A' || name[7] == 'a') &&
				(name[8] == 'T' || name[8] == 't') &&
				(name[9] == 'I' || name[9] == 'i') &&
				(name[10] == 'O' || name[10] == 'o') &&
				(name[11] == 'N' || name[11] == 'n'))
			{
				return new OrganizationHeaderImpl();
			}
			break;
		case 'H':
		case 'h':
			if (length == 4 &&
				(name[1] == 'I' || name[1] == 'i') &&
				(name[2] == 'D' || name[2] == 'd') &&
				(name[3] == 'E' || name[3] == 'e'))
			{
				return new HideHeaderImpl();
			}
			break;
		case 'M':
		case 'm':
			if (length == 12 &&
				(name[1] == 'A' || name[1] == 'a') &&
				(name[2] == 'X' || name[2] == 'x') &&
				 name[3] == '-' &&
				(name[4] == 'F' || name[4] == 'f') &&
				(name[5] == 'O' || name[5] == 'o') &&
				(name[6] == 'R' || name[6] == 'r') &&
				(name[7] == 'W' || name[7] == 'w') &&
				(name[8] == 'A' || name[8] == 'a') &&
				(name[9] == 'R' || name[9] == 'r') &&
				(name[10] == 'D' || name[10] == 'd') &&
				(name[11] == 'S' || name[11] == 's'))
			{
				return new MaxForwardsHeaderImpl();
			}
			else if (length == 12 &&
				(name[1] == 'I' || name[1] == 'i') &&
				(name[2] == 'M' || name[2] == 'm') &&
				(name[3] == 'E' || name[3] == 'e') &&
				 name[4] == '-' &&
				(name[5] == 'V' || name[5] == 'v') &&
				(name[6] == 'E' || name[6] == 'e') &&
				(name[7] == 'R' || name[7] == 'r') &&
				(name[8] == 'S' || name[8] == 's') &&
				(name[9] == 'I' || name[9] == 'i') &&
				(name[10] == 'O' || name[10] == 'o') &&
				(name[11] == 'N' || name[11] == 'n'))
			{
				return new MimeVersionHeaderImpl();
			}
			break;
		case 'D':
		case 'd':
			if (length == 4 &&
				(name[1] == 'A' || name[1] == 'a') &&
				(name[2] == 'T' || name[2] == 't') &&
				(name[3] == 'E' || name[3] == 'e'))
			{
				return new DateHeaderImpl();
			}
			break;
		}

		// an extension header
		return new ExtendedHeader(String.valueOf(name, 0, length));
	}

	/**
	 * creates a header that starts with 'C'
	 */
	private static HeaderImpl createCHeader(char[] name, int length) {
		switch (name[1]) {
		case 'o':
		case 'O': // Co...
			if (length == 7 &&
				(name[2] == 'N' || name[2] == 'n') &&
				(name[3] == 'T' || name[3] == 't') &&
				(name[4] == 'A' || name[4] == 'a') &&
				(name[5] == 'C' || name[5] == 'c') &&
				(name[6] == 'T' || name[6] == 't'))
			{
				return new ContactHeaderImpl();
			}
			if (length > 8 &&
				(name[2] == 'N' || name[2] == 'n') &&
				(name[3] == 'T' || name[3] == 't') &&
				(name[4] == 'E' || name[4] == 'e') &&
				(name[5] == 'N' || name[5] == 'n') &&
				(name[6] == 'T' || name[6] == 't') &&
				 name[7] == '-')
			{
				// Content-...
				if (length == 14 &&
					(name[8] == 'L' || name[8] == 'l') &&
					(name[9] == 'E' || name[9] == 'e') &&
					(name[10] == 'N' || name[10] == 'n') &&
					(name[11] == 'G' || name[11] == 'g') &&
					(name[12] == 'T' || name[12] == 't') &&
					(name[13] == 'H' || name[13] == 'h'))
				{
					return new ContentLengthHeaderImpl();
				}
				if (length == 12 &&
					(name[8] == 'T' || name[8] == 't') &&
					(name[9] == 'Y' || name[9] == 'y') &&
					(name[10] == 'P' || name[10] == 'p') &&
					(name[11] == 'E' || name[11] == 'e'))
				{
					return new ContentTypeHeaderImpl();
				}
				if (length == 16 &&
					(name[8] == 'L' || name[8] == 'l') &&
					(name[9] == 'A' || name[9] == 'a') &&
					(name[10] == 'N' || name[10] == 'n') &&
					(name[11] == 'G' || name[11] == 'g') &&
					(name[12] == 'U' || name[12] == 'u') &&
					(name[13] == 'A' || name[13] == 'a') &&
					(name[14] == 'G' || name[14] == 'g') &&
					(name[15] == 'E' || name[15] == 'e'))
				{
					return new ContentLanguageHeaderImpl();
				}
				if (length == 16 &&
					(name[8] == 'E' || name[8] == 'e') &&
					(name[9] == 'N' || name[9] == 'n') &&
					(name[10] == 'C' || name[10] == 'c') &&
					(name[11] == 'O' || name[11] == 'o') &&
					(name[12] == 'D' || name[12] == 'd') &&
					(name[13] == 'I' || name[13] == 'i') &&
					(name[14] == 'N' || name[14] == 'n') &&
					(name[15] == 'G' || name[15] == 'g'))
				{
					return new ContentEncodingHeaderImpl();
				}
				if (length == 19 &&
					(name[8] == 'D' || name[8] == 'd') &&
					(name[9] == 'I' || name[9] == 'i') &&
					(name[10] == 'S' || name[10] == 's') &&
					(name[11] == 'P' || name[11] == 'p') &&
					(name[12] == 'O' || name[12] == 'o') &&
					(name[13] == 'S' || name[13] == 's') &&
					(name[14] == 'I' || name[14] == 'i') &&
					(name[15] == 'T' || name[15] == 't') &&
					(name[16] == 'I' || name[16] == 'i') &&
					(name[17] == 'O' || name[17] == 'o') &&
					(name[18] == 'N' || name[18] == 'n'))
				{
					return new ContentDispositionHeaderImpl();
				}
			}
			break;
		case 'A':
		case 'a': // Ca...
			if (length == 9 &&
				(name[2] == 'L' || name[2] == 'l') &&
				(name[3] == 'L' || name[3] == 'l') &&
				 name[4] == '-' &&
				(name[5] == 'I' || name[5] == 'i') &&
				(name[6] == 'N' || name[6] == 'n') &&
				(name[7] == 'F' || name[7] == 'f') &&
				(name[8] == 'O' || name[8] == 'o'))
			{
				return new CallInfoHeaderImpl();
			}
			if (length == 7 &&
				(name[2] == 'L' || name[2] == 'l') &&
				(name[3] == 'L' || name[3] == 'l') &&
				 name[4] == '-' &&
				(name[5] == 'I' || name[5] == 'i') &&
				(name[6] == 'D' || name[6] == 'd'))
			{
				return new CallIdHeaderImpl();
			}
			break;
		case 'S':
		case 's': // Cs...
			if (length == 4 &&
				(name[2] == 'E' || name[2] == 'e') &&
				(name[3] == 'Q' || name[3] == 'q'))
			{
				return new CSeqHeaderImpl();
			}
			break;
		}

		// an extension
		return new ExtendedHeader(String.valueOf(name, 0, length));
	}

	/**
	 * creates a header that starts with 'A'
	 */
	private static HeaderImpl createAHeader(char[] name, int length) {
		switch (name[1]) {
		case 'L':
		case 'l': // Al...
			if (length == 12 &&
				(name[2] == 'L' || name[2] == 'l') &&
				(name[3] == 'O' || name[3] == 'o') &&
				(name[4] == 'W' || name[4] == 'w') &&
				 name[5] == '-' &&
				(name[6] == 'E' || name[6] == 'e') &&
				(name[7] == 'V' || name[7] == 'v') &&
				(name[8] == 'E' || name[8] == 'e') &&
				(name[9] == 'N' || name[9] == 'n') &&
				(name[10] == 'T' || name[10] == 't') &&
				(name[11] == 'S' || name[11] == 's'))
			{
				return new AllowEventsHeaderImpl();
			}
			if (length == 5 &&
				(name[2] == 'L' || name[2] == 'l') &&
				(name[3] == 'O' || name[3] == 'o') &&
				(name[4] == 'W' || name[4] == 'w'))
			{
				return new AllowHeaderImpl();
			}
			if (length == 10 &&
				(name[2] == 'E' || name[2] == 'e') &&
				(name[3] == 'R' || name[3] == 'r') &&
				(name[4] == 'T' || name[4] == 't') &&
				 name[5] == '-' &&
				(name[6] == 'I' || name[6] == 'i') &&
				(name[7] == 'N' || name[7] == 'n') &&
				(name[8] == 'F' || name[8] == 'f') &&
				(name[9] == 'O' || name[9] == 'o'))
			{
				return new AlertInfoHeaderImpl();
			}
			break;
		case 'C':
		case 'c': // Ac...
			if (length > 5 &&
				(name[2] == 'C' || name[2] == 'c') &&
				(name[3] == 'E' || name[3] == 'e') &&
				(name[4] == 'P' || name[4] == 'p') &&
				(name[5] == 'T' || name[5] == 't'))
			{
				// Accept...
				if (length == 6) {
					return new AcceptHeaderImpl();
				}
				if (length == 15 && name[6] == '-') {
					// Accept-xxxxxxxx
					if ((name[7] == 'E' || name[7] == 'e') &&
						(name[8] == 'N' || name[8] == 'n') &&
						(name[9] == 'C' || name[9] == 'c') &&
						(name[10] == 'O' || name[10] == 'o') &&
						(name[11] == 'D' || name[11] == 'd') &&
						(name[12] == 'I' || name[12] == 'i') &&
						(name[13] == 'N' || name[13] == 'n') &&
						(name[14] == 'G' || name[14] == 'g'))
					{
						return new AcceptEncodingHeaderImpl();
					}
					if ((name[7] == 'L' || name[7] == 'l') &&
						(name[8] == 'A' || name[8] == 'a') &&
						(name[9] == 'N' || name[9] == 'n') &&
						(name[10] == 'G' || name[10] == 'g') &&
						(name[11] == 'U' || name[11] == 'u') &&
						(name[12] == 'A' || name[12] == 'a') &&
						(name[13] == 'G' || name[13] == 'g') &&
						(name[14] == 'E' || name[14] == 'e'))
					{
						return new AcceptLanguageHeaderImpl();
					}
				}
			}
			break;
		case 'U':
		case 'u': // Au...
			if (length == 13 &&
				(name[2] == 'T' || name[2] == 't') &&
				(name[3] == 'H' || name[3] == 'h') &&
				(name[4] == 'O' || name[4] == 'o') &&
				(name[5] == 'R' || name[5] == 'r') &&
				(name[6] == 'I' || name[6] == 'i') &&
				(name[7] == 'Z' || name[7] == 'z') &&
				(name[8] == 'A' || name[8] == 'a') &&
				(name[9] == 'T' || name[9] == 't') &&
				(name[10] == 'I' || name[10] == 'i') &&
				(name[11] == 'O' || name[11] == 'o') &&
				(name[12] == 'N' || name[12] == 'n'))
			{
				return new AuthorizationHeaderImpl();
			}
			break;
		}

		// an extension
		return new ExtendedHeader(String.valueOf(name, 0, length));
	}

	/**
	 * creates a header that starts with 'S'
	 */
	private static HeaderImpl createSHeader(char[] name, int length) {
		switch (name[1]) {
		case 'U':
		case 'u': // Su...
			if (length == 9 &&
				(name[2] == 'P' || name[2] == 'p') &&
				(name[3] == 'P' || name[3] == 'p') &&
				(name[4] == 'O' || name[4] == 'o') &&
				(name[5] == 'R' || name[5] == 'r') &&
				(name[6] == 'T' || name[6] == 't') &&
				(name[7] == 'E' || name[7] == 'e') &&
				(name[8] == 'D' || name[8] == 'd'))
			{
				return new SupportedHeaderImpl();
			}
			if (length == 7 &&
				(name[2] == 'B' || name[2] == 'b') &&
				(name[3] == 'J' || name[3] == 'j') &&
				(name[4] == 'E' || name[4] == 'e') &&
				(name[5] == 'C' || name[5] == 'c') &&
				(name[6] == 'T' || name[6] == 't'))
			{
				return new SubjectHeaderImpl();
			}
			if (length == 18 &&
				(name[2] == 'B' || name[2] == 'b') &&
				(name[3] == 'S' || name[3] == 's') &&
				(name[4] == 'C' || name[4] == 'c') &&
				(name[5] == 'R' || name[5] == 'r') &&
				(name[6] == 'I' || name[6] == 'i') &&
				(name[7] == 'P' || name[7] == 'p') &&
				(name[8] == 'T' || name[8] == 't') &&
				(name[9] == 'I' || name[9] == 'i') &&
				(name[10] == 'O' || name[10] == 'o') &&
				(name[11] == 'N' || name[11] == 'n') &&
				 name[12] == '-' &&
				(name[13] == 'S' || name[13] == 's') &&
				(name[14] == 'T' || name[14] == 't') &&
				(name[15] == 'A' || name[15] == 'a') &&
				(name[16] == 'T' || name[16] == 't') &&
				(name[17] == 'E' || name[17] == 'e'))
			{
				return new SubscriptionStateHeaderImpl();
			}
			break;
		case 'E':
		case 'e': // Se...
			if (length == 6 &&
				(name[2] == 'R' || name[2] == 'r') &&
				(name[3] == 'V' || name[3] == 'v') &&
				(name[4] == 'E' || name[4] == 'e') &&
				(name[5] == 'R' || name[5] == 'r'))
			{
				return new ServerHeaderImpl();
			}
			break;
		}

		// an extension
		return new ExtendedHeader(String.valueOf(name, 0, length));
	}

	/**
	 * creates a header that starts with 'R'
	 */
	private static HeaderImpl createRHeader(char[] name, int length) {
		switch (name[1]) {
		case 'E':
		case 'e': // Re...
			if (length == 12 &&
				(name[2] == 'S' || name[2] == 's') &&
				(name[3] == 'P' || name[3] == 'p') &&
				(name[4] == 'O' || name[4] == 'o') &&
				(name[5] == 'N' || name[5] == 'n') &&
				(name[6] == 'S' || name[6] == 's') &&
				(name[7] == 'E' || name[7] == 'e') &&
				 name[8] == '-' &&
				(name[9] == 'K' || name[9] == 'k') &&
				(name[10] == 'E' || name[10] == 'e') &&
				(name[11] == 'Y' || name[11] == 'y'))
			{
				return new ResponseKeyHeaderImpl();
			}
			if (length == 12 &&
				(name[2] == 'C' || name[2] == 'c') &&
				(name[3] == 'O' || name[3] == 'o') &&
				(name[4] == 'R' || name[4] == 'r') &&
				(name[5] == 'D' || name[5] == 'd') &&
				 name[6] == '-' &&
				(name[7] == 'R' || name[7] == 'r') &&
				(name[8] == 'O' || name[8] == 'o') &&
				(name[9] == 'U' || name[9] == 'u') &&
				(name[10] == 'T' || name[10] == 't') &&
				(name[11] == 'E' || name[11] == 'e'))
			{
				return new RecordRouteHeaderImpl();
			}
			if (length == 7 &&
				(name[2] == 'Q' || name[2] == 'q') &&
				(name[3] == 'U' || name[3] == 'u') &&
				(name[4] == 'I' || name[4] == 'i') &&
				(name[5] == 'R' || name[5] == 'r') &&
				(name[6] == 'E' || name[6] == 'e'))
			{
				return new RequireHeaderImpl();
			}
			if (length == 11 &&
				(name[2] == 'T' || name[2] == 't') &&
				(name[3] == 'R' || name[3] == 'r') &&
				(name[4] == 'Y' || name[4] == 'y') &&
				 name[5] == '-' &&
				(name[6] == 'A' || name[6] == 'a') &&
				(name[7] == 'F' || name[7] == 'f') &&
				(name[8] == 'T' || name[8] == 't') &&
				(name[9] == 'E' || name[9] == 'e') &&
				(name[10] == 'R' || name[10] == 'r'))
			{
				return new RetryAfterHeaderImpl();
			}
			if (length == 8 &&
				(name[2] == 'P' || name[2] == 'p') &&
				(name[3] == 'L' || name[3] == 'l') &&
				(name[4] == 'A' || name[4] == 'a') &&
				(name[5] == 'C' || name[5] == 'c') &&
				(name[6] == 'E' || name[6] == 'e') &&
				(name[7] == 'S' || name[7] == 's'))
			{
				return new ReplacesHeaderImpl();
			}
			if (length == 8 &&
				(name[2] == 'F' || name[2] == 'f') &&
				(name[3] == 'E' || name[3] == 'e') &&
				(name[4] == 'R' || name[4] == 'r') &&
				name[5] == '-' &&
				(name[6] == 'T' || name[6] == 't') &&
				(name[7] == 'O' || name[7] == 'o'))
			{
				return new ReferToHeaderImpl();
			}
			break;
		case 'O':
		case 'o': // Ro...
			if (length == 5 &&
				(name[2] == 'U' || name[2] == 'u') &&
				(name[3] == 'T' || name[3] == 't') &&
				(name[4] == 'E' || name[4] == 'e'))
			{
				return new RouteHeaderImpl();
			}
			break;
		case 'A':
		case 'a': // RA..
			if (length == 4 &&
				(name[2] == 'C' || name[2] == 'c') &&
				(name[3] == 'K' || name[3] == 'k'))
			{
				return new RAckHeaderImpl();
			}
			break;
		case 'S':
		case 's': // RS..
			if (length == 4 &&
				(name[2] == 'E' || name[2] == 'e') &&
				(name[3] == 'Q' || name[3] == 'q'))
			{
				return new RSeqHeaderImpl();
			}
			break;
		}

		// an extension
		return new ExtendedHeader(String.valueOf(name, 0, length));
	}

	/**
	 * creates a header that starts with 'E'
	 */
	private static HeaderImpl createEHeader(char[] name, int length) {
		switch (name[1]) {
		case 'X':
		case 'x': // Ex...
			if (length == 7 &&
				(name[2] == 'P' || name[2] == 'p') &&
				(name[3] == 'I' || name[3] == 'i') &&
				(name[4] == 'R' || name[4] == 'r') &&
				(name[5] == 'E' || name[5] == 'e') &&
				(name[6] == 'S' || name[6] == 's'))
			{
				return new ExpiresHeaderImpl();
			}
			break;
		case 'V':
		case 'v': // Ev...
			if (length == 5 &&
				(name[2] == 'E' || name[2] == 'e') &&
				(name[3] == 'N' || name[3] == 'n') &&
				(name[4] == 'T' || name[4] == 't'))
			{
				return new EventHeaderImpl();
			}
			break;
		case 'N':
		case 'n': // En...
			if (length == 10 &&
				(name[2] == 'C' || name[2] == 'c') &&
				(name[3] == 'R' || name[3] == 'r') &&
				(name[4] == 'Y' || name[4] == 'y') &&
				(name[5] == 'P' || name[5] == 'p') &&
				(name[6] == 'T' || name[6] == 't') &&
				(name[7] == 'I' || name[7] == 'i') &&
				(name[8] == 'O' || name[8] == 'o') &&
				(name[9] == 'N' || name[9] == 'n'))
			{
				return new EncryptionHeaderImpl();
			}
			break;
		case 'R':
		case 'r': // Er...
			if (length == 10 &&
				(name[2] == 'R' || name[2] == 'r') &&
				(name[3] == 'O' || name[3] == 'o') &&
				(name[4] == 'R' || name[4] == 'r') &&
				 name[5] == '-' &&
				(name[6] == 'I' || name[6] == 'i') &&
				(name[7] == 'N' || name[7] == 'n') &&
				(name[8] == 'F' || name[8] == 'f') &&
				(name[9] == 'O' || name[9] == 'o'))
			{
				return new ErrorInfoHeaderImpl();
			}
			break;
		}

		// an extension
		return new ExtendedHeader(String.valueOf(name, 0, length));
	}

	/**
	 * creates a header that starts with 'P'
	 */
	private static HeaderImpl createPHeader(char[] name, int length) {
		switch (name[1]) {
		case 'A': // Pa..
		case 'a':
			if (length == 4 &&
				(name[2] == 'T' || name[2] == 't') &&
				(name[3] == 'H' || name[3] == 'h'))
			{
				return new PathHeaderImpl();
			}
			break;
		case 'R':
		case 'r': // Pr...
			if (length < 3) {
				break;
			}
			switch (name[2]) {
			case 'O':
			case 'o': // Pro...
				if (length > 6 &&
					(name[3] == 'X' || name[3] == 'x') &&
					(name[4] == 'Y' || name[4] == 'y') &&
					name[5] == '-')
				{
					// Proxy-...
					if (length == 19 &&
						(name[6] == 'A' || name[6] == 'a') &&
						(name[7] == 'U' || name[7] == 'u') &&
						(name[8] == 'T' || name[8] == 't') &&
						(name[9] == 'H' || name[9] == 'h') &&
						(name[10] == 'O' || name[10] == 'o') &&
						(name[11] == 'R' || name[11] == 'r') &&
						(name[12] == 'I' || name[12] == 'i') &&
						(name[13] == 'Z' || name[13] == 'z') &&
						(name[14] == 'A' || name[14] == 'a') &&
						(name[15] == 'T' || name[15] == 't') &&
						(name[16] == 'I' || name[16] == 'i') &&
						(name[17] == 'O' || name[17] == 'o') &&
						(name[18] == 'N' || name[18] == 'n'))
					{
						return new ProxyAuthorizationHeaderImpl();
					}
					if (length == 18 &&
						(name[6] == 'A' || name[6] == 'a') &&
						(name[7] == 'U' || name[7] == 'u') &&
						(name[8] == 'T' || name[8] == 't') &&
						(name[9] == 'H' || name[9] == 'h') &&
						(name[10] == 'E' || name[10] == 'e') &&
						(name[11] == 'N' || name[11] == 'n') &&
						(name[12] == 'T' || name[12] == 't') &&
						(name[13] == 'I' || name[13] == 'i') &&
						(name[14] == 'C' || name[14] == 'c') &&
						(name[15] == 'A' || name[15] == 'a') &&
						(name[16] == 'T' || name[16] == 't') &&
						(name[17] == 'E' || name[17] == 'e'))
					{
						return new ProxyAuthenticateHeaderImpl();
					}
					if (length == 13 &&
						(name[6] == 'R' || name[6] == 'r') &&
						(name[7] == 'E' || name[7] == 'e') &&
						(name[8] == 'Q' || name[8] == 'q') &&
						(name[9] == 'U' || name[9] == 'u') &&
						(name[10] == 'I' || name[10] == 'i') &&
						(name[11] == 'R' || name[11] == 'r') &&
						(name[12] == 'E' || name[12] == 'e'))
					{
						return new ProxyRequireHeaderImpl();
					}
				}
				break;
			case 'I':
			case 'i': // Pri...
				if (length == 8 &&
					(name[3] == 'O' || name[3] == 'o') &&
					(name[4] == 'R' || name[4] == 'r') &&
					(name[5] == 'I' || name[5] == 'i') &&
					(name[6] == 'T' || name[6] == 't') &&
					(name[7] == 'Y' || name[7] == 'y'))
				{
					return new PriorityHeaderImpl();
				}
				break;
			}
			break;
		case '-': // P-...
			if (length == 19 &&
				(name[2] == 'A' || name[2] == 'a') &&
				(name[3] == 'S' || name[3] == 's') &&
				(name[4] == 'S' || name[4] == 's') &&
				(name[5] == 'E' || name[5] == 'e') &&
				(name[6] == 'R' || name[6] == 'r') &&
				(name[7] == 'T' || name[7] == 't') &&
				(name[8] == 'E' || name[8] == 'e') &&
				(name[9] == 'D' || name[9] == 'd') &&
				(name[10] == '-') &&
				(name[11] == 'I' || name[11] == 'i') &&
				(name[12] == 'D' || name[12] == 'd') &&
				(name[13] == 'E' || name[13] == 'e') &&
				(name[14] == 'N' || name[14] == 'n') &&
				(name[15] == 'T' || name[15] == 't') &&
				(name[16] == 'I' || name[16] == 'i') &&
				(name[17] == 'T' || name[17] == 't') &&
				(name[18] == 'Y' || name[18] == 'y'))
			{
				return new PAssertedIdentityHeaderImpl();
			}
			if (length == 20 &&
				(name[2] == 'P' || name[2] == 'p') &&
				(name[3] == 'R' || name[3] == 'r') &&
				(name[4] == 'E' || name[4] == 'e') &&
				(name[5] == 'F' || name[5] == 'f') &&
				(name[6] == 'E' || name[6] == 'e') &&
				(name[7] == 'R' || name[7] == 'r') &&
				(name[8] == 'R' || name[8] == 'r') &&
				(name[9] == 'E' || name[9] == 'e') &&
				(name[10] == 'D' || name[10] == 'd') &&
				(name[11] == '-') &&
				(name[12] == 'I' || name[12] == 'i') &&
				(name[13] == 'D' || name[13] == 'd') &&
				(name[14] == 'E' || name[14] == 'e') &&
				(name[15] == 'N' || name[15] == 'n') &&
				(name[16] == 'T' || name[16] == 't') &&
				(name[17] == 'I' || name[17] == 'i') &&
				(name[18] == 'T' || name[18] == 't') &&
				(name[19] == 'Y' || name[19] == 'y'))
			{
				return new PPreferredIdentityHeaderImpl();
			}
			break;
		}

		// an extension
		return new ExtendedHeader(String.valueOf(name, 0, length));
	}

	/**
	 * creates a header that starts with 'I'
	 */
	private static HeaderImpl createIHeader(char[] name, int length) {
		if (length == 11 &&
			(name[1] == 'N' || name [1] == 'n') &&
			(name[2] == '-') &&
			(name[3] == 'R' || name [3] == 'r') &&
			(name[4] == 'E' || name [4] == 'e') &&
			(name[5] == 'P' || name [5] == 'p') &&
			(name[6] == 'L' || name [6] == 'l') &&
			(name[7] == 'Y' || name [7] == 'y') &&
			(name[8] == '-') &&
			(name[9] == 'T' || name [9] == 't') &&
			(name[10] == 'O' || name [10] == 'o'))
		{
			return new InReplyToHeaderImpl();
		}
		// see if the name matches a proprietary IBM-* header.
		// if it does, create it with a flag that specifies
		// network-created (not application-created) header.
		if (length == 22 &&
			(name[1] == 'B' || name[1] == 'b') &&
			(name[2] == 'M' || name[2] == 'm') &&
			(name[3] == '-') &&
			(name[4] == 'T' || name[4] == 't') &&
			(name[5] == 'R' || name[5] == 'r') &&
			(name[6] == 'A' || name[6] == 'a') &&
			(name[7] == 'N' || name[7] == 'n') &&
			(name[8] == 'S' || name[8] == 's') &&
			(name[9] == 'A' || name[9] == 'a') &&
			(name[10] == 'C' || name[10] == 'c') &&
			(name[11] == 'T' || name[11] == 't') &&
			(name[12] == 'I' || name[12] == 'i') &&
			(name[13] == 'O' || name[13] == 'o') &&
			(name[14] == 'N' || name[14] == 'n') &&
			(name[15] == 'T' || name[15] == 't') &&
			(name[16] == 'I' || name[16] == 'i') &&
			(name[17] == 'M' || name[17] == 'm') &&
			(name[18] == 'E' || name[18] == 'e') &&
			(name[19] == 'O' || name[19] == 'o') &&
			(name[20] == 'U' || name[20] == 'u') &&
			(name[21] == 'T' || name[21] == 't'))
		{
			return new IbmTransactionTimeoutHeader(false);
		}
		if (length == 26 &&
			(name[1] == 'B' || name[1] == 'b') &&
			(name[2] == 'M' || name[2] == 'm') &&
			(name[3] == '-') &&
			(name[4] == 'R' || name[4] == 'r') &&
			(name[5] == 'E' || name[5] == 'e') &&
			(name[6] == 'T' || name[6] == 't') &&
			(name[7] == 'R' || name[7] == 'r') &&
			(name[8] == 'A' || name[8] == 'a') &&
			(name[9] == 'N' || name[9] == 'n') &&
			(name[10] == 'S' || name[10] == 's') &&
			(name[11] == 'M' || name[11] == 'm') &&
			(name[12] == 'I' || name[12] == 'i') &&
			(name[13] == 'S' || name[13] == 's') &&
			(name[14] == 'S' || name[14] == 's') &&
			(name[15] == 'I' || name[15] == 'i') &&
			(name[16] == 'O' || name[16] == 'o') &&
			(name[17] == 'N' || name[17] == 'n') &&
			(name[18] == 'I' || name[18] == 'i') &&
			(name[19] == 'N' || name[19] == 'n') &&
			(name[20] == 'T' || name[20] == 't') &&
			(name[21] == 'E' || name[21] == 'e') &&
			(name[22] == 'R' || name[22] == 'r') &&
			(name[23] == 'V' || name[23] == 'v') &&
			(name[24] == 'A' || name[24] == 'a') &&
			(name[25] == 'L' || name[25] == 'l'))
		{
			return new IbmRetransmissionIntervalHeader(false);
		}
		if (length == 29 &&
			(name[1] == 'B' || name[1] == 'b') &&
			(name[2] == 'M' || name[2] == 'm') &&
			(name[3] == '-') &&
			(name[4] == 'R' || name[4] == 'r') &&
			(name[5] == 'E' || name[5] == 'e') &&
			(name[6] == 'T' || name[6] == 't') &&
			(name[7] == 'R' || name[7] == 'r') &&
			(name[8] == 'A' || name[8] == 'a') &&
			(name[9] == 'N' || name[9] == 'n') &&
			(name[10] == 'S' || name[10] == 's') &&
			(name[11] == 'M' || name[11] == 'm') &&
			(name[12] == 'I' || name[12] == 'i') &&
			(name[13] == 'S' || name[13] == 's') &&
			(name[14] == 'S' || name[14] == 's') &&
			(name[15] == 'I' || name[15] == 'i') &&
			(name[16] == 'O' || name[16] == 'o') &&
			(name[17] == 'N' || name[17] == 'n') &&
			(name[18] == 'M' || name[18] == 'm') &&
			(name[19] == 'A' || name[19] == 'a') &&
			(name[20] == 'X' || name[20] == 'x') &&
			(name[21] == 'I' || name[21] == 'i') &&
			(name[22] == 'N' || name[22] == 'n') &&
			(name[23] == 'T' || name[23] == 't') &&
			(name[24] == 'E' || name[24] == 'e') &&
			(name[25] == 'R' || name[25] == 'r') &&
			(name[26] == 'V' || name[26] == 'v') &&
			(name[27] == 'A' || name[27] == 'a') &&
			(name[28] == 'L' || name[28] == 'l'))
		{
			return new IbmRetransmissionMaxIntervalHeader(false);
		}

		// an extension
		return new ExtendedHeader(String.valueOf(name, 0, length));
	}

	/**
	 * creates a short named header (compact form)
	 */
	private static HeaderImpl createShortNameHeader(char[] name, int length) {
		// only one char
		char first = name[0];
		switch (first) {
		case SipConstants.TO_SHORT:
		case SipConstants.TO_SHORT_CAP:
			return new ToHeaderImpl(true);
		case SipConstants.FROM_SHORT:
		case SipConstants.FROM_SHORT_CAP:
			return new FromHeaderImpl(true);
		case SipConstants.CALL_ID_SHORT:
		case SipConstants.CALL_ID_SHORT_CAP:
			return new CallIdHeaderImpl(true);
		case SipConstants.CONTACT_SHORT:
		case SipConstants.CONTACT_SHORT_CAP:
			return new ContactHeaderImpl(true);
		case SipConstants.CONTENT_LENGTH_SHORT:
		case SipConstants.CONTENT_LENGTH_SHORT_CAP:
			return new ContentLengthHeaderImpl(true);
		case SipConstants.VIA_SHORT:
		case SipConstants.VIA_SHORT_CAP:
			return new ViaHeaderImpl(true);
		case SipConstants.CONTENT_ENCODING_SHORT:
		case SipConstants.CONTENT_ENCODING_SHORT_CAP:
			return new ContentEncodingHeaderImpl(true);
		case SipConstants.CONTENT_TYPE_SHORT:
		case SipConstants.CONTENT_TYPE_SHORT_CAP:
			return new ContentTypeHeaderImpl(true);
		case SipConstants.SUPPORTED_SHORT:
		case SipConstants.SUPPORTED_SHORT_CAP:
			return new SupportedHeaderImpl(true);
		case SipConstants.SUBJECT_SHORT:
		case SipConstants.SUBJECT_SHORT_CAP:
			return new SubjectHeaderImpl(true);
		case SipConstants.EVENT_SHORT:
		case SipConstants.EVENT_SHORT_CAP:
			return new EventHeaderImpl(true);
		case SipConstants.ALLOW_EVENTS_SHORT:
		case SipConstants.ALLOW_EVENTS_SHORT_CAP:
			return new AllowEventsHeaderImpl(true);
		case SipConstants.REFER_TO_SHORT:
		case SipConstants.REFER_TO_SHORT_CAP:
			return new ReferToHeaderImpl(true);
		}

		// an extension (with one letter? that's sick)
		return new ExtendedHeader(String.valueOf(name, 0, length));
	}
}
