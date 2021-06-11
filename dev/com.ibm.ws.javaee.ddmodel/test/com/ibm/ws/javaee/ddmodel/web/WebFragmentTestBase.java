/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.web;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.jmock.Expectations;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.WebFragment;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.ws.javaee.version.ServletVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class WebFragmentTestBase extends DDTestBase {

    WebFragment parse(String xml) throws Exception {
        return parse(xml, WebApp.VERSION_3_0);
    }

    WebFragment parse(String xmlText, int maxVersion) throws Exception {
        OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        ArtifactContainer artifactContainer = mockery.mock(ArtifactContainer.class, "artifactContainer" + mockId++);

        wireOverlay(rootOverlay, artifactContainer);
        
        Container moduleRoot = mockery.mock(Container.class, "moduleRoot" + mockId++);
        Entry fragmentEntry = mockery.mock(Entry.class, "fragmentEntry" + mockId++);
        Container fragmentRoot = mockery.mock(Container.class, "fragmentRoot" + mockId++);
        Entry ddEntry = mockery.mock(Entry.class, "ddEntry" + mockId++);

        wireFragment(moduleRoot, fragmentEntry, fragmentRoot, ddEntry, xmlText);

        WebFragmentAdapter adapter = mockFragmentAdapter(maxVersion);

        try {
            return adapter.adapt(fragmentRoot, rootOverlay, artifactContainer, fragmentRoot);
        } catch ( UnableToAdaptException e ) {
            Throwable cause = e.getCause();
            throw cause instanceof Exception ? (Exception) cause : e;
        }
    }

    private void wireOverlay(OverlayContainer rootOverlay, ArtifactContainer artifactContainer) {
        mockery.checking(new Expectations() {
            {
                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(any(Class.class)));
                will(returnValue(null));
                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(any(Class.class)), with(any(Object.class)));

                allowing(artifactContainer).getPath();
                will(returnValue("/"));
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void wireFragment(
            Container moduleRoot,
            Entry fragmentEntry, Container fragmentRoot,
            Entry ddEntry, String xmlText) throws UnsupportedEncodingException, UnableToAdaptException {
        
        mockery.checking(new Expectations() {
            {
                allowing(moduleRoot).getPhysicalPath();
                will(returnValue("/root/wlp/usr/servers/server1/apps/web.war"));
                allowing(moduleRoot).adapt(Entry.class);
                will(returnValue(null));

                allowing(fragmentEntry).getRoot();
                will(returnValue(moduleRoot));
                allowing(fragmentEntry).getPath();
                will(returnValue("/WEB-INF/lib/fragment1.jar"));
        
                allowing(fragmentRoot).adapt(Entry.class);
                will(returnValue(fragmentEntry));                
                allowing(fragmentRoot).getPath();
                will(returnValue("/"));
                allowing(fragmentRoot).getEntry(WebFragment.DD_NAME);
                will(returnValue(ddEntry));
        
                allowing(ddEntry).getRoot();
                will(returnValue(moduleRoot));
                allowing(ddEntry).getPath();
                will(returnValue('/' + WebFragment.DD_NAME));
                allowing(ddEntry).adapt(InputStream.class);
                will(returnValue(new ByteArrayInputStream(xmlText.getBytes("UTF-8"))));
            }
        });        
    }
    
    private WebFragmentAdapter mockFragmentAdapter(int maxVersion) {
        @SuppressWarnings("unchecked")
        ServiceReference<ServletVersion> versionRef = mockery.mock(ServiceReference.class, "sr" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(versionRef).getProperty(ServletVersion.VERSION);
                will(returnValue(maxVersion));
            }
        });
        
        WebFragmentAdapter adapter = new WebFragmentAdapter();
        adapter.setVersion(versionRef);

        return adapter;
    }
    
    protected static final String webFragment30() {
        return "<web-fragment" +
               " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-fragment_3_0.xsd\"" +
               " version=\"3.0\"" +
               " id=\"WebFragment_ID\"" +
               ">";
    }

    protected static final String webFragment31() {
        return "<web-fragment" +
               " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-fragment_3_1.xsd\"" +
               " version=\"3.1\"" +
               " id=\"WebFragment_ID\"" +
               ">";
    }

    protected static final String webFragment40() {
        return "<web-fragment" +
               " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-fragment_4_0.xsd\"" +
               " version=\"4.0\"" +
               " id=\"WebFragment_ID\"" +
               ">";
    }

    protected static String webFragmentTail() {
        return "</web-fragment>";
    }
}
