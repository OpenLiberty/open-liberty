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
import jain.protocol.ip.sip.address.NameAddress;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.address.URI;

import java.util.HashMap;

import javax.servlet.sip.ServletParseException;

import com.ibm.ws.jain.protocol.ip.sip.address.AddressFactoryImpl;
import com.ibm.ws.jain.protocol.ip.sip.address.NameAddressImpl;
import com.ibm.ws.jain.protocol.ip.sip.address.URIImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ParametersImpl;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;
import com.ibm.ws.sip.parser.util.LRUStringCache;

/**
 * Base class for all parsers.
 * 
 * @author Assaf Azaria
 */
public class SipParser extends Lexer implements Separators
{
	// constants
	private static final char[] s_userNameSeparators = { COLON, AT };
	private static final char[] s_hostNameSeparators = { COLON, SEMICOLON, GREATER_THAN, SP, QUESTION };
	
	/**
	 * should the parser unescape escaped characters.
	 * this should be set to true for parsing messages from the network,
	 * and to false when parsing message components from the application.
	 * (the application should not pass escaped characters anyway, but in
	 * case it does, they should not be unescaped).
	 */ 
	private boolean m_unescape;

	//indicate that we parse To, From, Contact headers
	private boolean m_parseHeaderParameters;
	
	/**
	 * constructor
	 * @param unescape should the parser unescape escaped characters
	 */
	public SipParser(boolean unescape) {
		m_unescape = unescape;
	}
	
	/**
	 * no-arg constructor, defaults to parsing incoming network messages
	 */
	public SipParser() {
		this(true);
	}
	
	//
	// API.
	//
	
	public void setParseHeaderParameters(boolean headerParameters) {
		m_parseHeaderParameters = headerParameters;
	}

	/**
	 * parses a list of parameters separeated by the given separator
	 * @param separator the separator
	 * @param escaped true if parameters can be escaped, in which case
	 *  this operation will decode them.
	 *  this decoding is only performed if m_unescape is true, meaning
	 *  we are parsing incoming network messages
	 * @param uri whether the parameter map is for uri or not
	 * @return a new list of parameters
	 * @throws SipParseException
	 */
	public ParametersImpl parseParametersMap(char separator, boolean escaped,boolean uri)
		throws SipParseException
	{
		String key, value;
		boolean quote;
		ParametersImpl params = new ParametersImpl();
		while (true) {
			lws();
			if(uri) {

				key = sipUriParam();
			}
			else {
				key = sipToken();
			}
			if (key.length() == 0) {
				throw new SipParseException("Expected parameter key", toString());
			}
			lws();
			// some parameters can come with a key and without a value
			if (LA(1) == EQUALS) {
				match(EQUALS);
				lws();
            	
				if (LA(1) == DOUBLE_QUOTE) {
					quote = true;
					value = quotedString();
					lws();
				}
				else {
					quote = false;
					if(uri) {

						value = sipUriParam();
					}
					else {
						value = sipParam();
					}
					lws();
				} 
	
			}
			else {
				quote = false;
				value = "";
			}
			
			if (escaped && m_unescape) {
				value = Coder.decode(value);
			}
			params.setParameter(key, value, quote);

			if (LA(1) != separator) {
				break;
			}
			match(separator);
		}

		return params;
	}
    
	/**
	 * Parse a quoted string and include the quotes.
	 * @throws SipParseException
	 */
	public String quotedStringWithQuotes() throws SipParseException
	{
		LRUStringCache cache = LRUStringCache.getInstance();
		CharsBuffer buf = CharsBuffersPool.getBuffer();
		
		buf.append(DOUBLE_QUOTE);
		buf.append(quotedString());
		buf.append(DOUBLE_QUOTE);
		
		String ret = cache.get(buf.getCharArray(), 0, buf.getCharCount());
		CharsBuffersPool.putBufferBack(buf);
		return ret;
	}

