/*******************************************************************************
/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.javaee.version.ServletVersion;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.osgi.WebContainer;

/**
 * OLGH PR 20004 -- Removed ServiceReference
 */
public class SRTServletContainerVersionTest {

    private final Mockery context = new Mockery();
    private int mockId = 1;

    @SuppressWarnings("unchecked")
    @Test
    public void testServlet50ObjectCreation() throws Exception {
        WebContainer webContainer = new WebContainer();

        Class<WebContainer> clazz = (Class<WebContainer>) webContainer.getClass();

        Field versionSetter = clazz.getDeclaredField("loadedContainerSpecLevel");

        versionSetter.setAccessible(true);
        versionSetter.set(webContainer, 50);

        assertTrue("Returned webApp container version should be 50", WebContainer.getServletContainerSpecLevel() == WebContainer.SPEC_LEVEL_50);

        context.assertIsSatisfied();
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testServlet40ObjectCreation() throws Exception {
        WebContainer webContainer = new WebContainer();
        // final ServiceReference<ServletVersion> versionRef = context.mock(ServiceReference.class, "sr" + mockId++);

        // context.checking(new Expectations() {
        //     {
        //         // allowing(versionRef).getProperty(ServletVersion.VERSION);
        //         allowing(webContainer).setVersion(40);
        //         will(returnValue(WebContainer.SPEC_LEVEL_40));
        //     }
        // });

        Class<WebContainer> clazz = (Class<WebContainer>) webContainer.getClass();

        Field versionSetter = clazz.getDeclaredField("loadedContainerSpecLevel");

        versionSetter.setAccessible(true);
        versionSetter.set(webContainer, 40);

        assertTrue("Returned webApp container version should be 40", WebContainer.getServletContainerSpecLevel() == WebContainer.SPEC_LEVEL_40);

        context.assertIsSatisfied();
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testServlet31ObjectCreation() throws Exception {
        WebContainer webContainer = new WebContainer();
        // final ServiceReference<ServletVersion> versionRef = context.mock(ServiceReference.class, "sr" + mockId++);

        // context.checking(new Expectations() {
        //     {
        //         allowing(versionRef).getProperty(ServletVersion.VERSION);
        //         will(returnValue(WebContainer.SPEC_LEVEL_31));

        //     }
        // });

        Class<WebContainer> clazz = (Class<WebContainer>) webContainer.getClass();

        Field versionSetter = clazz.getDeclaredField("loadedContainerSpecLevel");

        versionSetter.setAccessible(true);
        versionSetter.set(webContainer, 31);

        assertTrue("Returned webApp container version should be 31", WebContainer.getServletContainerSpecLevel() == WebContainer.SPEC_LEVEL_31);

        context.assertIsSatisfied();
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testServlet30ObjectCreation() throws Exception {
        WebContainer webContainer = new WebContainer();
        // final ServiceReference<ServletVersion> versionRef = context.mock(ServiceReference.class, "sr" + mockId++);

        // context.checking(new Expectations() {
        //     {
        //         allowing(versionRef).getProperty(ServletVersion.VERSION);
        //         will(returnValue(WebContainer.SPEC_LEVEL_30));
        //     }
        // });

        Class<WebContainer> clazz = (Class<WebContainer>) webContainer.getClass();

        Field versionSetter = clazz.getDeclaredField("loadedContainerSpecLevel");

        versionSetter.setAccessible(true);
        versionSetter.set(webContainer, 30);

        assertTrue("Returned webApp container version should be 30", WebContainer.getServletContainerSpecLevel() == WebContainer.SPEC_LEVEL_30);

        context.assertIsSatisfied();
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    // @SuppressWarnings("unchecked")
    // @Test
    // public void testServletErrorVersion() throws Exception {
    //     WebContainer webContainer = new WebContainer();
    //     // final ServiceReference<ServletVersion> versionRef = context.mock(ServiceReference.class, "sr" + mockId++);

    //     // context.checking(new Expectations() {
    //     //     {

    //     //         allowing(versionRef).getProperty(ServletVersion.VERSION);
    //     //         will(returnValue(WebContainer.SPEC_LEVEL_UNLOADED));
    //     //     }
    //     // });

    //     Class<WebContainer> clazz = (Class<WebContainer>) webContainer.getClass();

    //     Field versionSetter = clazz.getDeclaredField("loadedContainerSpecLevel");

    //     versionSetter.setAccessible(true);
    //     versionSetter.set(webContainer, -1);

    //     assertTrue("Returned webApp container version should be 30", WebContainer.getServletContainerSpecLevel() == WebContainer.SPEC_LEVEL_30);

    //     context.assertIsSatisfied();
    //     ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    // }
}
