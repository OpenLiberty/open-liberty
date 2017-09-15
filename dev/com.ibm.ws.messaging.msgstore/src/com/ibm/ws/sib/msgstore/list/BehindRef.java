package com.ibm.ws.sib.msgstore.list;
/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.lang.ref.SoftReference;

import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;

/**
 * An entry in the list of AILs behind the current position of a cursor.
 */
public class BehindRef extends SoftReference
{
   

    public BehindRef _next;
    public BehindRef _prev;

    public BehindRef(AbstractItemLink ail)
    {
        super(ail);
    }

    public AbstractItemLink getAIL()
    {
        Object ref = get();
        if (ref == null)
            return null;
        else
            return(AbstractItemLink)ref;
    }

    public String toString()
    {
        Object ref = get();
        if (ref == null)
            return "[#]";
        else
        {
            StringBuffer sb = new StringBuffer();
            sb.append('[');
            sb.append(((AbstractItemLink)ref).getPosition());
            sb.append(']');
            return sb.toString();
        }
    }
}
