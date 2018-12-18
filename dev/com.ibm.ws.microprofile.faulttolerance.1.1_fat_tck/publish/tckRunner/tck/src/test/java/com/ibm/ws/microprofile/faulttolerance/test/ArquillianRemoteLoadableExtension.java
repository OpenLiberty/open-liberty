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
package com.ibm.ws.microprofile.faulttolerance.test;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;

import com.ibm.ws.fat.util.tck.HamcrestArchiveProcessor;
import com.ibm.ws.fat.util.tck.TestObserverArchiveProcessor;

/**
 * We register an extension class with Arquillian that amends the vanilla tck's wars
 * to add any libraries that are not in the default Liberty environment as well as 
 * logs the start and stop time for tests. 
 */
public class ArquillianRemoteLoadableExtension implements RemoteLoadableExtension {
    @Override
    public void register(ExtensionBuilder extensionBuilder) {
        System.out.println("WLP: Adding Extension com.ibm.ws.microprofile.config12.test.TestObserver");
        extensionBuilder.observer(TestObserver.class);
    }
}
