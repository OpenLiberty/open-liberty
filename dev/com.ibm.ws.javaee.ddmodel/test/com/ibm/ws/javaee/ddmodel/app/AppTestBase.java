/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.app;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.jmock.Expectations;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class AppTestBase extends DDTestBase {

    protected Application parse(String xml) throws Exception {
        return parse(xml, JavaEEVersion.VERSION_6_0);
    }

    protected Application parse(final String xml, Version platformVersion) throws Exception {
        final String versionText = platformVersion.toString();

        // Mock up data structures needed to perform parsing:

        // Mock up the parameters to 'ApplicationAdapter.adapt':
        final Container root = mockery.mock(Container.class, "root" + mockId++);
        final OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        final ArtifactContainer artifactContainer = mockery.mock(ArtifactContainer.class, "artifactContainer" + mockId++);
        final Container containerToAdapt = mockery.mock(Container.class, "containerToAdapt" + mockId++);

        // Mock up the entry for the application descriptor:
        final Entry entry = mockery.mock(Entry.class, "entry" + mockId++);

        // Mock up the JavaEEVersion reference, which carries the application feature version:
        @SuppressWarnings("unchecked")
        final ServiceReference<JavaEEVersion> versionRef = mockery.mock(ServiceReference.class, "sr" + mockId++);

        // Teach the mock objects their expectations.  These are determined by
        // examination of the application adapter code.

        // containerToAdapt3.getEntry("META-INF/application.xml")

        mockery.checking(new Expectations() {
            {
                // Required by calls to getFromNonPersistentCache and addToNonPersistentCache.
                // The actual return value doesn't matter.
                allowing(artifactContainer).getPath();
                will(returnValue("/"));

                // Give the root overlay the non-persistent cache API.
                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(any(Class.class)));
                will(returnValue(null));
                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(any(Class.class)), with(any(Object.class)));

                // Teach the container to answer the entry for the application descriptor.
                allowing(containerToAdapt).getEntry(Application.DD_NAME);
                will(returnValue(entry));

                // Fill in the path and input stream APIs for the descriptor entry.
                allowing(entry).getPath();
                will(returnValue('/' + Application.DD_NAME));
                allowing(entry).adapt(InputStream.class);
                will(returnValue(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

                // Teach the version reference to answer the platform version.
                allowing(versionRef).getProperty(JavaEEVersion.VERSION);
                will(returnValue(versionText));
            }
        });

        // Parse using the mock data structures:

        ApplicationAdapter adapter = new ApplicationAdapter();

        adapter.setVersion(versionRef);

        try {
            return adapter.adapt(root, rootOverlay, artifactContainer, containerToAdapt);
        } catch (UnableToAdaptException e) {
            Throwable cause = e.getCause();
            throw cause instanceof Exception ? (Exception) cause : e;
        }
    }

    // 1.2
    // 1.3
    // 1.4, http://java.sun.com/xml/ns/j2ee
    // 5,   http://java.sun.com/xml/ns/javaee
    // 6,   http://java.sun.com/xml/ns/javaee
    // 7,   http://xmlns.jcp.org/xml/ns/javaee
    // 8,   http://xmlns.jcp.org/xml/ns/javaee
    // 9,   https://jakarta.ee/xml/ns/jakartaee

    // 1.2 and 1.3 are DD based:

    protected static String app12() {
        return "<!DOCTYPE application PUBLIC" +
               " \"-//Sun Microsystems, Inc.//DTD J2EE Application 1.2//EN\"" +
               " \"http://java.sun.com/j2ee/dtds/application_1_2.dtd\">" +
               "<application>";
    }

    protected static String app13() {
        return "<!DOCTYPE application PUBLIC" +
               " \"-//Sun Microsystems, Inc.//DTD J2EE Application 1.3//EN\"" +
               " \"http://java.sun.com/j2ee/dtds/application_1_3.dtd\">" +
               "<application>";
    }

    // 1.4, 5.0. 6.0, 7.0, and 8.0 are schema based:

    protected static String app14() {
        return "<application" +
               " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee" +
               " http://java.sun.com/xml/ns/j2ee/application_1_4.xsd\"" +
               " version=\"1.4\"" +
               " id=\"Application_ID\"" +
               ">";
    }

    protected static String app50() {
        return "<application" +
               " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
               " http://java.sun.com/xml/ns/javaee/application_5.xsd\"" +
               " version=\"5\"" +
               " id=\"Application_ID\"" +
               ">";
    }

    protected static String app60() {
        return "<application" +
               " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
               " http://java.sun.com/xml/ns/javaee/application_6.xsd\"" +
               " version=\"6\"" +
               " id=\"Application_ID\"" +
               ">";
    }

    protected static String app70() {
        return "<application" +
               " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee" +
               " http://xmlns.jcp.org/xml/ns/javaee/application_7.xsd\"" +
               " version=\"7\"" +
               " id=\"Application_ID\"" +
               ">";
    }

    protected static String app80() {
        return "<application" +
               " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee" +
               " http://xmlns.jcp.org/xml/ns/javaee/application_8.xsd\"" +
               " version=\"8\"" +
               " id=\"Application_ID\"" +
               ">";
    }

    protected static String app90() {
        return "<application" +
               " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee" +
               " https://jakarta.ee/xml/ns/jakartaee/application_9.xsd\"" +
               " version=\"9\"" +
               " id=\"Application_ID\"" +
               ">";
    }

    protected static String appTail() {
        return "</application>";
    }
}
