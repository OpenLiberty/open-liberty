/*******************************************************************************
 * Copyright (c) 2003,2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jain.protocol.ip.sip.message;

import jain.protocol.ip.sip.SipException;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.AcceptEncodingHeader;
import jain.protocol.ip.sip.header.AcceptHeader;
import jain.protocol.ip.sip.header.AcceptLanguageHeader;
import jain.protocol.ip.sip.header.CSeqHeader;
import jain.protocol.ip.sip.header.CallIdHeader;
import jain.protocol.ip.sip.header.ContactHeader;
import jain.protocol.ip.sip.header.ContentEncodingHeader;
import jain.protocol.ip.sip.header.ContentLengthHeader;
import jain.protocol.ip.sip.header.ContentTypeHeader;
import jain.protocol.ip.sip.header.DateHeader;
import jain.protocol.ip.sip.header.EncryptionHeader;
import jain.protocol.ip.sip.header.ExpiresHeader;
import jain.protocol.ip.sip.header.FromHeader;
import jain.protocol.ip.sip.header.Header;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.OrganizationHeader;
import jain.protocol.ip.sip.header.RecordRouteHeader;
import jain.protocol.ip.sip.header.RetryAfterHeader;
import jain.protocol.ip.sip.header.TimeStampHeader;
import jain.protocol.ip.sip.header.ToHeader;
import jain.protocol.ip.sip.header.UserAgentHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.header.ContentLengthHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.HeaderFactoryImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.HeaderSeparator;
import com.ibm.ws.jain.protocol.ip.sip.message.HeaderIteratorImpl.HeaderIteratorListener;
import com.ibm.ws.sip.parser.DatagramMessageParser;
import com.ibm.ws.sip.parser.MessageParser;
import com.ibm.ws.sip.parser.Separators;
import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;

/**
 * The Jain sip message object.
 * 
 * @see RequestImpl
 * @see ResponseImpl
 * @author Assaf Azaria, April 2003.
 */
public abstract class MessageImpl implements Message, Externalizable, HeaderIteratorListener {
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 2332293185980703896L;

	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(MessageImpl.class);
    
	//
	// constants.
	//
	static final String CHARSET = "charset";

	/** true if hiding message content in logs */
	private static final boolean s_hideBody = SIPTransactionStack.instance().getConfiguration().hideMessageContent();

	/** true if  should remove empty headers in a comma-separated header value */
	private static final boolean s_removeEmptyCommaSeparatedHeaders = SIPTransactionStack.instance().getConfiguration().removeEmptyCommaSeparatedHeaders();

	//
	// Members.
	//

	/**
	 * a node in a chain of headers.
	 */
	static class HeaderEntry implements Serializable {
		
		/**
	     * Class Logger. 
	     */
	    private static final LogMgr c_logger = Log.get(HeaderEntry.class);
		
		/** the header this is currently pointing at */
		private HeaderImpl m_header;

		/** next header in the chain. null if this is the bottom header */
		private HeaderEntry m_next;

		/** previous header in the chain. null if this is the top header */
		private HeaderEntry m_prev;

		/** constructor */
		private HeaderEntry(HeaderEntry prev, HeaderImpl header, HeaderEntry next) {
			m_prev = prev;
			m_header = header;
			m_next = next;
			
        	if (c_logger.isTraceDebugEnabled()) {		
            	c_logger.traceDebug(this, "HeaderEntry", "m_prev is " + m_prev + " m_header is " + m_header + " m_next is " + m_next); 
            }
		}

		HeaderImpl getHeader() {
			return m_header;
		}

		HeaderEntry getNext() {
			return m_next;
		}

		HeaderEntry getPrev() {
			return m_prev;
		}
	}

	/**
	 * the first header in this message.
	 * null if no headers in message.
	 */
	private HeaderEntry m_top;

	/**
	 * the last header in this message.
	 * null if no headers in message.
	 */
	private HeaderEntry m_bottom;

	// direct access fields are declared public so that reflection can see them

	/** direct pointer to the top via header */
	public HeaderEntry m_topVia;

	/** direct pointer to the call-id header */
	public HeaderEntry m_callId;

	/** direct pointer to the cseq header */
	public HeaderEntry m_cseq;

	/** direct pointer to the To header */
	public HeaderEntry m_to;

	/** direct pointer to the From header */
	public HeaderEntry m_from;

	/**
	 * The message body.
	 */
	private byte[] m_body;

	/**
	 * The sip version.
	 */
	private SipVersion m_version = SipVersionFactory.createVersion();

	/**
	 * true if this is a loopback message,
	 * false if a network message
	 */
	private boolean m_loopback = false;

	/** direct accessor for top via */
	private static final Field TOP_VIA;
	
	/** direct accessor for call-id */
	private static final Field CALL_ID;
	
	/** direct accessor for cseq */
	private static final Field CSEQ;
	
	/** direct accessor for to header */
	private static final Field TO;
	
	/** direct accessor for from header */
	private static final Field FROM;
	
	/**
	 * used only for object serialization.
	 * if this is non-null, all other members are null, and vise versa
	 */
	private Message m_deserializedMessage;

	/**
	 * header form enumeration. this is equivalent to {@link javax.servlet.sip.SipServletMessage#setHeaderForm(javax.servlet.sip.SipServletMessage.HeaderForm)}
	 * but re-defined here, because the stack does not depend on JSR 289.
	 * @see javax.servlet.sip.SipServletMessage#setHeaderForm(javax.servlet.sip.SipServletMessage.HeaderForm
	 */
	public static enum HeaderForm {
		COMPACT, // compact form
		DEFAULT, // header-specific
		LONG     // full form
	};
	private HeaderForm m_headerForm;

	// initialize direct header accesors
	static {
    	try {
            TOP_VIA = MessageImpl.class.getField("m_topVia");
            CALL_ID = MessageImpl.class.getField("m_callId");
            CSEQ = MessageImpl.class.getField("m_cseq");
            TO = MessageImpl.class.getField("m_to");
            FROM = MessageImpl.class.getField("m_from");
        }
    	catch (SecurityException e) {
    	    throw e;
        }
    	catch (NoSuchFieldException e) {
			// todo in 1.4 throw new RuntimeException(e);
			throw new RuntimeException(e.getMessage());
		}
	}

	//
	// Constructors.
	//

	/**
	 * Construct a new message object.
	 */
	MessageImpl() {
		m_top = null;
		m_bottom = null;
		m_topVia = null;
		m_callId = null;
		m_cseq = null;
		m_to = null;
		m_from = null;
		m_deserializedMessage = null;
		m_headerForm = null;
	}

	//
	// Operations.
	//

	/**
	 * finds a frequently-used header, and returns the class member
	 * for accessing that header.
	 * 
	 * @param name name of header to find
	 * @return the class member for the desired header. null if desired header
	 * is not exclusivly referenced.
	 */
	private static Field getDirectAccessMember(String name) {
		if (name.length() == 1) {
			switch (name.charAt(0)) {
	        case SipConstants.VIA_SHORT:
	        case SipConstants.VIA_SHORT_CAP:
	        	return TOP_VIA;
	        case SipConstants.TO_SHORT:
	        case SipConstants.TO_SHORT_CAP:
				return TO;
	        case SipConstants.FROM_SHORT:
	        case SipConstants.FROM_SHORT_CAP:
				return FROM;
	        case SipConstants.CALL_ID_SHORT:
	        case SipConstants.CALL_ID_SHORT_CAP:
				return CALL_ID;
			}
        	return null;
		}
		
		switch (name.charAt(0)) {
		case 'V': case 'v': // Via?
			if (name.length() == 3 &&
				(name.charAt(1) == 'i' || name.charAt(1) == 'I') &&
				(name.charAt(2) == 'a' || name.charAt(2) == 'A'))
			{
				return TOP_VIA;
			}
			break;
		case 'T': case 't': // To?
			if (name.length() == 2 &&
				(name.charAt(1) == 'o' || name.charAt(1) == 'O'))
			{
				return TO;
			}
			break;
		case 'F': case 'f': // From?
			if (name.length() == 4 &&
				(name.charAt(1) == 'r' || name.charAt(1) == 'R') &&
				(name.charAt(2) == 'o' || name.charAt(2) == 'O') &&
				(name.charAt(3) == 'm' || name.charAt(3) == 'M'))
			{
				return FROM;
			}
			break;
		case 'C': case 'c': // Call-ID? CSeq?
			if (name.length() == 7 &&
				(name.charAt(1) == 'a' || name.charAt(1) == 'A') &&
				(name.charAt(2) == 'l' || name.charAt(2) == 'L') &&
				(name.charAt(3) == 'l' || name.charAt(3) == 'L') &&
				(name.charAt(4) == '-') &&
				(name.charAt(5) == 'i' || name.charAt(5) == 'I') &&
				(name.charAt(6) == 'd' || name.charAt(6) == 'D'))
			{
				return CALL_ID;
			}
			else if (name.length() == 4 &&
					(name.charAt(1) == 's' || name.charAt(1) == 'S') &&
					(name.charAt(2) == 'e' || name.charAt(2) == 'E') &&
					(name.charAt(3) == 'q' || name.charAt(3) == 'Q'))
			{
				return CSEQ;
			}
			break;
		}

		return null;
	}
	
