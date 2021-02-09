/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.singletonstore.jar.teststorage;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * For fragments and EE7 clarifications, it is helpful to track which
 * container initializers have triggered. We want some, but not others,
 * depending on fragment configuration and specification level.
 *
 * This is a simple store that is singleton to the classloader that will
 * record a simple string. Initializers can push into it, and we can verify
 * the contents later.
 *
 */
public class SingletonStore {

    private final List<String> callOrder = new ArrayList<String>();

    protected SingletonStore() {
        super();
    }

    private static SingletonStore instance = null;

    public static SingletonStore getInstance() {
        if (instance == null)
            instance = new SingletonStore();

        return instance;
    }

    public void pushCall(String caller) {
        instance.callOrder.add(caller);
    }

    public List<String> getCallOrder() {
        return instance.callOrder;
    }
}
