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
 * This class is used as a structure to pass text contained in the underlying
 * character buffer of the scanner. The offset and length fields allow the
 * buffer to be re-used without creating new character arrays.
 * <p>
 * <strong>Note:</strong> Methods that are passed an XMLString structure
 * should consider the contents read-only and not make any modifications
 * to the contents of the buffer. The method receiving this structure
 * should also not modify the offset and length if this structure (or
 * the values of this structure) are passed to another method.
 * <p>
 * <strong>Note:</strong> Methods that are passed an XMLString structure
 * are required to copy the information out of the buffer if it is to be
 * saved for use beyond the scope of the method. The contents of the 
 * structure are volatile and the contents of the character buffer cannot
 * be assured once the method that is passed this structure returns.
 * Therefore, methods passed this structure should not save any reference
 * to the structure or the character array contained in the structure.
 *
 * @author Eric Ye, IBM
 * @author Andy Clark, IBM
 *
 */
public class XMLString {

    //
    // Data
    //

    /** The character array. */
    public char[] ch;

    /** The offset into the character array. */
    public int offset;

    /** The length of characters from the offset. */
    public int length;

    //
    // Constructors
    //

    /** Default constructor. */
    public XMLString() {
    } // <init>()

    /**
     * Constructs an XMLString structure preset with the specified
     * values.
     * 
     * @param ch     The character array.
     * @param offset The offset into the character array.
     * @param length The length of characters from the offset.
     */
    public XMLString(char[] ch, int offset, int length) {
        setValues(ch, offset, length);
    } // <init>(char[],int,int)

    /**
     * Constructs an XMLString structure with copies of the values in
     * the given structure.
     * <p>
     * <strong>Note:</strong> This does not copy the character array;
     * only the reference to the array is copied.
     *
     * @param string The XMLString to copy.
     */
    public XMLString(XMLString string) {
        setValues(string);
    } // <init>(XMLString)

    //
    // Public methods
    //

    /**
     * Initializes the contents of the XMLString structure with the
     * specified values.
     * 
     * @param ch     The character array.
     * @param offset The offset into the character array.
     * @param length The length of characters from the offset.
     */
    public void setValues(char[] ch, int offset, int length) {
        this.ch = ch;
        this.offset = offset;
        this.length = length;
    } // setValues(char[],int,int)

    /**
     * Initializes the contents of the XMLString structure with copies
     * of the given string structure.
     * <p>
     * <strong>Note:</strong> This does not copy the character array;
     * only the reference to the array is copied.
     * 
     * @param s
     */
    public void setValues(XMLString s) {
        setValues(s.ch, s.offset, s.length);
    } // setValues(XMLString)

    /** Resets all of the values to their defaults. */
    public void clear() {
        this.ch = null;
        this.offset = 0;
        this.length = -1;
    } // clear()

    /**
     * Returns true if the contents of this XMLString structure and
     * the specified array are equal.
     * 
     * @param ch     The character array.
     * @param offset The offset into the character array.
     * @param length The length of characters from the offset.
     */
    public boolean equals(char[] ch, int offset, int length) {
        if (ch == null) {
            return false;
        }
        if (this.length != length) {
            return false;
        }

        for (int i=0; i<length; i++) {
            if (this.ch[this.offset+i] != ch[offset+i] ) {
                return false;
            }
        }
        return true;
    } // equals(char[],int,int):boolean

    /**
     * Returns true if the contents of this XMLString structure and
     * the specified string are equal.
     * 
     * @param s The string to compare.
     */
    public boolean equals(String s) {
        if (s == null) {
            return false;
        }
        if ( length != s.length() ) {
            return false;
        }

        // is this faster than call s.toCharArray first and compare the 
        // two arrays directly, which will possibly involve creating a
        // new char array object.
        for (int i=0; i<length; i++) {
            if (ch[offset+i] != s.charAt(i)) {
                return false;
            }
        }

        return true;
    } // equals(String):boolean

    //
    // Object methods
    //

    /** Returns a string representation of this object. */
    public String toString() {
        return length > 0 ? new String(ch, offset, length) : "";
    } // toString():String

} // class XMLString