	/**
	 * parses a NameAddress object from the stream
	 * 
	 * @return a new NameAddress object
	 * @throws SipParseException in case the parse failed
	 */
	public NameAddress parseNameAddress() throws SipParseException {
		String name = null; // optional
		URI address = null; // mandatory
		CharsBuffer buff = CharsBuffersPool.getBuffer();
		
		// name address format is one of: 
		// "name" <address> or name <address> or <address> or address
		for (int i = 1; LA(i) != ENDL && address == null; i++) {
			char n = LA(i);
			switch (n) {
			case LESS_THAN: // name <address> or <address>
				buff.rtrim();
				LRUStringCache cache = LRUStringCache.getInstance();
				name = cache.get(buff.getCharArray(), 0, buff.getCharCount());
				consume(i - 1);
				address = parseURIwithBrackets();
				break;
			case DOUBLE_QUOTE: // "name" <address>
				consume(i - 1);
				name = quotedString();
				lws();
				address = parseURIwithBrackets();
				break;
			case COLON: // uri
				address = parseURI();
				break;
			default:
				buff.append(n);
			}
		}
		
		CharsBuffersPool.putBufferBack(buff);
		if (address == null) throw new SipParseException("the address syntax is bad ");

		NameAddress nameAddress = new NameAddressImpl();
		if (name != null && name.length() > 0) {
			nameAddress.setDisplayName(name);
		}
		
		
		nameAddress.setAddress(address); 
		return nameAddress;
	}
    
	/**
	 * parses a uri surrounded by brackets 
	 */
	public URI parseURIwithBrackets() throws SipParseException {
		match(LESS_THAN);
		URI uri = parseURI(true);
		match(GREATER_THAN);
		lws(); // RAQUOT  =  ">" SWS ; right angle quote
		
		return uri;
	}
    
	/**
	 * Parse a uri form the stream.
	 * 
	 * @param uriWithBrackets whether the URI is surrounded 
	 * by brackets
	 * @return a URI
	 * @throws SipParseException in case the parse failed.
	 */
	public URI parseURI(boolean uriWithBrackets) throws SipParseException
	{
		String scheme = nextToken(COLON);
		match(COLON);
		return parseURI(scheme, uriWithBrackets);
	}
	
	/**
	 * @return a URI
	 * @throws SipParseException in case the parse failed.
	 */
	public URI parseURI() throws SipParseException
	{
		return parseURI(false);
	}
	
	/**
	 * parses the scheme data part of a uri
	 * 
	 * @param <var>scheme</var> SIP or SIPS or whatever
	 * @return a URI
	 * @throws SipParseException in case the parse failed.
	 */
	public URI parseURI(String scheme) throws SipParseException{
		return parseURI(scheme, false);
	}

