/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.csi;

import com.ibm.websphere.csi.StatefulSessionKey;
import com.ibm.websphere.csi.StatefulSessionKeyFactory;
import com.ibm.ws.util.UUID;

/**
 * This class provides a simple factory for keys for stateful session beans.
 */
public class SessionKeyFactoryImpl implements StatefulSessionKeyFactory
{
    /**
     * Return newly created <code>StatefulSessionKey</code> instance.
     * Note, the returned object will be for a key that is unique
     * within a cluster.
     */
    @Override
    public StatefulSessionKey create()
    {
        UUID uuid = new UUID(); // d204278
        return new StatefulSessionKeyImpl(uuid);
    }

    /**
     * Create using bytes from a previously generated unique UUID.
     *
     * @param bytes are the bytes from the unique UUID.
     */
    @Override
    public StatefulSessionKey create(byte[] bytes)
    {
        return new StatefulSessionKeyImpl(new UUID(bytes));
    }

} // SessionKeyFactoryImpl
