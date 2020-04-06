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

import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;

/**
 * stateless class that encodes/decodes SIP constructs
 * based on the rules defined in rfc3261 25.1
 * 
 * @author ran
 */
public class Coder
{
	/** prevent re-escaping of pre-escaped parameters (false by default) */
	private static final boolean s_detectPreEscapedParams =
		SIPTransactionStack.instance().getConfiguration().detectPreEscapedParams();
	
	/**
	 * tests whether given character is "reserved" per rfc3261 25.1:
	 * reserved = ";" / "/" / "?" / ":" / "@" / "&" / "=" / "+"
	 *            / "$" / ","
	 * 
	 * @param c character to test
	 * @return true if reserved
	 */
	public static boolean isReserved(char c) {
		switch (c) {
		case ';':
		case '/':
		case '?':
		case ':':
		case '@':
		case '&':
		case '=':
		case '+':
		case '$':
		case ',':
			return true;
		default:
			return false;
		}
	}

	/**
	 * tests whether given character is "unreserved" per rfc3261 25.1:
	 * unreserved = alphanum / mark
	 * 
	 * @param c character to test
	 * @return true if unreserved
	 */
	public static boolean isUnreserved(char c) {
		return isAlphanum(c) || isMark(c);
	}

	/**
	 * tests whether given character is "alpha" per rfc 2234:
	 * ALPHA = %x41-5A / %x61-7A   ; A-Z / a-z
	 * 
	 * @param c character to test
	 * @return true if it's "alpha"
	 */
	public static boolean isAlpha(char c) {
		return ('A' <= c && c <= 'Z')
			|| ('a' <= c && c <= 'z');
	}

	/**
	 * tests whether given character is alpha-numeric per rfc3261 25.1:
	 * alphanum = ALPHA / DIGIT
	 * 
	 * @param c character to test
	 * @return true if it's alpha-numeric
	 */
	public static boolean isAlphanum(char c) {
		return isAlpha(c) || isDigit(c);
	}

	/**
	 * tests whether given character is a "mark" per rfc3261 25.1:
	 * mark = "-" / "_" / "." / "!" / "~" / "*" / "'"
	 *        / "(" / ")"
	 * 
	 * @param c character to test
	 * @return true if it's a mark
	 */
	public static boolean isMark(char c) {
		switch (c) {
		case '-':
		case '_':
		case '.':
		case '!':
		case '~':
		case '*':
		case '\'':
		case '(':
		case ')':
			return true;
		default:
			return false;
		}
	}

	/**
	 * tests whether given character is "user-unreserved" per rfc3261 25.1:
	 * user-unreserved = "&" / "=" / "+" / "$" / "," / ";" / "?" / "/"
	 * 
	 * @param c character to test
	 * @return true if user-unreserved
	 */
	public static boolean isUserUnreserved(char c) {
		switch (c) {
		case '&':
		case '=':
		case '+':
		case '$':
		case ',':
		case ';':
		case '?':
		case '/':
			return true;
		default:
			return false;
		}
	}

	/**
	 * tests whether given character is unreserved for a password.
	 * password = *( unreserved / escaped / "&" / "=" / "+" / "$" / "," )
	 * 
	 * @param c character to test
	 * @return true if unreserved for a password
	 */
	public static boolean isPasswordUnreserved(char c) {
		switch (c) {
		case '&':
		case '=':
		case '+':
		case '$':
		case ',':
			return true;
		default:
			return false;
		}
	}

	/**
	 * tests whether given character is "param-unreserved" per rfc3261 25.1:
	 * param-unreserved = "[" / "]" / "/" / ":" / "&" / "+" / "$"
	 * 
	 * @param c character to test
	 * @return true if param-unreserved
	 */
	public static boolean isParamUnreserved(char c) {
		switch (c) {
		case '[':
		case ']':
		case '/':
		case ':':
		case '&':
		case '+':
		case '$':
			return true;
		default:
			return false;
		}
	}