	/**
	 * @param member reflected class member
	 * @return entry to the desired header. null if does not exist,
	 * or if not exclusively referenced.
	 */
	private HeaderEntry getDirectHeaderEntry(Field member) {
	    try {
            return (HeaderEntry)member.get(this);
        }
	    catch (IllegalAccessException e) {
	        // todo log
	        return null;
        }
	}
	
	/**
	 * @param member reflected class member
	 * @param entry the entry to assign
	 * @return true on success
	 */
	private boolean setDirectHeaderEntry(Field member, HeaderEntry entry) {
	    try {
            member.set(this, entry);
	        return true;
        }
	    catch (IllegalAccessException e) {
	        // todo log
	        return false;
        }
	}
    
	/**
	 * helper method to find the first header with specific header name
	 * @param headerName name of header to look for
	 * @param direct direct iterator to requested header. may be null.
	 * @return entry pointing at requested header, or null if not found
	 */
	private HeaderEntry topHeader(String headerName, HeaderEntry direct) {
		if (direct != null) {
			return direct;
		}
		HeaderEntry e;
		for (e = m_top; e != null; e = e.m_next) {
			Header h = e.m_header;
			if (headerNamesEqual(h.getName(), headerName))
				break;
		}
		return e;
	}

	/**
	 * helper method to find the last header with specific header name
	 * @param headerName name of header to look for
	 * @param direct direct iterator to requested header. may be null.
	 * @return entry pointing at requested header, or null if not found
	 */
	private HeaderEntry bottomHeader(String headerName, HeaderEntry direct) {
		HeaderEntry e = topHeader(headerName, direct);
		if (e == null) {
			return null;
		}
		
		for (HeaderEntry lookAhead = e.m_next;
			lookAhead != null;
			lookAhead = lookAhead.m_next)
		{
			Header h = lookAhead.m_header;
			if (!headerNamesEqual(h.getName(), headerName))
				break;
			e = lookAhead;
		}
		return e;
	}
	
	/**
	 * Adds list of Headers to top/bottom of Message's header list.
	 * Note that the Headers are added in same order as in List.
	 * @param <var>headers</var> List of Headers to be added
	 * @param <var>top</var> indicates if Headers are to be added at top/bottom
	 * of Message's header list
	 * @throws IllegalArgumentException if headers is null, empty, contains any
	 * null objects, or contains any objects that are not Header objects from
	 * the same JAIN SIP implementation as this Message
	 */
	public void addHeaders(String headerName, List headers, boolean top) throws IllegalArgumentException{
		if (headerName == null) {
			throw new IllegalArgumentException("Message: Null headerName");
		}
		if (headers == null) {
			throw new IllegalArgumentException("Message: Null headers");
		}
		if (headers.isEmpty()) {
			throw new IllegalArgumentException("Message: empty header list");
		}

		HeaderEntry head = null; // first header in new list
		HeaderEntry tail = null; // last header in new list
		Iterator i = headers.iterator();

		while (i.hasNext()) {
			// validate input header
			Object o = i.next();
			if (!(o instanceof HeaderImpl)) {
				throw new IllegalArgumentException("Message: All headers must"
					+ " be from IBM jain sip implementation."
					+ " received " + o.getClass().toString());
			}
			HeaderImpl h = (HeaderImpl)o;
			if (!headerNamesEqual(h.getName(), headerName)) {
				throw new IllegalArgumentException("Message: expected header name ["
					+ headerName
					+ "] received [" + h.getName() + ']');
			}
			
			// chain input headers to each other
			HeaderEntry n = new HeaderEntry(tail, h, null);
        	if (c_logger.isTraceDebugEnabled()) {			
        		c_logger.traceDebug(this, "addHeaders", "m_prev(tail) is " + tail + " m_header(h) is " + h + " m_next(null) is " + null); 
            }
			if (head == null) {
				head = n;
			}
			if (tail != null) {
				tail.m_next = n;
			}
			tail = n;
		}
		
		// insert new chain in existing chain
		HeaderEntry l; // header to be on the left of new chain
		HeaderEntry r; // header to be on the right of new chain
		
		Field member = getDirectAccessMember(headerName);
		HeaderEntry direct = member == null
			? null
			: getDirectHeaderEntry(member);

		if (top) {
			// insert new chain before existing headers
			r = topHeader(headerName, direct);
			l = r == null ? m_bottom : r.m_prev;

			// update direct pointer if there is one
			if (member != null) {
				setDirectHeaderEntry(member, head);
			}
		}
		else {
			// insert new chain after existing headers
			l = bottomHeader(headerName, direct);
			r = l == null ? m_top : l.m_next;

			// update direct pointer if there is one and it's not set
			if (member != null && direct == null) {
				setDirectHeaderEntry(member, head);
			}
		}
		head.m_prev = l;
		if (l == null) {
			m_top = head;
		}
		else {
			l.m_next = head;
		}
		tail.m_next = r;
		if (r == null) {
			m_bottom = tail;
		}
		else {
			r.m_prev = tail;
		}
	}

	/**
	 * Adds Header to top/bottom of Message's header list
	 * @param <var>header</var> Header to be added
	 * @param <var>top</var> indicates if Header is to be added at top/bottom
	 * @throws IllegalArgumentException if header is null or is not from
	 * the same JAIN SIP implementation as this Message
	 */
	public void addHeader(Header header, boolean top)
		throws IllegalArgumentException
	{
		if (header == null) {
			throw new IllegalArgumentException("Message: Null Header");
		}
		if (!(header instanceof HeaderImpl)) {
			throw new IllegalArgumentException("Message: header must"
				+ " be from IBM jain sip implementation."
				+ " received " + header.getClass().toString());
		}
		HeaderImpl h = (HeaderImpl) header;

		// insert new header in existing chain
		HeaderEntry n; // the new header
		HeaderEntry l; // header to be on the left of new header
		HeaderEntry r; // header to be on the right of new header

		String headerName = header.getName();
		Field member = getDirectAccessMember(headerName);
		HeaderEntry direct = member == null
			? null
			: getDirectHeaderEntry(member);

		if (top) {
			// insert new header before existing headers
			r = topHeader(headerName, direct);
			l = r == null ? m_bottom : r.m_prev;
			n = new HeaderEntry(l, h, r);
        	if (c_logger.isTraceDebugEnabled()) {			
        		c_logger.traceDebug(this, "addHeader", "m_prev(l) is " + l + " m_header(h) is " + h + " m_next(r) is " + r); 
            }

			// update direct pointer if there is one
			if (member != null) {
				setDirectHeaderEntry(member, n);
			}
		}
		else {
			// insert new header after existing headers
			l = bottomHeader(headerName, direct);
			r = l == null ? m_top : l.m_next;
			n = new HeaderEntry(l, h, r);

			// update direct pointer if there is one and it's not set
			if (member != null && direct == null) {
				setDirectHeaderEntry(member, n);
			}
		}

		if (l == null) {
			m_top = n;
		}
		else {
			l.m_next = n;
		}
		if (r == null) {
			m_bottom = n;
		}
		else {
			r.m_prev = n;
		}
	}

