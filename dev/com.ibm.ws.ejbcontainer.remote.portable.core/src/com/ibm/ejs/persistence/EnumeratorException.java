/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.persistence;

public class EnumeratorException extends Exception
{
    private static final long serialVersionUID = 4412754110898535663L;

    public EnumeratorException()
    {
        this.detail = null;
    }

    public EnumeratorException(String s)
    {
        super(s);
        this.detail = null;
    }

    public EnumeratorException(String s, Throwable detail)
    {
        super(s);
        this.detail = detail;
    }

    public String toString()
    {
        String s = "com.ibm.ejs.persitence.EnumeratorException";
        if (detail != null) {
            s += "\n\toriginal exception:\n";
            s += detail.toString();
        }
        return s;
    }

    private final Throwable detail;
}
