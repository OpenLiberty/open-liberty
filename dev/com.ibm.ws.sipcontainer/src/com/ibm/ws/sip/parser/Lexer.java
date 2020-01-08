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
package com.ibm.ws.sip.parser;

import jain.protocol.ip.sip.SipParseException;

import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.LRUStringCache;

/** 
 * base class for a string tokenizer
 * 
 * @author Assaf Azaria, Oct 2003.
 * @modified ran Feb 2005
 */
public class Lexer implements Separators
{
    //
    // Members.
    //

    /** String holding the actual buffer */
    private char[] m_buffer;
    
    /** number of chars in buffer */
    private int m_length;
    
    /**
     * The location index.
     */
    private int m_index = 0;
    
    /**
     * Single instance of String cache used by all lexers. 
     */
    private static LRUStringCache m_cache = LRUStringCache.getInstance();
    
    /**
     * Construct an empty lexer (for the pool).
     */
    public Lexer()
    {
    }
    
    /**
     * reset this lexer's state.
     */
    public void rewind()
    {
    	m_index = 0;
    }
    
    /**
     * mark current position before parse attempt.
     * if the following parse attempt fails, rewind(int) restores
     * the position before the failed parse attempt.
     * 
     * @return the current position
     */
    public int mark() {
    	return m_index;
    }
    
    /**
     * rewind back to last known mark
     * @param mark last known mark
     */
    public void rewind(int mark) {
    	m_index = mark;
    }

    //
    // Operations.
    //
    
    /**
	 * sets the string for this lexer to analyze
	 * 
	 * @param src the input stream
	 * @param length input stream size
	 */
	public void setSrc(char[] src, int length) {
		setSrc(src, 0, length);
	}

	/**
	 * sets the string for this lexer to analyze
	 * 
	 * @param src the input stream
	 * @param offset start location
	 * @param length number of chars to parse
	 */
	public void setSrc(char[] src, int offset, int length) {
		m_buffer = src;
		m_length = offset + length;
		rewind(offset);
	}

	/**
     * gets a single character from the internal buffer at a specific location
     * @param index index into the character internal buffer
     * @return the requested char by value
	 * @see com.ibm.ws.sip.parser.Lexer#getChar(int)
	 */
	public char getChar(int index) {
		return m_buffer[index];
	}
	
	/**
     * @return number of chars in the internal buffer
	 * @see com.ibm.ws.sip.parser.Lexer#length()
	 */
	public int length() {
		return m_length;
	}
	
    /**
	 * Get the next token 'til the given delimiter or the end of the string.
	 * 
	 * @return
	 */
	public String nextToken(char delim) {
		// left trim
		lws();

		// get token
		int start = m_index;
		int end = -1;
		int length = length();

		for (; m_index < length; m_index++) {
			char c = LA();
			if (c == delim) {
				break;
			}
			if (c != ' ')
				end = m_index; // last non-whitespace (right trim)
		}

		int len = end == -1 ? 0 : end - start + 1;
		return m_cache.get(m_buffer, start, len);
	}
	
	/**
	 * Parse a quoted string (without the quotes).
	 * 
	 * @return the string (unquoted).
	 */
	public String quotedString() throws SipParseException {
		match(DOUBLE_QUOTE);

		// find unescaped double-quotes
		boolean escaped = false;
		int i;

		for (i = m_index; i < m_length; i++) {
			char c = m_buffer[i];
			if (c == '"' && !escaped) {
				break;
			}
			escaped = !escaped && c == '\\';
		}

		int len = i - m_index;
		String s = m_cache.get(m_buffer, m_index, len);
		s = unescapeQuotedString(s);
		m_index = i+1; // skip quote
		return s;
	}

