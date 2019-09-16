/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import java.util.Map;
import java.util.Set;

/**
 * The <code>Parameterable</code> interface is used to indicate a SIP header field value
 * with optional parameters. 
 * All of the Address header fields are <code>Parameterable</code>, including Contact, From,
 * To, Route, Record-Route, and Reply-To. 
 *  
 * In addition, the header fields Accept, Accept-Encoding, Alert-Info, 
 * Call-Info, Content-Disposition, Content-Type, Error-Info, 
 * Retry-After and Via are also <code>Parameterable</code>.
 *
 * @since 1.1
 */
public interface Parameterable extends Cloneable {

	/**
	 * Returns a clone of this <code>Parameterable</code>. 
	 * The cloned <code>Parameterable</code> has identical field value and parameters.
	 * 
	 * @return a clone of this <code>Parameterable</code>
	 */
	Object clone();

	/**
	 * Compares the given <code>Parameterable</code> type with this one. 
	 * The comparison rules to be used for the <code>Parameterable</code> 
	 * comparison should be taken as specified in the underlying specifications. 
	 * 
	 * Most of the headers of <code>Parameterable</code> type are defined in RFC 3261, 
	 * however for others their respective specifications should be consulted 
	 * for comaprison.
	 * 
	 * Overrides equals in class Object
	 * 
	 * @param o - given <code>Parameterable</code> to be compared with this.
	 * @return true if the two <code>Parameterable</code> are deemed equal.
	 */
	boolean equals(Object o);
	
	/**
	 * Returns the value of the named parameter, or null if it is not set. 
	 * A zero-length String indicates a flag parameter.
	 * @param key - a String specifying the parameter name
	 * @return value of the specified parameter
	 * @Throws java.lang.NullPointerException - if key is null
	 */
	String getParameter(String key);
	
	/**
	 * Returns an Iterator of the names of all parameters contained in this object. 
	 * The order is the order of appearance of the parameters in the <code>Parameterable</code>.
	 * 
	 * @return an Iterator of String objects that are the names of the 
	 * 			parameters contained in this object
	 */
	Iterator<String> getParameterNames();
	
	/**
	 * Returns a Collection view of the parameter name-value mappings contained
	 * in this <code>Parameterable</code>. The order is the order of appearance
	 *  of the parameters in the <code>Parameterable</code>.
	 * 
	 * @return a <code>Set</code> of <code>Map.Entry</code> objects that are 
	 * 			the parameters on this <code>Parameterable</code>.
	 */
	Set<Map.Entry<String,String>> getParameters();
	
	/**
	 * Returns the field value as a string. 
	 * @return the field value, not including parameters
	 */
	String getValue(); 

	/**
	 * Removes the named parameter from this object. Nothing is done if the 
	 * object did not already contain the specific parameter.
	 * 
	 * @param name - parameter name
	 * @throws IllegalStateException 
	 * 		- if parameters cannot be modified for this object 
	 */
	void removeParameter(String name) throws IllegalStateException; 

	/**
	 * Sets the value of the named parameter. 
	 * If this object previously contained a value for the given parameter name,
	 * then the old value is replaced by the specified value. 
	 * 
	 * The setting of a flag parameter is indicated by specifying a zero-length
	 * String for the parameter value. 
	 * Calling this method with null value is equivalent to calling 
	 * removeParameter(String)
	 *  
	 * @param name - parameter name
	 * @param value - new parameter value 
	 * @throws java.lang.IllegalStateException - if parameters cannot be modified for this object
	 * 		   java.lang.NullPointerException - if name parameter is null
	 */
	void setParameter(String name,String value) throws IllegalStateException; 

	 
	/**
	 * Set the value of the field. 
	 * @param value - the new header field value, not including parameters
	 * @throws IllegalStateException - if the header field cannot be modified for this object
	 */
	void setValue(String value) throws IllegalStateException; 
}
