/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.loginModuleClassloading;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

public class LoginModuleClassloadingExtension implements Extension {

    private Class<?> loginModuleClass;
    private Exception loginModuleException;

    public void testLoadTccl(@Observes AfterBeanDiscovery event) {
        try {
            loginModuleClass = Thread.currentThread().getContextClassLoader().loadClass(LoginModuleClassloadingTestServlet.LOGIN_MODULE_CLASS);
        } catch (ClassNotFoundException e) {
            loginModuleException = e;
        }
    }

    public Class<?> getLoginModuleClass() {
        return loginModuleClass;
    }

    public Exception getLoginModuleException() {
        return loginModuleException;
    }

}
