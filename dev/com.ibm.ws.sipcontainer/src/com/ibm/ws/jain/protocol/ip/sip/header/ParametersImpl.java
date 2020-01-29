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
package com.ibm.ws.jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.Parameters;
import jain.protocol.ip.sip.SipParseException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ibm.ws.sip.parser.Coder;
import com.ibm.ws.sip.parser.Separators;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;

/**
 * a list of key=value pairs.
 * the key is a case-insensitive string.
 * the value is a case-sensitive, optional string.
 * 
 * @author ran
 */
public class ParametersImpl implements Parameters, Cloneable, Serializable
{
    /**
     * nested class representing a key-value pair.
     * key is case-insensitive
     */
    public static class Parameter implements Cloneable,Serializable {
    	/** case-insensitive key */
    	private String m_key;
    	
    	/** optional value */
    	private String m_val;
    	
    	/** true if value is quoted */
    	private boolean m_quote;
    	
    	/** private constructor */
    	private Parameter(String key, String val, boolean quote) {
    		m_key = key;
    		m_val = val;
    		m_quote = quote;
    	}
    	
    	/** @return the parameter's key */
    	public String getKey() {
    		return m_key;
    	}
    	
    	/** @return the parameter's value, null if value not set */
    	public String getValue() {
    		return m_val;
    	}
    	
    	/** @return true if value is quoted */
    	public boolean getQuote() {
    		return m_quote;
    	}

    	/** @see java.lang.Object#equals(java.lang.Object) */
    	public boolean equals(Object other) {
    		if (this == other) {
    			return true;
    		}
    		if (!(other instanceof Parameter)) {
    			return false;
    		}
    		Parameter p = (Parameter)other;
    		return p.m_key.equalsIgnoreCase(m_key);
    	}
    	
    	/** @see java.lang.Object#hashCode() */
		public int hashCode() {
			int hash = 0;
			int len = m_key.length();

			for (int i = 0; i < len; i++) {
				char c = m_key.charAt(i);
				hash = 31*hash + Character.toLowerCase(c);
			}
			return hash;
		}
		
		/**
		 * duplicates this parameter
		 * @see java.lang.Object#clone()
		 */
		public Object clone() {
			Parameter p;
			try {
				p = (Parameter)super.clone();
				p.m_key = m_key;
				p.m_val = m_val;
				p.m_quote = m_quote;
			}
			catch (CloneNotSupportedException e) {
				throw new InternalError();
			}
			return p;
		}
	}

	/**
	 * nested iterator class to iterate over parameter keys
	 */
	private static class ParameterIterator implements Iterator {
		/** the backing parameter list */
		private ArrayList m_list;

		/** index of element to be returned by subsequent call to next */
		private int m_index;
		
		/** true if it's legal to call remove() now */
		private boolean m_canRemove;

		/** private constructor */
		private ParameterIterator(ArrayList list) {
			m_list = list;
			m_index = 0;
			m_canRemove = false;
		}

		/** @see java.util.Iterator#hasNext() */
		public boolean hasNext() {
			return m_index < m_list.size();
		}

		/** @see java.util.Iterator#next() */
		public Object next() {
			try {
				Parameter p = (Parameter)m_list.get(m_index);
				m_index++;
				m_canRemove = true;
				return p.m_key;
			}
			catch (IndexOutOfBoundsException e) {
				throw new NoSuchElementException(e.getMessage());
			}
		}

		/** @see java.util.Iterator#remove() */
		public void remove() {
			if (!m_canRemove) {
				throw new IllegalStateException();
			}
			m_canRemove = false;
			m_list.remove(m_index-1);
			m_index--;
		}
	}
	
	/** default capacity for SIP headers or URIs */
	private static final int DEFAULT_CAPACITY = 3;
	
	/** list of parameters */
	private ArrayList m_list;
	
	/**
	 * constructor
	 * @param initialCapacity initial capacity of internal array
	 */
	public ParametersImpl(int initialCapacity) {
		m_list = new ArrayList(initialCapacity);
	}
	
	/**
	 * constructor with default capacity
	 */
	public ParametersImpl() {
		this(DEFAULT_CAPACITY);
	}
	
