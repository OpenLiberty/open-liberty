/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ejbext;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.jmock.Expectations;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejbext.EJBJarExt;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.ws.javaee.ddmodel.ejb.EJBJarDDParserVersion;
import com.ibm.ws.javaee.ddmodel.ejb.EJBJarEntryAdapter;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 * test the ejb-jar-ext.xml parser
 * -concentrate on the pristine path where the ejb-jarext.xml file is well formed
 * -testing entity and relationships is optional
 * -testing error handling is secondary
 * 
 */

public class EJBJarExtTestBase extends DDTestBase {
    protected boolean isWarModule = false;

    private EJBJarExt parse(final String xml, String path, EJBJar ejbJar) throws Exception {
        final WebModuleInfo moduleInfo = isWarModule ? mockery.mock(WebModuleInfo.class, "webModuleInfo" + mockId++) : null;
        return parse(xml, new EJBJarExtAdapter(), path, EJBJar.class, ejbJar, WebModuleInfo.class, moduleInfo);
    }

    EJBJarExt parse(final String xml) throws Exception {
        return parse(xml, isWarModule ? EJBJarExtAdapter.XML_EXT_IN_WEB_MOD_NAME : EJBJarExtAdapter.XML_EXT_IN_EJB_MOD_NAME, null);
    }

    public EJBJarExt getEJBJarExt(String jarString) throws Exception {
        return parse(jarString);
    }

    public EJBJarExt parseEJBJarExtension(final String xml, EJBJar ejbJar) throws Exception {
        return parse(xml, isWarModule ? EJBJarExtAdapter.XMI_EXT_IN_WEB_MOD_NAME : EJBJarExtAdapter.XMI_EXT_IN_EJB_MOD_NAME, ejbJar);
    }

    public EJBJar parseEJBJar(String xml) throws Exception {
        return parseEJBJar(xml, EJBJar.VERSION_3_2);
    }

    protected String getEJBJarPath() {
        return isWarModule ? "WEB-INF/ejb-jar.xml" : "META-INF/ejb-jar.xml";
    }

    public EJBJar parseEJBJar(final String xml, final int maxVersion) throws Exception {
        EJBJarEntryAdapter adapter = new EJBJarEntryAdapter();
        @SuppressWarnings("unchecked")
        final ServiceReference<EJBJarDDParserVersion> versionRef = mockery.mock(ServiceReference.class, "sr" + mockId++);
        final Container root = mockery.mock(Container.class, "root" + mockId++);
        final Entry entry = mockery.mock(Entry.class, "entry" + mockId++);
        final OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        final ArtifactEntry artifactEntry = mockery.mock(ArtifactEntry.class, "artifactContainer" + mockId++);
        final NonPersistentCache nonPC = mockery.mock(NonPersistentCache.class, "nonPC" + mockId++);
        final WebModuleInfo moduleInfo = isWarModule ? mockery.mock(WebModuleInfo.class, "webModuleInfo" + mockId++) : null;

        mockery.checking(new Expectations() {
            {
                allowing(artifactEntry).getPath();
                will(returnValue('/' + getEJBJarPath()));

                allowing(root).adapt(NonPersistentCache.class);
                will(returnValue(nonPC));
                allowing(nonPC).getFromCache(WebModuleInfo.class);
                will(returnValue(moduleInfo));

                allowing(entry).getPath();
                will(returnValue('/' + getEJBJarPath()));

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
        } catch (UnableToAdaptException e) {
            Throwable cause = e.getCause();
            throw cause instanceof Exception ? (Exception) cause : e;
        }
    }

    static final String ejbJar21() {
        return "<ejb-jar" +
               " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
               " version=\"2.1\"" +
               " id=\"EJBJar_ID\"" +
               ">";
    }

    static final String ejbJarExtension(String attrs) {
        return "<ejbext:EJBJarExtension" +
               " xmlns:ejbext=\"ejbext.xmi\"" +
               " xmlns:xmi=\"http://www.omg.org/XMI\"" +
               " xmlns:ejb=\"ejb.xmi\"" +
               " xmi:version=\"2.0\"" +
               " " + attrs +
               ">" +
               "<ejbJar href=\"META-INF/ejb-jar.xml#EJBJar_ID\"/>";
    }

    static final String ejbJarExt10() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               " <ejb-jar-ext" +
               " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_0.xsd\"" +
               " version=\"1.0\"" +
               ">";
    }

    static final String ejbJarExt11() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               " <ejb-jar-ext" +
               " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_1.xsd\"" +
               " version=\"1.1\"" +
               ">";
    }
}
