/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.jakarta.data.datastore.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

@Startup
@Singleton
public class StartupSingletonEJB {

    //TODO Renable when  javax.naming.NameNotFoundException is fixed
    //FutureEMBuilder: InitialContext.doLookup(dataStore)
    @Inject
    EJBModuleDSDRepo repo;

    @PostConstruct
    public void init() {
        repo.acquire(0);
    }
}
