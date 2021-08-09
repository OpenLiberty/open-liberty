/*******************************************************************************
 * Copyright (c) 2003,2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.parser.util;

import java.io.CharArrayWriter;
import java.math.BigDecimal;

import com.ibm.ws.sip.parser.MessageParser;

/**
 * @author Amir Perlman, Dec 21 2004
 * 
 * Provides similar API to StringBuffer but enables optimization as it allows
 * access to its internal buffers. Should be used with care since its internal
 * buffer may be manipulated outside of this class. Beware that CharsBuffer is
 * not java.nio.CharBuffer .
 */
public class CharsBuffer extends CharArrayWriter {
    /**
     * Byte buffer used for converting char[] to byte[].
     */
    private byte[] _bytes = new byte[0];

    /**
     * Number of bytes available to read from the converted to UTF-8 byte []
     */
    private int _bytesAvailable = 0;

    /**
     * Gets refernce to the internal char array. Should be used with care as the
     * internal buffer is shared and might change its content or might be
     * replaced if buffer needs to grow.
     * 
     * @return
     */
    public char[] getCharArray() {
        return buf;
    }

    /**
     * Gets content size of the char array
     * 
     * @return number of chars written
     */
    public int getCharCount() {
        return count;
    }

    /**
     * gets one char at a specific position
     * @param index requested position
     * @return the char at the requested position
     */
    public char charAt(int index) {
    	return buf[index];
    }

    /**
     * Append character to existing content. Added this function to avoid
     * changing code that has append in it because of previous use of
     * StringBuffer
     * 
     * @param c
     */
    public CharsBuffer append(char c) {
        write(c);
        return this;
    }

    /**
     * Append float value to existing content. Added this function to avoid
     * changing code that has append in it because of previous use of
     * StringBuffer
     * 
     * @param value
     */
    public CharsBuffer append(float value) {
        append(Float.toString(value));
        return this;
    }
    
	/**
	 * writes a double number to this buffer,
	 * in plain (non-scientific) format.
	 * @param value the number to write
	 */
	public void append(double value) {
		BigDecimal bd = new BigDecimal(value);
		append(bd.toPlainString());
	}

    /**
     * Append int value to existing content. Added this function to avoid
     * changing code that has append in it because of previous use of
     * StringBuffer
     * 
     * @param value
     */
    public CharsBuffer append(int value) {
    	if (value == 0) {
			append('0');
			return this;
		}
    	if (value < 0) {
			append('-');
			value = -value;
		}
    	appendNumber(value);
		return this;
    }
    
    /**
	 * Append long value to existing content.
	 * 
	 * @param value
	 */
    public CharsBuffer append(long value) {
    	if (value == 0) {
			append('0');
			return this;
		}
    	if (value < 0) {
			append('-');
			value = -value;
		}
    	appendNumber(value);
		return this;
    }

    /**
     * Append short value to existing content.
     * 
     * @param value
     */
    public CharsBuffer append(short value) {
    	if (value == 0) {
			append('0');
			return this;
		}
    	if (value < 0) {
			append('-');
			value = (short)-value;
		}
    	appendNumber(value);
		return this;
    }

    /**
     * appends a positive int to the buffer.
     * @param value the number to append
     */
    private void appendNumber(int value) {
		char dig = (char)('0' + value % 10); // rightmost digit
		int remain = value / 10;
    	if (remain > 0) {
        	// recurse digits from right to left
    		appendNumber(remain);
    	}
		append(dig);
    }

    /**
     * appends a positive long to the buffer.
     * @param value the number to append
     */
    private void appendNumber(long value) {
		char dig = (char)('0' + value % 10); // rightmost digit
		long remain = value / 10;
    	if (remain > 0) {
        	// recurse digits from right to left
    		appendNumber(remain);
    	}
		append(dig);
    }

    /**
     * appends a positive short to the buffer.
     * @param value the number to append
     */
    private void appendNumber(short value) {
		char dig = (char)('0' + value % 10); // rightmost digit
		long remain = value / 10;
    	if (remain > 0) {
        	// recurse digits from right to left
    		appendNumber(remain);
    	}
		append(dig);
    }

    /**
     * Append the object's string representationto existing content.
     * 
     * @param obj
     */
    public CharsBuffer append(Object obj) {
        append(String.valueOf(obj));
        return this;
    }

