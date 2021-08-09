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
import jain.protocol.ip.sip.header.SubjectHeader;

import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
* Subject header implementation. 
* 
* @author Assaf Azaria, Mar 2003. 
*/

public class SubjectHeaderImpl extends HeaderImpl implements SubjectHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 392132720986897258L;

	/**
	 * The subject string.
	 */
	String m_subject = "";

    /** true if header is created in compact form, false if full form */
    private final boolean m_compactForm;

    /**
     * default constructor
     */
    public SubjectHeaderImpl() {
        this(false);
    }

    /**
     * constructor with compact/full form specification
     */
    public SubjectHeaderImpl(boolean compactForm) {
        super();
        m_compactForm = compactForm;
    }
	
    /**
    * Gets subject of SubjectHeader
    * @return subject of SubjectHeader
    */
    public String getSubject()
    {
    	return m_subject;
    }

	/**
	  * Sets subject of SubjectHeader
	  * @param <var>subject</var> subject
	  * @throws IllegalArgumentException if subject is null
	  * @throws SipParseException if subject is not accepted by implementation
	  */
    public void setSubject(String sub)
        throws IllegalArgumentException, SipParseException
    {
        
        if (sub == null)
        {
        	throw new IllegalArgumentException("Null subject"); 
        } 
        
        m_subject = sub;
    }
    
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		m_subject = parser.toString();
	}
	
	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
    protected void encodeValue(CharsBuffer buf)
	{
		buf.append(m_subject);
	} 
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof SubjectHeaderImpl)) {
			return false;
		}
		SubjectHeaderImpl o = (SubjectHeaderImpl)other;
		
		if (m_subject == null || m_subject.length() == 0) {
			if (o.m_subject == null || o.m_subject.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_subject == null || o.m_subject.length() == 0) {
				return false;
			}
			else {
				return m_subject.equals(o.m_subject);
			}
		}
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
	
	/* (non-Javadoc)
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#getName(boolean)
	 */
	@Override
	public String getName(boolean isUseCompactHeaders) {
		if (isUseCompactHeaders){
			return String.valueOf(SipConstants.SUBJECT_SHORT);
		}
		return getName();
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#isCompactFormSupported()
	 */
	public boolean isCompactFormSupported() {
		return true;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#isCompactForm()
	 */
	public boolean isCompactForm() {
		return m_compactForm;
	}
}
