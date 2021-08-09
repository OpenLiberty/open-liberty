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

package io.openliberty.microprofile.lra.test;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.arquillian.test.spi.TestClass;

import org.eclipse.microprofile.lra.tck.service.spi.LRARecoveryService;
import io.openliberty.microprofile.lra.test.LRARecoveryServiceImpl;

/**
 * Adds the LRARecoveryServiceImpl to all arquillian archives and exposes it
 * as a loadable service via META-INF/services (which is required by the LRA
 * TCK)
 */
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
