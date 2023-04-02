/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package com.ibm.ws.classloading.exporting.test;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Component TestUser exercises Bell service test behaviors in the course of tracking new services.
 */
@Component(immediate=true)
public class TestUser {
    private static final TraceComponent tc = Tr.register(TestUser.class);
    @Activate
    protected void activate(final BundleContext context) throws InvalidSyntaxException {
        Filter filter = context.createFilter("(&(implementation.class=*)(exported.from=*))");
        ServiceTracker<Object, Object> tracker = new ServiceTracker<Object, Object>(context, filter, new ServiceTrackerCustomizer<Object, Object>() {
            @Override
            public Object addingService(ServiceReference<Object> ref) {
                Object service =  context.getService(ref);

                if (service instanceof TestInterface) {
                    String isThere = ((TestInterface) service).isThere("impl");
                    logEventResult("addingService", isThere);
                } else if (service instanceof TestInterface2) {
                    String isThere2 = ((TestInterface2) service).isThere2("impl");
                    logEventResult("addingService", isThere2);
                }

                if (service instanceof TestInterface) {
                    System.out.println("TestUser addingService: " + ((TestInterface) service).hasProperties(service.getClass().getSimpleName()));
                } else if (service instanceof TestInterface2) {
                    System.out.println("TestUser addingService: " + ((TestInterface2) service).hasProperties2(service.getClass().getSimpleName()));
                }

                if (service instanceof TestInterface) {
                    System.out.println("TestUser addingService: " + ((TestInterface) service).hasUpdatedProperties(service.getClass().getSimpleName()));
                } else if (service instanceof TestInterface2) {
                    System.out.println("TestUser addingService: " + ((TestInterface2) service).hasUpdatedProperties2(service.getClass().getSimpleName()));
                }

                return service;
            }

            void logEventResult(String event, String result) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, event, result);
                else
                   // This is handy for debugging
                   System.out.println("TestUser: " + event + ": " + result);
            }

            @Override
            public void modifiedService(ServiceReference<Object> ref, Object service) {
            }

            @Override
            public void removedService(ServiceReference<Object> ref, Object service) {
                context.ungetService(ref);
            }});
        tracker.open();
    }
}
