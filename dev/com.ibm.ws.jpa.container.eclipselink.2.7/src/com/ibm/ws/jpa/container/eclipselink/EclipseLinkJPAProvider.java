/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.eclipselink;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jpa.AbstractJPAProviderIntegration;
import com.ibm.ws.jpa.JPAProviderIntegration;

@Component(service = { JPAProviderIntegration.class }, property = { "service.ranking:Integer=20" })
public class EclipseLinkJPAProvider extends AbstractJPAProviderIntegration {
    public EclipseLinkJPAProvider() {
        super();
        providersUsed.add(PROVIDER_ECLIPSELINK); // Avoid 'third party provider' info message when first used
    }

    /**
     * @see com.ibm.ws.jpa.JPAProvider#getDefaultProviderName()
     */
    @Override
    public String getProviderClassName() {
        return PROVIDER_ECLIPSELINK;
    }

    // TODO updatePersistenceUnitProperties -- need to remove an logging properties as logging is handled via Liberty logging

    // TODO supportsEntityManagerPooling -- need to investigate using EntityManager pooling.
}