	/**
	 * tests whether given character is a control character per rfc2396 2.4.3:
	 * control = <US-ASCII coded characters 00-1F and 7F hexadecimal>
	 *  
	 * @param c character to test
	 * @return true if it's a control character
	 */
	public static boolean isControl(char c) {
		return 0 <= c && c <= 0x1f;
	}

	/**
	 * tests whether given character is the space character per rfc2396 2.4.3:
	 * space = <US-ASCII coded character 20 hexadecimal>
	 *  
	 * @param c character to test
	 * @return true if it's the whitespace character
	 */
	public static boolean isSpace(char c) {
		return c == ' ';
	}

	/**
	 * tests whether given character is a delimiter character per rfc2396 2.4.3:
	 * delims = "<" | ">" | "#" | "%" | <">
	 *  
	 * @param c character to test
	 * @return true if it's a delimiter
	 */
	public static boolean isDelim(char c) {
		switch (c) {
		case '<':
		case '>':
		case '#':
		case '%':
		case '"':
			return true;
		default:
			return false;
		}
	}

	/**
	 * tests whether given character is an unwise character per rfc2396 2.4.3:
	 * unwise = "{" | "}" | "|" | "\" | "^" | "[" | "]" | "`"
	 *  
	 * @param c character to test
	 * @return true if it's an unwise character
	 */
	public static boolean isUnwise(char c) {
		switch (c) {
		case '{':
		case '}':
		case '|':
		case '\\':
		case '^':
		case '[':
		case ']':
		case '`':
			return true;
		default:
			return false;
		}
	}

	/**
	 * tests whether given character is "Excluded US-ASCII Character"
	 * per rfc2396 2.4.3
	 *  
	 * @param c character to test
	 * @return true if excluded
	 */
	public static boolean isExcluded(char c) {
		return isControl(c) || isSpace(c) || isDelim(c) || isUnwise(c);
	}
	
	/**
	 * tests whether given character is "unescaped Tel Uri character" 
	 * true if "UNESCAPE_HASH_CHAR_IN_TELURI" custom property is set for true.
	 * For now only "#" character as parameter will return true.
	 * More characters can be added to this function if we find that they don't have to be escaped according to RFC3966
	 *  
	 * @param c character to test
	 * @return true if in the list of UNESCAPED_TEL_CHARACTERS custom property
	 */
	public static boolean isUnescapedTelChars(char c) {
		/* The boolean value of the  "UNESCAPE_HASH_CHAR_IN_TELURI" custom property */
		Boolean isUnescapeHashChar = ApplicationProperties.getProperties().getBoolean(com.ibm.ws.sip.properties.StackProperties.UNESCAPE_HASH_CHAR_IN_TELURI);
		
		if (isUnescapeHashChar && c=='#') {
			return true;
		}
		return false;  
	}
	
	/**
	 * tests whether given character is a digit per rfc2234 6.1:
	 * DIGIT = %x30-39  ; 0-9
	 * 
	 * @param c character to test
	 * @return true if a digit
	 */
	public static boolean isDigit(char c) {
		return '0' <= c && c <= '9';
	}

	/**
	 * tests whether given character is a hexadecimal digit per rfc2234 6.1:
	 * HEXDIG = DIGIT / "A" / "B" / "C" / "D" / "E" / "F"
	 *  
	 * @param c character to test
	 * @return true if a hex digit
	 */
	public static boolean isHexdig(char c) {
		if (isDigit(c)) {
			return true;
		}
		return ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
	}

	/**
	 * tests whether given character is a word char per rfc3261 25.1:
	 * word = 1*(alphanum / "-" / "." / "!" / "%" / "*" / "_" / "+" / "`" /
	 *        "'" / "~" / "(" / ")" / "<" / ">" / ":" / "\" / DQUOTE / "/" /
	 *        "[" / "]" / "?" / "{" / "}" )
	 *  
	 * @param c character to test
	 * @return true if a word char
	 */
	public static boolean isWord(char c) {
		if (isAlphanum(c)) {
			return true;
		}
		switch (c) {
		case '-': case '.': case '!': case '%': case '*': case '_': case '+':
		case '`': case '\'': case '~': case '(': case ')': case '<': case '>':
		case ':': case '\\': case '"': case '/': case '[': case ']': case '?':
		case '{': case '}':
			return true;
		default:
			return false;
		}
	}

