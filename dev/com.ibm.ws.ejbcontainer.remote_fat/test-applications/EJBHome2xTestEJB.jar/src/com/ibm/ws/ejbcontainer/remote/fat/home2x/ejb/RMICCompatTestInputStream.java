/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.home2x.ejb;

import java.io.Serializable;
import java.util.List;

import org.omg.CORBA.Any;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA_2_3.portable.InputStream;

/**
 * Dummy InputStream implementation that ensures generated stubs are RMIC
 * compatible and call read_value rather than read_abstract_interface.
 */
public class RMICCompatTestInputStream extends InputStream {
    private final Serializable value;

    RMICCompatTestInputStream(Serializable value) {
        this.value = value;
    }

    @Override
    public Serializable read_value(@SuppressWarnings("rawtypes") Class clz) {
        // Allow the call if the class is List.class.  This is the only method
        // that is implemented in this class.
        if (clz != List.class) {
            throw new NO_IMPLEMENT();
        }

        // A real implementation would copy, but we don't care.
        return value;
    }

    @Override
    public boolean read_boolean() {
        throw new NO_IMPLEMENT();
    }

    @Override
    public char read_char() {
        throw new NO_IMPLEMENT();
    }

    @Override
    public char read_wchar() {
        throw new NO_IMPLEMENT();
    }

    @Override
    public byte read_octet() {
        throw new NO_IMPLEMENT();
    }

    @Override
    public short read_short() {
        throw new NO_IMPLEMENT();
    }

    @Override
    public short read_ushort() {
        throw new NO_IMPLEMENT();
    }

    @Override
    public int read_long() {
        throw new NO_IMPLEMENT();
    }

    @Override
    public int read_ulong() {
        throw new NO_IMPLEMENT();
    }

    @Override
    public long read_longlong() {
        throw new NO_IMPLEMENT();
    }

    @Override
    public long read_ulonglong() {
        throw new NO_IMPLEMENT();
    }

    @Override
    public float read_float() {
        throw new NO_IMPLEMENT();
    }

    @Override
    public double read_double() {
        throw new NO_IMPLEMENT();
    }

    @Override
    public String read_string() {
        throw new NO_IMPLEMENT();
    }

    @Override
    public String read_wstring() {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void read_boolean_array(boolean[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void read_char_array(char[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void read_wchar_array(char[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void read_octet_array(byte[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void read_short_array(short[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void read_ushort_array(short[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void read_long_array(int[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void read_ulong_array(int[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void read_longlong_array(long[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void read_ulonglong_array(long[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void read_float_array(float[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void read_double_array(double[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public org.omg.CORBA.Object read_Object() {
        throw new NO_IMPLEMENT();
    }

    @Override
    public TypeCode read_TypeCode() {
        throw new NO_IMPLEMENT();
    }

    @Override
    public Any read_any() {
        throw new NO_IMPLEMENT();
    }
}
