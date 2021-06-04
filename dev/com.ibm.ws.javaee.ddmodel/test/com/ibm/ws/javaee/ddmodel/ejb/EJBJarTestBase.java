/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ejb;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 * test the ejb-jar.xml parser
 *
 * -concentrate on the pristine path where the ejb-jar.xml file is well formed
 * -testing entity and relationships is optional
 * -testing error handling is secondary
 *
 * -Error handling philosophy:
 *
 * As determined by Glann marcy and Brett Kail, the easiest thing to do at
 * this point is to change the parser to return a "sensible default",
 * but if not possible, unwind by discarding objects until something
 * valid is returned (e.g., EJBRelation.getRelationshipRoles() should be
 * discarded if only one <ejb-relation/> is specified).
 *
 * If we can match the defaults used by WCCM (by looking at
 * WCCMBASE/ws/code/jst.j2ee.core.mofj2ee), that would be ideal.
 */

public class EJBJarTestBase {
    private static final Mockery mockery = new Mockery();
    private static volatile int mockId;

    protected static EJBJar parse(String xml, int maxVersion) throws Exception {
        EJBJarEntryAdapter adapter = new EJBJarEntryAdapter();
        @SuppressWarnings("unchecked")
        ServiceReference<EJBJarDDParserVersion> versionRef = mockery.mock(ServiceReference.class, "sr" + mockId++);
        Container root = mockery.mock(Container.class, "root" + mockId++);
        Entry entry = mockery.mock(Entry.class, "entry" + mockId++);
        OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        ArtifactEntry artifactEntry = mockery.mock(ArtifactEntry.class, "artifactContainer" + mockId++);
        NonPersistentCache nonPC = mockery.mock(NonPersistentCache.class, "nonPC" + mockId++);

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
                will(returnValue(maxVersion));
            }
        });

        adapter.setVersion(versionRef);

        try {
            return adapter.adapt(root, rootOverlay, artifactEntry, entry);

        } catch ( UnableToAdaptException e ) {
            Throwable cause = e.getCause();
            throw ( (cause instanceof Exception) ? (Exception) cause : e );
        }
    }

    //

    protected static String ejbJar11Head() {
        return "<!DOCTYPE ejb-jar PUBLIC" +
               " \"-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN\"" +
               " \"http://java.sun.com/j2ee/dtds/ejb-jar_1_1.dtd\">" +
               "<ejb-jar>";
    }

    protected static String ejbJar20Head() {
        return "<!DOCTYPE ejb-jar PUBLIC \"-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 2.0//EN\"" +
               " \"http://java.sun.com/dtd/ejb-jar_2_0.dtd\">" +
               "<ejb-jar>";
    }

    protected static String ejbJar21Head() {
        return "<ejb-jar" +
               " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
               " version=\"2.1\"" +
               ">";
    }

    protected static String ejbJar30Head(String attrs) {
        return "<ejb-jar" +
               " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd\"" +
               " version=\"3.0\"" +
               " " + attrs +
               ">";
    }

    protected static String ejbJar31Head(String attrs) {
        return "<ejb-jar" +
               " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_1.xsd\"" +
               " version=\"3.1\"" +
               " " + attrs +
               ">";
    }

    protected static String ejbJar32Head(String attrs) {
        return "<ejb-jar" +
               " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/ejb-jar_3_2.xsd\"" +
               " version=\"3.2\"" +
               " " + attrs +
               ">";
    }

    protected static String ejbJar40Head(String attrs) {
        return "<ejb-jar" +
               " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/ejb-jar_4_0.xsd\"" +
               " version=\"4.0\"" +
               " " + attrs +
               ">";
    }
    
    protected static String ejbJarTail() {
        return "</ejb-jar>";
    }
    
    //
    
    protected static String ejbJar11(String text) {
        return ejbJar11Head() + text + ejbJarTail();
    }

    protected static String ejbJar20(String text) {
        return ejbJar20Head() + text + ejbJarTail();        
    }
    
    protected static String ejbJar21(String text) {
        return ejbJar21Head() + text + ejbJarTail();        
    }

    protected static String ejbJar30(String attrs, String text) {
        return ejbJar30Head(attrs) + text + ejbJarTail();        
    }

    protected static String ejbJar31(String attrs, String text) {
        return ejbJar31Head(attrs) + text + ejbJarTail();
    }

    protected static String ejbJar32(String attrs, String text) {
        return ejbJar32Head(attrs) + text + ejbJarTail();
    }
    
    protected static String ejbJar40(String attrs, String text) {
        return ejbJar40Head(attrs) + text + ejbJarTail();
    }

    protected static String ejbJar(int version, String attrs, String text) {
        String head;

        if ( version == EJBJar.VERSION_1_1 ) {
            head = ejbJar11Head();
        } else if ( version == EJBJar.VERSION_2_0 ) {
            head = ejbJar20Head();
        } else if ( version == EJBJar.VERSION_2_1 ) {
            head = ejbJar21Head();
        } else if ( version == EJBJar.VERSION_3_0 ) {
            head = ejbJar30Head(attrs);
        } else if ( version == EJBJar.VERSION_3_1 ) {
            head = ejbJar31Head(attrs);
        } else if ( version == EJBJar.VERSION_3_2 ) {
            head = ejbJar32Head(attrs);
        } else if ( version == EJBJar.VERSION_4_0 ) {
            head = ejbJar40Head(attrs);
        } else {
            throw new IllegalArgumentException("Unknown EJBJar version [ " + version + " ]");
        }
        
        return head + text + ejbJarTail();
    }
}
