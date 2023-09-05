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

import java.io.ByteArrayInputStream;

import org.omg.CORBA.Any;
import org.omg.CORBA.Object;
import org.omg.CORBA.TypeCode;

/**
 * Test output stream used while encoding/decoding a token.
 * Only the read_long and read_octet methods are needed for the current tests.
 */
public class InputStreamTestDouble extends org.omg.CORBA_2_3.portable.InputStream {

    private final ByteArrayInputStream bais;

    public InputStreamTestDouble(byte[] data) {
        bais = new ByteArrayInputStream(data);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_Object()
     */
    @Override
    public Object read_Object() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_TypeCode()
     */
    @Override
    public TypeCode read_TypeCode() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_any()
     */
    @Override
    public Any read_any() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_boolean()
     */
    @Override
    public boolean read_boolean() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_boolean_array(boolean[], int, int)
     */
    @Override
    public void read_boolean_array(boolean[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_char()
     */
    @Override
    public char read_char() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_char_array(char[], int, int)
     */
    @Override
    public void read_char_array(char[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_double()
     */
    @Override
    public double read_double() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_double_array(double[], int, int)
     */
    @Override
    public void read_double_array(double[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_float()
     */
    @Override
    public float read_float() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_float_array(float[], int, int)
     */
    @Override
    public void read_float_array(float[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_long()
     */
    @Override
    public int read_long() {
        int fourthByte = bais.read();
        int thirdByte = bais.read();
        int secondByte = bais.read();
        int firstByte = bais.read();
        int value = (fourthByte << 24) + (thirdByte << 16) + (secondByte << 8) + firstByte;
        return value;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_long_array(int[], int, int)
     */
    @Override
    public void read_long_array(int[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_longlong()
     */
    @Override
    public long read_longlong() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_longlong_array(long[], int, int)
     */
    @Override
    public void read_longlong_array(long[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_octet()
     */
    @Override
    public byte read_octet() {
        return (byte) bais.read();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_octet_array(byte[], int, int)
     */
    @Override
    public void read_octet_array(byte[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_short()
     */
    @Override
    public short read_short() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_short_array(short[], int, int)
     */
    @Override
    public void read_short_array(short[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_string()
     */
    @Override
    public String read_string() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_ulong()
     */
    @Override
    public int read_ulong() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_ulong_array(int[], int, int)
     */
    @Override
    public void read_ulong_array(int[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_ulonglong()
     */
    @Override
    public long read_ulonglong() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_ulonglong_array(long[], int, int)
     */
    @Override
    public void read_ulonglong_array(long[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_ushort()
     */
    @Override
    public short read_ushort() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_ushort_array(short[], int, int)
     */
    @Override
    public void read_ushort_array(short[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_wchar()
     */
    @Override
    public char read_wchar() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_wchar_array(char[], int, int)
     */
    @Override
    public void read_wchar_array(char[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.omg.CORBA.portable.InputStream#read_wstring()
     */
    @Override
    public String read_wstring() {
        // TODO Auto-generated method stub
        return null;
    }
}
