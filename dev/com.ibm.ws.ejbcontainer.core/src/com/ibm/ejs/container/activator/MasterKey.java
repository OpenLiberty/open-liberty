/*******************************************************************************
 * Copyright (c) 1999 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.activator;

import com.ibm.ejs.container.BeanId;

/**
 * A <code>MasterKey</code> is a specialized <code>TransactionKey</code>
 * used to lookup the master instance of a BeanO in the cache. The
 * master instance of a BeanO is identified by its <code>BeanId</code>
 * and a null <code>ContainerTx</code>.
 * <p>
 * 
 */

class MasterKey extends TransactionKey
{

    /**
     * Create new <code>MasterKey</code> instance associated with
     * given bean id.
     */

    MasterKey(BeanId id) {

        super(null, id);

    } // MasterKey

    public String toString() {

        return "MasterKey(" + id + ")";

    } // toString

} // MasterKey