	/**
	 * parses the scheme data part of a uri
	 * 
	 * @param <var>scheme</var> SIP or SIPS or whatever
	 * @param uriWithBrackets whether the URI is surrounded
	 * @return a URI
	 * @throws SipParseException in case the parse failed.
	 */
	public URI parseURI(String scheme, boolean uriWithBrackets) throws SipParseException
	{
		if (scheme.equalsIgnoreCase("SIP") || scheme.equalsIgnoreCase("SIPS"))
		{
			return parseSipURLInternally(scheme, uriWithBrackets);
		}
		
		if (scheme.equalsIgnoreCase("TEL")) {
			return parseTelURLInternally(scheme, uriWithBrackets);
		}
		// validate scheme.
		// scheme = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
		if (scheme.length() < 1) {
			throw new SipParseException("empty scheme in URI");
		}
		if (!Character.isLetter(scheme.charAt(0))) {
			throw new SipParseException(scheme, "expected letter in scheme but found ["
				+ scheme.charAt(0) + ']');
		}
		for (int i = scheme.length()-1; i > 0; i--) {
			char c = scheme.charAt(i);
			if (!Character.isLetterOrDigit(c)
				&& c != '+' && c != '-' && c != '.')
			{ 
				throw new SipParseException(scheme, "expected letter/digit/+/-/. in scheme but found ["
					+ c + ']');
			}
		}
        
		// We do not parse (for now?) absolute uris. 
		URI url = new URIImpl(scheme);
		String data = null;
		
		if (! uriWithBrackets && m_parseHeaderParameters){
			//we need to parse until the parameters since the parameters belongs to the header 
			if (find(SEMICOLON) != -1){
				data = nextToken(SEMICOLON);
			}else {
				//if parameters are not found, parse until the first header or until the end of the uri
				data = nextToken(QUESTION);
			}
		}else {
			data = nextToken(GREATER_THAN);
		}
		
		if (m_unescape) {
			data = Coder.decode(data);
		}
		
		url.setSchemeData(data);
		return url;

	}
	/**
     * Private method 
     * parses the tel url using the following methods: parseIsGlobal, parsePhoneNumber, parseParam
     * It is based on the fact that it is only being called from methods
     * that verifies that the scheme is tel 
     * 
     * Parse a tel url with the given scheme. (TEL).
	 * 
	 * @param scheme the scheme.
	 * @param uriWithBrackets whether the URI is surrounded
	 * @throws SipParseException
	 */
	private URI parseTelURLInternally(String scheme, boolean uriWithBrackets) throws SipParseException
	{
		
		String uri = null;
		if(uriWithBrackets){
			// uri - holds the uri within the '<>' if exists
			 uri = nextToken(GREATER_THAN);
			if (LA() == GREATER_THAN) {
				consume();
			}
		}
		else{
			// uri - holds the uri 
				uri = nextToken(SEMICOLON);					
		}
		// headerParams - holds the headers parameters outside of the '<>' if exists
		String headerParams = nextToken(SP);
		
		
		URI retUrl = new URIImpl(scheme);
		String data = null;
		// url - holds the full url without '<>'
		String url = uri+headerParams;
		
		if (uri.isEmpty()) {
			throw new SipParseException("null URL");
		}

		int length = url.length();
		int position = parseIsGlobal(url);
		boolean isGlobal = position!=0;
		if (position >= length) {
			throw new SipParseException("unexpected end of URL [" + url
					+ ']');
		}
		position = parsePhoneNumber(url, position, isGlobal);
		//we need to stop the parsing, the rest of the string is header parameters
		if (!(m_parseHeaderParameters && ! uriWithBrackets)){
			position = parseParam(uri, position);
		}
			
		data = uri;
		
		position += headerParams.length();
		if (position != length) {
			throw new SipParseException("unexpected character in URL [" + url
					+ "] at position [" + position + ']');
		}
		
		if (m_unescape) {
			data = Coder.decode(data);
		}
		
		// have to return the lexer to it's previous situation
		rewind();
		if(uriWithBrackets) {
			nextToken(GREATER_THAN);
		}
		else {
			nextToken(SEMICOLON);	
		}
		
		retUrl.setSchemeData(data);
		return retUrl;
	}
	/**
	 * Parse out the is global indicator
	 * @param url the raw URL string
	 * @return string position after consuming the characters needed for this match
	 * @throws ServletParseException if url is invalid
	 */
	private int parseIsGlobal(String url) throws SipParseException
	{
		boolean isGlobal = url.charAt(0) == '+';
		if (isGlobal) {
			return 1;
		}
		return 0;
		
	}
	
