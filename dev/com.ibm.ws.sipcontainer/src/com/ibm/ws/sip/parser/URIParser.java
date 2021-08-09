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
import jain.protocol.ip.sip.address.URI;

/**
 * A parser for URI's.
 * 
 * @author Assaf Azaria
 */
public class URIParser extends SipParser implements Separators
{
	/**
	 * constructor
	 * @param unescape should the parser unescape escaped characters
	 */
	public URIParser(boolean unescape) {
		super(unescape);
	}
    
	/**
	 * no-arg constructor, defaults to parsing URIs in incoming network messages
	 */
	public URIParser() {
		this(true);
	}
    
    /**
     * Parse the uri string into a jain URI object.
     * 
     * @param uriToParse the raw uri string.
     * @throws SipParseException
     */
    public URI parse(String uriToParse) throws SipParseException
    {
    	return parse(uriToParse.toCharArray(), 0, uriToParse.length());
    }
	
    /**
     * parses the uri string into a jain URI object.
     * 
     * @param buffer raw uri string
     * @param offset buffer position to start reading from
     * @param length raw uri string size
     * @return a new URI
     * @throws SipParseException
     */
    public URI parse(char[] buffer, int offset, int length)
    	throws SipParseException
    {
    	setSrc(buffer, offset, length);
        URI toRet = parseURI();
    	rewind();
    	return toRet;
    }
	
    /**
     * Parse the uri string into a jain URI object.
     * 
     * @param <var>scheme</var> scheme
     * @param <var>schemeData</var> scheme data
     * @throws SipParseException if scheme or schemeData is not accepted by 
     * implementation
     */
    public URI parse(String scheme, String schemeData)
        throws SipParseException
    {
    	
        char[] buffer = schemeData.toCharArray();
        int length = schemeData.length();
        setSrc(buffer, length);
        
        boolean uriWithBrackets = buffer[length-1]== '>';
        
		URI toRet = parseURI(scheme, uriWithBrackets );
        int position = mark();
        //the expresion was wrongly parsed if the position isn't at the end of the string or 
        //if there are brackets and the position is on > which is the last char
        if ((!uriWithBrackets && (position < length)) || (uriWithBrackets&&(position < length -1))) {
            throw new SipParseException("Expected end-of-string in ["
            	+ schemeData + "] at position [" + position + ']',
                schemeData);
        }
        rewind();
        return toRet;
    }
	
	/**
	 * Parse the uri sstring into a jain Name Address object
	 * 
	 * @param uriToParse the name address to parse
	 * @throws SipParseException
	 */
	public NameAddress parseNameAddress(String uriToParse) throws SipParseException
	{
    	setSrc(uriToParse.toCharArray(), uriToParse.length());
		NameAddress nameAddr = super.parseNameAddress();
    	rewind();
		return nameAddr;
	}    
}
