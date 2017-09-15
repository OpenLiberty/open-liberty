/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.jdbc;

import java.io.Serializable;
import java.util.Collection;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jdbc.DataSourceService;
import com.ibm.ws.kernel.service.util.PrivHelper;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.rsadapter.AdapterUtil;

/**
 * Serialized form of DataSourceWrapper.
 */
public class SerializedDataSourceWrapper implements Serializable {
    private static final long serialVersionUID = -7663502167082261791L;
    private static final TraceComponent tc = Tr.register(SerializedDataSourceWrapper.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * Filter for the DataSourceService in OSGI.
     */
    private final String filter;

    /**
     * Resource reference settings. This field must be an instance of {@link com.ibm.ws.resource.ResourceRefInfo}. The
     * static type is Serializable to avoid FindBugs warnings.
     */
    private final Serializable resourceRefInfo;

    SerializedDataSourceWrapper(String filter, ResourceRefInfo resourceRefInfo) {
        this.filter = filter;
        this.resourceRefInfo = (Serializable) resourceRefInfo;
    }

    private Object readResolve() throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "readResolve", filter, resourceRefInfo);

        Object dataSource;
        BundleContext bundleContext = PrivHelper.getBundleContext(FrameworkUtil.getBundle(DataSourceService.class));
        ServiceReference<DataSourceService> dssvcRef = null;
        try {
            Collection<ServiceReference<DataSourceService>> srs = PrivHelper.getServiceReferences(bundleContext,DataSourceService.class, filter);
            if (srs.isEmpty())
                throw new IllegalStateException(filter);

            dssvcRef = srs.iterator().next();
            DataSourceService dssvc = PrivHelper.getService(bundleContext,dssvcRef);

            dataSource = dssvc.createResource((ResourceRefInfo) resourceRefInfo);
        } catch (Exception x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "readResolve", x);
            throw x;
        } finally {
            if (dssvcRef != null)
                bundleContext.ungetService(dssvcRef);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "readResolve", dataSource);
        return dataSource;
    }
}
