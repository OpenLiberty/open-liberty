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
import jain.protocol.ip.sip.header.ContactHeader;
import jain.protocol.ip.sip.header.NameAddressHeader;
import jain.protocol.ip.sip.header.ParametersHeader;
import jain.protocol.ip.sip.header.RecordRouteHeader;

import java.util.Date;

import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * Contant header implementation.
 * 
 * @see NameAddressHeader
 * @see ParametersHeader
 * @see RecordRouteHeader
 *
 * @author Assaf Azaria, Mar 2003.
 */
public class ContactHeaderImpl extends NameAddressHeaderImpl
implements ContactHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 2721338566178208060L;

	//
	// Constants.
	//
	public static final String CONTACT_ACTION= "action";
	public static final String CONTACT_EXPIRE= "expires";
	public static final String CONTACT_Q     = "q";

	//
	// Members.
	//

	/** 
	 * The comment.
	 */
	protected String m_comment;

	/**
	 * The wild card flag.
	 */
	protected boolean m_wildCard;

	/** true if header is created in compact form, false if full form */
	private final boolean m_compactForm;

	/**
	 * default constructor
	 */
	public ContactHeaderImpl() {
		this(false);
	}

	/**
	 * constructor with compact/full form specification
	 */
	public ContactHeaderImpl(boolean compactForm) {
		super();
		m_compactForm = compactForm;
	}

	//
	// Methods.
	//

	/**
	 * Returns boolean value indicating whether ContactHeader is a wild card
	 * @return boolean value indicating whether ContactHeader is a wild card
	 */
	public boolean isWildCard()
	{
		return m_wildCard;
	}

	/**
	 * Sets ContactHeader to wild card (replaces NameAddress with "*")
	 */
	public void setWildCard()
	{
		m_wildCard = true;
	}

	/**
	 * Gets comment of ContactHeader
	 * (Returns null if comment does not exist)
	 * @return comment of ContactHeader
	 */
	public String getComment()
	{
		return m_comment;
	}

	/**
	 * Gets boolean value to indicate if ContactHeader
	 * has comment
	 * @return boolean value to indicate if ContactHeader
	 * has comment
	 */
	public boolean hasComment()
	{
		return m_comment != null;
	}

	/**
	 * Sets comment of ContactHeader
	 * @param comment String to set
	 * @throws IllegalArgumentException if comment is null
	 * @throws SipParseException if comment is not accepted by implementation
	 */
	public void setComment(String comment)
	throws IllegalArgumentException, SipParseException
	{
		if (comment == null)
		{
			throw new IllegalArgumentException("Contact: null comment");
		}

		m_comment = comment;
	}

	/**
	 * Removes comment from ContactHeader (if it exists)
	 */
	public void removeComment()
	{
		m_comment = null;
	}

	/**
	 * Sets q-value of ContactHeader
	 * @param <var>qValue</var> q-value
	 * @throws SipParseException if qValue is not accepted by implementation
	 */
	public void setQValue(float qValue) throws SipParseException
	{
		if (qValue < 0.0)
		{
			throw new SipParseException("AcceptLangHeader: Q Value < 0", "");
		}
		if (qValue > 1.0)
		{
			throw new SipParseException("AcceptLangHeader: Q value > 1.0", "");
		}

		setParameter(CONTACT_Q, "" + qValue);
	}

	/**
	 * Gets q-value of ContactHeader
	 * (Returns negative float if comment does not exist)
	 * @return q-value of ContactHeader
	 */
	public float getQValue()
	{
		String q = getParameter(CONTACT_Q);
		if (q == null) return -1.0f;

		try
		{
			return Float.parseFloat(q);
		}
		catch (NumberFormatException e)
		{
			return -1.0f;
		}
	}

	/**
	 * Gets boolean value to indicate if ContactHeader
	 * has q-value
	 * @return boolean value to indicate if ContactHeader
	 * has q-value
	 */
	public boolean hasQValue()
	{
		return hasParameter(CONTACT_Q);
	}

	/**
	 * Removes q-value from ContactHeader (if it exists)
	 */
	public void removeQValue()
	{
		removeParameter(CONTACT_Q);
	}

	/**
	 * Sets action of ContactHeader
	 * @param action String to set
	 * @throws IllegalArgumentException if action is null
	 * @throws SipParseException if action is not accepted by implementation
	 */
	public void setAction(String action)
	throws IllegalArgumentException, SipParseException
	{
		if (action == null)
		{
			throw new IllegalArgumentException("Contact: null action");
		}

		setParameter(CONTACT_ACTION, action);
	}

	/**
	 * Gets boolean value to indicate if ContactHeader
	 * has action
	 * @return boolean value to indicate if ContactHeader
	 * has action
	 */
	public boolean hasAction()
	{
		return hasParameter(CONTACT_ACTION);
	}

	/**
	 *  remove the Action field
	 */
	public void removeAction()
	{
		removeParameter(CONTACT_ACTION);
	}

	/** 
	 * get the action field
	 */
	public String getAction()
	{
		return getParameter(CONTACT_ACTION);
	}

	/**
	 * Sets expires of ContactHeader to a number of delta-seconds
	 * @param expiryDeltaSeconds long to set
	 * @throws SipParseException if expiryDeltaSeconds 
	 * 	is not accepted by implementation
	 */
	public void setExpires(long expiryDeltaSeconds) throws SipParseException
	{
		if (expiryDeltaSeconds < 0)
		{
			throw new SipParseException("Contant: Expires value not legal", "");
		}

		setParameter(CONTACT_EXPIRE, "" + expiryDeltaSeconds);

	}

	/**
	 * Sets expires of ContactHeader to a date
	 * @param <var>expiryDate</var> date of expiry
	 * @throws IllegalArgumentException if expiryDate is null
	 * @throws SipParseException if expiryDate is not accepted by implementation
	 */
	public void setExpires(Date expiryDate)
	throws IllegalArgumentException, SipParseException
	{
		if (expiryDate == null)
		{
			throw new IllegalArgumentException("Contact: Null date");
		}

		long expTime = expiryDate.getTime();
		long currentTime = new Date().getTime();
		long expiresDelta = (expTime - currentTime) / 1000;

		setParameter(CONTACT_EXPIRE, "" + expiresDelta);
	}

	/**
	 * Gets expires as delta-seconds of ContactHeader
	 * (returns negative long if expires does not exist)
	 * @return expires as delta-seconds of ContactHeader
	 */
	public long getExpiresAsDeltaSeconds()
	{
		String delta = getParameter(CONTACT_EXPIRE);
		if (delta == null)
		{
			return -1;
		}
		try 
		{
			return Long.parseLong(delta);	
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	/**
	 * Gets expires as date of ContactHeader
	 * (Returns null if expires value does not exist)
	 * @return expires as date of ContactHeader
	 */
	public Date getExpiresAsDate()
	{
		long expiryDelta = -1;
		String delta = getParameter(CONTACT_EXPIRE);
		if (delta == null)
		{
			return null;
		}
		try 
		{
			expiryDelta = Long.parseLong(delta);	
		}
		catch (NumberFormatException e)
		{
			return null;
		}

		long currentTime = new Date().getTime();
		return new Date(currentTime + expiryDelta * 1000);
	}

	/**
	 * Gets boolean value to indicate if ContactHeader
	 * has expires
	 * @return boolean value to indicate if ContactHeader
	 * has expires
	 */
	public boolean hasExpires()
	{
		return hasParameter(CONTACT_EXPIRE);
	}

	/**
	 * Removes expires from ContactHeader (if it exists)
	 */
	public void removeExpires()
	{
		removeParameter(CONTACT_EXPIRE);
	}

	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		if (parser.LA(1) == STAR) {
			int i = 2;
			boolean isWildCard = true;
			char c;
			while ((c=parser.LA(i)) != ENDL) {
				if (c != SP) {
					isWildCard = false;
					break;
				}
				i++;
			}
		/** APAR #PM81797
       	 if the custom property is set to false we behave as before and pare any contact header which starts with a "*"
       	 as "Contact: *" , and set the wildCard to true
       	 if the custom property is set to true, we behave according to RFC3261 and distinguish between 
       	 "Contact: *" to "Contact: *"*(token) 
			 **/
			if (isWildCard) {
				setWildCard();
				return;
			}
		}

		// address and parameters
		super.parseValue(parser);

		// comment
		if (parser.LA(1) == LPAREN) {
			parser.match(LPAREN);
			setComment(parser.nextToken(RPAREN));
			parser.match(RPAREN);
		}
	}

	/**
	 * encodes the value of this header
	 */
	protected void encodeValue(CharsBuffer buffer) {
		if (m_wildCard) {
			buffer.append('*');
			return;
		}

		// encode parameters and name-address
		super.encodeValue(buffer);

		// encode the comment
		if (hasComment()) {
			buffer.append(LPAREN);
			buffer.append(m_comment);
			buffer.append(RPAREN);
		}
	}

	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!super.valueEquals(other)) {
			return false;
		}
		if (!(other instanceof ContactHeaderImpl)) {
			return false;
		}
		ContactHeaderImpl o = (ContactHeaderImpl)other;

		if (m_wildCard) {
			if (!o.m_wildCard) {
				return false;
			}
		}
		else {
			if (o.m_wildCard) {
				return false;
			}
		}

		if (m_comment == null || m_comment.length() == 0) {
			if (o.m_comment == null || o.m_comment.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_comment == null || o.m_comment.length() == 0) {
				return false;
			}
			else {
				return m_comment.equals(o.m_comment);
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
		return true;
	}

	/**
	 * @return the separator preceeding the list of parameters
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getListSeparator()
	 */
	protected char getListSeparator() {
		return SEMICOLON;
	}

	/**
	 * @return the separator between parameters
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getParamSeparator()
	 */
	protected char getParamSeparator() {
		return SEMICOLON;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#getName(boolean)
	 */
	@Override
	public String getName(boolean isUseCompactHeaders) {
		if (isUseCompactHeaders){
			return String.valueOf(SipConstants.CONTACT_SHORT);
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
