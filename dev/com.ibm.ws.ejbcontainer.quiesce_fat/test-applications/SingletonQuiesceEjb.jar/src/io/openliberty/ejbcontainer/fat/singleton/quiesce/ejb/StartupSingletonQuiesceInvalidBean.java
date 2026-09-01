/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

package io.openliberty.ejbcontainer.fat.singleton.quiesce.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Startup singleton bean with an invalid (non-Boolean) value for the
 * io.openliberty.ejb.destroyOnQuiesce property configured in ejb-jar.xml.
 * The invalid value is treated as false (default), so the bean is destroyed at
 * application stop rather than during server quiesce.
 */
@Startup
@Singleton(name = "StartupSingletonQuiesceInvalid")
@DependsOn("StartupSingletonQuiesceInvalidBnd")
public class StartupSingletonQuiesceInvalidBean implements SingletonQuiesce {

    @PostConstruct
    public void postConstruct() {
        System.out.println("PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceInvalid:");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceInvalid:");
    }

    @Override
    public String getName() {
        return "SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceInvalid";
    }
}
