/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
  *******************************************************************************/
package com.ibm.ws.fat.util.tck;

import org.jboss.arquillian.container.test.spi.client.deployment.CachedAuxilliaryArchiveAppender;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Add SLF4J classes from the classpath, required for TestNG >= 7.5
 */
public class Slf4JArchiveAppender extends CachedAuxilliaryArchiveAppender {

    private static final String SLF4J_SERVICE_PROVIDER = "META-INF/services/org.slf4j.spi.SLF4JServiceProvider";

    @Override
    protected Archive<?> buildArchive() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "slf4j.jar");
        archive.addPackages(true, "org.slf4j");
        if (Thread.currentThread().getContextClassLoader().getResource(SLF4J_SERVICE_PROVIDER) != null) {
            archive.addAsResource(SLF4J_SERVICE_PROVIDER, SLF4J_SERVICE_PROVIDER);
        }
        return archive;
    }

}