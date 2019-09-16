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
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;

/**
 * Dummy OutputStream implementation that ensures generated stubs are RMIC
 * compatible and call write_value rather than Util.writeAbstractObject.
 */
public class RMICCompatTestOutputStream extends OutputStream {
    public Serializable value;

    @Override
    public void write_value(Serializable value, @SuppressWarnings("rawtypes") Class clz) {
        // Allow the call if the class is List.class.  This is the only method
        // that is implemented in this class.
        if (clz != List.class) {
            throw new NO_IMPLEMENT();
        }
        this.value = value;
    }

    @Override
    public InputStream create_input_stream() {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_boolean(boolean value) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_char(char value) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_wchar(char value) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_octet(byte value) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_short(short value) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_ushort(short value) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_long(int value) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_ulong(int value) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_longlong(long value) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_ulonglong(long value) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_float(float value) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_double(double value) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_string(String value) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_wstring(String value) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_boolean_array(boolean[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_char_array(char[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_wchar_array(char[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_octet_array(byte[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_short_array(short[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_ushort_array(short[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_long_array(int[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_ulong_array(int[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_longlong_array(long[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_ulonglong_array(long[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_float_array(float[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_double_array(double[] value, int offset, int length) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_Object(org.omg.CORBA.Object value) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_TypeCode(TypeCode value) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void write_any(Any value) {
        throw new NO_IMPLEMENT();
    }
}