	/**
	 * Transforms the given string to unescaped form. Normal characters are
	 * not changed. "Quoted pairs" are unescaped, per 3261-25.1:
	 * quoted-pair = "\" (%x00-09 / %x0B-0C / %x0E-7F)
	 * 
	 * @param s The quoted string, without the opening and closing quotes.
	 * @return The string, unescaped.
	 */
	public static String unescapeQuotedString(String s) {
		// instantiate a new string only if at least one quoted-pair
		StringBuilder b = null; 

		int length = s.length();
		for (int i = 0; i < length; i++) {
			int quoted = -1; // value of the quoted-pair character
			char c1 = s.charAt(i);
			if (c1 == '\\' && i < length-1) {
				// looks like quoted-pair
				char c2 = s.charAt(i+1);
				if (
					(0x0E <= c2 && c2 <= 0x7F) ||
					(0x0B <= c2 && c2 <= 0x0C) ||
					(0x00 <= c2 && c2 <= 0x09))
				{
					// yes, it's a quoted-pair
					quoted = c2;
				}
			}
			if (quoted != -1) {
				if (b == null) {
					// this is the first quoted-pair character
					b = new StringBuilder(64);
					if (i > 0) {
						b.append(s, 0, i);
					}
				}
				b.append((char)quoted);
				i++; // skip the quoted-pair
			}
			else if (b != null) {
				b.append(c1);
			}
		}
		return b == null
			? s
			: b.toString();
	}

	/**
	 * writes a string to the buffer, using the escaping rules of a
	 * quoted-string, but without the surrounding quotes
	 * @param string the source string that requires escaping
	 * @param buffer destination buffer
	 */
	public static void writeNoQuotes(CharSequence string, CharsBuffer buffer) {
		int length = string.length();
		for (int i = 0; i < length; i++) {
			char c = string.charAt(i);
			switch (c) {
			case '\r':
			case '\n':
				// replace (an illegal) CR/LF with a whitespace
				c = ' ';
				break;
			default:
				if (needEscaping(c)) {
					// escape special character as a quoted pair
					buffer.append('\\');
				}
			}
			buffer.append(c);
		}
	}

	/**
	 * determines if the given char needs to be escaped in a quoted-string,
	 * per 3261-25.1:
	 * "In quoted strings, quotation marks (") and backslashes (\) need to be escaped."
	 * 
	 * in addition to that, any non-printable character (%x00-19, %x7f)
	 * must be escaped
	 * 
	 * @param c char to write
	 * @return true if needs escaping, false if not
	 */
	public static boolean needEscaping(char c) {
		return c == '"' || c == '\\' || (0 <= c && c <= 0x19) || c == 0x7F;
	}

    /**
	 * Get the next token 'til the first of the given set of delimiters or the
	 * end of the string.
	 * 
	 * @return
	 */
    public String nextToken(char[] delims)
    {
		// left trim
		lws();

		// get token
		int start = m_index;
		int end = start;
		int length = length();

		for (; m_index < length; m_index++) {
			char c = LA();
			if (delimited(c, delims)) {
				break;
			}
			if (c != ' ')
				end = m_index+1; // last non-whitespace (right trim)
		}

		int len = end - start;
		return m_cache.get(m_buffer, start, len);
    }
    
    /**
     * tests if given char is one of the listed delimiters
     * @param c char to test
     * @param delims list of delimiters
     * @return true if given char is listed, otherwise false
     */
    private static boolean delimited(char c, char[] delims) {
        for (int i = 0; i < delims.length; i++) {
            if (c == delims[i]) {
           		return true;
           	} 
        }
        return false;
    }

    /**
    * Look ahead to the next character in the buffer.
    */
    public char LA()
    {
        return LA(1);
    }

    /**
     * Look ahead to the k character in the buffer.
     * 
     * @param k The number of characters to look ahead to.
     */
    public char LA(int k)
    {
        if (m_index + k-1 < length())
        {
            return getChar(m_index + k-1);
        }
        else
        {
            return Separators.ENDL;
        }

    }

    /**
     * Consume a character.
     */
    public void consume()
    {
        consume(1);
    }

    /**
     * Consume a bunch of characters.
     * @param k
     */
    public void consume(int k)
    {
        m_index += k;
    }

