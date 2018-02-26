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

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * We register an extension class with Arquillian that amends the vanilla tck's wars
 * to add any libraries that are not in the default Liberty environment
 */
public class ArquillianLoadableExtension implements LoadableExtension {
    @Override
    public void register(ExtensionBuilder extensionBuilder) {
        System.out.println("WLP: Adding Extension com.ibm.ws.microprofile.rest.client.test.ArchiveProcessor");
        extensionBuilder.service(ApplicationArchiveProcessor.class, ArchiveProcessor.class);
    }
}