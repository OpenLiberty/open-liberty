/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ejs.container;

import java.rmi.RemoteException;

import javax.ejb.EnterpriseBean;

/**
 * This class exists for ABI compatibility with ejbdeploy'ed home beans.
 */
public abstract class EntityBeanO
                extends BeanO
{
    public EntityBeanO(EJSContainer container, EJSHome home)
    {
        super(container, home);
    }

    /**
     * Return the underlying bean instance. This method exists for ABI
     * compatibility with ejbdeploy'ed home beans.
     */
    public abstract EnterpriseBean getEnterpriseBean()
                    throws RemoteException;

    // The remaining methods are for callers in the core container to entities
    // where it's not convenient to refactor entity support out of the callers.

    abstract void postFind()
                    throws RemoteException;

    abstract void postHomeMethodCall();

    abstract void load(ContainerTx containerTx, boolean forUpdate)
                    throws RemoteException;

    abstract void enlistForOptionA(ContainerTx containerTx);

    abstract int getLockMode();
}
