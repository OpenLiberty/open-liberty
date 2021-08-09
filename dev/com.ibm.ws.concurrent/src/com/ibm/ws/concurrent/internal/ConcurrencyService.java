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
package com.ibm.ws.concurrent.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ws.concurrent.ext.ConcurrencyExtensionProvider;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, service = ConcurrencyService.class)
public class ConcurrencyService {
    @Reference(policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.OPTIONAL, target = "(id=unbound)")
    ConcurrencyExtensionProvider extensionProvider;
}
