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

import java.util.Enumeration;

@SuppressWarnings("unchecked")
class HeaderFieldNames implements Enumeration
{
    HeaderField _curr;
    HeaderFieldNames(HeaderField field)
    {
        _curr = field;
    }

    public boolean hasMoreElements()
    {
        return _curr.hasMoreFields();
    }

    public Object nextElement()
    {
        HeaderField _ret = _curr;
        _curr = _curr.getNextField();
        return _ret.getName();
    }
}