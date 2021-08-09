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
import org.omg.CORBA_2_3.portable.InputStream;

public class TestInputStreamImpl
                extends InputStream
{
    private TestMethodCalls ivCalls;

    TestInputStreamImpl(TestMethodCalls calls)
    {
        ivCalls = calls;
    }

    @SuppressWarnings("unchecked")
    private <T> T checkCall(String methodName)
    {
        String utilMethodName = TestUtilDelegateImpl.resetMethodName();
        return (T) ivCalls.invoke(utilMethodName == null ? methodName : utilMethodName + "+" + methodName, (Object[]) null);
    }

    @Override
    public boolean read_boolean()
    {
        return (Boolean) checkCall("read_boolean");
    }

    @Override
    public char read_char()
    {
        return (Character) checkCall("read_char");
    }

    @Override
    public char read_wchar()
    {
        return (Character) checkCall("read_wchar");
    }

    @Override
    public byte read_octet()
    {
        return (Byte) checkCall("read_octet");
    }

    @Override
    public short read_short()
    {
        return (Short) checkCall("read_short");
    }

    @Override
    public short read_ushort()
    {
        return (Short) checkCall("read_ushort");
    }

    @Override
    public int read_long()
    {
        return (Integer) checkCall("read_long");
    }

    @Override
    public int read_ulong()
    {
        return (Integer) checkCall("read_ulong");
    }

    @Override
    public long read_longlong()
    {
        return (Long) checkCall("read_longlong");
    }

    @Override
    public long read_ulonglong()
    {
        return (Long) checkCall("read_ulonglong");
    }

    @Override
    public float read_float()
    {
        return (Float) checkCall("read_float");
    }

    @Override
    public double read_double()
    {
        return (Double) checkCall("read_double");
    }

    @Override
    public String read_string()
    {
        return checkCall("read_string");
    }

    @Override
    public String read_wstring()
    {
        return checkCall("read_wstring");
    }

    @Override
    public void read_boolean_array(boolean[] value, int offset, int length)
    {
        checkCall("read_boolean_array");
    }

    @Override
    public void read_char_array(char[] value, int offset, int length)
    {
        checkCall("read_char_array");
    }

    @Override
    public void read_wchar_array(char[] value, int offset, int length)
    {
        checkCall("read_wchar_array");
    }

    @Override
    public void read_octet_array(byte[] value, int offset, int length)
    {
        checkCall("read_octet_array");
    }

    @Override
    public void read_short_array(short[] value, int offset, int length)
    {
        checkCall("read_short_array");
    }

    @Override
    public void read_ushort_array(short[] value, int offset, int length)
    {
        checkCall("read_ushort_array");
    }

    @Override
    public void read_long_array(int[] value, int offset, int length)
    {
        checkCall("read_long_array");
    }

    @Override
    public void read_ulong_array(int[] value, int offset, int length)
    {
        checkCall("read_ulong_array");
    }

    @Override
    public void read_longlong_array(long[] value, int offset, int length)
    {
        checkCall("read_longlong_array");
    }

    @Override
    public void read_ulonglong_array(long[] value, int offset, int length)
    {
        checkCall("read_ulonglong_array");
    }

    @Override
    public void read_float_array(float[] value, int offset, int length)
    {
        checkCall("read_float_array");
    }

    @Override
    public void read_double_array(double[] value, int offset, int length)
    {
        checkCall("read_double_array");
    }

    @Override
    public org.omg.CORBA.Object read_Object()
    {
        return checkCall("read_Object");
    }

    @Override
    public TypeCode read_TypeCode()
    {
        return checkCall("read_TypeCode");
    }

    @Override
    public Any read_any()
    {
        return checkCall("read_any");
    }

    @Override
    public org.omg.CORBA.Object read_Object(@SuppressWarnings("rawtypes") Class clz)
    {
        return checkCall("read_Object@1");
    }

    @Override
    public Serializable read_value()
    {
        return checkCall("read_value");
    }

    @Override
    public Serializable read_value(@SuppressWarnings("rawtypes") Class klass)
    {
        return checkCall("read_value@1");
    }

    @Override
    public Object read_abstract_interface()
    {
        return checkCall("read_abstract_interface");
    }

    @Override
    public Object read_abstract_interface(@SuppressWarnings("rawtypes") Class klass)
    {
        return checkCall("read_abstract_interface@1");
    }
}
