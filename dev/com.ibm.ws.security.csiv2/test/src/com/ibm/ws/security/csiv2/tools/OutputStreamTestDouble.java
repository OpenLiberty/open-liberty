/*
 * Copyright (c) 2015,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.security.csiv2.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.omg.CORBA.Any;
import org.omg.CORBA.Object;
import org.omg.CORBA.TypeCode;

/**
 * Test output stream used while encoding/decoding a token.
 * Only toByteArray, write_long and write_octet are needed for the current tests.
 */
public class OutputStreamTestDouble extends org.omg.CORBA_2_3.portable.OutputStream {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#create_input_stream()
     */
    @Override
    public org.omg.CORBA.portable.InputStream create_input_stream() {
        // TODO Auto-generated method stub
        return null;
    }

    public byte[] toByteArray() {
        return baos.toByteArray();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_Object(org.omg.CORBA.Object)
     */
    @Override
    public void write_Object(Object value) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_TypeCode(org.omg.CORBA.TypeCode)
     */
    @Override
    public void write_TypeCode(TypeCode value) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_any(org.omg.CORBA.Any)
     */
    @Override
    public void write_any(Any value) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_boolean(boolean)
     */
    @Override
    public void write_boolean(boolean value) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_boolean_array(boolean[], int, int)
     */
    @Override
    public void write_boolean_array(boolean[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_char(char)
     */
    @Override
    public void write_char(char value) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_char_array(char[], int, int)
     */
    @Override
    public void write_char_array(char[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_double(double)
     */
    @Override
    public void write_double(double value) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_double_array(double[], int, int)
     */
    @Override
    public void write_double_array(double[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_float(float)
     */
    @Override
    public void write_float(float value) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_float_array(float[], int, int)
     */
    @Override
    public void write_float_array(float[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_long(int)
     */
    @Override
    public void write_long(int value) {
        baos.write((byte) (value >> 24));
        baos.write((byte) (value >> 16));
        baos.write((byte) (value >> 8));
        baos.write((byte) value);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_long_array(int[], int, int)
     */
    @Override
    public void write_long_array(int[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_longlong(long)
     */
    @Override
    public void write_longlong(long value) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_longlong_array(long[], int, int)
     */
    @Override
    public void write_longlong_array(long[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_octet(byte)
     */
    @Override
    public void write_octet(byte value) {
        try {
            baos.write(new byte[] { value });
        } catch (IOException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_octet_array(byte[], int, int)
     */
    @Override
    public void write_octet_array(byte[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_short(short)
     */
    @Override
    public void write_short(short value) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_short_array(short[], int, int)
     */
    @Override
    public void write_short_array(short[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_string(java.lang.String)
     */
    @Override
    public void write_string(String value) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_ulong(int)
     */
    @Override
    public void write_ulong(int value) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_ulong_array(int[], int, int)
     */
    @Override
    public void write_ulong_array(int[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_ulonglong(long)
     */
    @Override
    public void write_ulonglong(long value) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_ulonglong_array(long[], int, int)
     */
    @Override
    public void write_ulonglong_array(long[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_ushort(short)
     */
    @Override
    public void write_ushort(short value) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_ushort_array(short[], int, int)
     */
    @Override
    public void write_ushort_array(short[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_wchar(char)
     */
    @Override
    public void write_wchar(char value) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_wchar_array(char[], int, int)
     */
    @Override
    public void write_wchar_array(char[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.OutputStream#write_wstring(java.lang.String)
     */
    @Override
    public void write_wstring(String value) {
        // TODO Auto-generated method stub

    }

}
