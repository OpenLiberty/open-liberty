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

import javax.servlet.http.HttpServletResponse;


abstract class HeaderField implements Serializable
{
    public abstract int getIntValue();
    public abstract long getDateValue();
    public abstract String getStringValue();
    public abstract void setIntValue(int val);
    public abstract void setDateValue(long date);
    public abstract void setStringValue(String s);
    public abstract String getName();
    public abstract void transferHeader(HttpServletResponse resp);
    public abstract HeaderField getNextField();
    public abstract boolean hasMoreFields();
    public abstract boolean isNil();
}
