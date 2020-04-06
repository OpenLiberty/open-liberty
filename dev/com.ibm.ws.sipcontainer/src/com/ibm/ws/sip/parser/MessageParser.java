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

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.URI;
import jain.protocol.ip.sip.header.CSeqHeader;
import jain.protocol.ip.sip.header.CallIdHeader;
import jain.protocol.ip.sip.header.ContentLengthHeader;
import jain.protocol.ip.sip.header.ContentTypeHeader;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.MaxForwardsHeader;
import jain.protocol.ip.sip.header.NameAddressHeader;
import jain.protocol.ip.sip.header.RequireHeader;
import jain.protocol.ip.sip.header.SecurityHeader;
import jain.protocol.ip.sip.header.TimeStampHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Message;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.extensions.RAckHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.RSeqHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.SupportedHeader;
import com.ibm.ws.jain.protocol.ip.sip.header.ExtendedHeader;
import com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.CancelRequest;
import com.ibm.ws.jain.protocol.ip.sip.message.RequestImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.RequestLine;
import com.ibm.ws.jain.protocol.ip.sip.message.ResponseImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.SipVersion;
import com.ibm.ws.jain.protocol.ip.sip.message.SipVersionFactory;
import com.ibm.ws.jain.protocol.ip.sip.message.StatusLine;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;

/**
 * base class for a parser that decodes SIP message from the network
 * 
 * @author ran
 */
public abstract class MessageParser
{
	/** class Logger */
	private static final LogMgr s_logger = Log.get(MessageParser.class);

	// standard method constants.
	// see getMethod()
	private static final byte[] INVITE = { 'I', 'N', 'V', 'I', 'T', 'E' };
	private static final byte[] ACK = { 'A', 'C', 'K' };
	private static final byte[] BYE = { 'B', 'Y', 'E' };
	private static final byte[] CANCEL = { 'C', 'A', 'N', 'C', 'E', 'L' };
	private static final byte[] OPTIONS = { 'O', 'P', 'T', 'I', 'O', 'N', 'S' };
	private static final byte[] INFO = { 'I', 'N', 'F', 'O' };
	private static final byte[] PRACK = { 'P', 'R', 'A', 'C', 'K' };
	private static final byte[] REGISTER = { 'R', 'E', 'G', 'I', 'S', 'T', 'E', 'R' };
	private static final byte[] SUBSCRIBE = { 'S', 'U', 'B', 'S', 'C', 'R', 'I', 'B', 'E' };
	private static final byte[] NOTIFY = { 'N', 'O', 'T', 'I', 'F', 'Y' };
	private static final byte[] PUBLISH = { 'P', 'U', 'B', 'L', 'I', 'S', 'H' };
	private static final byte[] MESSAGE = { 'M', 'E', 'S', 'S', 'A', 'G', 'E' };
	private static final byte[] REFER = { 'R', 'E', 'F', 'E', 'R' };
	private static final byte[] UPDATE = { 'U', 'P', 'D', 'A', 'T', 'E' };
	private static final byte[] KEEPALIVE = { 'K', 'E', 'E', 'P', 'A', 'L', 'I', 'V', 'E' };
	private static final byte[] PROXYERROR = { 'P', 'R', 'O', 'X', 'Y', 'E', 'R', 'R', 'O', 'R' };

	/**
	 * error found in message while parsing. null if no error
	 * if the message is a request, this error is returned in the response
	 * according to RFC3261 21.4.1
	 */
	private String m_error;
	
	/** error code in case of parse error. 0 means no error */
	private int m_errorCode;

	/**
	 * the internal parser for parsing the URI from the request-URI
	 */
	private URIParser m_uriParser;
	
	/**
	 * Indicates whether the parser is in start-line hunting mode.
	 */
	protected boolean m_startLineHuntingMode;

    /** should the stack accept non-UTF8 bytes */
    private static final boolean s_acceptNonUtf8ByteSequences =
    	ApplicationProperties.getProperties().getBoolean(StackProperties.ACCEPT_NON_UTF8_BYTES);

	/**
	 * constructs a new message parser
	 */
	public MessageParser() {
		m_error = null;
		m_errorCode = 0;
		m_uriParser = new URIParser();
		m_startLineHuntingMode = false;
	}
	
	/**
	 * parses one message from the given stream
	 * @param buffer incoming byte stream
	 * @return a complete message, or null if not enough bytes
	 */
	public abstract Message parse(SipMessageByteBuffer buffer);
	
