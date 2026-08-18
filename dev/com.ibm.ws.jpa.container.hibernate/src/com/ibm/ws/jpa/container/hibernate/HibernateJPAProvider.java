/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jpa.container.hibernate;

import java.util.Map;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jpa.AbstractJPAProviderIntegration;
import com.ibm.ws.jpa.JPAProviderIntegration;

/**
 * Integrates Hibernate ORM as the default application persistence provider.
 */
@Component(service = JPAProviderIntegration.class, property = "service.ranking:Integer=20")
public class HibernateJPAProvider extends AbstractJPAProviderIntegration {

    public HibernateJPAProvider() {
        providersUsed.add(PROVIDER_HIBERNATE);
    }

    @Override
    public String getProviderClassName() {
        return PROVIDER_HIBERNATE;
    }

    @Override
    public void disablePersistenceUnitLogging(Map<String, Object> integrationProperties) {
        integrationProperties.put("hibernate.show_sql", "false");
        integrationProperties.put("hibernate.format_sql", "false");
        integrationProperties.put("hibernate.highlight_sql", "false");
    }
}
