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
package test.common;

import java.util.concurrent.atomic.AtomicLong;

import org.jmock.Expectations;
import org.jmock.Mockery;
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
 * ComponentContext cc = mockery.mock(ComponentContext.class);
 * TestService test = mockery.mock(TestService.class);
 * ServiceReference&lt;TestService> testSR = ccMockery.mockService(cc, "services", service);
 * 
 * TestComponent comp = new TestComponent();
 * comp.addTest(testSR);
 * comp.activate(cc);
 * // Test the component...
 * }
 * </pre>
 */
public class ComponentContextMockery {
    private static AtomicLong nextServiceID = new AtomicLong(1);

    private static <T> ServiceReference<T> mockService(Mockery mockery, final ComponentContext cc, final String name, final T service) {
        final long id = nextServiceID.getAndIncrement();
        @SuppressWarnings("unchecked")
        final ServiceReference<T> sr = mockery.mock(ServiceReference.class, ServiceReference.class.getName() + "-" + id);
        mockery.checking(new Expectations() {
            {
                allowing(sr).getProperty(Constants.SERVICE_ID);
                will(returnValue(id));
                allowing(sr).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(cc).locateService(name, sr);
                will(returnValue(service));
            }
        });
        return sr;
    }

    private final Mockery mockery;

    public ComponentContextMockery(Mockery mockery) {
        this.mockery = mockery;
    }

    /**
     * Mock a service reference for a component context.
     * 
     * @param cc the mock component context
     * @param refName the reference name of the service in the owning component
     * @param service the service
     * @return a mock service reference
     */
    public <T> ServiceReference<T> mockService(ComponentContext cc, String refName, T service) {
        return mockService(mockery, cc, refName, service);
    }
}
