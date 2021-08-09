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

public class LinkStateLogicallyUnlinked implements LinkState
{
    private static final LinkStateLogicallyUnlinked _instance = new LinkStateLogicallyUnlinked();

    private static final String _toString = "LogicallyUnlinked";

    static LinkState instance()
    {
        return _instance;
    }

    /**
     * private constructor so state can only 
     * be accessed via instance method.
     */
    private LinkStateLogicallyUnlinked() {}

    public String toString()
    {
        return _toString;
    }
}

