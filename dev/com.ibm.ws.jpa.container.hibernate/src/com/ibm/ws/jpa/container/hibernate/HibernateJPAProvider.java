/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.jpa.container.hibernate;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jpa.AbstractJPAProviderIntegration;
import com.ibm.ws.jpa.JPAProviderIntegration;

@Component(service = { JPAProviderIntegration.class }, property = { "service.ranking:Integer=20" })
public class HibernateJPAProvider extends AbstractJPAProviderIntegration {
    public HibernateJPAProvider() {
        super();
        providersUsed.add(PROVIDER_HIBERNATE); // Avoid 'third party provider' info message when first used
    }

    /**
     * @see com.ibm.ws.jpa.JPAProvider#getProviderClassName()
     */
    @Override
    public String getProviderClassName() {
        return PROVIDER_HIBERNATE;
    }
}