	/**
	 * @param index index of parameter
	 * @return the parameter at specified index
	 */
	public Parameter get(int index) {
		return (Parameter)m_list.get(index);
	}
	
	/**
	 * finds parameter given the key
	 * @param key parameter key
	 * @return index of parameter in array, or -1 if not found
	 */
	private int indexOf(String key) {
		int size = m_list.size();
		for (int i = 0; i < size; i++) {
			Parameter p = (Parameter)m_list.get(i);
			if (p.m_key.equalsIgnoreCase(key)) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * finds parameter given the key
	 * @param key parameter key
	 * @return the parameter, or null if not found
	 */
	private Parameter get(String key) {
		int size = m_list.size();
		for (int i = 0; i < size; i++) {
			Parameter p = (Parameter)m_list.get(i);
			if (p.m_key.equalsIgnoreCase(key)) {
				return p;
			}
		}
		return null;
	}
	
	/**
	 * @return number of parameters in list
	 */
	public int size() {
		return m_list.size();
	}
	
	/**
	 * Gets the value of specified parameter
	 * (Note - zero-length String indicates flag parameter)
	 * (Returns null if parameter does not exist)
	 * @param <var>name</var> name of parameter to retrieve
	 * @return the value of specified parameter
	 * @throws IllegalArgumentException if name is null
	 * @see jain.protocol.ip.sip.Parameters#getParameter(java.lang.String)
	 */
	public String getParameter(String name) throws IllegalArgumentException {
		if (name == null) {
			throw new IllegalArgumentException("null name");
		}
		Parameter p = get(name);
		return p == null ? null : p.m_val;
	}

	/**
	 * Sets value of parameter
	 * (Note - zero-length value String indicates flag parameter)
	 * @param <var>name</var> name of parameter
	 * @param <var>value</var> value of parameter
	 * @throws IllegalArgumentException if name or value is null
	 * @throws SipParseException if name or value is not accepted by implementation
	 * @see jain.protocol.ip.sip.Parameters#setParameter(java.lang.String, java.lang.String)
	 */
	public void setParameter(String name, String value)
		throws IllegalArgumentException, SipParseException
	{
		setParameter(name, value, false);
	}

	/**
	 * Sets value of parameter
	 * (Note - zero-length value String indicates flag parameter)
	 * @param <var>name</var> name of parameter
	 * @param <var>value</var> value of parameter
	 * @param <var>quote</var> true if value is quoted
	 * @throws IllegalArgumentException if name or value is null
	 */
	public void setParameter(String name, String value, boolean quote)
		throws IllegalArgumentException
	{
		if (name == null) {
			throw new IllegalArgumentException("null name");
		}
		if (value == null) {
			throw new IllegalArgumentException("null value");
		}
		
		Parameter p = get(name);
		if (p == null) {
			// add new key
			p = new Parameter(name, value, quote);
		 	m_list.add(p);
		}
		else {
			// replace old with new
			p.m_key = name; // update with new letter case
			p.m_val = value;
			p.m_quote = quote;
		}
	}

	/**
	 * Gets boolean value to indicate if Parameters
	 * has any parameters
	 * @return boolean value to indicate if Parameters
	 * has any parameters
	 * @see jain.protocol.ip.sip.Parameters#hasParameters()
	 */
	public boolean hasParameters() {
		return !m_list.isEmpty();
	}

	/**
	 * Gets boolean value to indicate if Parameters
	 * has specified parameter
	 * @return boolean value to indicate if Parameters
	 * has specified parameter
	 * @throws IllegalArgumentException if name is null
	 * @see jain.protocol.ip.sip.Parameters#hasParameter(java.lang.String)
	 */
	public boolean hasParameter(String name) throws IllegalArgumentException {
		if (name == null) {
			throw new IllegalArgumentException("null name");
		}
		return get(name) != null;
	}

	/**
	 * Removes specified parameter from Parameters (if it exists)
	 * @param <var>name</var> name of parameter
	 * @throws IllegalArgumentException if parameter is null
	 * @see jain.protocol.ip.sip.Parameters#removeParameter(java.lang.String)
	 */
	public void removeParameter(String name) throws IllegalArgumentException {
		if (name == null) {
			throw new IllegalArgumentException("null name");
		}
		int i = indexOf(name);
		if (i != -1) {
			m_list.remove(i);
		}
	}

	/**
	 * Removes all parameters from Parameters (if any exist)
	 * @see jain.protocol.ip.sip.Parameters#removeParameters()
	 */
	public void removeParameters() {
		m_list.clear();
	}

	/**
	 * Gets Iterator of parameter names
	 * (Note - objects returned by Iterator are Strings)
	 * (Returns null if no parameters exist)
	 * @see jain.protocol.ip.sip.Parameters#getParameters()
	 */
	public Iterator getParameters() {
		return new ParameterIterator(m_list);
	}

	/** @see java.lang.Object#equals(java.lang.Object) */
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ParametersImpl)) {
			return false;
		}
		ParametersImpl o = (ParametersImpl)other;
		int nParams = m_list.size();
		if (nParams != o.m_list.size()) {
			return false;
		}
		
