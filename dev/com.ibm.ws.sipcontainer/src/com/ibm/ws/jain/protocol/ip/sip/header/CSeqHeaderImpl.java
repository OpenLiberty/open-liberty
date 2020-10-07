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
import jain.protocol.ip.sip.header.CSeqHeader;

import com.ibm.ws.sip.parser.CharArray;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
* CSeq Header implementation.
* 
* @author Assaf Azaria, Mar 2003.
*/
public class CSeqHeaderImpl extends HeaderImpl implements CSeqHeader
{
    /** Serialization UID (do not change) */
    private static final long serialVersionUID = 1501450667995306211L;

    //
    // Members.
    //

    /**
     * The method.
     */
    private String m_method;

    /**
     * The sequence number.
     */
    private long m_seqNumber;

    /**
     * @throws SipParseException
     */
    public CSeqHeaderImpl() {
        super();
    }

	/** 
	 * 
	 */
	public CSeqHeaderImpl(CharArray value) {
		super(value);
	}

    /**
     * Set sequence number of CSeqHeader
     * @param <var>sequenceNumber</var> sequence number
     * @throws SipParseException if sequenceNumber is not accepted by implementation
     */
    public void setSequenceNumber(long sequenceNumber) throws SipParseException
    {
        m_seqNumber = sequenceNumber;
    }

    /**
     * Set method of CSeqHeader
     * @param meth String to set
     * @throws IllegalArgumentException if method is null
     * @throws SipParseException if method is not accepted by implementation
     */
    public void setMethod(String meth)
        throws IllegalArgumentException, SipParseException
    {
        if (meth == null)
        {
        	throw new IllegalArgumentException("Cseq: Null method"); 
        } 
        
        m_method = meth;
    }

    /**
    * Gets sequence number of CSeqHeader
    * @return sequence number of CSeqHeader
    */
    public long getSequenceNumber()
    {
    	return m_seqNumber;
    }

    /**
    * Gets method of CSeqHeader
    * @return method of CSeqHeader
    */
    public String getMethod()
    {
    	return m_method;
    }
    
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		// CSeq = "CSeq" HCOLON 1*DIGIT LWS Method
		long sequenceNumber = parser.longNumber();
		if (!Character.isSpaceChar(parser.LA())) {
			throw new SipParseException("Bad CSeq", "");
		}
		String method = parser.nextToken(ENDL);
		setSequenceNumber(sequenceNumber);
		setMethod(method);
	}
    
	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
    protected void encodeValue(CharsBuffer ret)
	{
		ret.append(m_seqNumber).append(SP).append(m_method.toUpperCase());
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof CSeqHeaderImpl)) {
			return false;
		}
		CSeqHeaderImpl o = (CSeqHeaderImpl)other;
		
		if (m_seqNumber != o.m_seqNumber) {
			return false;
		}
		if (m_method == null || m_method.length() == 0) {
			if (o.m_method == null || o.m_method.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_method == null || o.m_method.length() == 0) {
				return false;
			}
			else {
				return m_method.equals(o.m_method);
			}
		}
	}
	
	/**
	 * calculates a hashcode for this cseq header 
	 */
	public int hashCode() {
		if (!isParsed()) {
			return super.hashCode();
		}
		int nameHash = name.hashCode();
		int valueHash = m_method.hashCode() ^ (int)m_seqNumber;
		int hash = nameHash ^ valueHash;
        return hash;
	}
	/**
	 * @return the name of this header 
	 * @see jain.protocol.ip.sip.header.Header#getName()
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Creates and returns a copy of Header
	 * @returns a copy of Header
	 */
	public Object clone()
	{
		// This is required in case someone will inherit 
		// from this class.
		return super.clone(); 
	}

	/**
	 * determines whether or not this header can have nested values
	 */
	public boolean isNested() {
		return false;
	}
}
