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

package com.ibm.ws.lra.test;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.arquillian.test.spi.TestClass;

import org.eclipse.microprofile.lra.tck.service.spi.LRARecoveryService;
import com.ibm.ws.lra.test.LRARecoveryServiceImpl;

public class LRATckArchiveProcessor implements ApplicationArchiveProcessor {
    
    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (applicationArchive instanceof WebArchive) {
            ((WebArchive) applicationArchive).addClass(LRARecoveryServiceImpl.class)
            .addAsServiceProviderAndClasses(LRARecoveryService.class, LRARecoveryServiceImpl.class);
        } else if (applicationArchive instanceof JavaArchive) {
            ((JavaArchive) applicationArchive).addClass(LRARecoveryServiceImpl.class)
            .addAsServiceProviderAndClasses(LRARecoveryService.class, LRARecoveryServiceImpl.class);
        }
        
    }

}


/*

import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;


  // We weave in the hamcrest jar that is used by some of the microprofile config tck tests.
  //  The build.gradle file pull the hamcrest jar from maven and puts it in the lib directory
 
public class TestLoggingObserverArchiveProcessor implements ApplicationArchiveProcessor {

    private static final Logger LOG = Logger.getLogger(TestLoggingObserverArchiveProcessor.class.getName());

     // @see org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor#process(org.jboss.shrinkwrap.api.Archive, org.jboss.arquillian.test.spi.TestClass)
    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (applicationArchive instanceof WebArchive) {
            LOG.log(Level.INFO, "WLP: Adding observer for test start and finish to {0}", applicationArchive.getName());
            ((WebArchive) applicationArchive).addClass(TestLoggingObserver.class)
                            .addClass(TestLoggingObserverExtension.class)
                            .addAsServiceProvider(RemoteLoadableExtension.class, TestLoggingObserverExtension.class);
        } else if (applicationArchive instanceof JavaArchive) {
            LOG.log(Level.INFO, "WLP: Adding observer for test start and finish to {0}", applicationArchive.getName());
            ((JavaArchive) applicationArchive).addClass(TestLoggingObserver.class)
                            .addClass(TestLoggingObserverExtension.class)
                            .addAsServiceProvider(RemoteLoadableExtension.class, TestLoggingObserverExtension.class);
        } else {
            LOG.log(Level.INFO, "Attempted to add the test observer to archive {0} but it was not a WebArchive or a JavaArchive", applicationArchive);
        }
    }
}
*/