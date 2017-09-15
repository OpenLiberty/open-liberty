/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.factory;

import javax.naming.RefAddr;

/**
 * A RefAddr to a MBLinkInfo object.
 */
public class MBLinkInfoRefAddr extends RefAddr
{
    private static final long serialVersionUID = -4514350245445974238L;

    static final String ADDR_TYPE = "MBLinkInfo";

    final private MBLinkInfo ivInfo;

    /**
     * Constructs a new instance.
     */
    public MBLinkInfoRefAddr(MBLinkInfo info)
    {
        super(ADDR_TYPE);
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
