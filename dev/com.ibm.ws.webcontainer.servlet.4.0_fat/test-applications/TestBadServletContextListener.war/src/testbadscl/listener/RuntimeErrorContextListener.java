/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package testbadscl.listener;

import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * The ApplicationMBean and checks for the status of the
 * TestBadServletContextListener.war web application
 * which has this listener and is expected to fail to
 * start due to runtime exception thrown here.
 *
 */
@WebListener
public class RuntimeErrorContextListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(RuntimeErrorContextListener.class.getName());

    /**
     *
     */
    @Override
    public void contextDestroyed(ServletContextEvent scEvent) {
        String logThis = "RuntimeErrorContextListener destroy invoked";
        LOG.info(logThis);
        scEvent.getServletContext().log(logThis);
    }

    /**
     * explicitly throws RuntimeException to
     * test property "stopAppStartUponListenerException"
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {

        ServletContext context = sce.getServletContext();
        context.log("RuntimeErrorContextListener contextInitialized invoked");

        String logThis = "RuntimeErrorContextListener contextInitialized explicitly throws RuntimeException";
        LOG.info(logThis);

        throw new java.lang.RuntimeException(logThis);

    }
}
