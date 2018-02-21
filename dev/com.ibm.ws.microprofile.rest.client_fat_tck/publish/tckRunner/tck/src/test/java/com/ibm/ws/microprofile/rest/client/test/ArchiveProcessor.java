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
package com.ibm.ws.microprofile.rest.client.test;

import java.io.File;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * We may need to  weave in the TCK jar for some tests that do not package the correct interface classes
 */
public class ArchiveProcessor implements ApplicationArchiveProcessor {

    private final static String WLP_DIR = System.getProperty("wlp");

    /* (non-Javadoc)
     * @see org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor#process(org.jboss.shrinkwrap.api.Archive, org.jboss.arquillian.test.spi.TestClass)
     */
    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        System.out.println("WLP: ArchiveProcessor.process(...) - no-op");
//        if (applicationArchive instanceof WebArchive) {
//            File file = new File(WLP_DIR, "/usr/servers/FATServer/microprofile-rest-client-tck-1.0.1.jar");
//            System.out.println("WLP: Adding Jar:" + file.getAbsolutePath() + " to " + applicationArchive.getName());
//            ((WebArchive) applicationArchive).addAsLibraries(file);
//        }
        if (applicationArchive instanceof WebArchive) {
          File file = new File(WLP_DIR, "/usr/servers/FATServer/wiremock-standalone-2.14.0.jar");
          System.out.println("WLP: Adding Jar:" + file.getAbsolutePath() + " to " + applicationArchive.getName());
          ((WebArchive) applicationArchive).addAsLibraries(file);
      }
    }
}