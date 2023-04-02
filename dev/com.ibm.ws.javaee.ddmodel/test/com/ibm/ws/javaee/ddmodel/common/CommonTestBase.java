/*******************************************************************************
 * Copyright (c) 2014,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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

public class CommonTestBase {
    private final Mockery mockery = new Mockery();
    private int mockId;

    @SuppressWarnings("deprecation")
    private EJBJar parseEJBJar(String xml) throws Exception {
        OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        ArtifactEntry artifactEntry = mockery.mock(ArtifactEntry.class, "artifactEntry" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(EJBJar.class));
                will(returnValue(null));
                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(EJBJar.class), with(any(EJBJar.class)));
                
                allowing(artifactEntry).getPath();
                will(returnValue("META-INF/ejb-jar.xml"));
            }
        });
        
        NonPersistentCache nonPC = mockery.mock(NonPersistentCache.class, "nonPC" + mockId++);
        Container moduleRoot = mockery.mock(Container.class, "moduleRoot" + mockId++);
        Entry ddEntry = mockery.mock(Entry.class, "ddEntry" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(nonPC).getFromCache(WebModuleInfo.class);
                will(returnValue(null));

                allowing(moduleRoot).adapt(NonPersistentCache.class);
                will(returnValue(nonPC));
                
                allowing(moduleRoot).adapt(Entry.class);
                will(returnValue(null));
                allowing(moduleRoot).getPhysicalPath();
                will(returnValue("/root/wlp/usr/servers/server1/apps/ejbJar.jar"));                
                allowing(moduleRoot).getPath();
                will(returnValue("/"));

                allowing(ddEntry).getRoot();
                will(returnValue(moduleRoot));
                allowing(ddEntry).getPath();
                will(returnValue("/META-INF/ejb-jar.xml"));

                allowing(ddEntry).adapt(InputStream.class);
                will(returnValue(new ByteArrayInputStream(xml.getBytes("UTF-8"))));
            }
        });
        
        @SuppressWarnings("unchecked")
        ServiceReference<EJBJarDDParserVersion> versionRef = mockery.mock(ServiceReference.class, "sr" + mockId++);
        
        mockery.checking(new Expectations() {
            {        
                allowing(versionRef).getProperty(EJBJarDDParserVersion.VERSION);
                will(returnValue(EJBJar.VERSION_3_2));
            }
        });

        EJBJarEntryAdapter adapter = new EJBJarEntryAdapter();
        adapter.setVersion(versionRef);

        try {
            return adapter.adapt(moduleRoot, rootOverlay, artifactEntry, ddEntry);
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
                         " version=\"3.1\">" +
                         "<enterprise-beans>" +
                             "<session>" +
                                 refString +
                             "</session>" +
                         "</enterprise-beans>" +
                     "</ejb-jar>";
        return parseEJBJar(xml).getEnterpriseBeans().get(0);
    }

    protected com.ibm.ws.javaee.dd.common.JNDIEnvironmentRefs parse(String refString) throws Exception {
        String xml = "<ejb-jar" +
                         " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                         " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                         " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/ejb-jar_3_2.xsd\"" +
                         " version=\"3.2\">" +
                         "<enterprise-beans>" +
                             "<session>" +
                                 refString +
                             "</session>" +
                         "</enterprise-beans>" +
                     "</ejb-jar>";
        
        return parseEJBJar(xml).getEnterpriseBeans().get(0);
    }
}
