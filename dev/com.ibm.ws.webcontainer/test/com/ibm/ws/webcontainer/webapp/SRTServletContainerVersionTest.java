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
package com.ibm.ws.webcontainer.webapp;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.javaee.version.ServletVersion;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.osgi.WebContainer;

/**
 *
 */
public class SRTServletContainerVersionTest {

    private final Mockery context = new Mockery();
    private int mockId = 1;

    @SuppressWarnings("unchecked")
    @Test
    public void testServlet31ObjectCreation() throws Exception {
        WebContainer webContainer = new WebContainer();
        final ServiceReference<ServletVersion> versionRef = context.mock(ServiceReference.class, "sr" + mockId++);

        context.checking(new Expectations() {
            {
                allowing(versionRef).getProperty(ServletVersion.VERSION);
                will(returnValue(WebContainer.SPEC_LEVEL_31));

            }
        });

        Class<WebContainer> clazz = (Class<WebContainer>) webContainer.getClass();

        Method versionSetter = clazz.getDeclaredMethod("setVersion", ServiceReference.class);

        versionSetter.setAccessible(true);
        versionSetter.invoke(webContainer, versionRef);

        assertTrue("Returned webApp container version should be 31", WebContainer.getServletContainerSpecLevel() == WebContainer.SPEC_LEVEL_31);

        context.assertIsSatisfied();
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testServlet30ObjectCreation() throws Exception {
        WebContainer webContainer = new WebContainer();
        final ServiceReference<ServletVersion> versionRef = context.mock(ServiceReference.class, "sr" + mockId++);

        context.checking(new Expectations() {
            {
                allowing(versionRef).getProperty(ServletVersion.VERSION);
                will(returnValue(WebContainer.SPEC_LEVEL_30));
            }
        });

        Class<WebContainer> clazz = (Class<WebContainer>) webContainer.getClass();

        Method versionSetter = clazz.getDeclaredMethod("setVersion", ServiceReference.class);

        versionSetter.setAccessible(true);
        versionSetter.invoke(webContainer, versionRef);

        assertTrue("Returned webApp container version should be 30", WebContainer.getServletContainerSpecLevel() == WebContainer.SPEC_LEVEL_30);

        context.assertIsSatisfied();
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testServletErrorVersion() throws Exception {
        WebContainer webContainer = new WebContainer();
        final ServiceReference<ServletVersion> versionRef = context.mock(ServiceReference.class, "sr" + mockId++);

        context.checking(new Expectations() {
            {

                allowing(versionRef).getProperty(ServletVersion.VERSION);
                will(returnValue(WebContainer.SPEC_LEVEL_UNLOADED));
            }
        });

        Class<WebContainer> clazz = (Class<WebContainer>) webContainer.getClass();

        Method versionSetter = clazz.getDeclaredMethod("setVersion", ServiceReference.class);

        versionSetter.setAccessible(true);
        versionSetter.invoke(webContainer, versionRef);

        assertTrue("Returned webApp container version should be 30", WebContainer.getServletContainerSpecLevel() == WebContainer.SPEC_LEVEL_30);

        context.assertIsSatisfied();
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }
}
