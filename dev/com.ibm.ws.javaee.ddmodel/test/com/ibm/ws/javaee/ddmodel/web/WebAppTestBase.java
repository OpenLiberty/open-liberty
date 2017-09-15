/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
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

import org.jmock.Expectations;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.ws.javaee.version.ServletVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class WebAppTestBase extends DDTestBase {
    protected boolean isWarModule = false;

    protected WebApp parse(final String xml) throws Exception {
        return parseWebApp(xml, WebApp.VERSION_3_0);
    }

    WebApp parseWebApp(final String xml, final int maxVersion) throws Exception {
        WebAppEntryAdapter adapter = new WebAppEntryAdapter();
        final Container root = mockery.mock(Container.class, "root" + mockId++);
        final OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        final ArtifactEntry artifactEntry = mockery.mock(ArtifactEntry.class, "artifactEntry" + mockId++);
        final Entry entry = mockery.mock(Entry.class, "entry" + mockId++);
        final ServiceReference<ServletVersion> versionRef = mockery.mock(ServiceReference.class, "sr" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(artifactEntry).getPath();
                will(returnValue('/' + WebApp.DD_NAME));

                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(any(Class.class)));
                will(returnValue(null));

                allowing(entry).getPath();
                will(returnValue('/' + WebApp.DD_NAME));

                allowing(entry).adapt(InputStream.class);
                will(returnValue(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(any(Class.class)), with(any(Object.class)));

                allowing(versionRef).getProperty(ServletVersion.VERSION);
                will(returnValue(maxVersion));
            }
        });

        adapter.setVersion(versionRef);

        try {
            return adapter.adapt(root, rootOverlay, artifactEntry, entry);
        } catch (UnableToAdaptException e) {
            Throwable cause = e.getCause();
            throw cause instanceof Exception ? (Exception) cause : e;
        }
    }

    protected static final String webApp24() {
        return "<web-app" +
               " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd\"" +
               " version=\"2.4\"" +
               " id=\"WebApp_ID\"" +
               ">";
    }

    protected static final String webApp30() {
        return "<web-app" +
               " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"" +
               " version=\"3.0\"" +
               " id=\"WebApp_ID\"" +
               ">";
    }

    protected static final String webApp31() {
        return "<web-app" +
               " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd\"" +
               " version=\"3.1\"" +
               " id=\"WebApp_ID\"" +
               ">";
    }

}
