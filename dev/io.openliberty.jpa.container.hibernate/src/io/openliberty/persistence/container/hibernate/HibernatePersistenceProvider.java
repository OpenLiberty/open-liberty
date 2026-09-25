/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *

 *******************************************************************************/
package io.openliberty.persistence.container.hibernate;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jpa.AbstractJPAProviderIntegration;
import com.ibm.ws.jpa.JPAProviderIntegration;

@Component(service = { JPAProviderIntegration.class }, property = { "service.ranking:Integer=20" })
public class HibernatePersistenceProvider extends AbstractJPAProviderIntegration {
    public HibernatePersistenceProvider() {
        super();
        providersUsed.add(PROVIDER_HIBERNATE); 
    }

    /**
     * @see com.ibm.ws.jpa.JPAProvider#getProviderClassName()
     */
    @Override
    public String getProviderClassName() {
        return PROVIDER_HIBERNATE;
    }
}
