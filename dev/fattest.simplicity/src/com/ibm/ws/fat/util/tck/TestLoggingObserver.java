/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util.tck;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.EventContext;
import org.jboss.arquillian.test.spi.event.suite.TestEvent;

public class TestLoggingObserver {  

    private static final Logger LOG = Logger.getLogger(TestLoggingObserver.class.getName());
      
    void logTestStartStop(@Observes(precedence = 0) EventContext<TestEvent> testEventContext) {
        String testName = testEventContext.getEvent().getTestMethod().getName();

        LOG.log(Level.INFO, "Starting test: {0}", testName);
        testEventContext.proceed();
        LOG.log(Level.INFO, "Test complete: {0}", testName);
    }  
}  
