/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.resource.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.ibm.wsspi.resource.ResourceFactory;

public class ResourceFactoryTracker implements ServiceTrackerCustomizer<ResourceFactory, ResourceFactoryTrackerData> {
    private static final String FILTER = "(&" +
                                         "(" + Constants.OBJECTCLASS + "=" + ResourceFactory.class.getName() + ")" +
                                         "(" + ResourceFactory.JNDI_NAME + "=*)" +
                                         "(" + ResourceFactory.CREATES_OBJECT_CLASS + "=*)" +
                                         ")";

    private ServiceTracker<ResourceFactory, ResourceFactoryTrackerData> tracker;

    public void activate(BundleContext context) throws InvalidSyntaxException {
        Filter filter = FrameworkUtil.createFilter(FILTER);
        tracker = new ServiceTracker<ResourceFactory, ResourceFactoryTrackerData>(context, filter, this);
        tracker.open();
    }

    public void deactivate(ComponentContext cc) {
        tracker.close();
    }

    @Override
    public ResourceFactoryTrackerData addingService(ServiceReference<ResourceFactory> ref) {
        ResourceFactoryTrackerData data = new ResourceFactoryTrackerData(ref.getBundle().getBundleContext());
        data.register(ref);
        return data;
    }

    @Override
    public void modifiedService(ServiceReference<ResourceFactory> ref, ResourceFactoryTrackerData data) {
        data.modifed(ref);
    }

    @Override
    public void removedService(ServiceReference<ResourceFactory> ref, ResourceFactoryTrackerData data) {
        data.unregister();
    }
}