	/**
	 * matches the given string to a Call-ID, per rfc3261 25.1:
	 * callid = word [ "@" word ]
	 * @param s string to test
	 * @return true if a valid Call-ID, false otherwise
	 */
	public static boolean isCallId(String s) {
		int length = s.length();
		boolean at = false;
		for (int i = 0; i < length; i++) {
			char c = s.charAt(i);
			if (!Coder.isWord(c)) {
				if (c == '@' && !at && 0 < i && i < length-1) {
					// allow one @ sign in the middle
					at = true;
					continue;
				}
				return false;
			}
		}
		return true;
	}

	/**
	 * escapes a single character
	 * @param char unescaped char
	 * @param buf the outbound buffer
	 */
	public static void escape(char c, CharsBuffer buf) {
		int i1 = (c & 0xf0) >> 4;
		int i2 = c & 0xf;
		char c1 = Character.forDigit(i1, 16);
		char c2 = Character.forDigit(i2, 16);
		buf.append('%');
		// According to RFC 3986, the percent encoded uris should be normalized to use uppercase letter.
		buf.append(Character.toUpperCase(c1));
		buf.append(Character.toUpperCase(c2));
	}

    /**
     * encodes a general string, escaping characters where necessary
	 * assumption: an unescaped char is not part of a multi-byte char.
	 * 
	 * @param data unescaped data, as defined in rfc2396
     * @param buf the outbound buffer
     */
    public static void encode(String data, CharsBuffer buf) {
		if (data == null) {
			return;
		}
		int len = data.length();
		for (int i = 0; i < len; i++) {
			char c = data.charAt(i);
			if (isExcluded(c)) {
				escape(c, buf);
			}
			else {
				// no need to escape.
				// note that we do not check if it's already escaped,
				// so it's the caller's responsibility not to encode twice
				buf.append(c);
			}
		}
    }
    
    /**
     * encodes a TelUri string, escaping characters where necessary
	 * assumption: an unescaped char is not part of a multi-byte char.
	 * 
	 * @param data unescaped data, as defined in rfc3966
     * @param buf the outbound buffer
     */
    public static void encodeTelURI(String data, CharsBuffer buf) {
		if (data == null) {
			return;
		}
		int len = data.length();
		for (int i = 0; i < len; i++) {
			char c = data.charAt(i);
			/* if 'c' has to be excluded and isn't a special character of tel-uri that doesn't need to be escaped */
			if (isExcluded(c) && !isUnescapedTelChars(c)) {
				escape(c, buf);
			}
			else {
				// no need to escape.
				// note that we do not check if it's already escaped,
				// so it's the caller's responsibility not to encode twice
				buf.append(c);
			}
		}
    }
    
	/**
	 * encodes the user part, escaping characters where necessary
	 * @param data unescaped string
	 * @param buf the outbound buffer
	 */
	public static void encodeUser(String data, CharsBuffer buf) {
		int len = data.length();
		for (int i = 0; i < len; i++) {
			char c = data.charAt(i);
			if (isUnreserved(c) || isUserUnreserved(c)) {
				// no need to escape.
				// note that we do not check if it's already escaped,
				// so it's the caller's responsibility not to encode twice
				buf.append(c);
			}
			else {
				escape(c, buf);
			}
		}
	}

	/**
	 * encodes the password, escaping characters where necessary
	 * @param data unescaped string
	 * @param buf the outbound buffer
	 */
	public static void encodePassword(String data, CharsBuffer buf) {
		int len = data.length();
		for (int i = 0; i < len; i++) {
			char c = data.charAt(i);
			if (isUnreserved(c) || isPasswordUnreserved(c)) {
				// no need to escape.
				// note that we do not check if it's already escaped,
				// so it's the caller's responsibility not to encode twice
				buf.append(c);
			}
			else {
				escape(c, buf);
			}
		}
	}

