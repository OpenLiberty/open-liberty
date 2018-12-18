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
package com.ibm.ws.microprofile.metrics.test;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;

import com.ibm.ws.fat.util.tck.HamcrestArchiveProcessor;
import com.ibm.ws.fat.util.tck.TestObserverArchiveProcessor;

public class ArquillianLoadableExtension implements LoadableExtension {
	@Override
	public void register(ExtensionBuilder extensionBuilder) {
		System.out.println("WLP: Adding Extension com.ibm.ws.fat.util.tck.HamcrestArchiveProcessor");
		extensionBuilder.service(ApplicationArchiveProcessor.class, HamcrestArchiveProcessor.class);
		System.out.println("WLP: Adding Extension com.ibm.ws.fat.util.tck.TestObserverArchiveProcessor");
		extensionBuilder.service(ApplicationArchiveProcessor.class, TestObserverArchiveProcessor.class);
	}
}
