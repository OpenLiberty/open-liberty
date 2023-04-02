/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi20.fat.apps.events.ejb;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

/**
 * Singleton statup bean
 */
@Singleton
@Startup
public class SingletonStartupBean {

    @Inject
    EJBApplicationScopedBean bean;

    @PostConstruct
    private void init() {
        System.out.println("StatelessStartupEJB @PostConstruct");
        bean.test();
    }

    public void test() {
        System.out.println("StatelessStartupEJB test");
    }

}
