/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.servlet.sip;

/**
 * Represents <code>tel</code> URLs as defined by RFC 2806. Tel URLs
 * represent telephone numbers. SIP servlet containers may be able to
 * route requests based on tel URLs but are not required to.
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc2806.txt">RFC 2806</a>
 */
public interface TelURL extends URI {
	
	/**
	 * Compares the given TelURL with this TelURL. The comparison rules to be 
	 * followed must be as specified in RFC 3966 section 5.
	 * 
	 * @param o - the TelURL which is to be compared with this. 
	 * @return true if the two URLs are equal as per RFC 3966.
	 */
	boolean equals(Object o);
	
	/**
	 * 
	 * Returns the phone context of this TelURL for local numbers or 
	 * null if the phone number is global 
	 * 
	 * @return - the phone-context of this TelURL for local numbers 
	 * 			or null if the phone number is global
	 */
	String getPhoneContext();
    
    /**
     * Returns the phone number of this <code>TelURL</code>. The
     * returned string includes any visual separators present in the
     * phone number part of the URL but does <em>not</em> include a
     * leading "+" for global tel URLs.
     * 
     * @return the number of this <code>TelURL</code>
     */
    String getPhoneNumber();
    
    /**
     * Returns true if this <code>TelURL</code> is global, and false
     * otherwise.
     * 
     * @return true if this tel URL is in global form
     */
    boolean isGlobal();
    
    /**
     * Sets the (global) phone number of this TelURL. The specified number 
     * must be a valid global number for the "tel" scheme as described in 
     * RFC3966 (URLs for Telephone Calls). The following usage of this method 
     * will result in valid global phone number:
     * 
     * setPhoneNumber("+1-201-555-0123")
     *  
     * @param number
     * 
     * @throws IllegalArgumentException - if the phone number was invalid according to 
     * validation rules specified in RFC3966 
     */
    void setPhoneNumber(String number) throws IllegalArgumentException;
    
    /**
     * Sets the (local) phone number of this TelURL. The specified 
     * number must be a local phone number for the "tel" scheme as 
     * described in RFC3966 (URLs for Telephone Calls). The following 
     * usage of this method will result in a valid local phone number:
     * 
     * setPhoneNumber("7042","example.com")
     * 
     * @param number - the new local phone number
     * @param phoneContext  - the phone-context parameter of this TelURI 
     *
     * @throws IllegalArgumentException - if the phone number was invalid according to 
     * validation rules specified in RFC3966 
     */
    void setPhoneNumber(String number, String phoneContext) throws IllegalArgumentException;
    
    
    /**
     * Returns the <code>String</code> representation of this
     * <code>TelURL</code>. Any reserved characters will be properly escaped
     * according to RFC2396.
     * 
     * @return  the <code>String</code> representation of this
     *          <code>TelURL</code>
     */
    String toString();
}
