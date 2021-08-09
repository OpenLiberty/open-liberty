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

class NilHeaderField extends HeaderField
{
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3545796567979079990L;
	private static NilHeaderField _instance;
    private NilHeaderField()
    {
    }

    public static synchronized NilHeaderField instance()
    {
        if (_instance == null)
        {
            _instance = new NilHeaderField();
        }
        return _instance;
    }

    public int getIntValue()
    {
        throw new IllegalStateException("Nil Header Field");
    }

    public long getDateValue()
    {
        throw new IllegalStateException("Nil Header Field");
    }

    public String getStringValue()
    {
        throw new IllegalStateException("Nil Header Field");
    }

    public void setIntValue(int val)
    {
        throw new IllegalStateException("Nil Header Field");
    }

    public void setDateValue(long date)
    {
        throw new IllegalStateException("Nil Header Field");
    }

    public void setStringValue(String s)
    {
        throw new IllegalStateException("Nil Header Field");
    }

    public String getName()
    {
        throw new IllegalStateException("Nil Header Field");
    }

    public void transferHeader(HttpServletResponse resp)
    {
        //do nothing, this is the end of the transferHeader propogation.
    }

    public HeaderField getNextField()
    {
        throw new NoSuchElementException();
    }

    public boolean hasMoreFields()
    {
        return false;
    }

    public boolean isNil()
    {
        return true;
    }
}