	/**
	 * called after a successful parse(),
	 * to check if there are more bytes left worth trying to parse
	 * 
	 * @return true if there is any chance to successfully parse
	 *  another message
	 */
	public abstract boolean hasMore();
	
	/**
	 * @return true if this parser requires a content-length header
	 *  to be present in the message
	 */
	protected abstract boolean contentLengthHeaderRequired();
	
	/**
	 * @return error encountered during message parse, null if parsed successfully
	 */
	public String getError() {
		return m_error;
	}
	
	/**
	 * @return the error code to return in case of parse error
	 */
	public int getErrorCode() {
		return m_errorCode;
	}
	
	/**
	 * called to clear the last parse error
	 */
	public void clearError() {
		m_error = null;
		m_errorCode = 0;
	}

	/**
	 * this method is called for every CRLF that the parser receives,
	 * before the start-line, and once again for the start-line itself.
	 * the stream parses uses this for managing keepalives.
	 * the datagram parser ignores this.
	 * @param empty true if it's only CRLF (not a start line) and false
	 *  if this is the start line.
	 */
	protected abstract void crlfReceived(boolean empty);

	/**
	 * parses the start-line of the sip message, and creates either a new
	 * request object or a new response object based on the line contents
	 * 
	 * @param buffer a byte buffer containing the incoming message bytes.
	 *  upon return, the read position of this buffer points one beyond
	 *  the end of this buffer. if there were not enough bytes for a
	 *  start-line, the buffer position is not changed
	 * @param line work buffer for decoding the start-line before parsing.
	 *  we first decode from the input byte stream into this buffer,
	 *  and then parse from this buffer to the start-line object
	 * @return a new Message object, initialized with only a start-line.
	 *  null if not enough bytes to make a complete start-line,
	 *  or if a serious parse error occurs
	 */
	protected Message parseStartLine(
		SipMessageByteBuffer buffer,
		CharsBuffer line)
	{
		// true if SIP is configured to send a "400 Bad request" in case of wrong request-line, instead of dropping the message
		boolean isSend400ForWrongRequestUri = ApplicationProperties.getProperties().getBoolean(StackProperties.SEND_400_FOR_WRONG_REQUEST_LINE);
		m_error = null;
		m_errorCode = 0;

		// 1. get the first non-empty line in the message
		boolean emptyLine;
		do {
			line.reset();
			if (!readLine(buffer, line)) {
				return null; // not enough bytes for a start-line
			}
			emptyLine = line.getCharCount() == 0;
			crlfReceived(emptyLine);
		} while (emptyLine);

		char[] content = line.getCharArray();
		int length = line.getCharCount();

		if (length < 3) {
			// not enough bytes for a start-line
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug("Error: message start-line too short");
			}
			if (!isSend400ForWrongRequestUri) {
			return null;
			}
			// if "isSend400ForWrongRequestUri" is configured to true
			// don't return null, but a request with "ILLEGAL" method
			else {
				setError(Response.BAD_REQUEST, "Bad Request - start line is too short");
				RequestImpl reqImpl = new RequestImpl();
				reqImpl.setRequestLine(new RequestLine(null, Request.ILLEGAL));
				return reqImpl;
			}
		}
		
