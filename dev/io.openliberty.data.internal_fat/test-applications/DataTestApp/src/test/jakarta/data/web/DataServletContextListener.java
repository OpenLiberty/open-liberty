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
package test.jakarta.data.web;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Servlet context listener that uses Jakarta Data to pre-populate the table
 * that is used by the Prime entity.
 */
@WebListener
public class DataServletContextListener implements ServletContextListener {

    @Inject
    private Primes primes;

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        System.out.println("DataServletContextListener.contextInitialized:" +
                           " populate tables for " + //primes.toString());
                           " TODO: enable the above to reproduce deadlock due to" +
                           " Checkpoint delaying the initialization that the" +
                           " repository waits when first used.");
    }

}
