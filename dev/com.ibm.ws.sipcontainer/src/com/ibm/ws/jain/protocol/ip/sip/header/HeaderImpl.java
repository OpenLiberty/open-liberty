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
package com.ibm.ws.jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.CallIdHeader;
import jain.protocol.ip.sip.header.ContactHeader;
import jain.protocol.ip.sip.header.ContentEncodingHeader;
import jain.protocol.ip.sip.header.ContentLengthHeader;
import jain.protocol.ip.sip.header.ContentTypeHeader;
import jain.protocol.ip.sip.header.FromHeader;
import jain.protocol.ip.sip.header.Header;
import jain.protocol.ip.sip.header.SubjectHeader;
import jain.protocol.ip.sip.header.ToHeader;
import jain.protocol.ip.sip.header.ViaHeader;

import java.util.HashSet;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.extensions.SupportedHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.simple.AllowEventsHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.simple.EventHeader;
import com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl.HeaderForm;
import com.ibm.ws.sip.parser.CharArray;
import com.ibm.ws.sip.parser.Separators;
import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;
import com.ibm.ws.sip.parser.util.ObjectPool;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;

/**
* A Generic Header.
* 
* @author Assaf Azaria, Mar 2003. 
*/
public abstract class HeaderImpl implements Header, Separators
{
    /** Serialization UID (do not change) */
    private static final long serialVersionUID = -4001774780461425221L;
    
    /** class Logger */
	private static final LogMgr s_logger = Log.get(HeaderImpl.class);

	/**
	 * The parsers pool.
	 */
	private static ObjectPool s_parsersPool = new ObjectPool(SipParser.class);
	
    //
    // Members. 
    //
    /**
     * The header's value before parsing.
     * A header is always in 1 of 2 states: unparsed or parsed.
     * The m_value member indicates this state.
     * If m_value is not null, the header is not parsed. In this case,
     * getValue() simply returns m_value as a String object.
     * If m_value is null, the header is parsed.
     * in this case, getValue() calls the derived encodeValue() method
     * to serialize the header value components into one String object.
     */
    private CharArray m_value;
    
	/**
	 * Construct a new HeaderImpl object.
	 */
	public HeaderImpl() {
		this(null);
	}
	
    /**
     * Construct a new HeaderImpl object.
     * 
     * @param value char array containing header's value.
     */
    public HeaderImpl(CharArray value) {
        m_value = value;
    }

	/**
	 * Set the value for this header.
	 * 
	 * @param value the Header's value. 
	 */
    public void setValue(String value)
        throws IllegalArgumentException, SipParseException
    {
        if (value == null) {
        	throw new IllegalArgumentException("Header: null value"); 
        }
        CharArray newValue = CharArray.getFromPool(value);
        setValue(newValue);
    }
    
	/**
	 * Set the value for this header.
	 * declared final since called from constructor 
	 * 
	 * @param value the Header's value. may be null.
	 */
    public final void setValue(CharArray value) {
        if (m_value != null) {
        	m_value.returnToPool();
        }
        m_value = value;
    }
    
	/**
	 * Get the name of this header. 
     */
    public abstract String getName();
    
	/**
	 * Get the name of this header
	 * 
	 *  @param isUseCompactHeaders - flag indicating whether to return 
	 *  the compact form of the header
     */
    public String getName(boolean isUseCompactHeaders){
    	return getName();
    }
    
	/**
	 * Get the header's value.
	 * Subclasses should NOT overide this method. They should overide
	 * encodeValue(). 
	 */
    public final String getValue()
    {
   		if (m_value == null) {
   			// parsed
   			return encodeValue();
   		}
   		else {
   			// not parsed
   			int length = m_value.getLength();
   			return length > 0
   				? String.valueOf(m_value.getArray(), 0, length)
   				: "";
   		}
    }
    
    /**
     * Get the encoded value of this header as a string
     */
    protected final String encodeValue()
    {
    	CharsBuffer writer = CharsBuffersPool.getBuffer();
        encodeValue(writer);
        
        String value = writer.toString();
        CharsBuffersPool.putBufferBack(writer);
        
        return value; 
    }
    
    /**
     * Dumps the encoded value of this header into the specified writer. 
     * This should be overriden by subclasses.
     */
    protected void encodeValue(CharsBuffer writer)
    {
    	if (m_value == null) {
    		// todo, is this an error?
    		return;
    	}
    	int length = m_value.getLength();
    	if (length > 0) {
    		writer.append(m_value.getArray(), length);
    	}
    }

	/**
	 * determines if this type of header supports compact form.
	 * headers that support compact form must override this.
	 * @return true if compact form supported, false otherwise.
	 */
	public boolean isCompactFormSupported() {
		return false;
	}

	/**
	 * determines if the application created this header in compact form.
	 * headers that support compact form must override this.
	 * @return true if created by the application using the compact form
	 *  header name, false otherwise.
	 */
	public boolean isCompactForm() {
		return false;
	}

