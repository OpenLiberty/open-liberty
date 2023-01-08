/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package test.app;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * A test context listener which holds up the application start
 */
@WebListener
public class SlowStopAppListener implements ServletContextListener {

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        int count = 40; //default number of seconds to timeout the startup
        
        System.err.println("Sleeping for approx " + count + " seconds.");
        for (int i = 0; i < count; i++) {
            try {
                System.err.println("SlowApp is sleeping, zzzzzzzz");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //test failed, so re-throw as a runtime error
                System.err.println("SlowApp was interrupted.");
                throw new RuntimeException(e);
            }
        }

        System.err.println("TEST : SlowStopApp finished initialization");

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     *
     * Delay the stopping of this listener for 40 seconds, which will exceed the default timeout
     * for App Manager to wait for an application
     */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        System.err.println("TEST : SlowStopApp exited");
    }

}
