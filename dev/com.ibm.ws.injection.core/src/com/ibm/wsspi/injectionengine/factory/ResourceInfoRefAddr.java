/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine.factory;

import javax.naming.RefAddr;

/**
 * A RefAddr to a ResourceInfo object.
 */
public class ResourceInfoRefAddr extends RefAddr
{
    private static final long serialVersionUID = 1229903471104651956L;

    /**
     * Constant of type for this address.
     **/
    public static final String Addr_Type = "ResourceInfo";

    private final ResourceInfo ivInfo;

    /**
     * Constructs a new instance.
     */
    public ResourceInfoRefAddr(ResourceInfo info)
    {
        super(Addr_Type);
        ivInfo = info;
    }

    /**
     * @see javax.naming.RefAddr#getContent()
     */
    @Override
    public Object getContent()
    {
        return ivInfo;
    }

}
