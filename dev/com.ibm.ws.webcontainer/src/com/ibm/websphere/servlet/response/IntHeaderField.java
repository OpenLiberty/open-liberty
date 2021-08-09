/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.response;

import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.TraceNLS;

class IntHeaderField extends HeaderField
{
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3258125851886630194L;
	private HeaderField _next;
    private String _name;
    private int _value;
    private static TraceNLS nls = TraceNLS.getTraceNLS(IntHeaderField.class, "com.ibm.ws.webcontainer.resources.Messages");

    public IntHeaderField(String name, int value, HeaderField next)
    {
        _name = name;
        _value = value;
        _next = next;
    }

    public int getIntValue()
    {
        return _value;
    }

    public long getDateValue()
    {
        throw new IllegalStateException(nls.getString("Unsupported.conversion","Unsupported conversion"));
    }

    public String getStringValue()
    {
        return new Integer(getIntValue()).toString();
    }

    public void setIntValue(int val)
    {
        _value = val;
    }

    public void setDateValue(long date)
    {
        throw new IllegalStateException(nls.getString("Unsupported.conversion","Unsupported conversion"));
    }

    public void setStringValue(String s)
    {
        _value = (new Integer(s)).intValue();
    }

    public String getName()
    {
        return _name;
    }

    public void transferHeader(HttpServletResponse resp)
    {
        _next.transferHeader(resp);
        resp.setIntHeader(getName(), getIntValue());
    }

    public HeaderField getNextField()
    {
        if (hasMoreFields())
        {
            return _next;
        }
        else
        {
            throw new NoSuchElementException();
        }
    }

    public boolean hasMoreFields()
    {
        return true;
//        return !_next.isNil();
    }

    public boolean isNil()
    {
        return false;
    }
}