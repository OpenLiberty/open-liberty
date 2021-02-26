/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mb.interceptor.injection;

import javax.annotation.ManagedBean;

/**
 * Bean constructor injected to test constructors with parameters
 */
@ManagedBean("Injection")
public class InjectedManagedBean {

    private int id;
    private static int count;

    public InjectedManagedBean() {
        id = count++;
    }

    public int getID() {
        return id;
    }

    public void setID(int arg) {
        this.id = arg;
    }

    public static int getCount() {
        return count;
    }

}
