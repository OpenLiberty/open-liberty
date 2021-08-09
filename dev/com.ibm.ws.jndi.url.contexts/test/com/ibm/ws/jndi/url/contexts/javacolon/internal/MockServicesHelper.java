/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.url.contexts.javacolon.internal;

import java.util.Collection;
import java.util.Collections;

import javax.naming.NameClassPair;
import javax.naming.NamingException;

import org.jmock.Mockery;
import org.junit.Before;
import org.osgi.service.component.ComponentContext;

import test.common.ComponentContextMockery;

import com.ibm.ws.container.service.naming.JavaColonNamingHelper;
import com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace;

public class MockServicesHelper {
    // set up a mock BundleContext
    private final Mockery mocker = new Mockery();
    private final ComponentContextMockery ccMockery = new ComponentContextMockery(mocker);
    protected final JavaURLContextFactory factory = new JavaURLContextFactory();
    private final JavaColonNamingHelper mockNamingService = new MockNamingHelperService();

    // mock lookup object
    protected final static Object testObject = new Object();

    protected final static NameClassPair testPair = new NameClassPair("test", testObject.getClass().getName());

    @Before
    public void setupTest() throws Exception {
        ComponentContext cc = mocker.mock(ComponentContext.class);
        factory.addHelper(ccMockery.mockService(cc, "helpers", mockNamingService));
        factory.activate(cc);
    }

    private static final class MockNamingHelperService implements JavaColonNamingHelper {
        @Override
        public Object getObjectInstance(JavaColonNamespace namespace, String name) throws NamingException {
            //"bind" something under the name of test
            if (name.equals("test"))
                return testObject;
            else
                return null;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasObjectWithPrefix(JavaColonNamespace namespace, String name) {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public Collection<? extends NameClassPair> listInstances(JavaColonNamespace namespace, String nameInContext) throws NamingException {

            if (namespace == JavaColonNamespace.COMP_ENV && nameInContext.equals(""))
                return Collections.singletonList(testPair);
            else
                return Collections.emptyList();
        }

    }
}
