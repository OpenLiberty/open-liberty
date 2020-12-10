/*******************************************************************************
 * Copyright (c) 2010,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.cm.handle;

import java.util.Vector;

/**
 * A list of connection handles that were obtained within a servlet request, or by an EJB instance,
 * or within a contextual task/completion stage.
 * Null elements are not allowed.
 */
public class HandleList extends Vector<HandleListInterface.HandleDetails> implements HandleListInterface {
    private static final long serialVersionUID = -4425328702653290017L;

    private boolean destroyed;

    @Override
    public synchronized HandleList addHandle(HandleListInterface.HandleDetails a) {
        if (a == null)
            throw new NullPointerException();
        super.add(a);
        return this;
    }

    public synchronized void close() {
        destroyed = true;

        for (int i = elementCount; i > 0;) {
            HandleDetails r = (HandleDetails) elementData[--i];
            elementCount--;
            elementData[i] = null;
            r.close();
        }

        destroyed = false; //reset for re-use
    }

    public void componentDestroyed() {
        close();
    }

    @Override
    public synchronized HandleDetails removeHandle(Object h) {
        if (!destroyed) // prevents re-entry by the same thread in case close-->handle.close triggers an inline removeHandle
            for (int i = elementCount; i > 0;) {
                HandleDetails r = (HandleDetails) elementData[--i];
                if (r.forHandle(h)) {
                    elementData[i] = elementData[--elementCount];
                    elementData[elementCount] = null;
                    return r;
                }
            }
        return null;
    }

    @Override
    public synchronized void parkHandle() {
        for (int i = elementCount; i > 0;)
            ((HandleDetails) elementData[--i]).park();
    }

    @Override
    public synchronized void reAssociate() {
        for (int i = elementCount; i > 0;)
            ((HandleDetails) elementData[--i]).reassociate();
    }

    @Override
    public synchronized String toString() {
        return "HandleList@" + Integer.toHexString(System.identityHashCode(this)) + super.toString();
    }
}