	/**
	 * Sets all Headers of specified name in header list.
	 * Note that this method is equivalent to invoking .
	 * removeHeaders(headerName) followed by addHeaders(headers, top)
	 * @param <var>headerName</var> name of Headers to set
	 * @param <var>headers</var> List of Headers to be set
	 * @throws IllegalArgumentException if 
	 * headerName or headers is null, if headers is empty,
	 * contains any null elements, or contains any objects that are not 
	 * Header objects from the same JAIN SIP implementation as this 
	 * Message, or contains any Headers that
	 * don't match the specified header name
	 */
	public void setHeaders(String headerName, List headers)
		throws IllegalArgumentException
	{
		if (headerName == null) {
			throw new IllegalArgumentException("Message: Null headerName");
		}
		if (headers == null) {
			throw new IllegalArgumentException("Message: Null headers");
		}
		if (headers.isEmpty()) {
			throw new IllegalArgumentException("Message: empty header list");
		}

		Field member = getDirectAccessMember(headerName);
		HeaderEntry direct = member == null
			? null
			: getDirectHeaderEntry(member);
		HeaderEntry iOld = topHeader(headerName, direct); // iterator on existing headers
		Iterator iNew = headers.iterator(); // iterator on new (input) headers
		HeaderEntry i = null; // last header that was set
		HeaderEntry l = // header to the left of new chain
		iOld == null ? m_bottom : iOld.m_prev;
		HeaderEntry r = null; // header to the right of new chain

		// iterate input headers
		do {
			// validate input header
			Object o = iNew.next();
			if (!(o instanceof HeaderImpl)) {
				throw new IllegalArgumentException("Message: All headers must"
					+ " be from IBM jain sip implementation."
					+ " received " + o.getClass().toString());
			}
			HeaderImpl hNew = (HeaderImpl) o;
			if (!headerNamesEqual(hNew.getName(), headerName)) {
				throw new IllegalArgumentException("Message: expected header name ["
					+ headerName
					+ "] received [" + hNew.getName() + ']');
			}

			if (iOld == null) {
				// allocate a new placeholder
				boolean first = i == null;
				i = new HeaderEntry(i, hNew, r);
	        	if (c_logger.isTraceDebugEnabled()) {			
	        		c_logger.traceDebug(this, "setHeaders", "m_prev(i) is " + i + " m_header(hNew) is " + hNew + " m_next(r) is " + r); 
	            }

				if (first) {
					// this newly added header is the first one for this header name
					if (m_top == null) {
						// this newly added header is the first one in the message
						m_top = i;
					}
					if (member != null && direct == null) {
						// this newly added header is available for direct access
						direct = i;
						setDirectHeaderEntry(member, direct);
					}
					if (l == null) {
						m_top = i;
					}
					else {
						l.m_next = i;
					}
					i.m_prev = l;
				}
				else {
					i.m_prev.m_next = i;
				}
				if (r == null) {
					// this newly added header is the last one in the message
					m_bottom = i;
				}
				else {
					r.m_prev = i;
				}
			}
			else {
				
	        	if (c_logger.isTraceDebugEnabled()) {			
	        		c_logger.traceDebug(this, "setHeaders", "iOld.m_header is " + iOld.m_header + " hNew is " + hNew); 
	            }
				// replace old with new
				iOld.m_header = hNew;

				i = iOld;
				iOld = iOld.m_next;
				r = iOld;
				if (iOld != null) {
					Header hOld = iOld.m_header;
					if (!headerNamesEqual(hOld.getName(), headerName)) {
						iOld = null;
					}
				}
			}
		} while (iNew.hasNext());

		// remove exessive old headers (there were more old ones than new)
		if (iOld != null) {
			HeaderEntry last = iOld.m_prev; // last new one
			r = iOld.m_next;

			while (r != null) {
				Header hOld = r.m_header;
				if (!headerNamesEqual(hOld.getName(), headerName)) {
					r.m_prev = last;
					break;
				}
				r = r.m_next;
			}
			last.m_next = r; // bridge over exessive old headers
			if (r == null) {
				m_bottom = last;
			}
		}
	}

	/**
	 * Sets the first/last Header of header's name in Message's Header list.
	 * Note that this method is equivalent to invoking 
	 *   removeHeader(headerName, first)
	 *   followed by addHeader(header, first)
	 * @param <var>header</var> Header to set
	 * @param <var>first</var> indicates if first/last Header is to be set
	 * @throws IllegalArgumentException if header is null or is not from
	 * the same JAIN SIP implementation as this Message
	 */
	public void setHeader(Header header, boolean first)
		throws IllegalArgumentException
	{
		if (header == null) {    
			throw new IllegalArgumentException("Message: Null header"); 
		}
		if (!(header instanceof HeaderImpl)) {
			throw new IllegalArgumentException("Message: header must"
					+ " be from IBM jain sip implementation."
					+ " received " + header.getClass().toString());
		}
		HeaderImpl h = (HeaderImpl)header;
		String headerName = h.getName();
		Field member = getDirectAccessMember(headerName);
		HeaderEntry direct = member == null
			? null
			: getDirectHeaderEntry(member);
		
		HeaderEntry e = first
			? topHeader(headerName, direct)
			: bottomHeader(headerName, direct);

		if (e == null) {
			// no such header. append.
			e = new HeaderEntry(m_bottom, h, null);
			
        	if (c_logger.isTraceDebugEnabled()) {			
            	c_logger.traceDebug(this, "setHeader", "m_prev(m_bottom) is " + m_bottom + " m_header(h) is " + h + " m_next(null) is " + null); 
            }
			if (m_bottom != null) {
				m_bottom.m_next = e;
			}
			m_bottom = e;
			if (m_top == null) {
				m_top = e;
			}
			if (member != null && direct == null) {
				setDirectHeaderEntry(member, e);
			}
		}
		else {
			e.m_header = h;
		}
	}

	/**
	 * Removes first (or last) Header of specified name from Message's 
	 * Header list.
	 * Note if no Headers of specified name exist the method has no effect
	 * @param <var>headerName</var> name of Header to be removed
	 * @param <var>first</var> indicates whether first or last Header of 
	 *  specified name is to be removed
	 * @throws IllegalArgumentException if headerName is null
	 */
	public void removeHeader(String headerName, boolean first) throws IllegalArgumentException {
		removeHeader(headerName, null, first);
	}

	/**
	 * Removes specific Header of specified name from Message's Header
	 * list. Note if no Headers of specified name exist the method has no effect
	 * 
	 * @param headerName
	 * @param givenEntry
	 * @throws IllegalArgumentException
	 */
	protected void removeHeader(HeaderEntry headerEntry) throws IllegalArgumentException {
		if (headerEntry == null  || headerEntry.m_header == null){
			return;
		}
		String headerName = headerEntry.m_header.getName(); 
		removeHeader(headerName, headerEntry, true);
	}

	private void removeHeader(String headerName, HeaderEntry givenEntry, boolean first) throws IllegalArgumentException {
		if (headerName == null) {
			throw new IllegalArgumentException("Message: null headerName");
		}

		Field member = getDirectAccessMember(headerName);
		HeaderEntry direct = member == null ? null : getDirectHeaderEntry(member);

		HeaderEntry entryToRemove = givenEntry;
		if (givenEntry == null){
			entryToRemove = first ? topHeader(headerName, direct) : bottomHeader(headerName, direct);
			if (entryToRemove == null) {
				return;
			}
		}

		// bridge over removed header
		HeaderEntry lbridge = entryToRemove.m_prev;
		HeaderEntry rbridge = entryToRemove.m_next;
		if (lbridge == null) {
			if (m_top == entryToRemove) {
				m_top = rbridge;
			}
		} else {
			lbridge.m_next = rbridge;
		}
		if (rbridge == null) {
			if (m_bottom == entryToRemove) {
				m_bottom = lbridge;
			}
		} else {
			rbridge.m_prev = lbridge;
		}
		if (direct == entryToRemove) {
			// set new direct header now that the old one is gone
			direct = first ? entryToRemove.m_next : entryToRemove.m_prev;
			if (direct != null && !headerNamesEqual(direct.m_header.getName(), headerName)) {
				direct = null;
			}
			setDirectHeaderEntry(member, direct);
		}
	}

	/**
	 * Removes all Headers of specified name from Message's Header list.
	 * Note if no Headers of specified name exist the method has no effect
	 * @param <var>headername</var> name of Headers to be removed
	 * @throws IllegalArgumentException if headerName is null
	 */
	public void removeHeaders(String headerName)
		throws IllegalArgumentException
	{
		if (headerName == null) {
			throw new IllegalArgumentException("Message: null headerName");
		}
		
		Field member = getDirectAccessMember(headerName);
		HeaderEntry direct = member == null
			? null
			: getDirectHeaderEntry(member);
		
		HeaderEntry first = topHeader(headerName, direct);
		if (first == null) {
			return;
		}
		HeaderEntry last = first;
		for (HeaderEntry lookAhead = last.m_next;
			lookAhead != null;
			lookAhead = lookAhead.m_next)
		{
			Header h = lookAhead.m_header;
			if (!headerNamesEqual(h.getName(), headerName))
				break;
			last = lookAhead;
		}

		// bridge over removed headers
		HeaderEntry lbridge = first.m_prev;
		HeaderEntry rbridge = last.m_next;
		if (lbridge == null) {
			if (m_top == first) {
				m_top = rbridge;
			}
		}
		else {
			lbridge.m_next = rbridge;
		}
		if (rbridge == null) {
			if (m_bottom == last) {
				m_bottom = lbridge;
			}
		}
		else {
			rbridge.m_prev = lbridge;
		}
		if (direct == first) {
			setDirectHeaderEntry(member, null);
		}
	}

	/**
	 * Gets HeaderIterator of all Headers in Message.
	 * Note that order of Headers in HeaderIterator is same as order in
	 * Message (Returns null if no Headers exist).
	 * @return HeaderIterator of all Headers in Message
	 */
	public HeaderIterator getHeaders() {
		if (m_top == null) {
			return null;
		}
		return new HeaderIteratorImpl.General(m_top, this);
	}

	/**
	 * Gets HeaderIterator of all Headers in Message.
	 * No attempt is made to parse the headers
	 * 
	 * @return HeaderIterator of all Headers in Message
	 */
	public HeaderIterator getHeadersUnparsed() {
		return new HeaderIteratorImpl.Unparsed(m_top, this);
	}

	/**
	 * Gets HeaderIterator of all Headers of specified name in Message.
	 * Note that order of Headers in HeaderIterator is same as order 
	 * they appear in Message
	 * (Returns null if no Headers of specified name exist)
	 * @param <var>headerName</var> name of Headers to return
	 * @return HeaderIterator of all Headers of specified name in Message
	 * @throws IllegalArgumentException if headerName is null
	 */
	public HeaderIterator getHeaders(String headerName)
		throws IllegalArgumentException
	{
		return getHeaders(headerName, true);
	}

