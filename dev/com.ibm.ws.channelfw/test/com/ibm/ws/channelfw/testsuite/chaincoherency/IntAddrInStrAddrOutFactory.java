/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.testsuite.chaincoherency;

import com.ibm.wsspi.channelfw.exception.InvalidChannelFactoryException;

/**
 * Test factory for int-in str-out.
 */
public class IntAddrInStrAddrOutFactory extends ConnectorChannelFactory {
    /**
     * Constructor
     * 
     * @throws InvalidChannelFactoryException
     */
    public IntAddrInStrAddrOutFactory() throws InvalidChannelFactoryException {
        super();
        devAddr = Integer.class;
        appAddrs = new Class[] { String.class };
    }
}