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
package com.ibm.ws.classloading.test.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import com.ibm.ws.classloading.test.base.MyBaseClass;

/**
 * Startup singleton EJB that prints a message from its base class at startup.
 * The base class is in a shared library so switching the library changes the output.
 */
@Singleton
@Startup
@LocalBean
public class MyStartupSingletonBean extends MyBaseClass {

    public MyStartupSingletonBean() {}

    @PostConstruct
    public void startup() {
        printSomething();
    }
}
