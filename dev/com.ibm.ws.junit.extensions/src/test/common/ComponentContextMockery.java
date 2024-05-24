/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package test.common;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * Context for mocking objects related to a ComponentContext, particularly when the component uses ConcurrentServiceReference collections. Example usage:
 *
 * <pre>
 * private final Mockery mockery = new Mockery();
 * private final ComponentContextMockery ccMockery = new ComponentContextMockery(mockery);
 *
 * &#64;Test
 * public void testExample() {
 *     ComponentContext cc = mockery.mock(ComponentContext.class);
 *     TestService test = mockery.mock(TestService.class);
 *     ServiceReference&lt;TestService> testSR = ccMockery.mockService(cc, "services", service);
 *
 *     TestComponent comp = new TestComponent();
 *     comp.addTest(testSR);
 *     comp.activate(cc);
 * // Test the component...
 * }
 * </pre>
 */
public class ComponentContextMockery {
    private static final AtomicLong nextId = new AtomicLong();
    private static String TEST_SERVICE_NAME_KEY = "io.openliberty.test.service.name";

    public static <T> ServiceReference<T> mockService(Mockery mockery, final ComponentContext cc, final String name, final T service) {
        return mockService(mockery, cc, name, service, null, null);
    }

    public static <T> ServiceReference<T> mockService(Mockery mockery, final ComponentContext cc, final String name, final T service, final Object rank,
                                                      Map<String, Object> extraProps) {
        ServiceReference<T> sr = mockServiceReference(mockery, name, rank, extraProps);
        addService(cc, sr, service, mockery);
        return sr;
    }

    public static <T> void addService(ComponentContext cc, final ServiceReference<T> ref, T service, Mockery mockery) {
        mockery.checking(new Expectations() {
            {
                allowing(cc).locateService((String) ref.getProperty(TEST_SERVICE_NAME_KEY), ref);
                will(returnValue(service));
            }
        });
    }

    public static <T> ServiceReference<T> mockServiceReference(Mockery mockery, final String name) {
        return mockServiceReference(mockery, name, null, null);
    }

    public static <T> ServiceReference<T> mockServiceReference(Mockery mockery, final String name, final Object rank, Map<String, Object> extraProps) {
        final long thisID = nextId.getAndIncrement();
        @SuppressWarnings("unchecked")
        ServiceReference<T> ref = mockery.mock(ServiceReference.class, ServiceReference.class.getName() + "-" + thisID);

        final Hashtable<String, Object> serviceProps = new Hashtable<>();
        if (extraProps != null) {
            extraProps.forEach((k, v) -> {
                if (k != null && v != null) {
                    serviceProps.put(k, v);
                }
            });
        }
        if (rank != null) {
            serviceProps.put(Constants.SERVICE_RANKING, rank);
        } else {
            serviceProps.put(Constants.SERVICE_RANKING, 0);
        }

        serviceProps.put(Constants.SERVICE_ID, thisID);
        serviceProps.put(TEST_SERVICE_NAME_KEY, name);
        mockery.checking(new Expectations() {
            {
                serviceProps.forEach((k, v) -> {
                    allowing(ref).getProperty(k);
                    will(returnValue(v));
                });

                allowing(ref).getProperties();
                will(returnValue(serviceProps));

                allowing(ref).compareTo(with(any(Object.class)));
                will(new CustomAction("compareTo") {
                    @Override
                    public Object invoke(Invocation inv) throws Throwable {
                        ServiceReference<?> otherRef = (ServiceReference<?>) inv.getParameter(0);
                        final int thisRanking = !(rank instanceof Integer) ? 0 : ((Integer) rank).intValue();
                        Object obOtherRanking = otherRef.getProperty(Constants.SERVICE_RANKING);
                        final int otherRanking = !(obOtherRanking instanceof Integer) ? 0 : ((Integer) obOtherRanking).intValue();
                        if (thisRanking != otherRanking) {
                            if (thisRanking < otherRanking) {
                                return -1;
                            }
                            return 1;
                        }

                        Object obOtherID = otherRef.getProperty(Constants.SERVICE_ID);
                        long otherID = ((Long) obOtherID).longValue();
                        if (thisID == otherID) {
                            return 0;
                        }
                        if (thisID < otherID) {
                            return 1;
                        }
                        return -1;
                    }

                });
            }
        });
        return ref;
    }

    private final Mockery mockery;

    public ComponentContextMockery(Mockery mockery) {
        this.mockery = mockery;
    }

    /**
     * Mock a service reference for a component context.
     *
     * @param cc      the mock component context
     * @param refName the reference name of the service in the owning component
     * @param service the service
     * @return a mock service reference
     */
    public <T> ServiceReference<T> mockService(ComponentContext cc, String refName, T service) {
        return mockService(mockery, cc, refName, service);
    }
}
