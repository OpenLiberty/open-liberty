/*******************************************************************************
 * Copyright (c) 2014,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import com.ibm.ejs.j2c.HandleListInterface;
import com.ibm.ws.jca.cm.handle.HandleList;

/**
 * This is a HandleList proxy that is pushed onto the
 * {@link com.ibm.ws.threadContext.ConnectionHandleAccessorImpl} thread context
 * for beans that don't yet have an associated HandleList. This class uses
 * {@link EJBThreadData#getCallbackBeanO()} to determine the current bean, so
 * the callback bean must always be current before pushing this proxy.
 *
 * <p>This proxy is used to avoid the per-bean memory overhead of a HandleList
 * until a handle needs to be tracked. Since HandleList is only used for
 * non-smart handles and all Liberty connection factories support smart handles,
 * the majority of beans will never need a HandleList.
 */
class HandleListProxy implements HandleListInterface {
    public static final HandleListInterface INSTANCE = new HandleListProxy();

    private HandleListProxy() {
    }

    @Override
    public String toString() {
        BeanO beanO = EJSContainer.getCallbackBeanO();
        return super.toString() + '[' + beanO.getHandleList(false) + ", " + beanO + ']';
    }

    /**
     * Gets or optionally creates a handle list for the current callback bean.
     * If the current callback bean does not have a HandleList and
     * {@code create} is {@code false}, then {@code null} is returned.
     *
     * @param create {@code true} if a HandleList must be created
     * @return the HandleList or {@code null}
     */
    private HandleList getHandleList(boolean create) {
        BeanO beanO = EJSContainer.getCallbackBeanO();
        return beanO.getHandleList(create);
    }

    @Override
    public HandleList addHandle(HandleDetails a) {
        HandleList hl = getHandleList(true);

        hl.addHandle(a);
        return hl;
    }

    @Override
    public HandleDetails removeHandle(Object handle) {
        HandleList hl = getHandleList(false);

        if (hl != null) {
            return hl.removeHandle(handle);
        } else {
            return null;
        }
    }

    @Override
    public void reAssociate() {
        HandleList hl = getHandleList(false);

        if (hl != null) {
            hl.reAssociate();
        }
    }

    @Override
    public void parkHandle() {
        HandleList hl = getHandleList(false);

        if (hl != null) {
            hl.parkHandle();
        }
    }
}