	/**
	 * encodes the parameter value, escaping characters where necessary
	 * @param data unescaped string
	 * @param buf the outbound buffer
	 */
	public static void encodeParam(String data, CharsBuffer buf) {
		int len = data.length();
		for (int i = 0; i < len; i++) {
			char c = data.charAt(i);
			boolean esc;
			if (s_detectPreEscapedParams &&
				c == '%' &&
				i < len-2 &&
				isHexdig(data.charAt(i+1)) &&
				isHexdig(data.charAt(i+2)))
			{
				// special handling of pre-escaped characters:
				// the application has already escaped the parameter,
				// and specified (in config) that the stack should
				// not re-escape the "%" sign
				esc = false;
			}
			else if (isUnreserved(c) || isParamUnreserved(c)) {
				esc = false;
				// no need to escape.
				// note that we do not check if it's already escaped,
				// so it's the caller's responsibility not to encode twice
			}
			else {
				esc = true;
			}
			if (esc) {
				escape(c, buf);
			}
			else {
				buf.append(c);
			}
		}
	}

	/**
	 * decodes a URI component or parameter, converting from escaped to unescaped.
	 * assumption: an escaped char is not part of a multi-byte char.
	 * @param component escaped component, as defined in rfc2396
	 * @return the unescaped component, for application usage
	 * @throws SipParseException if bumped into illegal escaping
	 */
	public static String decode(String component) throws SipParseException {
		if (component == null) {
			return null;
		}
		int len = component.length();
		CharsBuffer buf = null;
		
		for (int i = 0; i < len; i++) {
			char c = component.charAt(i);
			if (c == '%') {
				// found escaped character.
				// next 2 chars represent the value of the escaped char
				if (i >= len-2) {
					throw new SipParseException("Illegal escaping - "
						+ "expected 2 hex digits, found end of component",
						component);
				}
				char c1 = component.charAt(++i);
				char c2 = component.charAt(++i);
				int i1 = Character.digit(c1, 16);
				int i2 = Character.digit(c2, 16);
				if (i1 < 0 || i1 > 15 || i2 < 0 || i2 > 15) {
					// illegal escaping
					throw new SipParseException("Illegal escaping - "
						+ "expected 2 hex digits, found some other character",
						component);
				}
				char escaped = (char)((i1 << 4) | i2);
				if (buf == null) {
					// this escaped char is the first one. initialize buffer.
					buf = CharsBuffersPool.getBuffer();
					buf.write(component, 0, i-2);
				}
				buf.append(escaped);
			}
			else if (buf != null) {
				buf.append(c);
			}
		}
		if (buf == null) {
			return component; // was not escaped
		}
		String result = buf.toString();
		CharsBuffersPool.putBufferBack(buf);
		return result;
	}
	
	/**
     * Checks if the given character is a token char, according to 3261-25.1:
     * token = 1*(alphanum / "-" / "." / "!" / "%" / "*" / "_" / "+" / "`" / "'" / "~" )
     * @param c The character to check.
     * @return true if a token char, false otherwise.
     */
    public static boolean isTokenChar(char c) {
    	if (isAlphanum(c)) {
    		return true;
    	}
		switch (c) {
		case '-':
		case '.':
		case '!':
		case '%':
		case '*':
		case '_':
		case '+':
		case '`':
		case '\'':
		case '~':
			return true;
		default:
			return false;
		}
    }
    
    /**
     * Checks if the given string is a token, according to 3261-25.1:
     * token = 1*(alphanum / "-" / "." / "!" / "%" / "*" / "_" / "+" / "`" / "'" / "~" )
     * @param s The string to check.
     * @return true if a token, false otherwise.
     */
    public static boolean isToken(CharSequence s) {
    	int length = s.length();
    	for (int i = 0; i < length; i++) {
    		char c = s.charAt(i);
    		if (!isTokenChar(c)) {
    			return false;
    		}
    	}
    	return true;
    }


}