	/**
	 * Gets HeaderIterator of all Headers of specified name in Message.
	 * Note that order of Headers in HeaderIterator is same as order 
	 * they appear in Message
	 * (Returns null if no Headers of specified name exist)
	 * @param <var>headerName</var> name of Headers to return
	 * @return HeaderIterator of all Headers of specified name in Message
	 * @throws IllegalArgumentException if headerName is null
	 */
	public HeaderIterator getHeadersUnparsed(String headerName)
		throws IllegalArgumentException
	{
		return getHeaders(headerName, false);
	}

	/**
	 * Gets HeaderIterator of all Headers of specified name in Message.
	 * Note that order of Headers in HeaderIterator is same as order 
	 * they appear in Message
	 * (Returns null if no Headers of specified name exist)
	 * @param <var>headerName</var> name of Headers to return
	 * @param <var>isParsed</var> should stack parse the headers or not
	 * @return HeaderIterator of all Headers of specified name in Message
	 * @throws IllegalArgumentException if headerName is null
	 */
	private HeaderIterator getHeaders(String headerName, boolean isParsed) {
		if (headerName == null) {
			throw new IllegalArgumentException("Message: headerName cannot be null");
		}

		Field member = getDirectAccessMember(headerName);
		HeaderEntry direct = member == null
			? null
			: getDirectHeaderEntry(member);
		
		HeaderEntry e = topHeader(headerName, direct);
		if (e == null) {
			return null;
		}
		if (isParsed) {
			return new HeaderIteratorImpl.Specific(e, this);
		} else {
			return new HeaderIteratorImpl.UnparsedSpecific(e, this);
		}
	}

