/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util.tck;

import java.io.File;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Abstract class used to weave files into TCK web archives
 */
public abstract class AbstractArchiveWeaver implements ApplicationArchiveProcessor {

    private static final Logger LOG = Logger.getLogger(AbstractArchiveWeaver.class.getName());

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor#process(org.jboss.shrinkwrap.api.Archive, org.jboss.arquillian.test.spi.TestClass)
     */
    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (applicationArchive instanceof WebArchive) {
            for (File file : getFilesToWeave()) {
                //File file = new File(WLP_DIR, "/usr/servers/FATServer/" + fileName);
                LOG.log(Level.INFO, "WLP: Adding Jar: {0} to {1}", new String[] { file.getAbsolutePath(), applicationArchive.getName() });
                ((WebArchive) applicationArchive).addAsLibraries(file);
            }
        } else {
            LOG.log(Level.INFO, "Attempted to add org.json jar(s) but {0} was not a WebArchive", applicationArchive);
        }
    }

    protected abstract Set<File> getFilesToWeave();
}