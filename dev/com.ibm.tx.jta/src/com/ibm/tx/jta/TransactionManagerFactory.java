/*
 * Copyright (c) 2017, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.tx.jta;

import com.ibm.tx.jta.impl.TranManagerSet;

public class TransactionManagerFactory {
    protected static ExtendedTransactionManager _tranManager;

    public static ExtendedTransactionManager getTransactionManager() {
        return null == _tranManager ? _tranManager = TranManagerSet.instance() : _tranManager;
    }

    // used by for example WAS TMFactory to set it's TM here to be consistent
    public static void setTransactionManager(ExtendedTransactionManager mgr) {
        _tranManager = mgr;
    }
}
