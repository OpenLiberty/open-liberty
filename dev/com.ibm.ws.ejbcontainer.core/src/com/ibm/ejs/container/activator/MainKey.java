/*******************************************************************************
 * Copyright (c) 1999, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.activator;

import com.ibm.ejs.container.BeanId;

/**
 * A <code>MainKey</code> is a specialized <code>TransactionKey</code>
 * used to lookup the main instance of a BeanO in the cache. The
 * main instance of a BeanO is identified by its <code>BeanId</code>
 * and a null <code>ContainerTx</code>.
 * <p>
 *
 */

class MainKey extends TransactionKey {

    /**
     * Create new <code>MainKey</code> instance associated with
     * given bean id.
     */

    MainKey(BeanId id) {

        super(null, id);

    } // MainKey

    @Override
    public String toString() {

        return "MainKey(" + id + ")";

    } // toString

} // MainKey