    /**
     * match the given character. 
     * 
     * @param chr the charachter to match.
     * @throws SipParseException if the character was not matched.
     */
    public void match(char chr) throws SipParseException
    {
        if (LA() == chr)
        {
            consume();
        }
        else
        {
        	StringBuffer buf = new StringBuffer("Expected [");
			buf.append(chr).append("] but got ");
			
			if (m_index < length()) {
				buf.append('[');
				buf.append(getChar(m_index));
				buf.append(']');
			}
			else {
				buf.append("index out of buffer");
			}
			throw new SipParseException(buf.toString(), "");
        }
    }

    /**
     * match the given string. 
     * 
     * @param str the string to match.
     * @throws SipParseException if the string was not matched.
     */
    public void match(String str) throws SipParseException
    {
        int orig = m_index;
        int length = str.length();
        
        for (int i = 0; i < length; i++)
        {
            if (LA() != str.charAt(i))
            {
                StringBuffer buf = new StringBuffer("Expected [");
				buf.append(str);
				buf.append("] but got [");
				buf.append(getChar(m_index));
				buf.append("] on [");
				buf.append(m_index);
				buf.append("] char");
            	m_index = orig;
                throw new SipParseException(buf.toString(), "");
            }
            consume();
        }
    }

    /**
     * Get the next number.
     * 
     * @throws SipParseException if there's no number on the buffer.
     */
    public int number() throws SipParseException
    {
    	char digit = LA();
    	if (!Character.isDigit(digit)) {
    		throw new SipParseException("Expected a number but got ["
    			+ digit + ']', "");
    	}
    	
    	int result = 0;
    	do {
    		consume();
    		int d = Character.digit(digit, 10);
    		int check = result;
    		result *= 10;
    		result += d;
    		if (result < check) {
    			// rolled back to 0
    			throw new SipParseException("Number too big");
    		}
    		digit = LA();
    	} while (Character.isDigit(digit));
    	
    	return result;
    }

    /**
     * Get the next long number.
     * @throws SipParseException if there's no number on the buffer.
     */
    public long longNumber() throws SipParseException
    {
    	char digit = LA();
    	if (!Character.isDigit(digit)) {
    		throw new SipParseException("Expected a number but got ["
       			+ digit + ']', "");
    	}
    	
    	long result = 0;
    	do {
    		consume();
    		int d = Character.digit(digit, 10);
    		long check = result;
    		result *= 10;
    		result += d;
    		if (result < check) {
    			// rolled back to 0
    			throw new SipParseException("Number too big");
    		}
    		digit = LA();
    	} while (Character.isDigit(digit));
    	
    	return result;
    }

    /**
     * Get the next short number.
     * @throws SipParseException if there's no number on the buffer.
     */
    public short shortNumber() throws SipParseException
    {
    	char digit = LA();
    	if (!Character.isDigit(digit)) {
    		throw new SipParseException("Expected a number but got ["
       			+ digit + ']', "");
    	}
    	
    	short result = 0;
    	do {
    		consume();
    		int d = Character.digit(digit, 10);
    		short check = result;
    		result *= 10;
    		result += d;
    		if (result < check) {
    			// rolled back to 0
    			throw new SipParseException("Number too big");
    		}
    		digit = LA();
    	} while (Character.isDigit(digit));
    	
    	return result;
    }

    /**
     * Linear white space.
     * LWS  =  [*WSP CRLF] 1*WSP ; linear whitespace
     */
    public void lws()
    {
        int n = 0;
        int lastWhitespace = 0;

        while (true) {
            char c = LA(n+1);

            switch (c) {
            case ' ': case '\t':
                lastWhitespace = ++n;
                break;
            case '\r': case '\n':
                n++;
                break;
            default:
                consume(lastWhitespace);
                return;
            }
        }
    }

    /**
     * match and return a sip token.
     * 
     * token       =  1*(alphanum / "-" / "." / "!" / "%" / "*"
     *                / "_" / "+" / "`" / "'" / "~" )
     *
     * @return
     */
    public String sipToken()
    {
        int offset = m_index; 
        while (LA() != ENDL)
        {
            char chr = LA();
            if (Character.isLetterOrDigit(chr)
                || chr == DOT
                || chr == PLUS
                || chr == EXCLAMATION
                || chr == TILDA
                || chr == PERCENT
                || chr == STAR
                || chr == MINUS
                || chr == UNDERSCORE
                || chr == APOSTROPHE
                || chr == '`')
            {
                consume();
            }
            else
            {
                break;
            }
        }
        
        return m_cache.get(m_buffer, offset, m_index - offset);
    }

