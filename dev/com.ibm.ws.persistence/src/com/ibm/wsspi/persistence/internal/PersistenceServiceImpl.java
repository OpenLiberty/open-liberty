/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import com.ibm.wsspi.persistence.internal.eclipselink.PsPersistenceProvider;

@Component(name = "persistenceService", service = { PersistenceService.class })
public class PersistenceServiceImpl implements PersistenceService {
    private final PsPersistenceProvider _provider;

    private final AtomicServiceReference<InMemoryUrlStreamHandler> _handler =
                    new AtomicServiceReference<InMemoryUrlStreamHandler>("InMemoryUrlStreamHandler");
    private final DatabaseManager _dbManager = new DatabaseManager();
    private URL _bundleRootUrl;

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) throws Exception {
        _handler.activate(cc);
        _bundleRootUrl = cc.getBundleContext().getBundle().getEntry("/");
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) throws Exception {
        InMemoryUrlStreamHandler handler = _handler.getService();
        if (handler != null ) {
            handler.clear();
        }
        _handler.deactivate(cc);
    }

    public PersistenceServiceImpl() {
        _provider = new PsPersistenceProvider();
    }

    @Override
    public PersistenceServiceUnit createPersistenceServiceUnit(PersistenceServiceUnitConfig conf) {
        conf.validate();

        PersistenceServiceUnitImpl pui =
                        new PersistenceServiceUnitImpl(conf, _provider, _handler.getServiceWithException(), _dbManager, _bundleRootUrl);
        // TODO -- could keep track of PersistenceServiceUnitImpl?
        return pui;
    }

    @Reference(name = "InMemoryUrlStreamHandler", service = InMemoryUrlStreamHandler.class)
    protected void setHandler(ServiceReference<InMemoryUrlStreamHandler> reference) {
        _handler.setReference(reference);
    }

    protected void unsetHandler(ServiceReference<InMemoryUrlStreamHandler> reference) {
        _handler.unsetReference(reference);
    }
}
