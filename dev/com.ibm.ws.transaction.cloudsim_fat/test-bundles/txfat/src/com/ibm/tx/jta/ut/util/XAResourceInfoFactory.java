/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.ut.util;

import java.util.HashMap;

import com.ibm.tx.jta.impl.XidImpl;

/**
 *
 */
public class XAResourceInfoFactory {
    private static final HashMap<Integer, XAResourceInfoImpl> _xaResInfoTable = new HashMap<Integer, XAResourceInfoImpl>();

    private XidImpl dummy;

    public static XAResourceInfoImpl getXAResourceInfo(int i) {
        if (_xaResInfoTable.get(i) == null) {
            _xaResInfoTable.put(i, new XAResourceInfoImpl(i));
        }

        return _xaResInfoTable.get(i);
    }
}