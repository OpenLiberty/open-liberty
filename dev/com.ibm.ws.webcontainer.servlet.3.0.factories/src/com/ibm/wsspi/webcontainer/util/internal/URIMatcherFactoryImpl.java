/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.wsspi.webcontainer.util.internal;

import org.osgi.service.component.annotations.Component;

import com.ibm.wsspi.webcontainer.util.URIMatcher;
import com.ibm.wsspi.webcontainer.util.URIMatcherFactory;

@Component(service=URIMatcherFactory.class, property = { "service.vendor=IBM" })
public class URIMatcherFactoryImpl implements URIMatcherFactory{

    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.util.URIMatcherFactory#createURIMatcher()
     */
    @Override
    public URIMatcher createURIMatcher() {
        return new URIMatcher();
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.util.URIMatcherFactory#createURIMatcher(boolean)
     */
    @Override
    public URIMatcher createURIMatcher(boolean scalable) {
        return new URIMatcher(scalable);
    }

}