    /**
     * Gets string representation of Header
     * @return string representation of Header
     */
    public String toString()
    {
        CharsBuffer buffer = CharsBuffersPool.getBuffer();
        writeNameToCharBuffer(buffer, HeaderForm.DEFAULT);
        writeValueToCharBuffer(buffer, false);
        
        String value = buffer.toString();
        CharsBuffersPool.putBufferBack(buffer);
        
        return value;
    }

    /**
     * Writes the header field name to the specified char buffer.  
     * @param buffer Destination buffer.
     * @param headerForm compact or full form, as requested by the transport,
     *  or by the application.
     */
    public void writeNameToCharBuffer(CharsBuffer buffer, HeaderForm headerForm) {
    	//get the header name in normal/compact form
    	boolean compact;
    	switch (headerForm) {
    	case COMPACT:
    		compact = true;
    		break;
    	case LONG:
    		compact = false;
    		break;
    	case DEFAULT:
    	default:
    		compact = isCompactForm();
    		break;
    	}

    	String name = getName(compact);
        buffer.append(name);
        buffer.append(COLON);
        //if we're in compact mode, we should better skip the space as well
        //otherwise, we leave it, for readability
        if (!compact){
            buffer.append(SP);
        }
    }

    /**
     * Writes the header value to the specified char buffer.  
     * @param buffer Destination buffer.
     * @param network true if the message is going to the network, false if
     *  it's just going to the log
     */
    public void writeValueToCharBuffer(CharsBuffer buffer, boolean network) {
        // before writing a header to the log file, check if configuration
        // specifies that its value should be hidden
        boolean hide;
        if (network) {
        	hide = false;
        }
        else {
        	HashSet<String> hiddenHeaders = SIPTransactionStack.instance().getConfiguration().getHiddenHeaders();
        	String fullFormName = getName();
        	hide = hiddenHeaders.contains(fullFormName);
        }
        if (hide) {
        	buffer.append("<hidden value>");
        }
        else {
        	writeValueToCharBuffer(buffer);
        }
    }
    
    /**
     * Write the header value part to the specified Char buffer
     * @param charWrite
     */
    public final void writeValueToCharBuffer(CharsBuffer buffer)
    {
        if (m_value == null) {
   			encodeValue(buffer);
   		}
   		else {
   			int length = m_value.getLength();
   			if (length > 0) {
   				buffer.append(m_value.getArray(), length);
   			}
   		}
    }
    
	/**
	 * Gets the hashcode for all headers. 
	 */
	public int hashCode() {
		int nameHash = getName().hashCode();
		int valueHash;
		if (m_value == null) {
	        CharsBuffer buffer = CharsBuffersPool.getBuffer();
	        writeValueToCharBuffer(buffer);
	        valueHash = buffer.hashCode();
	        CharsBuffersPool.putBufferBack(buffer);
		}
		else {
			valueHash = m_value.hashCode();
		}
		
		int hash = nameHash ^ valueHash;
        return hash;
	}

