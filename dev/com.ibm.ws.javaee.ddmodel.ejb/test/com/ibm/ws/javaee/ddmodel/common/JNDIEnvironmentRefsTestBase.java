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
package com.ibm.ws.javaee.ddmodel.common;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.ddmodel.ejb.EJBJarDDParserVersion;
import com.ibm.ws.javaee.ddmodel.ejb.EJBJarEntryAdapter;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class JNDIEnvironmentRefsTestBase {
    private final Mockery mockery = new Mockery();
    private int mockId;

    private EJBJar parseEJBJar(final String xml) throws Exception {
        EJBJarEntryAdapter adapter = new EJBJarEntryAdapter();
        @SuppressWarnings("unchecked")
        final ServiceReference<EJBJarDDParserVersion> versionRef = mockery.mock(ServiceReference.class, "sr" + mockId++);
        final Container root = mockery.mock(Container.class, "root" + mockId++);
        final Entry entry = mockery.mock(Entry.class, "entry" + mockId++);
        final OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        final ArtifactEntry artifactEntry = mockery.mock(ArtifactEntry.class, "artifactContainer" + mockId++);
        final NonPersistentCache nonPC = mockery.mock(NonPersistentCache.class, "nonPC" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(artifactEntry).getPath();
                will(returnValue("META-INF/ejb-jar.xml"));

                allowing(root).adapt(NonPersistentCache.class);
                will(returnValue(nonPC));
                allowing(nonPC).getFromCache(WebModuleInfo.class);
                will(returnValue(null));

                allowing(entry).getPath();
                will(returnValue("META-INF/ejb-jar.xml"));

                allowing(entry).adapt(InputStream.class);
                will(returnValue(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(EJBJar.class));
                will(returnValue(null));
                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(EJBJar.class), with(any(EJBJar.class)));

                allowing(versionRef).getProperty(EJBJarDDParserVersion.VERSION);
                will(returnValue(EJBJar.VERSION_3_2));
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

    protected com.ibm.ws.javaee.dd.common.JNDIEnvironmentRefs parseEJB31(String refString) throws Exception {
        String xml = "<ejb-jar" +
                     " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                     " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                     " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_1.xsd\"" +
                     " version=\"3.1\"" +
                     " <enterprise-beans>" +
                     "  <session>" +
                     refString +
                     "  </session>" +
                     " </enterprise-beans>" +
                     "</ejb-jar>";
        return parseEJBJar(xml).getEnterpriseBeans().get(0);
    }

    protected com.ibm.ws.javaee.dd.common.JNDIEnvironmentRefs parse(String refString) throws Exception {
        String xml = "<ejb-jar" +
                     " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                     " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                     " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/ejb-jar_3_2.xsd\"" +
                     " version=\"3.2\"" +
                     ">" +
                     " <enterprise-beans>" +
                     "  <session>" +
                     refString +
                     "  </session>" +
                     " </enterprise-beans>" +
                     "</ejb-jar>";
        return parseEJBJar(xml).getEnterpriseBeans().get(0);
    }
}