    /**
     * match and return a sip word.
     * 
     * word        =  1*(alphanum / "-" / "." / "!" / "%" / "*" /
     *                "_" / "+" / "`" / "'" / "~" /
     *                "(" / ")" / "<" / ">" /
     *                ":" / "\" / DQUOTE /
     *                "/" / "[" / "]" / "?" /
     *                "{" / "}" )
     * @return
     */
    public String sipWord()
    {
        int offset = m_index; 
        while (LA() != ENDL)
        {
            char chr = LA();
            if (Character.isLetterOrDigit(chr)
                || chr == DOT
                || chr == PLUS
                || chr == EXCLAMATION
                || chr == TILDA
                || chr == PERCENT
                || chr == STAR
                || chr == MINUS
                || chr == UNDERSCORE
                || chr == LPAREN
                || chr == RPAREN
                || chr == LESS_THAN
                || chr == GREATER_THAN
                || chr == COLON
                || chr == '\\'
                || chr == DOUBLE_QUOTE
                || chr == SLASH
                || chr == '['
                || chr == ']'
                || chr == QUESTION
                || chr == '{'
                || chr == '}')
            {
                consume();
            }
            else
            {
                break;
            }
        }
        return m_cache.get(m_buffer, offset, m_index - offset);
    }
    
	/**
     * match and return a non-quoted sip parameter value.
     * 
     * TODO: currently this is called for either URI parameters or non-quoted
     * header parameters. need to separate to two different calls, because URI
     * params may contain characters that are not allowed in header params.
     * 
     * @return the matched parameter
     */
    public String sipParam()
    {
		int offset = m_index; 
        while (LA() != ENDL)
		{
			char chr = LA();
			if (Character.isLetterOrDigit(chr)
				|| chr == DOT
				|| chr == PLUS
				|| chr == EXCLAMATION
				|| chr == TILDA
				|| chr == AT
				|| chr == PERCENT
				|| chr == STAR
				|| chr == MINUS
				|| chr == UNDERSCORE
				|| chr == LPAREN
				|| chr == RPAREN
				|| chr == COLON
				|| chr == '\\'
				|| chr == DOUBLE_QUOTE
				|| chr == SLASH
				|| chr == '['
				|| chr == ']'
				|| chr == APOSTROPHE
				|| chr == '`'
				|| chr == '{'
				|| chr == '}')
			{
				consume();
			}
			else
			{
				break;
			}
		}
		return m_cache.get(m_buffer, offset, m_index - offset);
	}

    /**
     * match and return a 1*paramchar for URI (used for parameter names and values in SIP URI- - RFC 3261 25.1
     * 
     * @return the matched parameter
     */
    public String sipUriParam() {
    	int offset = m_index;
    	while(LA() != ENDL) {
    		char chr = LA();
    		if(Coder.isParamUnreserved(chr) || Coder.isUnreserved(chr) || chr == '%' ) {
    			consume();
    		} 
    		else {
    			break;
    		}
    	}
    	return m_cache.get(m_buffer, offset, m_index - offset);
    }
    /**
	 * Find the given character and return its index.
	 */
    public int find(char chr) 
    {
        for (int i = 1; LA(i) != ENDL; i++)
        {
        	if (LA(i) == chr)
        	{
        		return i;
        	}
        }
        
        return -1;
    }
    
    /**
     * skips the next token. this is the same as calling:
     * nextToken(chr)
     * match(chr)
     * but nothing is returned (and nothing is allocated)
     * @param chr the delimiter to skip to
     */
    public void skip(char chr) {
    	int pos = find(chr);
    	if (pos > 0) {
    		consume(pos);
    	}
    }

	/**
	 * converts this lexer to a string
	 * @return string containing the buffer contents of this lexer
	 */
	public String toString() {
		
	    return m_cache.get(m_buffer, 0, m_length);
	}
}