    /**
     * Indicates whether some other Object is "equal to" this Header
     * (Note that obj must be have same class as this Header - which means it
     * must be from same JAIN SIP implementation)
     * @param <var>obj</var> the Object with which to compare this Header
     * @returns true if this Header is "equal to" the obj
     * argument; false otherwise
     */
    // TODO: set equal for each header according to rfc rules.
    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof HeaderImpl)) 
        {
        	return false;
        } 
        
        HeaderImpl other = (HeaderImpl)obj;
        if (!headerNamesEqual(getName(), other.getName())) {
        	return false;
        } 
        
        if (m_value == null) {
        	if (other.m_value == null) {
        		// compare the two parsed values
        		return valueEquals(other);
        	}
        	else {
        		// serialize this and compare with already-serialized-other
            	CharsBuffer thisOne = CharsBuffersPool.getBuffer();
                encodeValue(thisOne);
                char[] thisChars = thisOne.getCharArray();
                int thisCharsSize = thisOne.getCharCount();
                
                CharArray thatOne = other.m_value;
                boolean equal = thatOne.equals(thisChars, thisCharsSize);
                CharsBuffersPool.putBufferBack(thisOne);
                return equal;
        	}
        }
        else {
        	if (other.m_value == null) {
        		// serialize other and compare with already-serialized-this
            	CharsBuffer thatOne = CharsBuffersPool.getBuffer();
                other.encodeValue(thatOne);
                char[] thatChars = thatOne.getCharArray();
                int thatCharsSize = thatOne.getCharCount();
                
                CharArray thisOne = m_value;
                boolean equal = thisOne.equals(thatChars, thatCharsSize);
                CharsBuffersPool.putBufferBack(thatOne);
                return equal;
        	}
        	else {
        		// compare the two serialized values
        		return m_value.equals(other.m_value);
        	}
        }
    }
    
	/**
	 * compares two header names for equality.
	 * supports both formats - full and compact
	 */
	public static boolean headerNamesEqual(String h1, String h2) {
		if (h1.length() == 1) {
			h1 = getFullFormHeaderName(h1);
		}
		if (h2.length() == 1) {
			h2 = getFullFormHeaderName(h2);
		}
		return h1.equalsIgnoreCase(h2);
	}
	
    /**
     * converts header name from compact-form to full-form
     * @param name single-character name in compact form
     * @return the header name in full-form
     */
    private static String getFullFormHeaderName(String name) {
        switch (name.charAt(0)) {
        case SipConstants.TO_SHORT:
        case SipConstants.TO_SHORT_CAP:
			return ToHeader.name;
        case SipConstants.FROM_SHORT:
        case SipConstants.FROM_SHORT_CAP:
			return FromHeader.name;
        case SipConstants.CALL_ID_SHORT:
        case SipConstants.CALL_ID_SHORT_CAP:
			return CallIdHeader.name;
        case SipConstants.CONTACT_SHORT:
        case SipConstants.CONTACT_SHORT_CAP:
			return ContactHeader.name;
        case SipConstants.CONTENT_LENGTH_SHORT:
        case SipConstants.CONTENT_LENGTH_SHORT_CAP:
        	return ContentLengthHeader.name;
        case SipConstants.VIA_SHORT:
        case SipConstants.VIA_SHORT_CAP:
        	return ViaHeader.name;
        case SipConstants.CONTENT_ENCODING_SHORT:
        case SipConstants.CONTENT_ENCODING_SHORT_CAP:
        	return ContentEncodingHeader.name;
        case SipConstants.CONTENT_TYPE_SHORT:
        case SipConstants.CONTENT_TYPE_SHORT_CAP:
        	return ContentTypeHeader.name;
        case SipConstants.SUPPORTED_SHORT:
        case SipConstants.SUPPORTED_SHORT_CAP:
            return SupportedHeader.name;
        case SipConstants.SUBJECT_SHORT:
        case SipConstants.SUBJECT_SHORT_CAP:
			return SubjectHeader.name;
        case SipConstants.EVENT_SHORT:
        case SipConstants.EVENT_SHORT_CAP:
			return EventHeader.name;
        case SipConstants.ALLOW_EVENTS_SHORT:
        case SipConstants.ALLOW_EVENTS_SHORT_CAP:
			return AllowEventsHeader.name;
		case SipConstants.REFER_TO_SHORT:
		case SipConstants.REFER_TO_SHORT_CAP:
			return SipConstants.REFER_TO; // no Refer-To header defined by JAIN
        default:
        	return name;
        }
    }
    /**
     * compares two parsed header values
     * @param other the other header to compare with
     * @return true if both header values are identical, otherwise false
     */
    protected abstract boolean valueEquals(HeaderImpl other);
    
	/**
	 * determines whether or not this header can have nested values
	 */
	public abstract boolean isNested();
	
    /**
	 * Creates and returns a copy of Header
	 * @returns a copy of Header
	 */
	public Object clone()
	{
		try
		{
			HeaderImpl ret = (HeaderImpl)super.clone();
			if (m_value == null) {
				ret.m_value = null;
			}
			else {
				ret.m_value = (CharArray)m_value.clone();
			}
			return ret;
		}
		catch (CloneNotSupportedException e)
		{
			// Can't happen.
			// @PMD:REVIEWED:AvoidThrowingCertainExceptionTypesRule: by Amirk on 9/19/04 4:57 PM
			throw new Error("Clone not supported?");
		}
	}
	
	/**
	 * Check whether this header is parsed.
	 */
	public boolean isParsed()
	{
		return m_value == null;
	}
	
	/**
	 * parses the value of this header, if not already parsed
	 */
	public final void parse() throws SipParseException {
		if (m_value == null) {
			// already parsed
			return;
		}
		
		int length = m_value.getLength();
		if (length < 0) {
			length = 0;
		}
		SipParser parser = (SipParser)s_parsersPool.get();
		if (ToHeader.name.equals(getName()) || FromHeader.name.equals(getName()) || ContactHeader.name.equals(getName())
				|| shouldBeParsedAsAddress(getName())){
			parser.setParseHeaderParameters(true);
		}else{
			parser.setParseHeaderParameters(false);
		}
		
		parser.setSrc(m_value.getArray(), length);
		parseValue(parser);
        parser.rewind();
        s_parsersPool.putBack(parser);
		m_value.returnToPool();
		m_value = null;
	}
	
	/**
	 * Returns <tt>true</tt> if the given header should be parsed as an address
	 * header (specified in the custom property <tt>headers.parsed.as.address</tt>).
	 * 
	 * @param headerName the name of the header to check
	 * 
	 * @return <tt>true</tt> if the given header should be parsed as
	 *         an address header, <tt>false</tt> otherwise.
	 */
	private boolean shouldBeParsedAsAddress(String headerName) {
		HashSet<String> addressHeaders = SIPTransactionStack.instance().getConfiguration().getAddressHeaders();
		//This custom property is case-insensitive
		boolean result = addressHeaders.contains(headerName.toLowerCase());
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "shouldBeParsedAsAddress", headerName + " " + result);
		}
		return result;
	}
	
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected abstract void parseValue(SipParser parser) throws SipParseException;
}
