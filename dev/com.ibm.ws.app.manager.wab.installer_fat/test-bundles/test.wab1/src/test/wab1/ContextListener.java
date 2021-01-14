/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.wab1;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class ContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        String delay = System.getProperty("wab.test.delay");
        if (delay != null) {
            try {
                System.out.println("Start Blocking: " + delay + " " + getClass().getSimpleName());
                Thread.sleep(Long.valueOf(delay));
                System.out.println("Done Blocking: " + getClass().getSimpleName());
            } catch (NumberFormatException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {}

}