		// 2. parse the line into a start line
		if (content[0] == 'S' &&
			content[1] == 'I' &&
			content[2] == 'P')
		{
			// looks like a response
			ResponseImpl resImpl = new ResponseImpl();
			try {
				StatusLine statusLine = parseStatusLine(content, length);
				resImpl.setStatusLine(statusLine);
			}
			catch (SipParseException e) {
				setError(Response.BAD_REQUEST, "Bad Start-Line");
			}
			return resImpl;
		}
		else {
			// either a request or some garbage
			String method = null;
			RequestLine requestLine;
			try {
				requestLine = parseRequestLine(content, length);
				method = requestLine.getMethod();
			}
			catch (SipParseException e) {
				// garbage
				setError(Response.BAD_REQUEST, "Bad Request - wrong start line: " + e.getUnparsable());
				if (!isSend400ForWrongRequestUri) {
				return null;
				}
				// if "isSend400ForWrongRequestUri" is configured to true
				// don't return null, but a request with "ILLEGAL" method
				else {
					requestLine = new RequestLine();
					requestLine.setMethod(Request.ILLEGAL);
				}
			}
			// get here with a decent-looking request
			boolean isCancel = method != null && method.equals(Request.CANCEL);
			RequestImpl reqImpl = isCancel
				? new CancelRequest()
				: new RequestImpl();
			reqImpl.setRequestLine(requestLine);
			return reqImpl;
		}
	}

	/**
	 * parses a status line (the first line in a response).
	 * called from parseStartLine(), which already determined
	 * that the first 3 chars are SIP
	 * 
	 * @param line char array containing the status line to parse
	 * @param length number of chars in line
	 */
	private StatusLine parseStatusLine(char[] line, int length)
		throws SipParseException
	{
		StatusLine statusLine = new StatusLine();

		// version (SIP/2.0)
		int end = length;
		int i;
		for (i = 0; i < end; i++) {
			if (line[i] == ' ')
				break;
		}
		SipVersion version = SipVersionFactory.createSipVersion(line, 0, i);
		statusLine.setSipVersion(version);

		if (i >= end) {
			throw new SipParseException(
				"SP expected after version, found end of line", "");
		}

		// 3-digit status code
		int code;
		if (end - i < 3) {
			throw new SipParseException("Bad status code", "");
		}
		int digit1 = line[++i] - '0';
		int digit2 = line[++i] - '0';
		int digit3 = line[++i] - '0';
		if ((0 <= digit1 && digit1 <= 9) &&
			(0 <= digit2 && digit2 <= 9) &&
			(0 <= digit3 && digit3 <= 9))
		{
			code = 100 * digit1 + 10 * digit2 + digit3;
		}
		else {
			throw new SipParseException("Bad status code", "");
		}
		statusLine.setStatusCode(code);

		if (i >= end) {
			throw new SipParseException(
				"Space expected after status code, found end of line", "");
		}
		char space = line[++i];
		if (space != ' ') {
			throw new SipParseException("Space expected after status code", "");
		}

		// reason phrase
		while (++i < end) { // find start of reason phrase
			if (line[i] != ' ')
				break;
		}
		int start = i;
		int len = end - start;
		if (len > 0) {
			statusLine.setReasonPhrase(code, line, start, len);
		}
		return statusLine;
	}

	/**
	 * parses a request line (the first line in a request)
	 * 
	 * @param line char array containing the request line to parse
	 * @param length number of chars in line
	 */
	private RequestLine parseRequestLine(char[] line, int length)
		throws SipParseException
	{
		int end = length;

		// method
		int i;
		for (i = 0; i < end; i++) {
			if (line[i] == ' ')
				break;
		}
		String method = getMethod(line, 0, i);
		if (i >= end) {
			throw new SipParseException(
				"Space expected after method in request line, found end of line",
				"");
		}

		// request-uri
		int startReqUri = ++i; // skip space
		while (++i < end) { // find end of request URI
			if (line[i] == ' ')
				break;
		}

		if (i >= end) {
			throw new SipParseException(
				"Space expected after request URI in request line, found end of line",
				"");
		}
		URI uri = m_uriParser.parse(line, startReqUri, i - startReqUri);
		
		// sip-version (literally: SIP/2.0)
		if (line[++i] != 'S' ||
			line[++i] != 'I' ||
			line[++i] != 'P' ||
			line[++i] != '/' ||
			line[++i] != '2' ||
			line[++i] != '.' ||
			line[++i] != '0')
		{
			setError(Response.SIP_VERSION_NOT_SUPPORTED, "Version Not Supported");
		}
		return new RequestLine(uri, method);
	}

	/**
	 * returns the method encoded in given char array
	 * 
	 * @param array char array containing the method
	 * @param offset index of first char in array
	 * @param length number of chars in char array
	 * @return a String representation of the method
	 */
	private static String getMethod(char[] array, int offset, int length) {
		String method;
		byte[] compare;

		if (length > 2) {
			// guess the method by the first byte
			switch (array[offset]) {
			case 'I': // Invite or Info or Isomething
				switch (array[offset + 2]) {
				case 'V': // InVite or IsVmething
					method = Request.INVITE;
					compare = INVITE;
					break;
				case 'F': // InFo or IsFmething
					method = RequestImpl.INFO;
					compare = INFO;
					break;
				default: // extension
					method = null;
					compare = null;
				}
				break;
			case 'A': // Ack or Asomething
				method = Request.ACK;
				compare = ACK;
				break;
			case 'B': // Bye or Bsomething
				method = Request.BYE;
				compare = BYE;
				break;
			case 'C': // Cancel or Csomething
				method = Request.CANCEL;
				compare = CANCEL;
				break;
			case 'K': // Keepalive or Ksomething
				method = RequestImpl.KEEPALIVE;
				compare = KEEPALIVE;
				break;
			case 'O': // Options or Osomething
				method = Request.OPTIONS;
				compare = OPTIONS;
				break;
			case 'P': // Prack or Publish or PROXYERROR or Psomething
				switch (array[offset + 2]) {
				case 'A': // Prack or Pomething
					method = RequestImpl.PRACK;
					compare = PRACK;
					break;
				case 'B': // Publish or Pomething
					method = RequestImpl.PUBLISH;
					compare = PUBLISH;
					break;
				case 'O': // ProxyError or Pomething
					method = RequestImpl.PROXYERROR;
					compare = PROXYERROR;
					break;
				default: // extension
					method = null;
					compare = null;
				}
				break;
			case 'R': // Register or Refer or Rsomething
				switch (array[offset + 2]) {
				case 'G': // Register or Rsomething
					method = Request.REGISTER;
					compare = REGISTER;
					break;
				case 'F': // Refer or Rsomething
					method = RequestImpl.REFER;
					compare = REFER;
					break;
				default: // extension
					method = null;
					compare = null;
				}
				break;
			case 'S': // Subscribe or Ssomething
				method = RequestImpl.SUBSCRIBE;
				compare = SUBSCRIBE;
				break;
			case 'N': // Notify or Nsomething
				method = RequestImpl.NOTIFY;
				compare = NOTIFY;
				break;
			case 'M': // Message or Msomething
				method = RequestImpl.MESSAGE;
				compare = MESSAGE;
				break;
			case 'U': // Update or Usomething
				method = RequestImpl.UPDATE;
				compare = UPDATE;
				break;
			default: // extension - unknown first char
				method = null;
				compare = null;
			}
		}
		else {
			// extension - less than 3 chars
			method = null;
			compare = null;
		}

		if (compare != null) {
			// check if we guessed right
			if (length == compare.length) {
				for (int i = offset + length - 1; i >= 0; i--) {
					if (array[i] != compare[i]) {
						method = null;
						break;
					}
				}
			}
			else {
				method = null;
			}
		}

		if (method == null) {
			// guessed wrong - it's some extension method
			method = new String(array, offset, length);
		}

		return method;
	}

	
	/**
	 * parses the header section of an inbound sip message.
	 * @param source byte stream to read the headers from.
	 *  upon return, this stream points to the end of the headers section.
	 *  if there are not enough bytes to make a complete headers section,
	 *  points to the beginning of the first incomplete header line.
	 * @param dest message object to add headers to
	 * @param line work buffer for decoding header lines before parsing.
	 *  we first decode from the input byte stream into this buffer,
	 *  and then parse from this buffer to the header object
	 * @return true if parsed the whole header section,
	 *  false if not enough bytes for a complete header section
	 */
	protected boolean parseHeaders(
		SipMessageByteBuffer source,
		Message dest,
		CharsBuffer line)
	{
		line.reset();
		while (readLine(source, line)) {
			if (line.getCharCount() == 0) {
				return true; // found empty line that ends the headers section
			}
			parseHeaderLine(line, dest);
			line.reset();
		}
		return false;
	}
	
	/**
	 * parses a header line. a header line may contain multiple headers
	 * (of the same type) separated by a comma
	 * 
	 * @param source a single line containig one or more headers,
	 *  not including CRLF
	 * @param dest message object to add header(s) to
	 * @return true on success
	 */
	protected boolean parseHeaderLine(CharsBuffer source, Message dest) {
		char[] line = source.getCharArray();
		int length = source.getCharCount();
		int i = 0;
		int headerNameSize = 0;
		
		// measure size of header name
		while (i < length) {
			char c = line[i];
			if (c == ':') {
				if (headerNameSize == 0) {
					headerNameSize = i;
				}
				break;
			}
			if ((c == ' ' || c == '\t') && headerNameSize == 0) {
				// right-trim header name
				headerNameSize = i;
			}
			i++;
		}
		if (i == length) {
			setError(Response.BAD_REQUEST, "Bad Request. No colon in header line");
			return false;
		}
		if (headerNameSize == 0) {
			setError(Response.BAD_REQUEST, "Bad Request. Expected header name before colon");
			return false;
		}
		// start with a header that has a name and no value
		HeaderImpl header = HeaderCreator.createHeader(line, headerNameSize);

		i++; // skip colon
		
		// eat sp/ht before the value
		while (i < length && (line[i] == ' ' || line[i] == '\t')) {
			i++;
		}
		
		// parse header value
		int valueLen = length - i;
		boolean extendedHeader = header instanceof ExtendedHeader;
		boolean parseNestedHeader = false;
		if(extendedHeader) {
			parseNestedHeader = ((ExtendedHeader) header).isInCommaSeparated();
		}
		else {
			if(header.isNested()) parseNestedHeader = true;
		}
		if (parseNestedHeader) {
			// some headers can have multiple values nested in a single line
			// (e.g via: via1, via2, via3)
			parseNestedHeader(dest, header, line, i, valueLen);
		}
		else {
			CharArray hdrValue = getCharArray(line, i, valueLen);
			header.setValue(hdrValue);
			try {
				// diligent parsing ensures buffer is returned to pool
				header.parse();
			}
			catch (Exception e) {
				// even if failed to parse header,
				// we still add it to the message unparsed
				if (s_logger.isTraceDebugEnabled()) {
					s_logger.traceDebug(
						this,
						"parseHeaders",
						"Failed parsing header: " + header.getName(),
						e);
				}
				if (parsingOptional(header)) {
					// rfc 3261-16.3-1
				}
				else {
					String headerName = header.getName();
					setError(Response.BAD_REQUEST, "Bad " + headerName + " header");
				}
			}
			dest.addHeader(header, false);
		}
		return true;
	}

	/**
	 * called after parsing fails for a header value.
	 * checks if parsing is optional or mandatory for the given header,
	 * by inspecting the header name. some headers are critical for internal
	 * processing, and some are optional. optional headers should be passed
	 * up to the application whether or not the syntax is valid.
	 * critical headers with bad syntax are rejected on the spot.
	 * 
	 * @param header the header name to inspect
	 * @return true if parsing is optional, false if critical
	 */
	public static boolean parsingOptional(HeaderImpl header) {
		if (header instanceof NameAddressHeader) {
			// all name-addr headers are mandatory for internal processing
			return false;
		}
		if (header instanceof SecurityHeader) {
			// all security headers are mandatory for internal processing
			return false;
		}
		String name = header.getName();
		if (name == CallIdHeader.name ||
			name == ContentLengthHeader.name ||
			name == ContentTypeHeader.name ||
			name == CSeqHeader.name ||
			name == MaxForwardsHeader.name ||
			name == RAckHeader.name ||
			name == RequireHeader.name ||
			name == RSeqHeader.name ||
			name == SupportedHeader.name ||
			name == ViaHeader.name  ||
			name == TimeStampHeader.name)
		{
			return false;
		}
		return true;
	}

	/**
	 * parses one or more header values nested in one line
	 * 
	 * @param message the message object to add headers to
	 * @param header the header object to add first value to
	 * @param line char array containing source of header value
	 * @param offset index in char array to start reading from
	 * @param length number of chars to parse
	 * @throws SipParseException
	 * @see MessageConverter#parseNestedHeader()
	 */
	private void parseNestedHeader(
		Message message,
		HeaderImpl header,
		char[] line,
		int offset,
		int length)
	{
		// nested Headers can be separated by a comma.
		// commas are also allowed inside a header's value,
		// but only inside <> or ""
		boolean brackets = false;
		boolean parenths = false;
		
		int start = offset; // where each value starts
		int end = offset+length; // where the line ends
		boolean endval = false; // set to true when reached the end of a value
		boolean endl = false; // set to true when reached end of line
		
		for (int i = offset; i <= end; i++) {
			if (i < end) {
				// check for a real separator
				switch (line[i]) {
				case '"':
					parenths = !parenths;
					break;
				case '<':
					brackets = true;
					break;
				case '>':
					brackets = false;
					break;
				case ',':
					if (!brackets && !parenths) {
						endval = true;
					}
				}
			}
			else {
				// end of line is also end of value
				endl = true;
				endval = true;
			}
			
			if (endval) {
				// found a real separator or end of line
				int valLen = i - start;
				CharArray hdrValue = getCharArray(line, start, valLen);
				start = i+1; // where the next value starts
				
				// clone this header before parsing, to prepare the next header
				HeaderImpl nextHeader = endl ? null : (HeaderImpl)header.clone();
				
				// parse header value
				header.setValue(hdrValue);
				try {
					header.parse();
				}
				catch (Exception e) {
					// even if failed to parse header,
					// we still add it to the message unparsed
					if (s_logger.isTraceDebugEnabled()) {
						s_logger.traceDebug(
							this,
							"parseNestedHeader",
							"Failed parsing header: " + header.getName(),
							e);
					}
					if (parsingOptional(header)) {
						// rfc 3261-16.3-1
					}
					else {
						String headerName = header.getName();
						setError(Response.BAD_REQUEST, "Bad " + headerName + " header");
					}
				}
				message.addHeader(header, false);
				endval = false;
				header = nextHeader;
			}
		}
	}

	/**
	 * copies the message body from the incoming byte stream
	 * into the message object. the message object should already
	 * have a complete header part
	 * 
	 * @param buffer message stream pointing to start of body part
	 * @param message message object with complete header part
	 * @return false if body was incomplete, otherwise true.
	 *  true does not necessarily indicate successful body copying.
	 */
	protected boolean parseBody(SipMessageByteBuffer buffer, Message message) {
		// 1. get content-length so we know how many bytes to expect
		int contentLength, maxContentLength;
		try {
			ContentLengthHeader contentLengthHeader = message.getContentLengthHeader();
			if (contentLengthHeader == null) {
				// no content-length header
				if (contentLengthHeaderRequired()) {
					setError(Response.BAD_REQUEST, "Bad Request. Missing Content-Length header");
					contentLength = 0;
				}
				else {
					// implicit content-length
					contentLength = buffer.getRemaining();
				}
			}
			else {
				// explicit content-length
				contentLength = contentLengthHeader.getContentLength();
			}
		}
		catch (HeaderParseException e) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(
					this,
					"parseBody",
					"Failed parsing Content-Length header",
					e);
			}
			setError(Response.BAD_REQUEST, "Bad Request. Invalid Content-Length");
			contentLength = 0;
		}
		if (contentLength == 0) {
			if (message.hasBody()) {
				message.removeBody();
			}
			return true;
		}
		
		maxContentLength = ApplicationProperties.getProperties().getInt(
				StackProperties.MAX_CONTENT_LENGTH);
		if (maxContentLength < contentLength) {
			
			// It will expect at most maxContentLength bytes.
			if (buffer.getRemaining() < maxContentLength) {
				// expect more bytes
				return false;
			}
			
		} else if (buffer.getRemaining() < contentLength) {
			// expect more bytes
			return false;
		}
		
		// 2. get content-type header. we need this because JAIN
		// requires passing it as a parameter to setBody()
		ContentTypeHeader contentTypeHeader;
		try {
			contentTypeHeader = message.getContentTypeHeader();
		}
		catch (HeaderParseException e) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(
					this,
					"parseBody",
					"Failed parsing Content-Type header",
					e);
			}
			return true;
		}
		if (contentTypeHeader == null) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug("Error: no Content-Type header");
			}
			
		} else if (maxContentLength < contentLength) {
			
			// Rewind the body by max content length
			int currentPosition = buffer.getReadPos();
			buffer.rewind(currentPosition + maxContentLength);
			
			// Sets an error that content-length > maximum allowed content-length
			setError(Response.BAD_REQUEST, "Bad Request. Content-Length is greater than allowed");
			
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "parseBody", "Error: Content-Length is greater than allowed");
			}
			
		} else {
			byte[] body = new byte[contentLength];
			buffer.copyTo(body, 0, contentLength);
			
			try {
				message.setBody(body, contentTypeHeader);
			}
			catch (SipParseException e) {
				if (s_logger.isTraceDebugEnabled()) {
					s_logger.traceDebug(
						this,
						"parseBody",
						"Failed setting body",
						e);
				}
			}
		}
		return true;
	}

	/**
	 * reads a line from the given source byte stream till a CRLF is
	 * encountered, and writes it to the dest char stream. upon return,
	 * source buffer position is one beyond end of line, and dest buffer
	 * is filled with the line read, not including the terminating CRLF.
	 * if not enough bytes available for a complete line, source buffer
	 * position is unchanged, and dest buffer is filled with as many bytes
	 * as could be read
	 * 
	 * @param source the buffer to read from
	 * @param dest the buffer to write to
	 * @return true if a complete line was read
	 */
	private boolean readLine(SipMessageByteBuffer source, CharsBuffer dest) {
		byte[] bytes = source.getBytes();
		int offset = source.getReadPos();
		int length = source.getRemaining();
		int read = readLine(bytes, offset, length, dest);
		boolean complete = read > 0;
		if (complete) {
			// after reading one line successfuly,
			// fast-forward source buffer to the end of the line
			source.rewind(offset + read);
		}
		return complete;
	}
	
	/**
	 * reads a line from the given source byte stream till a CRLF is
	 * encountered, and writes it to the dest char stream. upon return,
	 * source buffer position is one beyond end of line, and dest buffer
	 * is filled with the line read, not including the terminating CRLF.
	 * if not enough bytes available for a complete line, dest buffer is
	 * filled with as many bytes as could be read
	 * 
	 * @param source the byte array to read from
	 * @param offset index into first byte to read in source array
	 * @param length number of bytes available for reading in source array 
	 * @param dest the buffer to write to
	 * @return number of bytes read from source, 
	 *  0 if source did not contain a complete line
	 */
	private int readLine(byte[] source, int offset, int length, CharsBuffer dest) {
		final int end = offset + length; // index to one-past source array
		for (int i = offset; i < end; i++) {
			byte b = source[i];
			char srcChar;
			char dstChar;

			if ((b & 0x80) == 0x80) {
				int size = utf8size(b);
				int value;
				if (size == -1) {
					// neither utf-8 or 7-bit-ascii
					if (s_logger.isTraceFailureEnabled()) {
						s_logger.traceFailure(this, "readLine", "Illgal byte value ["
							+ (int)(b & 255) + ']');
					}
					if (!s_acceptNonUtf8ByteSequences) {
						setError(Response.BAD_REQUEST, "Bad Message. Illegal Character.");
					}
					value = (int)(b & 255); // 8-bit ascii - not standard
					size = 1;
				}
				else {
					value = utf8(source, i, end-i, size);
					if (value == -1) {
						// utf-8 lead byte with no utf-8 trail byte.
						if (s_logger.isTraceFailureEnabled()) {
							s_logger.traceFailure(this, "readLine",
								"Illgal byte value, expected utf-8 trail byte following utf-8 lead byte ["
									+ (int)(b & 255) + ']');
						}
						if (!s_acceptNonUtf8ByteSequences) {
							setError(Response.BAD_REQUEST, "Bad Message. Illegal Character.");
						}
						value = (int)(b & 255); // 8-bit ascii - not standard
						size = 1;
					}
				}
				srcChar = (char)value;
				i += size-1;
			}
			else {
				// 7-bit ascii
				srcChar = (char)b;
			}

			// check for end-of-line
			boolean endl = false; // set to true if found a real end-of-line
			if (srcChar == '\r') {
				endl = true;
				
				// skip CRLF
				if (i < end-1) {
					byte ahead = source[i+1];
					if (ahead == '\n') {
						i++; // eat LF
					}
					else {
						// rfc2543 permits just CR
						if (s_logger.isTraceDebugEnabled()) {
							s_logger.traceDebug("Warning: line terminated with CR only");
						}
					}
				}
				else {
					// packet ends with CR. wait for more bytes.
					break;
				}
			}
			else if (srcChar == '\n') {
				endl = true; // in most cases it's a real end-of-line
				
				// rfc2543 permits just LF
				if (s_logger.isTraceDebugEnabled()) {
					s_logger.traceDebug("Warning: line terminated with LF only");
				}
			}
			
			// check if it's just fake end-of-line
			if (endl) {
				if (i < end-1) {
					// only check for fake end-of-line on non-empty line
					if (dest.getCharCount() > 0) {
						byte ahead = source[i+1];
						if (ahead == ' ' || ahead == '\t') {
							// fake end of line (rfc3261 7.3.1)
							endl = false;

							do { // eat whitespaces
								if (++i >= end-1) {
									// packet ends with whitespace
									return 0;
								}
								ahead = source[i+1];
							} while (ahead == ' ' || ahead == '\t');
						}
					}
					if (endl) {
						// real end of line
						return i - offset + 1;
					}
				}
				else {
					// found end-of-line at the end of the packet.
					// if it's an empty line, we are at the end of the header section.
					// otherwise, the packet is not complete, so we need to wait for more bytes
					// before knowing if it's a complete line.
					return dest.getCharCount() == 0 ? i - offset + 1 : 0;
				}
				dstChar = ' '; // replace fake-endl with a single space
			}
			else {
				dstChar = srcChar;
			}
			dest.append(dstChar);
		}
		return 0;
	}

	/**
	 * calculates the size of a unicode character
	 * @param b leading byte in the unicode character
	 * @return number of octets in character, or -1 if invalid character
	 */
	public static int utf8size(byte b) {
		if ((b & 0x80) == 0) { // 0.......
			return 1;
		}
		if ((b & 0xE0) == 0xC0) { // 110.....
			return 2;
		}
		if ((b & 0xF0) == 0xE0) { // 1110....
			return 3;
		}
		if ((b & 0xF8) == 0xF0) { // 11110...
			return 4;
		}
		if ((b & 0xFC) == 0xF8) { // 111110..
			return 5;
		}
		if ((b & 0xFE) == 0xFC) { // 1111110.
			return 6;
		}
		return -1;
	}

	/**
	 * decodes a single utf-8 char
	 * @param bytes byte array containing the character
	 * @param offset offset into the byte array
	 * @param length bytes available in the array
	 * @return the utf-8 character represented as int,
	 *  or -1 if not enough bytes in array
	 */
	public static int utf8(byte[] bytes, int offset, int length) {
		byte b = bytes[offset];
		int octets = utf8size(b);
		int value = utf8(bytes, offset, length, octets);
		return value;
	}

	/**
	 * decodes a single utf-8 char
	 * @param bytes byte array containing the character
	 * @param offset offset into the byte array
	 * @param length bytes available in the array
	 * @param octets pre-calculated character size. this can be calculated
	 *  by calling utf8size(byte) on the first byte.
	 * @return the utf-8 character represented as int,
	 *  or -1 if not enough bytes in array
	 */
	public static int utf8(byte[] bytes, int offset, int length, int octets) {
		// 1. decode leading byte
		byte b = bytes[offset];
		int value;

		switch (octets) {
		case 1:
			value = b; // 0.......
			break;
		case 2:
			value = b & 0x1f; // 110.....
			break;
		case 3:
			value = b & 0xf; // 1110....
			break;
		case 4:
			value = b & 0x7; // 11110...
			break;
		case 5:
			value = b & 0x3; // 111110..
			break;
		case 6:
			value = b & 0x1; // 1111110.
			break;
		default:
			return -1;
		}

		// 2. decode trailing bytes
		if (octets > length) {
			// not enough bytes for the unicode character
			return -1;
		}
		for (int i = 1; i < octets; i++) {
			b = bytes[offset + i];
			if ((b & 0xC0) != 0x80) {
				// expected 10......
				// todo throw something
				return -1;
			}
			value <<= 6;
			value |= (b & 0x3F);
		}
		return value;
	}

	/**
	 * copies array of chars into a newly allocated CharArray.
	 * whitespace to the left and right of the string is trimmed off.
	 * 
	 * @param source source array
	 * @param offset source array location to start copying from
	 * @param length number of chars to copy
	 * @return a newly allocated CharArray
	 */
	private CharArray getCharArray(char[] source, int offset, int length) {
		// trim off leading whitespace
		int off; // index of first non-whitespace byte
		for (off = offset; off < offset + length; off++) {
			if (source[off] > ' ')
				break;
		}
		// trim off trailing whitespace
		int len; // length after trim
		for (len = length - (off - offset); len >= 0; len--) {
			if (source[off + len - 1] > ' ')
				break;
		}

		// copy from byte array to char array
		CharArray result = CharArray.getFromPool(source, off, len);
		return result;
	}
	
	/**
	 * called by derived parsers when they encounter a parse error
	 * @param errorCode error code to return in the response
	 * @param error error description to be returned as the reason phrase
	 */
	protected void setError(int errorCode, String error) {
		if (m_error == null) {
			m_error = error;
			m_errorCode = errorCode;
		}
	}
	
	/**
	 * Sets the parser to be in start-line hunting mode.
	 * @param mode true if the parser should be in start-line hunting mode. Otherwise, false.
	 */
	public void setStartLineHuntingMode(boolean mode) {
		m_startLineHuntingMode = mode;
	}
	
	/**
     * Indicates whether the parser is in start-line hunting mode.
     * @return boolean value to indicate whether the parser is in start-line hunting mode.
	 */
	public boolean getStartLineHuntingMode() {
		return m_startLineHuntingMode;
	}
}
