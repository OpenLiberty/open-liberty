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
package com.ibm.ws.library.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.library.Library;

/**
 * Records all active libraries
 */

@Component(immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class LibraryStatusService {
    private static final TraceComponent tc = Tr.register(LibraryStatusService.class);

    public static final String LIBRARY_IDS = "active.library.ids";
    public static final String LIBRARY_PIDS = "active.library.pids";

    private final Collection<String> ids = new ConcurrentSkipListSet<String>();
    private final Collection<String> pids = new ConcurrentSkipListSet<String>();

    @SuppressWarnings("serial")
    private final Map<String, Object> properties = Collections.unmodifiableMap(new HashMap<String, Object>() {
        {
            put("service.vendor", "IBM");
            put(LIBRARY_IDS, ids);
            put(LIBRARY_PIDS, pids);
        }
    });

    @Reference(service = Library.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected Map<String, Object> setLibrary(ServiceReference<Library> libraryRef) {
        ids.add((String) libraryRef.getProperty("id"));
        pids.add((String) libraryRef.getProperty("service.pid"));
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Added library " + (String) libraryRef.getProperty("id") + " with pid " + (String) libraryRef.getProperty("service.pid"));
        return properties;
    }

    protected Map<String, Object> unsetLibrary(ServiceReference<Library> libraryRef) {
        ids.remove(libraryRef.getProperty("id"));
        pids.remove(libraryRef.getProperty("service.pid"));
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Removed library " + (String) libraryRef.getProperty("id") + " with pid " + (String) libraryRef.getProperty("service.pid"));
        return properties;
    }

}
