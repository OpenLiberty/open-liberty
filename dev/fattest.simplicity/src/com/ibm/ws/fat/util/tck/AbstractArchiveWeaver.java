/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
package com.ibm.ws.fat.util.tck;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
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

        if (getJarsToWeave().isEmpty() && getClassesToWeave().isEmpty() && getStringFilesToWeave().isEmpty()) {
            throw new IllegalStateException("Trying to weave into " + applicationArchive + " but had nothing to add");
        }

        if (applicationArchive instanceof WebArchive) {
            for (File file : getJarsToWeave()) {
                //File file = new File(WLP_DIR, "/usr/servers/FATServer/" + fileName);
                LOG.log(Level.INFO, "WLP: Adding Jar: {0} to {1}", new String[] { file.getAbsolutePath(), applicationArchive.getName() });
                ((WebArchive) applicationArchive).addAsLibraries(file);
            }

            for (Class clazz : getClassesToWeave()) {
                LOG.log(Level.INFO, "WLP: Adding Class: {0} to {1}", new String[] { clazz.getName(), applicationArchive.getName() });
                ((WebArchive) applicationArchive).addClass(clazz);
            }

            for (String path : getStringFilesToWeave().keySet()) {
                LOG.log(Level.INFO, "WLP: Adding asset: {0} to {1}", new String[] { path, applicationArchive.getName() });
                ((WebArchive) applicationArchive).addAsResource(getStringFilesToWeave().get(path), path);
            }
        } else {
            LOG.log(Level.INFO, "Attempted to add org.json jar(s) but {0} was not a WebArchive", applicationArchive);
        }
    }

    protected Set<File> getJarsToWeave() {
        return Collections.emptySet();
    }

    protected Map<String, StringAsset> getStringFilesToWeave() {
        return Collections.emptyMap();
    }

    protected Set<Class<?>> getClassesToWeave() {
        return Collections.emptySet();
    }
}
