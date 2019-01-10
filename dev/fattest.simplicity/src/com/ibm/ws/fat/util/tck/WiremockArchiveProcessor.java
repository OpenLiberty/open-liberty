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

import java.util.EnumSet;
import java.util.Set;
import java.io.File;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * We may need to  weave in the TCK jar for some tests that do not package the correct interface classes
 */
public class WiremockArchiveProcessor implements ApplicationArchiveProcessor {

    private static final Logger LOG = Logger.getLogger(ApplicationArchiveProcessor.class.getName());

    private final static String WLP_DIR = System.getProperty("wlp");
 
    /* (non-Javadoc)
     * @see org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor#process(org.jboss.shrinkwrap.api.Archive, org.jboss.arquillian.test.spi.TestClass)
     */
    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (applicationArchive instanceof WebArchive) {
            File wiremock = new File(WLP_DIR, "/usr/servers/FATServer/wiremock-standalone-2.14.0.jar");
            LOG.log(Level.INFO, "WLP: Adding Jar: {0} to {1}", new String[] {wiremock.getAbsolutePath(), applicationArchive.getName()});
            ((WebArchive) applicationArchive).addAsLibraries(wiremock);
        } else {
            LOG.log(Level.INFO, "Attempted to add hamcrest jar but {0} was not a WebArchive", applicationArchive);
        }
    }
}
