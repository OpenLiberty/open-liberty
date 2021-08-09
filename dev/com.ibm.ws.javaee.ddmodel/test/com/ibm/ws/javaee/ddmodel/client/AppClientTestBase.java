/*******************************************************************************
 * Copyright (c) 2018,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.jmock.Expectations;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class AppClientTestBase extends DDTestBase {

    protected ApplicationClient parse(String xml) throws Exception {
        return parse(xml, ApplicationClient.VERSION_6);
    }

    protected ApplicationClient parse(final String xml, final int maxVersion) throws Exception {
        ApplicationClientEntryAdapter adapter = new ApplicationClientEntryAdapter();
        final Container root = mockery.mock(Container.class, "root" + mockId++);
        final OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        final ArtifactEntry artifactEntry = mockery.mock(ArtifactEntry.class, "artifactEntry" + mockId++);
        final Entry entry = mockery.mock(Entry.class, "entry" + mockId++);
        @SuppressWarnings("unchecked")
        final ServiceReference<ApplicationClientDDParserVersion> versionRef =
            mockery.mock(ServiceReference.class, "sr" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(artifactEntry).getPath();
                will(returnValue('/' + ApplicationClient.DD_NAME));

                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(any(Class.class)));
                will(returnValue(null));

                allowing(entry).getPath();
                will(returnValue('/' + ApplicationClient.DD_NAME));

                allowing(entry).adapt(InputStream.class);
                will(returnValue(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(any(Class.class)), with(any(Object.class)));

                allowing(versionRef).getProperty(ApplicationClientDDParserVersion.VERSION);
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

    // 1.2
    // 1.3
    // 1.4, http://java.sun.com/xml/ns/j2ee
    // 5,   http://java.sun.com/xml/ns/javaee
    // 6,   http://java.sun.com/xml/ns/javaee
    // 7,   http://xmlns.jcp.org/xml/ns/javaee
    // 8,   http://xmlns.jcp.org/xml/ns/javaee

    protected static String appClient12() {
        return "<!DOCTYPE application-client PUBLIC" +
               " \"-//Sun Microsystems, Inc.//DTD J2EE Application Client 1.2//EN\"" + 
               " \"http://java.sun.com/j2ee/dtds/application_client_1_2.dtd\">" +
               "<application-client>";
    }

    protected static String appClient13() {
        return "<!DOCTYPE application-client PUBLIC" +
                " \"-//Sun Microsystems, Inc.//DTD J2EE Application Client 1.3//EN\"" + 
                " \"http://java.sun.com/j2ee/dtds/application_client_1_3.dtd\">" +
                "<application-client>";
    }

    protected static String appClient14() {
        return "<application-client" +
               " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
               " http://java.sun.com/xml/ns/javaee/application-client_1_4.xsd\"" +
               " version=\"1.4\"" +
               " id=\"ApplicationClient_ID\"" +
               ">";
    }

    protected static String appClient50() {
        return "<application-client" +
                " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                  " http://java.sun.com/xml/ns/javaee/application-client_5.xsd\"" +
                " version=\"5\"" +
                " id=\"ApplicationClient_ID\"" +
                ">";
    }

    protected static String appClient60() {
        return "<application-client" +
                " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                  " http://java.sun.com/xml/ns/javaee/application-client_6.xsd\"" +
                " version=\"6\"" +
                " id=\"ApplicationClient_ID\"" +
                ">";
    }

    protected static String appClient70() {
        return "<application-client" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee" +
                  " http://xmlns.jcp.org/xml/ns/javaee/application-client_7.xsd\"" +
                " version=\"7\"" +
                " id=\"ApplicationClient_ID\"" +
                ">";
    }    

    protected static String appClient80() {
        return "<application-client" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee" +
                  " http://xmlns.jcp.org/xml/ns/javaee/application-client_8.xsd\"" +
                " version=\"8\"" +
                " id=\"ApplicationClient_ID\"" +
                ">";
    }

    protected static String appClient90() {
        return "<application-client" +
                " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee " +
                  " https://jakarta.ee/xml/ns/jakartaee/application-client_9.xsd\"" +
                " version=\"9\"" +
                " id=\"ApplicationClient_ID\"" +
                ">";
    }

    protected static String appClientTail() {
        return "</application-client>";
   }
}
