/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
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
    public void testServlet40ObjectCreation() throws Exception {
        WebContainer webContainer = new WebContainer();
        final ServiceReference<ServletVersion> versionRef = context.mock(ServiceReference.class, "sr" + mockId++);

        context.checking(new Expectations() {
            {
                allowing(versionRef).getProperty(ServletVersion.VERSION);
                will(returnValue(WebContainer.SPEC_LEVEL_40));
            }
        });

        Class<WebContainer> clazz = (Class<WebContainer>) webContainer.getClass();

        Method versionSetter = clazz.getDeclaredMethod("setVersion", ServiceReference.class);

        versionSetter.setAccessible(true);
        versionSetter.invoke(webContainer, versionRef);

        assertTrue("Returned webApp container version should be 40", WebContainer.getServletContainerSpecLevel() == WebContainer.SPEC_LEVEL_40);

        context.assertIsSatisfied();
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

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
