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
 * Represents SIP addresses as found, for example, in From, To, and
 * Contact headers. Applications use addresses when sending requests
 * as a user agent client (UAC) and when redirecting an incoming
 * request.
 * 
 * <p>Addresses appear in a number of SIP headers and generally adhere
 * to the grammar (constituent non-terminals are defined in the SIP
 * specification, RFC 3261):
 * <pre>
 * (name-addr / addr-spec) *(SEMI generic-params)
 * </pre>
 * that is to say, Addresses consist of a URI, an optional display name,
 * and a set of name-value parameters.
 * 
 * <p>The <code>Address</code> interface is used to represent the value of 
 * all headers defined to contain one or more addresses as defined above.
 * Apart from From, To, and Contact, this includes
 * Route, Record-Route, Reply-To, Alert-Info, Call-Info, Error-Info,
 * as well as extension headers like P-Asserted-Identity,
 * P-Preferred-Identity, and Path.
 * 
 * <p><code>Address</code> objects can be constructed using one of the
 * <code>SipFactory.createAddress</code> methods and can be obtained
 * from messages using {@link SipServletMessage#getAddressHeader} and
 * {@link SipServletMessage#getAddressHeaders}.
 */
public interface Address extends Parameterable,Cloneable {

	/**
	 * Returns a clone of this <code>Parameterable</code>. 
	 * The cloned <code>Parameterable</code> has identical field value and parameters.
	 * 
	 * @return a clone of this <code>Parameterable</code>
	 */
	Object clone();
	
	/**
	 * 
	 *  Compares the given Address with this one.
	 *
	 *  As Addresses consist of a URI, an optional display name, 
	 *  and an optional set of name-value parameters, the following 
	 *  rules should be used for comparing them:
	 *
	 *   1. URI (SipURI, TelURL, etc.) within the Address should be 
	 *      compared based on their respective equals() methods.
	 *   2. Address parameters should be compared in the same way as 
	 *      URI parameters (as specified in RFC 3261 Section 19.1.4) 
	 *      with no restrictions for well-known URI params.
	 *   3. display-names should be ignored in the comparison.
	 *   
	 *   @param o - object to comare
	 *   
	 *   @return {@link Object#equals}
	 */
	boolean equals(java.lang.Object o);
    
    /**
     * Returns the display name of this <code>Address</code>. This is
     * typically a caller or callees real name and may be rendered by a
     * user agent, for example when alerting.
     * 
     * @return  display name of this <code>Address</code>, or null if
     *          one doesn't exist
     */
    String getDisplayName();
    
    /**
     * Returns the value of the "expires" parameter as delta-seconds.
     * 
     * @return value of "expires" parameter measured in delta-seconds,
     *      or -1 if the parameter does not exist
     */
    int getExpires();
    
    
    /**
     * Returns the value of the "q" parameter of this <code>Address</code>.
     * The "qvalue" indicates the relative preference amongst a set of
     * locations. "qvalue" values are decimal numbers from 0 to 1, with
     * higher values indicating higher preference.
     * 
     * @return  this <code>Address</code>' qvalue or -1.0 if this is not set
     */
    float getQ();
    
    /**
     * Returns the <code>URI</code> component of this <code>Address</code>.
     * This method will return <code>null</code> for wildcard addresses
     * (see {@link #isWildcard}. For non-wildcard addresses the result
     * will always be non-null.
     * 
     * @return the <code>URI</code> of this <code>Address</code>
     */
    URI getURI();
    
    /**
     * Returns true if this <code>Address</code> represents the "wildcard"
     * contact address. This is the case if it represents a Contact header
     * whose string value is "*".  Likewise,
     * {@link SipFactory#createAddress(String) SipFactory.createAddress("*")}
     * always returns a wildcard <code>Address</code> instance.
     * 
     * @return true if this <code>Address</code> represents the "wildcard"
     *      contact address, and false otherwise
     */
    boolean isWildcard();
    
    

    /**
     * Sets the display name of this <code>Address</code>.
     * 
     * @param name  display name
     * @throws IllegalStateException if this <code>Address</code> is used
     *     in a context where it cannot be modified
     */
    void setDisplayName(String name) throws IllegalStateException;

    /**
     * Sets the value of the "expires" parameter.
     * 
     * @param seconds new relative value of the "expires" parameter.
     *        A negative value causes the "expires" parameter to be removed.
     */
    void setExpires(int seconds);
	
	/**
     * Sets this <code>Address</code>s qvalue.
     * 
     * @param q new qvalue for this <code>Address</code> or -1 to remove
     *      the qvalue
     * @throws IllegalArgumentException if the new qvalue isn't between
     *      0.0 and 1.0 (inclusive) and isn't -1.0.
     */
    void setQ(float q) throws IllegalArgumentException; 

	/**
     * Sets the URI of this <code>Address</code>.
     * 
     * @param uri   new <code>URI</code> of this <code>Address</code>
     * @throws     java.lang.IllegalStateException - if this Address is used in a context where it cannot be modified 
     *                     java.lang.NullPointerException - on null uri.
     * @throws     NullPointerException - on null uri.
     */
    void setURI(URI uri) throws IllegalStateException, NullPointerException; 

	/**
     * Returns the value of this address as a <code>String</code>. The
     * resulting string must be a valid value of a SIP From or To header.
     * 
     * @return value of this <code>Address</code> as a <code>String</code>
     */
    String toString();
}
