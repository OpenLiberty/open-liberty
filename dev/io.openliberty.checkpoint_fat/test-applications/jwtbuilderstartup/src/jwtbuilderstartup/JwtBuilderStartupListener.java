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
package jwtbuilderstartup;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.ibm.websphere.security.jwt.JwtBuilder;

@WebListener
public class JwtBuilderStartupListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Runs before the checkpoint is taken, so the checkpoint phase is not "restored" yet.
        // The exception must be caught; otherwise the application would fail to start.
        try {
            JwtBuilder.create("workloadIdentityBuilder");
            System.out.println("TESTING - JwtBuilder.create succeeded during checkpoint");
        } catch (Exception e) {
            System.out.println("TESTING - JwtBuilder.create failed: " + e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // do nothing
    }
}
