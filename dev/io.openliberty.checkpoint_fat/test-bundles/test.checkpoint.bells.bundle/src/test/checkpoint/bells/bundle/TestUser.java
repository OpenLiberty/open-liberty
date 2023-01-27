/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package test.checkpoint.bells.bundle;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Component TestUser tracks new services.
 */
@Component(immediate = true)
public class TestUser {

    ServiceTracker<Object, Object> tracker;

    @Activate
    protected void activate(BundleContext context) throws Exception {
        Filter filter = context.createFilter("(&(implementation.class=*)(exported.from=*))");
        tracker = new ServiceTracker<Object, Object>(context, filter, new ServiceTrackerCustomizer<Object, Object>() {
            @Override
            public Object addingService(ServiceReference<Object> ref) {
                Object service = context.getService(ref);
                return service;
            }

            @Override
            public void modifiedService(ServiceReference<Object> ref, Object service) {
            }

            @Override
            public void removedService(ServiceReference<Object> ref, Object service) {
                context.ungetService(ref);
            }
        });
        tracker.open();
    }

    @Deactivate
    protected void deactivate(BundleContext context) {
        if (tracker != null) {
            tracker.close();
            tracker = null;
        }
    }
}
