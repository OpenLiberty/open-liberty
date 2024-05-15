/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package app2;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class EarlyStartupAppCode implements ServletContextListener {
    public static String PROP_FAIL_STARTUP = "io.openliberty.test.fail.startup";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("TESTING - contextInitialized");
        if (System.getProperty(PROP_FAIL_STARTUP) != null) {
            throw new RuntimeException("Fail Startup");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // do nothing
    }
}
