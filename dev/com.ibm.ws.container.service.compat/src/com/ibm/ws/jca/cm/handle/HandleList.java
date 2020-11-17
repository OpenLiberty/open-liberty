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

import java.util.ArrayList;

/**
 * TODO implementation needed. Still needs to be ported to Liberty.
 * The ArrayList/synchronized approach is only used temporarily because it
 * was a quick way of mocking up, in an oversimplified way for experimentation
 * purposes, what the real handle list does.
 */
public class HandleList extends ArrayList<HandleListInterface.HandleDetails> implements HandleListInterface {
    private static final long serialVersionUID = -4425328702653290017L;

    private boolean destroyed;

    @Override
    public synchronized HandleList addHandle(HandleListInterface.HandleDetails a) {
        super.add(a);
        return this;
    }

    public synchronized void close() {
        destroyed = true;

        for (int length = super.size(); length > 0;)
            super.remove(--length).close();

        destroyed = false; //reset for re-use
    }

    public void componentDestroyed() {
        close();
    }

    @Override
    public synchronized HandleDetails removeHandle(Object h) {
        if (!destroyed)
            for (int i = super.size(); i > 0;)
                if (get(--i).forHandle(h))
                    return super.remove(i);
        return null;
    }

    @Override
    public synchronized void parkHandle() {
        for (int i = super.size(); i > 0;)
            get(--i).park();
    }

    @Override
    public synchronized void reAssociate() {
        for (int i = super.size(); i > 0;) {
            get(--i).reassociate();
        }
    }

    @Override
    public synchronized String toString() {
        return "HandleList@" + Integer.toHexString(System.identityHashCode(this)) + super.toString();
    }
}