	/**
	 * Parse out the phone number from the url.
	 * 1*phone-digit
	 * phone-digit = "0" / "1" / "2" / "3" / "4" / "5" / "6" / "7" / "8" / "9" /
	 *               "-" / "." / "(" / ")" /
	 *               "*" / "#" / "A" / "B" / "C" / "D" /
	 *               "p" / "w"
	 * @param url the raw URL string
	 * @param position string position to begin matching
	 * @return string position after consuming the characters needed for this match
	 * @throws ServletParseException if url is invalid
	 */
	private int parsePhoneNumber(String url, int position, boolean isGlobal) throws SipParseException
	{
		int length = url.length();
		int i;
		for (i = position; i < length; i++) {
			char c = url.charAt(i);
			switch (c) {
			case '0': case '1': case '2': case '3': case '4':
			case '5': case '6': case '7': case '8': case '9':
			case '-': case '.': case '(': case ')':
			case '*': case '#':
			case 'A': case 'a':
			case 'B': case 'b':
			case 'C': case 'c':
			case 'D': case 'd':
			case 'p': case 'P':
			case 'w': case 'W':
				continue;
			case 'e': case 'E': 
			case 'f': case 'F': {
				// changes according to RFC 3966, allowing hexa digits to local numbers only (don't allow this to global numbers)
				if (!isGlobal) {
					continue;
				} else {
					throw new SipParseException("invalid number in tel URL [" + url + ']');
				}
			}

			}
			break;
		}
		if (i == position) {
			throw new SipParseException("invalid number in tel URL [" + url + ']');
		}

		return i;
	}
	
	/**
	 * Parse out the parameters from the url
	 * *(";" token ["=" token])
	 * @param url the raw URL string
	 * @param position string position to begin matching
	 * @return string position after consuming the characters needed for this match
	 * @throws ServletParseException if url is invalid
	 */
	private int parseParam(String url, int position) throws SipParseException
	{
		HashMap<String, String> params = new HashMap<String, String>();
		int length = url.length();
		int i = position;
		while (i < length) {
			char c = url.charAt(i);
			if (c != ';') {
				break;
			}

			// parse key
			String name = token(url, ++i);
			if (name == null) {
				break;
			}

			// parse value
			i += name.length();
			String value;
			if (i < length) {
				c = url.charAt(i);
				if (c == '=') {
					value = token(url, ++i);
					if (value == null) {
						break;
					}
					i += value.length();
				}
				else {
					value = "";
				}
			}
			else {
				value = "";
			}
			if (params.get(name) == null) {
				params.put(name, value);
			} else {
				throw new SipParseException("duplicate parameter [" + name
						+ "] in URL [" + url + ']');        		
			}
		}
		return i;
	}
	
	/**
	 * parses a token out of the given url string
	 * token = 1*(%x21 / %x23-27 / %x2A-2B / %x2D-2E / %x30-39
	 *         / %x41-5A / %x5E-7A / %x7C / %x7E)
	 * @param url the given url string
	 * @param position start position
	 * @return the matched string, null if no match
	 */
	private static String token(String url, int position) {
		int length = url.length();
		int i;
		for (i = position; i < length; i++) {
			char c = url.charAt(i);
			if (c == '!' || // %x21
					('#' <= c && c <= '\'') || // %x23-27
					('*' <= c && c <= '+') || // %x2A-2B
					('-' <= c && c <= '.') || // %x2D-2E
					('0' <= c && c <= '9') || // %x30-39
					('A' <= c && c <= 'Z') || // %x41-5A
					('^' <= c && c <= 'z') || // %x5E-7A
					c == '|' || // %x7C
					c == '~') // %x7E
			{
				continue;
			}
			break;
		}
		if (i == position) {
			return null;
		}
		return url.substring(position, i);
	}

	/**
	 * Parse a sip url with the given scheme. (SIP/SIPS).
	 * 
	 * @param scheme the shceme.
	 * @param uriWithBrackets whether the URI is surrounded
	 * @throws SipParseException
	 */
	public SipURL parseSipURL(String scheme, boolean uriWithBrackets) throws SipParseException
	{
		if (!(scheme.equalsIgnoreCase("SIP") || scheme.equalsIgnoreCase("SIPS")))
		{
			throw new SipParseException("Sip url must have a sip or sips scheme", "");
		}

        return parseSipURLInternally(scheme, uriWithBrackets);
    }