		// get here with same number of parameters
		for (int i = 0; i < nParams; i++) {
			Parameter p1 = (Parameter)m_list.get(i);
			Parameter p2 = (Parameter)o.m_list.get(i);
			
			// compare quote
			if (p1.getQuote() != p2.getQuote()) {
				return false;
			}
			
			// compare key - case insensitive
			if (!p1.equals(p2)) {
				return false;
			}
			
			// compare value - case sensitive
			if (p1.m_val == null || p1.m_val.length() == 0) {
				if (p2.m_val != null && p2.m_val.length() > 0) {
					// this one empty, other one set
					return false;
				}
				else {
					// both empty
				}
			}
			else {
				if (p2.m_val != null && p2.m_val.length() > 0) {
					// both set
					if (!p1.m_val.equals(p2.m_val)) {
						return false;
					}
				}
				else {
					// this one set, other one empty
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		int result = 0;
		
		for (int i = 0; i < m_list.size(); i++) {
			Parameter p = (Parameter)m_list.get(i);
			result ^= p.hashCode(); // case-insensitive hash for key
			String val = p.getValue();
			if (val != null && val.length() > 0) {
				result ^= val.hashCode(); // case-sensitive hash for value
			}
		}
		return result;
	}

	/**
	 * duplicates this list of parameters.
	 * modifying a parameter in the returned list
	 * does not affect the source list.
	 * 
	 * @return a new list of parameters identical to this one
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		ParametersImpl result;
		try {
			result = (ParametersImpl)super.clone();
		}
		catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
		int nParams = m_list.size();
		int capacity = nParams > DEFAULT_CAPACITY ? nParams : DEFAULT_CAPACITY;
		ArrayList newList = new ArrayList(capacity);
		
		for (int i = 0; i < nParams; i++) {
			Parameter src = (Parameter)m_list.get(i);
			Object dst = src.clone();
			newList.add(dst);
		}
		result.m_list = newList;
		return result;
	}

	/**
	 * encodes the paramters in canonical form
	 * @param buffer buffer to write parameters to
	 * @param separator character that separates between parameters
	 * @param escape whether or not to escape parameters
	 */
	public void encode(CharsBuffer buffer, char separator, boolean escape) {
		if (m_list == null) {
			return;
		}
		int nParams = m_list.size();

		for (int i = 0; i < nParams; i++) {
			Parameter p = (Parameter)m_list.get(i);

			String key = p.getKey();
			if (key.length() > 0) {
				buffer.append(key);

				String val = p.getValue();
				boolean hasVal = val != null && val.length() > 0;
				boolean quote = p.getQuote();
				if (quote || hasVal) {
					buffer.append(Separators.EQUALS);
					if (quote) {
						buffer.append(Separators.DOUBLE_QUOTE);
					}
					if (hasVal) {
						if (escape && !quote) {
							// todo: 1. different escaping for parameters and headers.
							//       2. different escaping for different param types.
							Coder.encodeParam(val, buffer);
						}
						else {
							buffer.append(val);
						}
					}
					if (quote) {
						buffer.append(Separators.DOUBLE_QUOTE);
					}
				}
			}

			if (i < nParams - 1) {
				buffer.append(separator);
			}
		}
	}

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        CharsBuffer buffer = CharsBuffersPool.getBuffer();
        encode(buffer, ';', false);
        String str = buffer.toString();
        CharsBuffersPool.putBufferBack(buffer);
        return str;
    }
}
