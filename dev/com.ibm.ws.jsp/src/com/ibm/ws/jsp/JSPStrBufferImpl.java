/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// Change History
// Create for defect  347278
// PM19500  Fix mem leak in WASJSPStrBufferImpl  07/28/10  pmdinh

package com.ibm.ws.jsp;

public class JSPStrBufferImpl implements  JSPStrBuffer {
	private final int MAX_JSP_STR_BUFFER_SIZE_AFTER_CLEAR = 512;			//PM19500
	StringBuffer sb = new StringBuffer();

	public JSPStrBufferImpl(){
	}
	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#append(boolean)
	 */
	public JSPStrBuffer append(boolean value) {
		sb.append(value);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#append(char)
	 */
	public JSPStrBuffer append(char ch) {
		sb.append(ch);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#append(char[], int, int)
	 */
	public JSPStrBuffer append(char[] chars, int start, int length) {
		sb.append(chars, start, length);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#append(char[])
	 */
	public JSPStrBuffer append(char[] chars) {
		sb.append(chars);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#append(java.lang.CharSequence)
	 */
	public JSPStrBuffer append(CharSequence sequence) {
		sb.append(sequence);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#append(double)
	 */
	public JSPStrBuffer append(double value) {
		sb.append(value);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#append(float)
	 */
	public JSPStrBuffer append(float value) {
		sb.append(value);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#append(int)
	 */
	public JSPStrBuffer append(int value) {
		sb.append(value);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#append(long)
	 */
	public JSPStrBuffer append(long value) {
		sb.append(value);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#append(java.lang.Object)
	 */
	public JSPStrBuffer append(Object value) {
		sb.append(value);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#append(java.lang.String)
	 */
	public JSPStrBuffer append(String string) {
		sb.append(string);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#append(java.lang.JSPStrBuffer)
	 */
	public JSPStrBuffer append(JSPStrBuffer sbuffer) {
		sb.append(sbuffer);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#capacity()
	 */
	public int capacity() {
		return sb.capacity();
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#charAt(int)
	 */
	public char charAt(int index) {
		return sb.charAt(index);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#delete(int, int)
	 */
	public JSPStrBuffer delete(int start, int end) {
		sb.delete(start, end);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#deleteCharAt(int)
	 */
	public JSPStrBuffer deleteCharAt(int location) {
		sb.deleteCharAt(location);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#ensureCapacity(int)
	 */
	public void ensureCapacity(int min) {
		sb.ensureCapacity(min);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		return sb.equals(o);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#getChars(int, int, char[], int)
	 */
	public void getChars(int start, int end, char[] buffer, int index) {
		sb.getChars(start, end, buffer, index);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#hashCode()
	 */
	public int hashCode() {
		return sb.hashCode();
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#indexOf(java.lang.String, int)
	 */
	public int indexOf(String subString, int start) {
		return sb.indexOf(subString, start);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#indexOf(java.lang.String)
	 */
	public int indexOf(String string) {
		return sb.indexOf(string);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#insert(int, boolean)
	 */
	public JSPStrBuffer insert(int index, boolean value) {
		sb.insert(index, value);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#insert(int, char)
	 */
	public JSPStrBuffer insert(int index, char ch) {
		sb.insert(index, ch);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#insert(int, char[], int, int)
	 */
	public JSPStrBuffer insert(int index, char[] chars, int start, int length) {
		sb.insert(index, chars, start, length);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#insert(int, char[])
	 */
	public JSPStrBuffer insert(int index, char[] chars) {
		sb.insert(index, chars);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#insert(int, java.lang.CharSequence)
	 */
	public JSPStrBuffer insert(int index, CharSequence sequence) {
		sb.insert(index, sequence);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#insert(int, double)
	 */
	public JSPStrBuffer insert(int index, double value) {
		sb.insert(index, value);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#insert(int, float)
	 */
	public JSPStrBuffer insert(int index, float value) {
		sb.insert(index, value);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#insert(int, int)
	 */
	public JSPStrBuffer insert(int index, int value) {
		sb.insert(index, value);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#insert(int, long)
	 */
	public JSPStrBuffer insert(int index, long value) {
		sb.insert(index, value);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#insert(int, java.lang.Object)
	 */
	public JSPStrBuffer insert(int index, Object value) {
		sb.insert(index, value);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#insert(int, java.lang.String)
	 */
	public JSPStrBuffer insert(int index, String string) {
		sb.insert(index, string);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#lastIndexOf(java.lang.String, int)
	 */
	public int lastIndexOf(String subString, int start) {
		return sb.lastIndexOf(subString, start);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#lastIndexOf(java.lang.String)
	 */
	public int lastIndexOf(String string) {
		return sb.lastIndexOf(string);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#length()
	 */
	public int length() {
		return sb.length();
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#replace(int, int, java.lang.String)
	 */
	public JSPStrBuffer replace(int start, int end, String string) {
		sb.replace(start, end, string);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#reverse()
	 */
	public JSPStrBuffer reverse() {
		sb.reverse();
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#setCharAt(int, char)
	 */
	public void setCharAt(int index, char ch) {
		sb.setCharAt(index, ch);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#setLength(int)
	 */
	public void setLength(int length) {
		sb.setLength(length);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#subSequence(int, int)
	 */
	public CharSequence subSequence(int start, int end) {
		return sb.subSequence(start, end);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#substring(int, int)
	 */
	public String substring(int start, int end) {
		return sb.substring(start, end);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#substring(int)
	 */
	public String substring(int start) {
		return sb.substring(start);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.jsp.JSPStrBuffer#toString()
	 */
	public String toString() {
		return sb.toString();
	}
	
	//PM19500
	public void clear(){
		if (sb.capacity() > this.MAX_JSP_STR_BUFFER_SIZE_AFTER_CLEAR){
			sb = new StringBuffer(this.MAX_JSP_STR_BUFFER_SIZE_AFTER_CLEAR);
		}
	}
	//PM19500
}
