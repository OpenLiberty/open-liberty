/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package com.ibm.test.updateable.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

@Singleton
@Startup
@LocalBean
public class MySingletonBean {

    private static boolean initRun = false;

    @PostConstruct
    public void init() {
        System.out.println("MySingletonBean - init");
        initRun = true;
    }

    public String getSomeString() {
        return "Hello from an updated version of MySingletonBean - initRun? " + initRun;
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("MySingletonBean - shutdown");
    }
}
