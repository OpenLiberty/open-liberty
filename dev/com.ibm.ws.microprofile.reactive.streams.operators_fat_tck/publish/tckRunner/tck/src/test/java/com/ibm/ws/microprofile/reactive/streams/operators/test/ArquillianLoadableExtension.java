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
package com.ibm.ws.microprofile.reactive.streams.operators.test;

import java.util.EnumSet;
import java.util.Set;

import com.ibm.ws.fat.util.tck.AbstractArquillianLoadableExtension;
import com.ibm.ws.fat.util.tck.TCKArchiveModifications;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;

public class ArquillianLoadableExtension extends AbstractArquillianLoadableExtension {

    @Override
    public void register(ExtensionBuilder extensionBuilder) {
        super.register(extensionBuilder);
        extensionBuilder.service(ApplicationArchiveProcessor.class, ReactiveStreamsArchiveProcessor.class);
    }

    @Override
    public Set<TCKArchiveModifications> getModifications() {
        return EnumSet.of(TCKArchiveModifications.TEST_LOGGER, TCKArchiveModifications.HAMCREST);
    }
}
