/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2counter.war.cdi.interceptors.servlets;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;

@SessionScoped
public class ServletCounter implements Serializable {
    //
    private static final long serialVersionUID = 1L;

    public ServletCounter() {
        this.count = 0;
    }

    private int count;

    @ServletCounterOperation
    public int increment() {
        return ++count;
    }

    @ServletCounterOperation
    public int decrement() {
        return --count;
    }

    @ServletCounterOperation
    public int getCount() {
        return count;
    }
}
