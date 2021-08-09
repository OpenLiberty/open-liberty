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

import java.io.Serializable;

import com.ibm.ws.sip.parser.util.ObjectPool;
import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;

/**
 * a reusable char buffer.
 * used as the source for parsing incoming messages
 * 
 * @author ran
 */
public class CharArray implements Cloneable, Serializable
{
    /** Serialization UID (do not change) */
	private static final long serialVersionUID = -4102504972399557418L;

	/** pool of char buffers */
    private static ObjectPool s_pool = new ObjectPool(CharArray.class);

    /** should the stack accept non-UTF8 bytes */
    private static final boolean s_acceptNonUtf8ByteSequences =
    	ApplicationProperties.getProperties().getBoolean(StackProperties.ACCEPT_NON_UTF8_BYTES);
    
	/** the char buffer */
	private char[] m_array;
	
	/** content size */
	private int m_length;
	
	/**
	 * allocates a char array from the pool
	 * @param length number of chars in array. this may be less than array.length
	 */
	public static CharArray getFromPool(int length) {
		CharArray instance = (CharArray)s_pool.get();
		instance.ensureSize(length);
		instance.m_length = length;
		return instance;
	}
	
	/**
	 * allocates a char array from the pool,
	 * and initializes it with contents.
	 *
	 * @param contents String to be copied into this array
	 */
	public static CharArray getFromPool(String contents) {
		int len = contents.length();
		CharArray instance = getFromPool(len);
        contents.getChars(0, len, instance.m_array, 0);
		return instance;
	}
	
	/**
	 * allocates a char array from the pool,
	 * and initializes it with contents, given a byte array as input
	 *
	 * @param contents byte array to be copied into this char array
	 * @param offset index of first char to copy from contents
	 * @param length number of bytes to copy from contents
	 * @return the initialized char array
	 */
	public static CharArray getFromPool(byte[] contents, int offset, int length) {
		CharArray instance = getFromPool(length);
		char[] dst = instance.m_array;
		int end = offset + length;
		int iDst = 0;

		for (int iSrc = offset; iSrc < end; iSrc++) {
			byte b = contents[iSrc];
			char c;
			if ((b & 0x80) == 0x80) {
				int size = MessageParser.utf8size(b);
				if (size == -1) {
					// neither utf-8 or 7-bit-ascii
					if (!s_acceptNonUtf8ByteSequences) {
						throw new IllegalArgumentException("illgal byte value ["
							+ (int)(b & 255) + ']');
					}
					c = (char)(b & 255); // 8-bit ascii - not standard
				}
				else {
					int value = MessageParser.utf8(contents, iSrc, end-iSrc, size);
					if (value == -1) {
						// utf-8 lead byte with no utf-8 trail byte.
						if (!s_acceptNonUtf8ByteSequences) {
							throw new IllegalArgumentException(
								"expected utf-8 trail byte following utf-8 lead byte ["
								+ (int)(b & 255) + ']');
						}
						c = (char)(b & 255); // 8-bit ascii - not standard
					}
					else {
						c = (char)value;
						iSrc += size-1;
					}
				}
			}
			else {
				// 7-bit ascii
				c = (char)b;
			}
			dst[iDst++] = c;
		}
		instance.m_length = iDst;
		return instance;
	}
	
	/**
	 * allocates a char array from the pool,
	 * and initializes it with contents.
	 *
	 * @param contents char array to be copied into this char array
	 * @param offset index of first char to copy from contents
	 * @param length number of bytes to copy from contents
	 */
	public static CharArray getFromPool(char[] contents, int offset, int length) {
		CharArray instance = getFromPool(length);
		if (length > 0) {
		    System.arraycopy(contents, offset, instance.m_array, 0, length);
		}
		return instance;
	}
	
	/**
	 * returns a char array to the pool when it's no longer needed
	 */
	public void returnToPool() {
		s_pool.putBack(this);
	}
	
	/**
	 * empty constructor for instantiation by the object pool
	 */
	public CharArray() {
		m_array = null;
		m_length = 0;
	}
	
