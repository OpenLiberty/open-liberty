/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.wsspi.persistence.internal;

import java.net.URL;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.persistence.PersistenceService;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;
import com.ibm.wsspi.persistence.PersistenceServiceUnitConfig;

/**
 * OSGi DS component that implements {@link PersistenceService} backed by Hibernate 8.x.
 *
 * This is the Hibernate equivalent of the EclipseLink-backed PersistenceServiceImpl in
 * com.ibm.ws.persistence.jakarta. It is activated by the persistenceService-3.0 feature.
 *
 * Component name differs from "persistenceService" (used by 1.0/2.0) to avoid conflict —
 * the feature system ensures only one variant is active at a time.
 */
@Component(name = "io.openliberty.persistence.service.hibernate",
           service = { PersistenceService.class })
public class HibernatePersistenceServiceImpl implements PersistenceService {

    private final AtomicServiceReference<InMemoryUrlStreamHandler> _handler =
                    new AtomicServiceReference<InMemoryUrlStreamHandler>("InMemoryUrlStreamHandler");

    private URL _bundleRootUrl;

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) throws Exception {
        _handler.activate(cc);
        _bundleRootUrl = cc.getBundleContext().getBundle().getEntry("/");
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) throws Exception {
        InMemoryUrlStreamHandler handler = _handler.getService();
        if (handler != null) {
            handler.clear();
        }
        _handler.deactivate(cc);
    }

    @Override
    public PersistenceServiceUnit createPersistenceServiceUnit(PersistenceServiceUnitConfig conf) {
        conf.validate();
        return new HibernatePersistenceServiceUnitImpl(conf,
                                                       _handler.getServiceWithException(),
                                                       _bundleRootUrl);
    }

    @Reference(name = "InMemoryUrlStreamHandler", service = InMemoryUrlStreamHandler.class)
    protected void setHandler(ServiceReference<InMemoryUrlStreamHandler> reference) {
        _handler.setReference(reference);
    }

    protected void unsetHandler(ServiceReference<InMemoryUrlStreamHandler> reference) {
        _handler.unsetReference(reference);
    }
}
