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
package io.openliberty.data.internal.ejb;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.ejbcontainer.EJBEndpoint;
import com.ibm.ws.ejbcontainer.EJBEndpoints;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

import io.openliberty.data.internal.tracker.ModuleTracker;

/**
 * Obtain names of components in Jakarta Enterprise Beans modules.
 */
@Component(configurationPid = "io.openliberty.data.internal.ejb.EJBModuleTracker",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = { ModuleMetaDataListener.class,
                       ModuleTracker.class })
public class EJBModuleTracker implements ModuleMetaDataListener, ModuleTracker {
    private static final TraceComponent tc = Tr.register(EJBModuleTracker.class);

    private final Map<J2EEName, SortedSet<String>> componentNames = new HashMap<>();

    @Override
    public String firstComponentName(J2EEName moduleName) {
        SortedSet<String> names = componentNames.get(moduleName);
        if (names == null || names.isEmpty())
            throw new NoSuchElementException();
        else
            return names.first();
    }

    @Override
    public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) throws MetaDataException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        Container container = event.getContainer();
        try {
            EJBEndpoints endpoints = container.adapt(EJBEndpoints.class);
            if (endpoints != null) {
                // TODO does not need to be a tree. Can use single string.
                TreeSet<String> names = new TreeSet<>();
                for (EJBEndpoint endpoint : endpoints.getEJBEndpoints())
                    names.add(endpoint.getName());
                if (!names.isEmpty())
                    componentNames.put(event.getMetaData().getJ2EEName(), names);

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc,
                             "EJB endpoints: " + endpoints,
                             "Found: " + names);
            }
        } catch (UnableToAdaptException x) {
            throw new MetaDataException(x);
        }
    }

    @Override
    public void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event) {
        componentNames.remove(event.getMetaData().getJ2EEName());
    }
}