	/**
	 * ensure that allocated size is large enough.
	 * invalidates existing array contents
	 * @param size the minumum size required
	 */
	private void ensureSize(int size) {
		if (m_array == null || m_array.length < size) {
			int round = roundSize(size);
			m_array = new char[round];
		}
	}
	
	/**
	 * calculates the recommended size to allocate
	 * 
	 * @param min the minimum number of bytes required
	 * @return smallest(2**n) that is greater or equal min
	 */
	private int roundSize(int min) {
        int bits;
        int round = min;
        for (bits = 0; round > 0; bits++) {
        	round /= 2;
        }
        round = 1 << bits;
        return round;
	}
	
	/**
	 * @return the char buffer
	 */
	public char[] getArray() {
		return m_array;
	}

	/**
	 * @return number of chars in the buffer
	 */
	public int getLength() {
		return m_length;
	}

	/**
	 * returns the character at specified position.
	 * does not verify position bounds
	 * 
	 * @param position index of character
	 * @return the character at position 'position'
	 */
	public char charAt(int position) {
		return m_array[position];
	}
	
	/**
	 * @return a reasonable hash code for the contents of this char array 
	 */
	public int hashCode() {
		if (m_array == null) {
			return 0;
		}
		
		int hash = 0;
		char[] array = m_array;
		int carry; // leftmost bit to be recycled
		
		for (int i = m_length-1; i >= 0; i--) {
			carry = (hash & 0x80000000) == 0 ? 0 : 1;
			hash <<= 1;
			hash |= carry;
			hash ^= array[i];
		}

        return hash;
	}
	
	/**
	 * compares to char arrays
	 * @return true only if contents is the same
	 */
	public boolean equals(Object obj) {
		// compare type
		if (!(obj instanceof CharArray))
			return false;
		
		// compare length
		CharArray other = (CharArray)obj;
		if (m_length != other.m_length)
			return false;
		
		int len = m_length;
		char[] a1 = m_array;
		char[] a2 = other.m_array;
		
		// compare existance of contents
		if (a1 == null || a2 == null) {
			if (a1 != null || a2 != null)
				return false;
		}
		
		// compare contents
		for (int i = 0; i < len; i++) {
			if (a1[i] != a2[i])
				return false;
		}
		return true;
	}
	
	/**
	 * compares this char array with some other char buffer
	 * @param array other char buffer
	 * @param length other char buffer content size
	 * @return true if content is equal
	 */
	public boolean equals(char[] array, int length) {
		// compare length
		if (m_length != length)
			return false;
		
		// compare contents
		char[] a1 = m_array;
		char[] a2 = array;
		
		for (int i = 0; i < length; i++) {
			if (a1[i] != a2[i])
				return false;
		}
		return true;
	}
	
	/**
	 * compares this char array with given string, case insensitive
	 * @param str string to compare with
	 * @return true if content is equal
	 */
	public boolean equalsIgnoreCase(String str) {
		// compare length
		int length = str.length();
		if (m_length != length)
			return false;
		
		// compare contents
		char[] array = m_array;
		for (int i = 0; i < length; i++) {
			if (Character.toLowerCase(array[i]) != Character.toLowerCase(str.charAt(i)))
				return false;
		}
		return true;
	}
	
	/**
	 * string representation for debugging
	 */
	public String toString() {
		if (m_array == null) {
			return "null";
		}
		else {
			return String.valueOf(m_array, 0, m_length);
		}
	}

	/**
	 * duplicates this char array
	 * @return a new copy of the same char array
	 */
	public Object clone() {
		try {
			CharArray ret = (CharArray)super.clone();
			ret.m_length = m_length;
			
			if (m_array == null) {
				ret.m_array = null;
			}
			else {
				ret.m_array = new char[m_array.length];
				System.arraycopy(m_array, 0, ret.m_array, 0, m_array.length);
			}
			return ret;
		}
		catch (CloneNotSupportedException e) {
			// todo
			return null;
		}
	}
}
