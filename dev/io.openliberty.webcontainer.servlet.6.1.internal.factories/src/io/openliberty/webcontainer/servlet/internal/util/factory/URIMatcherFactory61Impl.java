/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet.internal.util.factory;

import org.osgi.service.component.annotations.Component;

import com.ibm.wsspi.webcontainer.util.URIMatcher;
import com.ibm.wsspi.webcontainer.util.URIMatcherFactory;
import com.ibm.wsspi.webcontainer40.util.URIMatcher40;

@Component(service = URIMatcherFactory.class, property = { "service.vendor=IBM" })
public class URIMatcherFactory61Impl implements URIMatcherFactory {

    @Override
    public URIMatcher createURIMatcher() {
        return new URIMatcher40();
    }

    @Override
    public URIMatcher createURIMatcher(boolean scalable) {
        return new URIMatcher40(scalable);
    }

}
