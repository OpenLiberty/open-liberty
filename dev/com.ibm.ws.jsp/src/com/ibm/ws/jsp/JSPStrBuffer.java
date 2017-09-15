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
// PM19500  Mem leak in WASJSPStrBufferImpl  07/28/10  pmdinh

package com.ibm.ws.jsp;

public interface JSPStrBuffer {

	public abstract JSPStrBuffer append(boolean value);

	public abstract JSPStrBuffer append(char ch);

	public abstract JSPStrBuffer append(char[] chars, int start, int length);

	public abstract JSPStrBuffer append(char[] chars);

	public abstract JSPStrBuffer append(CharSequence sequence);

	public abstract JSPStrBuffer append(double value);

	public abstract JSPStrBuffer append(float value);

	public abstract JSPStrBuffer append(int value);

	public abstract JSPStrBuffer append(long value);

	public abstract JSPStrBuffer append(Object value);

	public abstract JSPStrBuffer append(String string);

	public abstract JSPStrBuffer append(JSPStrBuffer sbuffer);

	public abstract int capacity();

	public abstract char charAt(int index);
	
	public abstract JSPStrBuffer delete(int start, int end);

	public abstract JSPStrBuffer deleteCharAt(int location);

	public abstract void ensureCapacity(int min);

	public abstract boolean equals(Object o);

	public abstract void getChars(int start, int end, char[] buffer, int index);

	public abstract int hashCode();

	public abstract int indexOf(String subString, int start);

	public abstract int indexOf(String string);

	public abstract JSPStrBuffer insert(int index, boolean value);

	public abstract JSPStrBuffer insert(int index, char ch);

	public abstract JSPStrBuffer insert(int index, char[] chars, int start, int length);

	public abstract JSPStrBuffer insert(int index, char[] chars);

	public abstract JSPStrBuffer insert(int index, CharSequence sequence);

	public abstract JSPStrBuffer insert(int index, double value);

	public abstract JSPStrBuffer insert(int index, float value);

	public abstract JSPStrBuffer insert(int index, int value);

	public abstract JSPStrBuffer insert(int index, long value);

	public abstract JSPStrBuffer insert(int index, Object value);

	public abstract JSPStrBuffer insert(int index, String string);

	public abstract int lastIndexOf(String subString, int start);

	public abstract int lastIndexOf(String string);

	public abstract int length();

	public abstract JSPStrBuffer replace(int start, int end, String string);

	public abstract JSPStrBuffer reverse();

	public abstract void setCharAt(int index, char ch);

	public abstract void setLength(int length);

	public abstract CharSequence subSequence(int start, int end);

	public abstract String substring(int start, int end);

	public abstract String substring(int start);

	public abstract String toString();
	
	public abstract void clear();							//PM19500
}