	/**
	 * Gets first (or last) Header of specified name in Message
	 * (Returns null if no Headers of specified name exist)
	 * @param <var>headerName</var> name of Header to return
	 * @param <var>first</var> indicates whether the first or 
	 *  last Header of specified name is required
	 * @return first (or last) Header of specified name in Message's Header list
	 * @throws IllegalArgumentException if headername is null
	 */
	public Header getHeader(String headerName, boolean first)
		throws IllegalArgumentException, HeaderParseException
	{
		if (headerName == null) {
			throw new IllegalArgumentException("Message: null headerName");
		}

		Field member = getDirectAccessMember(headerName);
		HeaderEntry direct = member == null
			? null
			: getDirectHeaderEntry(member);
		
		HeaderEntry e = first
			? topHeader(headerName, direct)
			: bottomHeader(headerName, direct);
		
		if (e == null) {
			return null;
		}

		HeaderImpl h = e.m_header;
		try {
			h.parse();
		}
		catch (SipParseException x) {
			throw new HeaderParseException(h);
		}
		return h;
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has any headers
	 * @return boolean value to indicate if Message
	 * has any headers
	 */
	public boolean hasHeaders() {
		return m_top != null;
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has any headers of specified name
	 * @return boolean value to indicate if Message
	 * has any headers of specified name
	 * @param <var>headerName</var> header name
	 * @throws IllegalArgumentException if headerName is null
	 */
	public boolean hasHeaders(String headerName)
		throws IllegalArgumentException
	{
		if (headerName == null) {
			throw new IllegalArgumentException("Message: null headerName");
		}
		
		Field member = getDirectAccessMember(headerName);
		HeaderEntry direct = member == null
			? null
			: getDirectHeaderEntry(member);

		HeaderEntry e = topHeader(headerName, direct);
		return e != null;
	}

	/**
	 * Gets CallIdHeader of Message.
	 * (Returns null if no CallIdHeader exists)
	 * @return CallIdHeader of Message
	 */
	public CallIdHeader getCallIdHeader()
	{
		if (m_callId == null) {
			return null;
		}
		HeaderImpl h = m_callId.m_header;
		
		try {
			h.parse();
		}
		catch (SipParseException e) {
			// This header doesn't need parsing, so this 'can't' happen.
			return null;
		}  
		return (CallIdHeader)h;
	}

	/**
	 * Sets CallIdHeader of Message.
	 * @param <var>callIdHeader</var> CallIdHeader to set
	 * @throws IllegalArgumentException if callIdHeader is null
	 * or not from same JAIN SIP implementation
	 */
	public void setCallIdHeader(CallIdHeader callIdHeader)
		throws IllegalArgumentException
	{
		setHeader(callIdHeader, true);
	}

	/**
	 * Gets CSeqHeader of Message.
	 * (Returns null if no CSeqHeader exists)
	 * @return CSeqHeader of Message
	 */
	public CSeqHeader getCSeqHeader()
	{
		if (m_cseq == null) {
			return null;
		}
		HeaderImpl h = m_cseq.m_header;
		
		try {
			h.parse();
		}
		catch (SipParseException e) {
			// This header doesn't need parsing, so this 'can't' happen.
			return null;
		}  
		return (CSeqHeader)h;
	}

	/**
	 * Sets CSeqHeader of Message.
	 * @param <var>cSeqHeader</var> CSeqHeader to set
	 * @throws IllegalArgumentException if cSeqHeader is null
	 * or not from same JAIN SIP implementation
	 */
	public void setCSeqHeader(CSeqHeader cSeqHeader)
		throws IllegalArgumentException
	{
		setHeader(cSeqHeader, true);
	}

	/**
	 * Gets ToHeader of Message.
	 * (Returns null if no ToHeader exists)
	 * @return ToHeader of Message
	 * @throws HeaderNotSetException if no ToHeader exists
	 */
	public ToHeader getToHeader()
	{
		if (m_to == null) {
			return null;
		}
		HeaderImpl h = m_to.m_header;
		
		try {
			h.parse();
		}
		catch (SipParseException e) {
			return null;
		}  
		return (ToHeader)h;
	}

	/**
	 * Sets ToHeader of Message.
	 * @param <var>toHeader</var> ToHeader to set
	 * @throws IllegalArgumentException if toHeader is null
	 * or not from same JAIN SIP implementation
	 */
	public void setToHeader(ToHeader toHeader) throws IllegalArgumentException
	{
		setHeader(toHeader, true);    
	}

	/**
	 * Gets FromHeader of Message.
	 * (Returns null if no FromHeader exists)
	 * @return FromHeader of Message
	 * @throws HeaderNotSetException if no FromHeader exists
	 */
	public FromHeader getFromHeader()
	{
		if (m_from == null) {
			return null;
		}
		HeaderImpl h = m_from.m_header;
		
		try {
			h.parse();
		}
		catch (SipParseException e) {
			return null;
		}  
		return (FromHeader)h;
	}

	/**
	 * Sets FromHeader of Message.
	 * @param <var>fromHeader</var> FromHeader to set
	 * @throws IllegalArgumentException if fromHeader is null
	 * or not from same JAIN SIP implementation
	 */
	public void setFromHeader(FromHeader fromHeader)
		throws IllegalArgumentException
	{
		setHeader(fromHeader, true);
	}

	/**
	 * Gets HeaderIterator of ViaHeaders of Message.
	 * (Returns null if no ViaHeaders exist)
	 * @return HeaderIterator of ViaHeaders of Message
	 */
	public HeaderIterator getViaHeaders()
	{
		if (m_topVia == null) {
			return null;
		}
		return new HeaderIteratorImpl.Specific(m_topVia, this);
	}

	/**
	 * Sets ViaHeaders of Message.
	 * @param <var>viaHeaders</var> List of ViaHeaders to set
	 * @throws IllegalArgumentException if viaHeaders is null, empty, contains
	 * any elements that are null or not ViaHeaders from the same
	 * JAIN SIP implementation
	 */
	public void setViaHeaders(List viaHeaders) throws IllegalArgumentException
	{
		setHeaders(ViaHeader.name, viaHeaders);
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has ViaHeaders
	 * @return boolean value to indicate if Message
	 * has ViaHeaders
	 */
	public boolean hasViaHeaders()
	{
		return m_topVia != null;
	}

	/**
	 * Removes ViaHeaders from Message (if any exist).
	 */
	public void removeViaHeaders()
	{
		removeHeaders(ViaHeader.name);
	}

	/**
	 * Gets ContentTypeHeader of Message.
	 * (Returns null if no ContentTypeHeader exists)
	 * @return ContentTypeHeader of Message
	 * @throws HeaderParseException if implementation could not parse 
	 * header value
	 */
	public ContentTypeHeader getContentTypeHeader() throws HeaderParseException
	{
		return (ContentTypeHeader) getHeader(ContentTypeHeader.name, true);
	}

	/**
	 * Sets ContentTypeHeader of Message.
	 * @param <var>contentTypeHeader</var> ContentTypeHeader to set
	 * @throws IllegalArgumentException if contentTypeHeader is null
	 * or not from same JAIN SIP implementation
	 * @throws SipException if Message does not contain body
	 */
	public void setContentTypeHeader(ContentTypeHeader contentTypeHeader)
		throws IllegalArgumentException
	{
		setHeader(contentTypeHeader, true);
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has ContentTypeHeader
	 * @return boolean value to indicate if Message
	 * has ContentTypeHeader
	 */
	public boolean hasContentTypeHeader()
	{
		return hasHeaders(ContentTypeHeader.name);
	}

	/**
	 * Removes ContentTypeHeader from Message (if it exists)
	 */
	public void removeContentTypeHeader()
	{
		removeHeaders(ContentTypeHeader.name);
	}

	/**
	 * Gets DateHeader of Message.
	 * (Returns null if no DateHeader exists)
	 * @return DateHeader of Message
	 * @throws HeaderParseException if implementation could not parse 
	 *   header value
	 */
	public DateHeader getDateHeader() throws HeaderParseException
	{
		return (DateHeader) getHeader(DateHeader.name, true);
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has DateHeader
	 * @return boolean value to indicate if Message
	 * has DateHeader
	 */
	public boolean hasDateHeader()
	{
		return hasHeaders(DateHeader.name);
	}

	/**
	 * Sets DateHeader of Message.
	 * @param <var>dateHeader</var> DateHeader to set
	 * @throws IllegalArgumentException if dateHeader is null
	 * or not from same JAIN SIP implementation
	 */
	public void setDateHeader(DateHeader dateHeader)
		throws IllegalArgumentException
	{
		setHeader(dateHeader, true);
	}

	/**
	 * Removes DateHeader from Message (if it exists)
	 */
	public void removeDateHeader()
	{
		removeHeaders(DateHeader.name);  
	}

	/**
	 * Gets EncryptionHeader of Message.
	 * (Returns null if no EncryptionHeader exists)
	 * @return EncryptionHeader of Message
	 * @throws HeaderParseException if implementation could not parse 
	 *  	header value
	 */
	public EncryptionHeader getEncryptionHeader() throws HeaderParseException
	{
		return (EncryptionHeader)getHeader(EncryptionHeader.name, true);
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has EncryptionHeader
	 * @return boolean value to indicate if Message
	 * has EncryptionHeader
	 */
	public boolean hasEncryptionHeader()
	{
		return hasHeaders(EncryptionHeader.name);
	}

	/**
	 * Sets EncryptionHeader of Message.
	 * @param <var>encryptionHeader</var> EncryptionHeader to set
	 * @throws IllegalArgumentException if encryptionHeader is null
	 * or not from same JAIN SIP implementation
	 */
	public void setEncryptionHeader(EncryptionHeader encryptionHeader)
		throws IllegalArgumentException
	{
		setHeader(encryptionHeader, true);
	}

	/**
	 * Removes EncryptionHeader from Message (if it exists)
	 */
	public void removeEncryptionHeader()
	{
		removeHeaders(EncryptionHeader.name);
	}

	/**
	 * Gets UserAgentHeader of Message.
	 * (Returns null if no UserAgentHeader exists)
	 * @return UserAgentHeader of Message
	 * @throws HeaderParseException if implementation could not 
	 *    parse header value
	 */
	public UserAgentHeader getUserAgentHeader() throws HeaderParseException
	{
		return (UserAgentHeader)getHeader(UserAgentHeader.name, true);
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has UserAgentHeader
	 * @return boolean value to indicate if Message
	 * has UserAgentHeader
	 */
	public boolean hasUserAgentHeader()
	{
		return hasHeaders(UserAgentHeader.name);
	}

	/**
	 * Sets UserAgentHeader of Message.
	 * @param <var>userAgentHeader</var> UserAgentHeader to set
	 * @throws IllegalArgumentException if userAgentHeader is null
	 * or not from same JAIN SIP implementation
	 */
	public void setUserAgentHeader(UserAgentHeader userAgentHeader)
		throws IllegalArgumentException
	{
		setHeader(userAgentHeader, true);
	}

	/**
	 * Removes UserAgentHeader from Message (if it exists)
	 */
	public void removeUserAgentHeader()
	{
		removeHeaders(ViaHeader.name);  
	}

	/**
	 * Gets TimeStampHeader of Message.
	 * (Returns null if no TimeStampHeader exists)
	 * @return TimeStampHeader of Message
	 * @throws HeaderParseException if implementation could not parse 
	 *  	header value
	 */
	public TimeStampHeader getTimeStampHeader() throws HeaderParseException
	{
		return (TimeStampHeader)getHeader(TimeStampHeader.name, true);
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has TimeStampHeader
	 * @return boolean value to indicate if Message
	 * has TimeStampHeader
	 */
	public boolean hasTimeStampHeader()
	{
		return hasHeaders(TimeStampHeader.name);
	}

	/**
	 * Removes TimeStampHeader from Message (if it exists)
	 */
	public void removeTimeStampHeader()
	{
		removeHeaders(TimeStampHeader.name);  
	}

	/**
	 * Sets TimeStampHeader of Message.
	 * @param <var>timeStampHeader</var> TimeStampHeader to set
	 * @throws IllegalArgumentException if timeStampHeader is null
	 * or not from same JAIN SIP implementation
	 */
	public void setTimeStampHeader(TimeStampHeader timeStampHeader)
		throws IllegalArgumentException
	{
		setHeader(timeStampHeader, true);
	}

	/**
	 * Gets HeaderIterator of ContentEncodingHeaders of Message.
	 * (Returns null if no ContentEncodingHeaders exist)
	 * @return HeaderIterator of ContentEncodingHeaders of Message
	 */
	public HeaderIterator getContentEncodingHeaders()
	{
		return getHeaders(ContentEncodingHeader.name);
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has ContentEncodingHeaders
	 * @return boolean value to indicate if Message
	 * has ContentEncodingHeaders
	 */
	public boolean hasContentEncodingHeaders()
	{
		return hasHeaders(ContentEncodingHeader.name);
	}

	/**
	 * Removes ContentEncodingHeaders from Message (if any exist)
	 */
	public void removeContentEncodingHeaders()
	{
		removeHeaders(ContentEncodingHeader.name);  
	}

	/**
	 * Sets ContentEncodingHeaders of Message.
	 * @param <var>contentEncodingHeaders</var> List of 
	 * 		ContentEncodingHeaders to set
	 * @throws IllegalArgumentException if contentEncodingHeaders is null, 
	 *  	empty, contains any elements that are null or not 
	 * ContentEncodingHeaders from the same JAIN SIP implementation
	 */
	public void setContentEncodingHeaders(List contentEncodingHeaders)
		throws IllegalArgumentException, SipException
	{
		setHeaders(ContentEncodingHeader.name, contentEncodingHeaders);
	}

	/**
	 * Gets ContentLengthHeader of Message.
	 * (Returns null if no ContentLengthHeader exists)
	 * @return ContentLengthHeader of Message
	 * @throws HeaderParseException if implementation could not parse 
	 *  	header value
	 */
	public ContentLengthHeader getContentLengthHeader()
		throws HeaderParseException
	{
		return (ContentLengthHeader)getHeader(ContentLengthHeader.name, true);
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has ContentLengthHeader
	 * @return boolean value to indicate if Message
	 * has ContentLengthHeader
	 */
	public boolean hasContentLengthHeader()
	{
		return hasHeaders(ContentLengthHeader.name);
	}

	/**
	 * Removes ContentLengthHeader from Message (if it exists)
	 */
	public void removeContentLengthHeader()
	{
		removeHeaders(ContentLengthHeader.name);  
	}

	/**
	 * Sets ContentLengthHeader of Message.
	 * @param <var>contentLengthHeader</var> ContentLengthHeader to set
	 * @throws IllegalArgumentException if contentLengthHeader is null
	 * or not from same JAIN SIP implementation
	 */
	public void setContentLengthHeader(ContentLengthHeader contentLengthHeader)
		throws IllegalArgumentException
	{
		setHeader(contentLengthHeader, true);
	}

	/**
	 * Gets HeaderIterator of AcceptHeaders of Message.
	 * (Returns null if no AcceptHeaders exist)
	 * @return HeaderIterator of AcceptHeaders of Message
	 */
	public HeaderIterator getAcceptHeaders()
	{
		return getHeaders(AcceptHeader.name);
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has AcceptHeaders
	 * @return boolean value to indicate if Message
	 * has AcceptHeaders
	 */
	public boolean hasAcceptHeaders()
	{
		return hasHeaders(AcceptHeader.name);
	}

	/**
	 * Removes AcceptHeaders from Message (if any exist)
	 */
	public void removeAcceptHeaders()
	{
		removeHeaders(AcceptHeader.name);
	}

	/**
	 * Sets AcceptHeaders of Message.
	 * @param <var>acceptHeaders</var> List of AcceptHeaders to set
	 * @throws IllegalArgumentException if acceptHeaders is null, empty, 
	 * contains any elements that are null or not AcceptHeaders from the same
	 * JAIN SIP implementation
	 */
	public void setAcceptHeaders(List acceptHeaders)
		throws IllegalArgumentException
	{
		setHeaders(AcceptHeader.name, acceptHeaders);
	}

	/**
	 * Gets HeaderIterator of AcceptEncodingHeaders of Message.
	 * (Returns null if no AcceptEncodingHeaders exist)
	 * @return HeaderIterator of AcceptEncodingHeaders of Message
	 */
	public HeaderIterator getAcceptEncodingHeaders()
	{
		return getHeaders(AcceptEncodingHeader.name);
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has AcceptEncodingHeaders
	 * @return boolean value to indicate if Message
	 * has AcceptEncodingHeaders
	 */
	public boolean hasAcceptEncodingHeaders()
	{
		return hasHeaders(AcceptEncodingHeader.name);
	}

	/**
	 * Removes AcceptEncodingHeaders from Message (if any exist)
	 */
	public void removeAcceptEncodingHeaders()
	{
		removeHeaders(AcceptEncodingHeader.name);
	}

	/**
	 * Sets AcceptEncodingHeaders of Message.
	 * @param <var>acceptEncodingHeaders</var> List of AcceptEncodingHeaders 
	 * 		to set
	 * @throws IllegalArgumentException if acceptEncodingHeaders is null, 
	 * 	empty, contains any elements that are null or not 
	 * AcceptEncodingHeaders from the same JAIN SIP implementation
	 */
	public void setAcceptEncodingHeaders(List acceptEncodingHeaders)
		throws IllegalArgumentException
	{
		setHeaders(AcceptEncodingHeader.name, acceptEncodingHeaders);
	}

	/**
	 * Gets HeaderIterator of AcceptLanguageHeaders of Message.
	 * (Returns null if no AcceptLanguageHeaders exist)
	 * @return HeaderIterator of AcceptLanguageHeaders of Message
	 */
	public HeaderIterator getAcceptLanguageHeaders()
	{
		return getHeaders(AcceptLanguageHeader.name);
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has AcceptLanguageHeaders
	 * @return boolean value to indicate if Message
	 * has AcceptLanguageHeaders
	 */
	public boolean hasAcceptLanguageHeaders()
	{
		return hasHeaders(AcceptLanguageHeader.name);
	}

	/**
	 * Removes AcceptLanguageHeaders from Message (if any exist)
	 */
	public void removeAcceptLanguageHeaders()
	{
		removeHeaders(AcceptLanguageHeader.name);
	}

	/**
	 * Sets AcceptLanguageHeaders of Message.
	 * @param <var>acceptLanguageHeaders</var> List of AcceptLanguageHeaders 
	 *   to set
	 * @throws IllegalArgumentException if acceptLanguageHeaders is null, 
	 *  empty, contains
	 * any elements that are null or not AcceptLanguageHeaders from the same
	 * JAIN SIP implementation
	 */
	public void setAcceptLanguageHeaders(List acceptLanguageHeaders)
		throws IllegalArgumentException
	{
		setHeaders(AcceptLanguageHeader.name, acceptLanguageHeaders);
	}

	/**
	 * Gets ExpiresHeader of Message.
	 * (Returns null if no ExpiresHeader exists)
	 * @return ExpiresHeader of Message
	 * @throws HeaderParseException if implementation could not parse 
	 * 		header value
	 */
	public ExpiresHeader getExpiresHeader() throws HeaderParseException
	{
		return (ExpiresHeader)getHeader(ExpiresHeader.name, true);
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has ExpiresHeader
	 * @return boolean value to indicate if Message
	 * has ExpiresHeader
	 */
	public boolean hasExpiresHeader()
	{
		return hasHeaders(ExpiresHeader.name);
	}

	/**
	 * Removes ExpiresHeader from Message (if it exists)
	 */
	public void removeExpiresHeader()
	{
		removeHeaders(ExpiresHeader.name);  
	}

	/**
	 * Sets ExpiresHeader of Message.
	 * @param <var>expiresHeader</var> ExpiresHeader to set
	 * @throws IllegalArgumentException if expiresHeader is null
	 * or not from same JAIN SIP implementation
	 */
	public void setExpiresHeader(ExpiresHeader expiresHeader)
		throws IllegalArgumentException
	{
		setHeader(expiresHeader, true);
	}

	/**
	 * Gets HeaderIterator of ContactHeaders of Message.
	 * (Returns null if no ContactHeaders exist)
	 * @return HeaderIterator of ContactHeaders of Message
	 */
	public HeaderIterator getContactHeaders()
	{
		return getHeaders(ContactHeader.name);
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has ContactHeaders
	 * @return boolean value to indicate if Message
	 * has ContactHeaders
	 */
	public boolean hasContactHeaders()
	{
		return hasHeaders(ContactHeader.name);
	}

	/**
	 * Removes ContactHeaders from Message (if any exist)
	 */
	public void removeContactHeaders()
	{
		removeHeaders(ContactHeader.name);  
	}

	/**
	 * Sets ContactHeaders of Message.
	 * @param <var>contactHeaders</var> List of ContactHeaders to set
	 * @throws IllegalArgumentException if contactHeaders is null, empty, 
	 * contains any elements that are null or not ContactHeaders from the same
	 * JAIN SIP implementation
	 */
	public void setContactHeaders(List contactHeaders)
		throws IllegalArgumentException
	{
		setHeaders(ContactHeader.name, contactHeaders);
	}

	/**
	 * Gets OrganizationHeader of Message.
	 * (Returns null if no OrganizationHeader exists)
	 * @return OrganizationHeader of Message
	 * @throws HeaderParseException if implementation could not parse 
	 * 	header value
	 */
	public OrganizationHeader getOrganizationHeader()
		throws HeaderParseException
	{
		return (OrganizationHeader)getHeader(OrganizationHeader.name, true);
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has OrganizationHeader
	 * @return boolean value to indicate if Message
	 * has OrganizationHeader
	 */
	public boolean hasOrganizationHeader()
	{
		return hasHeaders(OrganizationHeader.name);
	}

	/**
	 * Removes OrganizationHeader from Message (if it exists)
	 */
	public void removeOrganizationHeader()
	{
		removeHeaders(OrganizationHeader.name);  
	}

	/**
	 * Sets OrganizationHeader of Message.
	 * @param <var>organizationHeader</var> OrganizationHeader to set
	 * @throws IllegalArgumentException if organizationHeader is null
	 * or not from same JAIN SIP implementation
	 */
	public void setOrganizationHeader(OrganizationHeader organizationHeader)
		throws IllegalArgumentException
	{
		setHeader(organizationHeader, true);
	}

	/**
	 * Gets HeaderIterator of RecordRouteHeaders of Message.
	 * (Returns null if no RecordRouteHeaders exist)
	 * @return HeaderIterator of RecordRouteHeaders of Message
	 */
	public HeaderIterator getRecordRouteHeaders()
	{
		return getHeaders(RecordRouteHeader.name);
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has RecordRouteHeaders
	 * @return boolean value to indicate if Message
	 * has RecordRouteHeaders
	 */
	public boolean hasRecordRouteHeaders()
	{
		return hasHeaders(RecordRouteHeader.name);
	}

	/**
	 * Removes RecordRouteHeaders from Message (if any exist)
	 */
	public void removeRecordRouteHeaders()
	{
		removeHeaders(RecordRouteHeader.name);  
	}

	/**
	 * Sets RecordRouteHeaders of Message.
	 * @param <var>recordRouteHeaders</var> List of RecordRouteHeaders to set
	 * @throws IllegalArgumentException if recordRouteHeaders is null, 
	 * empty, contains any elements that are null or not RecordRouteHeaders 
	 * from the same JAIN SIP implementation
	 */
	public void setRecordRouteHeaders(List recordRouteHeaders)
		throws IllegalArgumentException
	{
		setHeaders(RecordRouteHeader.name, recordRouteHeaders);
	}

	/**
	 * Gets RetryAfterHeader of Message.
	 * (Returns null if no RetryAfterHeader exists)
	 * @return RetryAfterHeader of Message
	 * @throws HeaderParseException if implementation could not parse 
	 * header value
	 */
	public RetryAfterHeader getRetryAfterHeader() throws HeaderParseException
	{
		return (RetryAfterHeader)getHeader(RetryAfterHeader.name, true);
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has RetryAfterHeader
	 * @return boolean value to indicate if Message
	 * has RetryAfterHeader
	 */
	public boolean hasRetryAfterHeader()
	{
		return hasHeaders(RetryAfterHeader.name);
	}

	/**
	 * Removes RetryAfterHeader from Message (if it exists)
	 */
	public void removeRetryAfterHeader()
	{
		removeHeaders(RetryAfterHeader.name);  
	}

	/**
	 * Sets RetryAfterHeader of Message.
	 * @param <var>retryAfterHeader</var> RetryAfterHeader to set
	 * @throws IllegalArgumentException if retryAfterHeader is null
	 * or not from same JAIN SIP implementation
	 */
	public void setRetryAfterHeader(RetryAfterHeader retryAfterHeader)
		throws IllegalArgumentException
	{
		setHeader(retryAfterHeader, true);
	}

	/**
	 * Gets body of Message as String
	 * (Returns null if no body exists). If a language is specified
	 * in the content Type header, it is used to encode the message
	 * into a string.
	 * @return body of Message as String
	 */
	public String getBodyAsString()
	{
		if(m_body == null)
		{
			return null;
		}
		
		try
		{
			//	Check if an encoding was specified.
			ContentTypeHeader ctHdr = getContentTypeHeader();
			String encoding = ctHdr == null ? null : ctHdr.getParameter(CHARSET);
			if (encoding == null || encoding.length() == 0)
			{
				encoding = SipConstants.UTF8;
			}
				
			return new String(m_body, encoding);
		}
		catch(UnsupportedEncodingException e)
		{
			// @PMD:REVIEWED:StringInstantiation: by Amirk on 9/19/04 5:05 PM
			return new String(m_body);
		}
		catch(SipParseException e)
		{
			// @PMD:REVIEWED:StringInstantiation: by Amirk on 9/19/04 5:05 PM
			return new String(m_body);
		}
	}

	/**
	 * Gets body of Message as byte array
	 * (Returns null if no body exists).
	 * @return body of Message as byte array
	 */
	public byte[] getBodyAsBytes()
	{
		return m_body;
	}

	/**
	 * Gets boolean value to indicate if Message
	 * has body
	 * @return boolean value to indicate if Message
	 * has body
	 */
	public boolean hasBody()
	{
		return m_body != null;
	}

	/**
	 * Sets body of Message (with ContentTypeHeader)
	 * @param <var>body</var> body to set
	 * @param <var>contentTypeHeader</var> ContentTypeHeader
	 * @throws IllegalArgumentException if body or contentTypeHeader is null, or
	 * contentTypeHeader is not from same JAIN SIP implementation
	 * @throws SipParseException if body is not accepted by implementation
	 */
	public void setBody(String body, ContentTypeHeader contentTypeHeader)
		throws IllegalArgumentException, SipParseException
	{
		if(body == null)
		{
			throw new IllegalArgumentException("Message: Null body");
		}
		if(contentTypeHeader == null)
		{
			throw new IllegalArgumentException("Message: Null contentTypeHeader");
		}
		
		String encoding = contentTypeHeader.getParameter(CHARSET);
		if (encoding == null || encoding.length() == 0)
		{
			encoding = SipConstants.UTF8;
		}
		
		try
        {
            m_body = body.getBytes(encoding);
			setContentTypeHeader(contentTypeHeader);
        }
        catch (UnsupportedEncodingException e1)
        {
            throw new SipParseException(e1.getMessage());
        }
		
		try
		{
			// Update the content length header accordingly.
			ContentLengthHeader content = 
				new HeaderFactoryImpl().createContentLengthHeader(m_body.length);
			setContentLengthHeader(content);
		}
		catch(SipParseException e)
		{
			// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 5:05 PM
			e.printStackTrace();
		} 
	}

	/**
	 * Sets body of Message (with ContentTypeHeader)
	 * @param <var>body</var> body to set
	 * @param <var>contentTypeHeader</var> ContentTypeHeader
	 * @throws IllegalArgumentException if body or contentTypeHeader is null, or
	 * contentTypeHeader is not from same JAIN SIP implementation
	 * @throws SipParseException if body is not accepted by implementation
	 */
	public void setBody(byte[] body, ContentTypeHeader contentTypeHeader)
		throws IllegalArgumentException, SipParseException
	{
		if(body == null)
		{
			throw new IllegalArgumentException("Message: null body");
		}
		if(contentTypeHeader == null)
		{
			throw new IllegalArgumentException("Message: null ContentTypeHeader");			
		}

		m_body = body;
		setContentTypeHeader(contentTypeHeader);
		
		try
		{
			// Update the content length header accordingly.
			ContentLengthHeader content = 
				new HeaderFactoryImpl().createContentLengthHeader(m_body.length);
			setContentLengthHeader(content);
		}
		catch(SipParseException e)
		{
			// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 5:05 PM
			e.printStackTrace();
		}
	}

	/**
	 * Internal method: Sets body of Message 
	 * @param <var>body</var> body to set
	 * @throws IllegalArgumentException if body is null 	 
	 */
	public void setBody(byte[] body)
		throws IllegalArgumentException
	{
		if(body == null)
		{
			throw new IllegalArgumentException("Message: null body");
		}
		
		m_body = body;
		
		try
		{
			// Update the content length header accordingly.
			ContentLengthHeader content = 
				new HeaderFactoryImpl().createContentLengthHeader(m_body.length);
			setContentLengthHeader(content);
		}
		catch(SipParseException e)
		{
			e.printStackTrace();
		// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 5:02 PM
		}
	}

	/**
	 * Removes body from Message and contentType header from body
	 * (if body exists)
	 */
	public void removeBody()
	{
		m_body = null;
    	
		// Remove the relevant headers.
		removeHeaders(ContentTypeHeader.name);
		removeHeaders(ContentEncodingHeader.name);
		try
		{
			ContentLengthHeader content = new HeaderFactoryImpl().createContentLengthHeader(0);
			setContentLengthHeader(content);
		}
		catch(SipParseException e)
		{
			// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 5:02 PM
			e.printStackTrace();
		}
	}

	/**
	 * Gets version major of Message.
	 * @return version major of Message
	 * @throws SipParseException if implementation could not parse 
	 *   version major
	 */
	public int getVersionMajor() throws SipParseException
	{
		String major = m_version.getVersionMajor();
		try
		{
			int ret = Integer.parseInt(major);
			return ret;
		}
		catch (NumberFormatException ex)
		{
			throw new SipParseException(ex.getMessage(), "");
		}
	}
    
	/**
	 * Gets version minor of Message.
	 * @return version minor of Message
	 * @throws SipParseException if implementation could not parse version minor
	 */
	public int getVersionMinor() throws SipParseException
	{
		String minor = m_version.getVersionMinor();
		try
		{
			int ret = Integer.parseInt(minor);
			return ret;
		}
		catch (NumberFormatException ex)
		{
			throw new SipParseException(ex.getMessage());
		}
	}

	/**
	 * Sets version of Message. Note that the version defaults to 2.0.
	 * (i.e. version major of 2 and version minor of 0)
	 * @param <var>versionMajor</var> version major
	 * @param <var>versionMinor</var> version minor
	 * @throws SipParseException if versionMajor or versionMinor are not 
	 * accepted by implementation
	 */
	public void setVersion(int versionMajor, int versionMinor)
		throws SipParseException
	{
		m_version = SipVersionFactory.createSipVersion(versionMajor, versionMinor);
	}

	/**
	 * @return the sip version of this message
	 */
	protected SipVersion getVersion() {
		return m_version;
	}

	/**
	 * Returns boolean value to indicate if Message is a Request.
	 * @return boolean value to indicate if Message is a Request
	 */
	public abstract boolean isRequest();

	/**
	 * Returns start line of Message
	 * @return start line of Message
	 */
	public abstract String getStartLine();

	/**
	 * Dump the start line to the specified buffer.
	 * @param buffer
     * @param network true if the message is going to the network, false if
     *  it's just going to the log
	 */
	public abstract void writeStartLineToBuffer(CharsBuffer buffer, boolean network);

	/**
	 * Indicates whether some other Object is "equal to" this Message
	 * (Note that obj must have the same Class as this Message - 
	 * this means that it must be from the same JAIN SIP implementation)
	 * @param <var>obj</var> the Object with which to compare this Message
	 * @return true if this Message is "equal to" the obj
	 * argument; false otherwise
	 */
	public boolean equals(Object object)
	{
		if(object == null || 
		!(object instanceof MessageImpl))
		{
			return false;
		}
		MessageImpl other = (MessageImpl) object;

		// Check start line.
		if (!getStartLine().equals(other.getStartLine()))
		{
			return false;
		}
		
		// Check body.
		if (m_body == null || m_body.length == 0) {
			if (other.m_body != null && other.m_body.length > 0) {
				return false;
			}
		}
		else {
			if (other.m_body == null || other.m_body.length == 0) {
				return false;
			}
			else {
				for (int i = 0; i < m_body.length; i++) {
					if (m_body[i] != other.m_body[i]) {
						return false;
					}
				}
			}
		}

		// Version.
		try
		{
			if (getVersionMajor() != other.getVersionMajor())
			{
				return false;
			}
			if (getVersionMinor() != other.getVersionMinor())
			{
				return false;
			}
		}
		catch (SipException e)
		{
			return false;
		}

		// Headers.
		HeaderEntry e1 = m_top;
		HeaderEntry e2 = other.m_top;

		while (e1 != null) {
			if (e2 == null) {
				return false;
			}
			HeaderImpl h1 = e1.m_header;
			HeaderImpl h2 = e2.m_header;
			if (!h1.equals(h2)) {
				return false;
			}
			e1 = e1.m_next;
			e2 = e2.m_next;
		}
		if (e2 != null) {
			return false;
		}
		return true;
	}

	/**
	 * Get the hash code for this object.
	 */
	public int hashCode()
	{
		return toString().hashCode();
	}

	/**
	 * Creates and returns copy of Message
	 * @return copy of Message
	 */
	public Object clone() {
		try {
			MessageImpl ret = (MessageImpl) super.clone();
			ret.m_top = ret.m_bottom = null;

			// clone headers
			for (HeaderEntry old = m_top; old != null; old = old.m_next) {
				HeaderImpl h = (HeaderImpl) old.m_header.clone();
				HeaderEntry n = new HeaderEntry(ret.m_bottom, h, null);
				if (ret.m_top == null) {
					ret.m_top = n;
				}
				if (ret.m_bottom != null) {
					ret.m_bottom.m_next = n;
				}
				ret.m_bottom = n;

				Field member = getDirectAccessMember(h.getName());
				if (member != null) {
					// update direct-access headers. the default clone()
					// implementation copies direct-access references from the
					// old message into the new one. need to replace those with
					// references to headers in the new message.
					HeaderEntry oldDirect = getDirectHeaderEntry(member);
					HeaderEntry newDirect = ret.getDirectHeaderEntry(member);
					if (newDirect == oldDirect) {
						ret.setDirectHeaderEntry(member, n);
					}
				}
			}
			
			if (ret.m_top == null) { //TODO for defect 170342
	        	if (c_logger.isTraceDebugEnabled()) {			
	        		c_logger.traceDebug(this, "clone", "ret.m_top is null");
	        		
	        		c_logger.traceDebug(this, "clone", "the stack trace is: ");
		        	for (StackTraceElement stackTrace: Thread.currentThread().getStackTrace()) {
		        		c_logger.traceDebug(this, "a frame is ", stackTrace.toString());
		        	}
	            }
			}

			// clone body
			if (m_body == null) {
				ret.m_body = null;
			}
			else {
				ret.m_body = new byte[m_body.length];
				System.arraycopy(m_body, 0, ret.m_body, 0, m_body.length);
			}

			// clone other class members
			ret.m_version = (SipVersion) m_version;
			ret.m_loopback = m_loopback;
			return ret;
		}
		catch (CloneNotSupportedException e) {
			// @PMD:REVIEWED:AvoidThrowingCertainExceptionTypesRule: by Amirk on 9/19/04 5:05 PM
			// Can't happen.
			throw new Error("Message: Clone error?");
		}
	}

	/**
	 * Gets string representation of Message
	 * @return string representation of Message
	 */
	public String toString()
	{
		CharsBuffer ret = CharsBuffersPool.getBuffer();
		writeHeadersToBuffer(ret, HeaderForm.DEFAULT, false);
		
		if (m_body != null) {
			if (s_hideBody) {
				ret.append("<hidden content>");
			}
			else {
				ret.append(getBodyAsString());
			}
			ret.append(Separators.NEWLINE);
		}
		String value = ret.toString();
		CharsBuffersPool.putBufferBack(ret);

		return value;
	}

	/**
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		try {
			SipMessageByteBuffer buf = SipMessageByteBuffer.fromMessage(this, HeaderForm.DEFAULT);
			buf.toStream(out);
			buf.reset();
		}
		catch (SipException e) {
			throw new InvalidObjectException(e.getMessage());
		}
	}

	/**
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		SipMessageByteBuffer buf = SipMessageByteBuffer.fromStream(in);
		MessageParser parser = DatagramMessageParser.getGlobalInstance();
		m_deserializedMessage = parser.parse(buf); // recycles the buffer
	}

	/**
	 * called by the ObjectInputStream after readObject completes,
	 * to replace the de-serialized object instance
	 * @see java.io.ObjectInputStream
	 */
	protected Object readResolve() throws ObjectStreamException {
		// todo get rid of this method, and do all the parsing in readObject
		if (m_deserializedMessage == null) {
			throw new InvalidObjectException("null message");
		}
		return m_deserializedMessage;
	}

	/**
	 * determines whether this message is as compact as it can be
	 * @return true if this message is fully compacted. false if there is at
	 *  least one header which supports compact form, and is not compact.
	 */
	public boolean isCompact() {
		HeaderForm form = getHeaderForm();
		switch (form) {
		case DEFAULT:
			for (HeaderEntry e = m_top; e != null; e = e.m_next) {
				HeaderImpl h = e.m_header;
				if (h.isCompactFormSupported() && !h.isCompactForm()) {
					return false;
				}
			}
			return true;
		case LONG:
			return false;
		case COMPACT:
			return true;
		default:
			throw new RuntimeException("unknown header form [" + form + ']');
		}
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getHeaderForm()
	 */
	public HeaderForm getHeaderForm() {
		HeaderForm form = m_headerForm;
		return form == null ? HeaderForm.DEFAULT : form;
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#setHeaderForm(javax.servlet.sip.SipServletMessage.HeaderForm)
	 */
	public void setHeaderForm(HeaderForm form) {
		m_headerForm = form;
	}

    /**
     * writes the message start-line + headers to the specified buffer
     * @param headerForm compact or full form, as requested by the transport
     * @param buf destination buffer
     * @param network true if the message is going to the network, false if
     *  it's just going to the log
	 */
	public void writeHeadersToBuffer(CharsBuffer buffer, HeaderForm headerForm,
		boolean network)
	{
		// start line
		writeStartLineToBuffer(buffer, network);

		boolean hasContentLengthHeader = false;
		boolean commaSeparated = false;
		HeaderSeparator headerSeparator = HeaderSeparator.instance();

		// headers
		if (headerForm == HeaderForm.DEFAULT) {
			// transport layer does not specify a header form, so we use the
			// form that was specified by the application for this instance.
			headerForm = getHeaderForm();
		}
		for (HeaderEntry e = m_top; e != null; e = e.m_next) {
			// write the header name and value (name: value)
			// or just the value if this is a comma-separated header field,
			// and this value is not the first one.
			HeaderImpl h = e.m_header;
			if (!commaSeparated) {
				h.writeNameToCharBuffer(buffer, headerForm);
			}
			h.writeValueToCharBuffer(buffer, network);

			// look one header object ahead, and decide how to separate this
			// value from the next (either newline or comma).
			String headerName = h.getName();
			
			String nextHeaderName = null;
			if (e.m_next != null) {
	        	if (c_logger.isTraceDebugEnabled()) {			
	        		c_logger.traceDebug(this, "writeHeadersToBuffer", "e is " + e.toString() + " e.m_next is " + e.m_next + " e.m_next.getHeader() is " + e.m_next.getHeader());
	            }
				nextHeaderName = e.m_next.getHeader().getName();
			}
			
			
			// commaSeperated is relevant only if the next header is the same,
			// and it'll be true if the header is defined to be nested (possibly)
			// and is declared as comma seperated by the custom property
			commaSeparated = ( headerName.equals(nextHeaderName) && h.isNested() )&& 
								(headerSeparator.isCommaSeparated(headerName, false));
			if (commaSeparated) {
				// By default, we should NOT remove empty headers.
				// If the customer sets the removeEmptyCommaSeparatedHeaders CP to true, 
				//  then we will only add the comma (and space) if the header value length is > 0).
				if ((s_removeEmptyCommaSeparatedHeaders == false) || (h.getValue().length() > 0)) {
					buffer.append(',');
					if (headerForm != HeaderForm.COMPACT) {
						buffer.append(' ');	
					}
				}
			}
			else {
				buffer.append('\r').append('\n');
			}
			if (headerNamesEqual(headerName, ContentLengthHeader.name)) {
				hasContentLengthHeader = true;
			}
		}

		// ensure content-length header
		if (!hasContentLengthHeader) {
			ContentLengthHeaderImpl h = new ContentLengthHeaderImpl();
			h.writeNameToCharBuffer(buffer, headerForm);
			h.writeValueToCharBuffer(buffer, network);
			buffer.append('\r').append('\n');
		}

		buffer.append('\r').append('\n');
	}

	/**
	 * compares two header names for equality.
	 * supports both formats - full and compact
	 */
	private static boolean headerNamesEqual(String h1, String h2) {
		return HeaderImpl.headerNamesEqual(h1, h2);
	}
	
	/**
	 * copies the following 5 headers from this into dest:
	 * To, From, CallID, CSeq, Via.
	 * does not validate headers, so they may be unparsed.
	 * @param dest message to copy headers to.
	 */
	public void copyCriticalHeaders(MessageImpl dest) {
		if (m_to != null) {
			dest.setHeader(m_to.m_header, true);
		}
		if (m_from != null) {
			dest.setHeader(m_from.m_header, true);
		}
		if (m_callId != null) {
			dest.setHeader(m_callId.m_header, true);
		}
		if (m_cseq != null) {
			dest.setHeader(m_cseq.m_header, true);
		}

		// todo copy via headers more efficiently
		dest.removeViaHeaders();
		if (m_topVia != null) {
			for (HeaderEntry e = m_topVia; e != null; e = e.m_next) {
				HeaderImpl via = e.m_header;
				if (!headerNamesEqual(via.getName(), ViaHeader.name)) {
					break;
				}
				dest.addHeader(via, false);
			}
		}
	}

	/**
	 * @return false if this message came from the loopback address
	 */
	public boolean isLoopback()
	{
		return m_loopback;
	}

	/**
	 * @param loopback true for a loopback message, false for a network message
	 */
	public void setLoopback(boolean loopback)
	{
		m_loopback = loopback;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.jain.protocol.ip.sip.message.HeaderIteratorImpl.HeaderIteratorListener#onEntryDeleted(com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl.HeaderEntry)
	 */
	public void onEntryDeleted(HeaderEntry headerEntry) {
		removeHeader(headerEntry);
	}

}