    /**
     * Append String contents to existing content
     * 
     * @param str
     */
    public CharsBuffer append(String str) {
        if (str == null) {
            str = "";
        }

        write(str, 0, str.length());
        return this;
    }

    /**
     * Append array of chars to existing content
     * 
     * @param str array of chars to be appended to the end of this buffer
     * @param length number of chars to take from char array. this may be
     * less than or equal to str.length
     */
    public CharsBuffer append(char[] str, int length) {
        write(str, 0, length);
        return this;
    }
    
	/**
	 * Appends array of bytes
	 * 
	 * @param bytes source byte array, utf-8 representation
	 * @param offset offset into source byte array
	 * @param length bytes available in source byte array
	 * @return true on success, false on failure
	 */
	public boolean append(byte[] bytes, int offset, int length) {
		final int end = offset + length; // index to one-past source array
		for (int i = offset; i < end; i++) {
			byte b = bytes[i];
			char c;
			
			if ((b & 0x80) == 0x80) {
				int size = MessageParser.utf8size(b);
				if (size == -1) {
					// packet sliced in the middle of a utf-8 character
					return false;
				}
				int value = MessageParser.utf8(bytes, i, end-i, size);
				c = (char)value;
				i += size-1;
			}
			else {
				// 7-bit ascii
				c = (char)b;
			}
			append(c);
		}
		return true;
	}

    /**
     * trims off trailing whitespace (right-trim)
     */
    public void rtrim() {
    	while (count > 0 && Character.isWhitespace(buf[count-1])) {
    		count--;
    	}
    }
    
    /**
     * rewinds buffer back to specified position
     * @param position number of bytes to remain in buffer after rewinding
     */
    public void rewind(int position) {
    	count = position;
    }

    /**
     * Get content of characters array as a byte array converted according to
     * UTF-8 encoding. The returned byte[] array is also unsafe and is reused by
     * this object.
     * 
     * @return The sequence of chars converted to byte array according to UTF-8
     *         encoding.
     */
    public byte[] getBytes() {
        //Make sure that the byte [] is big enough - at most conversion
        //from unicode to UTF-8 could result in 4 bytes for each character
        if (_bytes.length / 4 < buf.length) {
            _bytes = new byte[buf.length * 4];
        }

        //reset the number of bytes available
        _bytesAvailable = 0;

        //Convert unicode to utf-8. We need to switch to the java nio
        // conversion
        //API. At the moment I will not use is as it requires switching to the
        //nio buffer structures which we don't use yet.
        char c;
        int j = 0;
        for (int i = 0; i < size(); i++) {
            c = buf[i];
            if (c < 0x80) {
                _bytes[j++] = (byte) c;
            }
            else if (c < 0x800) {
                _bytes[j++] = (byte) (0xC0 | c >> 6);
                _bytes[j++] = (byte) (0x80 | c & 0x3F);
            }
            else if (c < 0x10000) {
                _bytes[j++] = (byte) (0xE0 | c >> 12);
                _bytes[j++] = (byte) (0x80 | c >> 6 & 0x3F);
                _bytes[j++] = (byte) (0x80 | c & 0x3F);
            }
            else if (c < 0x200000) {
                _bytes[j++] = (byte) (0xF0 | c >> 18);
                _bytes[j++] = (byte) (0x80 | c >> 12 & 0x3F);
                _bytes[j++] = (byte) (0x80 | c >> 6 & 0x3F);
                _bytes[j++] = (byte) (0x80 | c & 0x3F);
            }
        }

        _bytesAvailable = j;
        return _bytes;
    }

    /**
     * Gets the size of the byte[] after conversion has been made from chars to
     * bytes using the getBytes method. Should be called after the getBytes
     * method had made the conversion.
     */
    public int getBytesSize() {
        return _bytesAvailable;
    }

	/**
	 * @return a hash code for the contents of this buffer
	 */
	public int hashCode() {
		int hash = 0;
		char[] array = buf;
		int carry; // leftmost bit to be recycled
		
		for (int i = count-1; i >= 0; i--) {
			carry = (hash & 0x80000000) == 0 ? 0 : 1;
			hash <<= 1;
			hash |= carry;
			hash ^= array[i];
		}

        return hash;
	}
}
