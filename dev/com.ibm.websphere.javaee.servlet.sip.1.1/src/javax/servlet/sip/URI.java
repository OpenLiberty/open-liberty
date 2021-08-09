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

import java.util.Iterator;

/**
 * Base interface for any type of URI. These are used in the request line
 * of SIP requests to identify the callee and also in Contact, From, and
 * To headers.
 * 
 * <p>The only feature common to all URIs is that they can be represented
 * as strings beginning with a token identifying the <em>scheme</em> of
 * the URI followed by a colon followed by a scheme-specific part.
 * 
 * <p>The generic syntax of URIs is defined in RFC 2396.
 */
public interface URI extends Cloneable {
    /**
     * Returns a clone of this URI.
     *
     * @return URI a clone of this URI object
     */
    URI clone();
    
    /**
     * Compares the given URI with this URI. The comparison rules to be 
     * followed shall depend upon the underlying URI scheme being used. For 
     * general purpose URIs RFC 2396 should be consulted for equality. If the 
     * URIs are of scheme for which comparison rules are further specified in 
     * their specications,  then they must be 
     * used for any comparison. 
     * 
     * @param o the URI which is to be compared with this.
     * @return <code>true</code> if the two URIs are equal as per the provisions of their specifications.
     * @since 1.1 
     */
    boolean equals(Object o);
    
    
    /**
     * Returns the value of the named parameter, or null if 
     * it is not set. A zero-length String indicates flag parameter.
     *  
     * @param key - a String specifying the parameter name
     *  
     * @return value of the specified parameter
     * 
     * @throws NullPointerException - if the key is null.
     */
    String getParameter(java.lang.String key) throws NullPointerException;
    
    
    /**
     * 
     * Returns an Iterator over the names of all parameters present in this URI. 
     * 
     * @return - an iterator over strings containing the names of this 
     * 			 URLs parameters
     */
    Iterator<String> getParameterNames();
    
    
    /**
     * Returns the scheme of this <code>URI</code>,
     * for example "sip", "sips" or "tel".
     * 
     * @return the scheme of this <code>URI</code>
     */
    String getScheme();
    
    /**
     * Returns true if the scheme is "sip" or "sips", false otherwise.
     * 
     * @return true if the scheme is "sip" or "sips", false otherwise
     */
    boolean isSipURI();
    
    /**
     * Removes the named parameter from this URL. Nothing is done 
     * if the URL did not already contain the specific parameter.
     *  
     * @param name - parameter name
     */
    void removeParameter(String name);
    
    /**
     * Sets the value of the named parameter. If this URL previously 
     * contained a value for the given parameter name, then the old value 
     * is replaced by the specified value. The setting of a flag parameter 
     * is indicated by specifying a zero-length String for the parameter value.
     *  
     * @param name - parameter name
     * @param value - new parameter value 
     * 
     * @throws NullPointerException - on either name or value being null.
     * 
     */
    void setParameter(String name, String value) throws NullPointerException;
    

    /**
     * Returns the value of this <code>URI</code> as a <code>String</code>.
     * The result must be appropriately URL escaped.
     * 
     * @return <code>String</code> value of this <code>URI</code>
     */
    String toString();
    
}
