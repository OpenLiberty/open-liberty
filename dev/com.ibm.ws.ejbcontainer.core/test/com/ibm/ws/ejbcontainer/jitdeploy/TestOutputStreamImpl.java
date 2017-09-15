/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jitdeploy;

import java.io.Serializable;

import org.omg.CORBA.Any;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;

public class TestOutputStreamImpl
                extends OutputStream
{
    private final TestMethodCalls ivCalls;

    TestOutputStreamImpl(TestMethodCalls calls)
    {
        ivCalls = calls;
    }

    @Override
    public InputStream create_input_stream()
    {
        throw new UnsupportedOperationException();
    }

    private void checkCall(String methodName, Object value)
    {
        String utilMethodName = TestUtilDelegateImpl.resetMethodName();
        ivCalls.invoke(utilMethodName == null ? methodName : utilMethodName + "+" + methodName, value);
    }

    @Override
    public void write_boolean(boolean value)
    {
        checkCall("write_boolean", value);
    }

    @Override
    public void write_char(char value)
    {
        checkCall("write_char", value);
    }

    @Override
    public void write_wchar(char value)
    {
        checkCall("write_wchar", value);
    }

    @Override
    public void write_octet(byte value)
    {
        checkCall("write_octet", value);
    }

    @Override
    public void write_short(short value)
    {
        checkCall("write_short", value);
    }

    @Override
    public void write_ushort(short value)
    {
        checkCall("write_ushort", value);
    }

    @Override
    public void write_long(int value)
    {
        checkCall("write_long", value);
    }

    @Override
    public void write_ulong(int value)
    {
        checkCall("write_ulong", value);
    }

    @Override
    public void write_longlong(long value)
    {
        checkCall("write_longlong", value);
    }

    @Override
    public void write_ulonglong(long value)
    {
        checkCall("write_ulonglong", value);
    }

    @Override
    public void write_float(float value)
    {
        checkCall("write_float", value);
    }

    @Override
    public void write_double(double value)
    {
        checkCall("write_double", value);
    }

    @Override
    public void write_string(String value)
    {
        checkCall("write_string", value);
    }

    @Override
    public void write_wstring(String value)
    {
        checkCall("write_wstring", value);
    }

    @Override
    public void write_boolean_array(boolean[] value, int offset, int length)
    {
        checkCall("write_boolean_array", value);
    }

    @Override
    public void write_char_array(char[] value, int offset, int length)
    {
        checkCall("write_char_array", value);
    }

    @Override
    public void write_wchar_array(char[] value, int offset, int length)
    {
        checkCall("write_wchar_array", value);
    }

    @Override
    public void write_octet_array(byte[] value, int offset, int length)
    {
        checkCall("write_octet_array", value);
    }

    @Override
    public void write_short_array(short[] value, int offset, int length)
    {
        checkCall("write_short_array", value);
    }

    @Override
    public void write_ushort_array(short[] value, int offset, int length)
    {
        checkCall("write_ushort_array", value);
    }

    @Override
    public void write_long_array(int[] value, int offset, int length)
    {
        checkCall("write_long_array", value);
    }

    @Override
    public void write_ulong_array(int[] value, int offset, int length)
    {
        checkCall("write_ulong_array", value);
    }

    @Override
    public void write_longlong_array(long[] value, int offset, int length)
    {
        checkCall("write_longlong_array", value);
    }

    @Override
    public void write_ulonglong_array(long[] value, int offset, int length)
    {
        checkCall("write_ulonglong_array", value);
    }

    @Override
    public void write_float_array(float[] value, int offset, int length)
    {
        checkCall("write_float_array", value);
    }

    @Override
    public void write_double_array(double[] value, int offset, int length)
    {
        checkCall("write_double_array", value);
    }

    @Override
    public void write_Object(org.omg.CORBA.Object value)
    {
        checkCall("write_Object", value);
    }

    @Override
    public void write_TypeCode(TypeCode value)
    {
        checkCall("write_TypeCode", value);
    }

    @Override
    public void write_any(Any value)
    {
        checkCall("write_any", value);
    }

    @Override
    public void write_value(Serializable value)
    {
        checkCall("write_value", value);
    }

    @Override
    public void write_value(Serializable value, @SuppressWarnings("rawtypes") Class klass)
    {
        checkCall("write_value@2", value);
    }

    @Override
    public void write_abstract_interface(Object obj)
    {
        checkCall("write_abstract_interface", obj);
    }
}
