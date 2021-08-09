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

import java.io.Serializable;
import java.util.Enumeration;

import javax.servlet.http.HttpServletResponse;

class StoredHeader implements Serializable
{
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3905521588232138806L;
	private HeaderField _field;
    private int _size;


    public StoredHeader()
    {
        _field = NilHeaderField.instance();
    }
    /**
     * Retrieve the names of the header fields that have been set on this response.
     */
    @SuppressWarnings("unchecked")
    public Enumeration getHeaderNames()
    {
        return new HeaderFieldNames(_field);
    }

    /**
     * Retrieve a response header field by name.
     */
    public String getHeader(String name)
    {
        HeaderField f = getHeaderField(name);
        if (f != null)
        {
            return f.getStringValue();
        }
        else
        {
            return null;
        }
    }

    /**
     * Retrieve a response header as an int.
     */
    public int getIntHeader(String name)
    {
        HeaderField f = getHeaderField(name);
        if (f != null)
        {
            return f.getIntValue();
        }
        else
        {
            return -1;
        }
    }

    /**
     * Retrieve a response header as a date.
     */
    public long getDateHeader(String name)
    {
        HeaderField f = getHeaderField(name);
        if (f != null)
        {
            return f.getDateValue();
        }
        else
        {
            return -1;
        }
    }

    public boolean containsHeader(String name)
    {
        return getHeaderField(name) != null;
    }

    public synchronized void setHeader(String name, String value)
    {
        HeaderField f = getHeaderField(name);
        if (f != null)
        {
            f.setStringValue(value);
        }
        else
        {
            _field = new StringHeaderField(name, value, _field);
            _size++;
        }
    }

    public synchronized void setIntHeader(String name, int value)
    {
        HeaderField f = getHeaderField(name);
        if (f != null)
        {
            f.setIntValue(value);
        }
        else
        {
            _field = new IntHeaderField(name, value, _field);
            _size++;
        }
    }

    public synchronized void setDateHeader(String name, long value)
    {
        HeaderField f = getHeaderField(name);
        if (f != null)
        {
            f.setDateValue(value);
        }
        else
        {
            _field = new DateHeaderField(name, value, _field);
            _size++;
        }
    }

    public int getSize()
    {
        return _size;
    }

    public void transferHeader(HttpServletResponse resp)
    {
        _field.transferHeader(resp);
    }

    private HeaderField getHeaderField(String name)
    {
        String lName = name.toLowerCase();
        HeaderField f = _field;
        while (!f.isNil())
        {
            if (f.getName().toLowerCase().equals(lName))
            {
                return f;
            }
            if (f.hasMoreFields())
            {
                f = f.getNextField();
            }
            else
            {
                return null;
            }
        }
        return null;
    }
}
