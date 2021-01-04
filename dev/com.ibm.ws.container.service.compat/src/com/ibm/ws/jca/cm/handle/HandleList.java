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

import java.util.Arrays;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * A list of connection handles that were obtained within a servlet request, or by an EJB instance,
 * or within a contextual task/completion stage.
 * Null elements are not allowed.
 */
public class HandleList implements HandleListInterface {
    private HandleDetails[] list = new HandleDetails[10];
    private int size;

    @Override
    public HandleList addHandle(HandleListInterface.HandleDetails a) {
        if (a == null)
            throw new NullPointerException();
        synchronized (this) {
            if (size >= list.length) // resize if already at capacity
                System.arraycopy(list, 0, list = new HandleDetails[size + 10], 0, size);
            list[size++] = a;
        }
        return this;
    }

    public void close() {
        synchronized (this) {
            int numToClose = size;
            size = 0;
            for (int i = numToClose; i > 0;) {
                HandleDetails r = list[--i];
                list[i] = null;
                r.close(true);
            }
        }
    }

    public void componentDestroyed() {
        close();
    }

    @Override
    public HandleDetails removeHandle(Object h) {
        synchronized (this) {
            for (int i = size; i > 0;) {
                HandleDetails r = list[--i];
                if (r.forHandle(h)) {
                    list[i] = list[--size];
                    list[size] = null;
                    return r;
                }
            }
        }
        return null;
    }

    @Override
    public void parkHandle() {
        synchronized (this) {
            for (int i = size; i > 0;)
                list[--i].park();
        }
    }

    @Override
    public void reAssociate() {
        synchronized (this) {
            for (int i = size; i > 0;)
                list[--i].reassociate();
        }
    }

    @Override
    @Trivial
    public String toString() {
        String handleInfo;
        synchronized (this) {
            handleInfo = Arrays.toString(list);
        }

        return new StringBuilder(19 + handleInfo.length()) //
                        .append("HandleList@") //
                        .append(Integer.toHexString(System.identityHashCode(this))) //
                        .append(handleInfo) //
                        .toString();
    }
}
