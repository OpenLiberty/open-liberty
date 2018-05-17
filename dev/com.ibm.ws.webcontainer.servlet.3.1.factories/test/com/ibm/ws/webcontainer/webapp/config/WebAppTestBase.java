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
package com.ibm.ws.webcontainer.webapp.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.WebFragment;
import com.ibm.ws.javaee.ddmodel.web.WebAppEntryAdapter;
import com.ibm.ws.javaee.ddmodel.web.WebFragmentAdapter;
import com.ibm.ws.javaee.version.ServletVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class WebAppTestBase {

    protected final Mockery mockery = new Mockery();
    protected int mockId;

    public WebApp parse(final String xml) throws Exception {
        return parseWebApp(xml, WebApp.VERSION_3_0);
    }

    public static WebApp parseWebApp(final String xmlPath, final int maxVersion) throws Exception {

        final FileInputStream xml = new FileInputStream(new File(xmlPath));

        final Mockery mockery = new Mockery();
        int mockId = 0;

        WebAppEntryAdapter adapter = new WebAppEntryAdapter();
        final Container root = mockery.mock(Container.class, "root" + mockId++);
        final OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        final ArtifactEntry artifactEntry = mockery.mock(ArtifactEntry.class, "artifactEntry" + mockId++);
        final Entry entry = mockery.mock(Entry.class, "entry" + mockId++);
        final ServiceReference<ServletVersion> versionRef = mockery.mock(ServiceReference.class, "sr" + mockId++);
        // final FileInputStream webxml = new FileInputStream(new File("resources/mergeTestXmls/DataSourceOverrideWebxmlMerge/web.xml"));
        mockery.checking(new Expectations() {
            {
                allowing(artifactEntry).getPath();
                will(returnValue('/' + WebApp.DD_NAME));

                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(any(Class.class)));
                will(returnValue(null));

                allowing(entry).getPath();
                will(returnValue('/' + WebApp.DD_NAME));

                allowing(entry).adapt(InputStream.class);
                will(returnValue(xml));

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

    public WebFragment parseWebFragment(final String xml) throws Exception {
        return parseWebFragment(xml, WebApp.VERSION_3_0);
    }

    public static WebFragment parseWebFragment(final String xmlPath, final int maxVersion) throws Exception {

        final FileInputStream xml = new FileInputStream(new File(xmlPath));

        final Mockery mockery = new Mockery();
        int mockId = 0;

        WebFragmentAdapter adapter = new WebFragmentAdapter();
        final Container root = mockery.mock(Container.class, "root" + mockId++);
        final Entry entry = mockery.mock(Entry.class, "entry" + mockId++);
        final OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        final ArtifactContainer artifactContainer = mockery.mock(ArtifactContainer.class, "artifactContainer" + mockId++);
        final Container container = mockery.mock(Container.class, "container" + mockId++);
        final ServiceReference<ServletVersion> versionRef = mockery.mock(ServiceReference.class, "sr" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(artifactContainer).getPath();
                will(returnValue(WebFragment.DD_NAME));

                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(any(Class.class)));
                will(returnValue(null));

                allowing(container).getEntry(WebFragment.DD_NAME);
                will(returnValue(entry));

                allowing(entry).getPath();
                will(returnValue('/' + WebFragment.DD_NAME));

                allowing(entry).adapt(InputStream.class);
                will(returnValue(xml));

                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(any(Class.class)), with(any(Object.class)));

                allowing(versionRef).getProperty(ServletVersion.VERSION);
                will(returnValue(maxVersion));
            }
        });

        adapter.setVersion(versionRef);

        try {
            return adapter.adapt(root, rootOverlay, artifactContainer, container);
        } catch (UnableToAdaptException e) {
            Throwable cause = e.getCause();
            throw cause instanceof Exception ? (Exception) cause : e;
        }
    }

}
