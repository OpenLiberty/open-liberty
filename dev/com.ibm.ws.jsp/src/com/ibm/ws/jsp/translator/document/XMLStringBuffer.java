/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// jsp2.1work

package com.ibm.ws.jsp.translator.document;

/**
 * XMLString is a structure used to pass character arrays. However,
 * XMLStringBuffer is a buffer in which characters can be appended
 * and extends XMLString so that it can be passed to methods
 * expecting an XMLString object. This is a safe operation because
 * it is assumed that any callee will <strong>not</strong> modify
 * the contents of the XMLString structure.
 * <p> 
 * The contents of the string are managed by the string buffer. As
 * characters are appended, the string buffer will grow as needed.
 * <p>
 * <strong>Note:</strong> Never set the <code>ch</code>, 
 * <code>offset</code>, and <code>length</code> fields directly.
 * These fields are managed by the string buffer. In order to reset
 * the buffer, call <code>clear()</code>.
 * 
 * @author Andy Clark, IBM
 * @author Eric Ye, IBM
 *
 */
public class XMLStringBuffer
    extends XMLString {

    //
    // Constants
    //

    /** Default buffer size (32). */
    public static final int DEFAULT_SIZE = 32;

    //
    // Constructors
    //

    /**
     * 
     */
    public XMLStringBuffer() {
        this(DEFAULT_SIZE);
    } // <init>()

    /**
     * 
     * 
     * @param size 
     */
    public XMLStringBuffer(int size) {
        ch = new char[size];
    } // <init>(int)

    /** Constructs a string buffer from a char. */
    public XMLStringBuffer(char c) {
        this(1);
        append(c);
    } // <init>(char)

    /** Constructs a string buffer from a String. */
    public XMLStringBuffer(String s) {
        this(s.length());
        append(s);
    } // <init>(String)

    /** Constructs a string buffer from the specified character array. */
    public XMLStringBuffer(char[] ch, int offset, int length) {
        this(length);
        append(ch, offset, length);
    } // <init>(char[],int,int)

    /** Constructs a string buffer from the specified XMLString. */
    public XMLStringBuffer(XMLString s) {
        this(s.length);
        append(s);
    } // <init>(XMLString)

    //
    // Public methods
    //

    /** Clears the string buffer. */
    public void clear() {
        offset = 0;
        length = 0;
    }

    /**
     * append
     * 
     * @param c 
     */
    public void append(char c) {
        if (this.length + 1 > this.ch.length) {
                    int newLength = this.ch.length*2;
                    if (newLength < this.ch.length + DEFAULT_SIZE)
                        newLength = this.ch.length + DEFAULT_SIZE;
                    char[] newch = new char[newLength];
                    System.arraycopy(this.ch, 0, newch, 0, this.length);
                    this.ch = newch;
        }
        this.ch[this.length] = c;
        this.length++;
    } // append(char)

    /**
     * append
     * 
     * @param s 
     */
    public void append(String s) {
        int length = s.length();
        if (this.length + length > this.ch.length) {
            int newLength = this.ch.length*2;
            if (newLength < this.length + length + DEFAULT_SIZE)
                newLength = this.ch.length + length + DEFAULT_SIZE;
            char[] newch = new char[newLength];            
            System.arraycopy(this.ch, 0, newch, 0, this.length);
            this.ch = newch;
        }
        s.getChars(0, length, this.ch, this.length);
        this.length += length;
    } // append(String)

    /**
     * append
     * 
     * @param ch 
     * @param offset 
     * @param length 
     */
    public void append(char[] ch, int offset, int length) {
        if (this.length + length > this.ch.length) {
            char[] newch = new char[this.ch.length + length + DEFAULT_SIZE];
            System.arraycopy(this.ch, 0, newch, 0, this.length);
            this.ch = newch;
        }
        System.arraycopy(ch, offset, this.ch, this.length, length);
        this.length += length;
    } // append(char[],int,int)

    /**
     * append
     * 
     * @param s 
     */
    public void append(XMLString s) {
        append(s.ch, s.offset, s.length);
    } // append(XMLString)

} // class XMLStringBuffer