    /**
     * Private method avoids double-execution of 'equalsIgnoreCase'
     * checks in parseSipURI and parseSipURL above.
     * It is based on the fact that it is only being called from methods
     * that verifies that the scheme is sip or sips 
     * 
     * Parse a sip url with the given scheme. (SIP/SIPS).
	 * 
	 * @param scheme the scheme.
	 * @param uriWithBrackets whether the URI is surrounded
	 * @throws SipParseException
	 */
	private SipURL parseSipURLInternally(String scheme, boolean uriWithBrackets) throws SipParseException
	{
		SipURL url = null;
		String userName = null;
		String password = null;
		int portNum = 0;
		String userType = null;
		String hostname = null;
		ParametersImpl params = null;
		ParametersImpl headers = null;
		
		// user info.
		if (find(AT) != -1)
		{
			userName = nextToken(s_userNameSeparators);
			if (m_unescape) {
				userName = Coder.decode(userName);
			}
			if (userName == null || userName.length() == 0) {
				throw new SipParseException("expected user name");
			}

			if (LA(1) == COLON)
			{
				match(COLON);
				password = nextToken(AT);
				if (password.indexOf(COLON) != -1) {
					throw new SipParseException("password contains a colon");
				}
				if (m_unescape) {
					password = Coder.decode(password);
				}
			}

			match(AT);
			
			if (userName.indexOf(POUND) >= 0
				|| userName.indexOf(SEMICOLON) >= 0)
			{
				userType = SipURL.USER_TYPE_PHONE;
			}
			else
			{
				userType = SipURL.USER_TYPE_IP; 
			} 
		}

		boolean isIpv6 = false;
		if (LA() == '[') {
			consume();
			hostname = nextToken(']');
			consume();
			isIpv6 = true;
		}
		else {
			hostname = nextToken(s_hostNameSeparators);
			if (m_unescape) {
				hostname = Coder.decode(hostname);
			}
		}
		if (hostname == null || hostname.length() == 0) {
			throw new SipParseException("missing hostname");
		}
		if (!isIpv6 && !isHostname(hostname)) {
			throw new SipParseException("invalid hostname [" + hostname + ']', "");
		}else{
			//@ TODO - add validator for ipv6 addresses 
		}

		if (LA(1) == COLON)
		{
			match(COLON);
			portNum = number();
		}
		
		lws();
		
		//we need to stop the parsing, the rest of the string is header parameters
		if (m_parseHeaderParameters && ! uriWithBrackets) {
			url = AddressFactoryImpl.createSipURL(scheme,userName,password,
					hostname ,portNum,params, headers,userType,null);
		
			return url;	
		}
        
		// parameters
		if (LA(1) == SEMICOLON) {
			match(SEMICOLON);
			params = parseParametersMap(SEMICOLON, true,true);
		}

		// headers.
		if (LA(1) == QUESTION) {
			match(QUESTION);
			headers = parseParametersMap(AND, true , false);
		}

		url = AddressFactoryImpl.createSipURL(scheme, userName, password,
				hostname, portNum, params, headers, userType,null);
		
		return url;
	}
	
	/**
	 * return the next token as a sip version
	 */
	public String parseSipVersion(char delim) throws SipParseException {
		if (LA(1) == '2' &&
			LA(2) == '.' &&
			LA(3) == '0' &&
			!Character.isDigit(LA(4))) {
			consume(3);
			return "2.0";
		}
		String unknownVersion = nextToken(delim);
		throw new SipParseException("Unknown protocol version", unknownVersion);
	}

	/**
	 * matches the given string to a hostname, per rfc3261 25.1:
	 * hostname = *( domainlabel "." ) toplabel [ "." ]
	 * domainlabel = alphanum / alphanum *( alphanum / "-" ) alphanum
	 * toplabel = ALPHA / ALPHA *( alphanum / "-" ) alphanum
	 * 
	 * this is a simplified version that just checks that all characters
	 * are either alphanum, ".", or "-".
	 * 
	 * @param s string to test
	 * @return true if a valid hostname, false otherwise
	 */
	public static boolean isHostname(String s) {
		int length = s.length();
		for (int i = 0; i < length; i++) {
			char c = s.charAt(i);
			if (!Coder.isAlphanum(c) && c != '.' && c != '-') {
				return false;
			}
		}
		return true;
	}
}
