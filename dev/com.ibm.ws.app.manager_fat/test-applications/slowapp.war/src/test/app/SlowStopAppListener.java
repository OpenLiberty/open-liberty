/*******************************************************************************
 * Copyright (c) 2018-2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.app;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * A test context listener which holds up the application stop
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
        Object timeout = event.getServletContext().getAttribute("timeout");
        int count = 40; //default number of seconds to timeout the startup
        if (timeout != null) {
            try {
                count = Integer.parseInt(timeout.toString());
            } catch (NumberFormatException e) {
                System.err.println("Invalid timeout specified, using default value of " + count);
            }
        }
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
        Object timeout = event.getServletContext().getAttribute("timeout");
        int count = 40; //default number of seconds to timeout the shutdown
        if (timeout != null) {
            try {
                count = Integer.parseInt(timeout.toString());
            } catch (NumberFormatException e) {
                System.err.println("Invalid timeout specified, using default value of " + count);
            }
        }
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
        //this message will be looked for in the log to indicate pass / fail
        System.err.println("TEST : SlowStopApp exited");
    }

}
