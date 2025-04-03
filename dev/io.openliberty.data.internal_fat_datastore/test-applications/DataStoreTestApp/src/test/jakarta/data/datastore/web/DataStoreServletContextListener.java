/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.datastore.web;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import test.jakarta.data.datastore.lib.ServerDSEntity;

/**
 * Servlet context listener that uses Jakarta Data to pre-populate the table
 * that is used by the ServerDSResRefRepo entity.
 */
@WebListener
public class DataStoreServletContextListener implements ServletContextListener {

    @Inject
    ServerDSResRefRepo serverDSResRefRepo;

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        System.out.println("DataStoreServletContextListener.contextInitialized:" +
                           " populate tables for " + serverDSResRefRepo.toString());

        serverDSResRefRepo.write(ServerDSEntity.of("DSSCL-one", 1));
        serverDSResRefRepo.write(ServerDSEntity.of("DSSCL-two", 2));
    }

}
