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

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;

/**
 * We register an extension class with Arquillian logs the start and stop time for tests. 
 */
public class TestLoggingObserverExtension implements RemoteLoadableExtension {

    private static final Logger LOG = Logger.getLogger(TestLoggingObserverExtension.class.getName());

    @Override
    public void register(ExtensionBuilder extensionBuilder) {
        LOG.log(Level.INFO, "WLP: Registering TestLoggingObserver");
        extensionBuilder.observer(TestLoggingObserver.class);
    }
}
