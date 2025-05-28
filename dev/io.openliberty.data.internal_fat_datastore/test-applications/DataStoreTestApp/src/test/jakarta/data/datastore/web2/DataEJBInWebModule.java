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
package test.jakarta.data.datastore.web2;

import jakarta.ejb.Singleton;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;

import test.jakarta.data.datastore.lib.ServerDSEntity;

/**
 * An EJB in the web module that uses a Jakarta Data repository from a method
 * that observes startup of the application.
 */
@Singleton
public class DataEJBInWebModule {
    @Inject
    WebModule2DSResRefRepo repo;

    public void onStartup(@Observes Startup event) {
        System.out.println("Observed Startup of DataEJBInWebModule");

        repo.write(ServerDSEntity.of("DataEJBInWebModule.onStartup", 123));
    }

}