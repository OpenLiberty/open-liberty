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

import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * We weave in the hamcrest jar that is used by some of the microprofile config tck tests.
 * The build.gradle file pull the hamcrest jar from maven and puts it in the lib directory
 */
public class TestObserverArchiveProcessor implements ApplicationArchiveProcessor {

    private static final Logger LOG = Logger.getLogger(TestObserverArchiveProcessor.class.getName());
 
    /* (non-Javadoc)
     * @see org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor#process(org.jboss.shrinkwrap.api.Archive, org.jboss.arquillian.test.spi.TestClass)
     */
    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (applicationArchive instanceof WebArchive) {
            System.out.println("WLP: Adding observer for test start and finish to " + applicationArchive.getName());
            ((WebArchive) applicationArchive).addClass(TestObserver.class)
            .addClass(TestObserverExtension.class)
            .addAsServiceProvider(RemoteLoadableExtension.class, TestObserverExtension.class);
        } else {
            LOG.log(Level.WARNING, "Attempted to add the test observer to jar but " + applicationArchive + " was not a WebArchive");
        }
    }
}
