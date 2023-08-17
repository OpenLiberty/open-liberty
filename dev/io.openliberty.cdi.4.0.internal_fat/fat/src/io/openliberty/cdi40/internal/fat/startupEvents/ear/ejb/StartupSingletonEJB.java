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
package io.openliberty.cdi40.internal.fat.startupEvents.ear.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@Startup
@Singleton
public class StartupSingletonEJB {

    @Inject
    private EjbApplicationScopedBean bean;

    @PostConstruct
    private void ejbPostConstruct() {
        bean.observeEjbPostConstruct();
    }

    public void test() {
        bean.test();
    }

    @PreDestroy
    private void ejbPreDestroy() {
        bean.observeEjbPreDestroy();
    }
}
