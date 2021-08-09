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
package com.ibm.ws.javaee.ddmodel.bval;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import test.common.SharedOutputManager;

import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class ValidationConfigTestBase {

    protected static SharedOutputManager outputMgr;

    private final Mockery mockery = new Mockery();
    private int mockId;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    ValidationConfig parse(final String xml) throws Exception {
        return parse(xml, false);
    }

    ValidationConfig parse(final String xml, final boolean notBvalXml) throws Exception {
        ValidationConfigEntryAdapter adapter = new ValidationConfigEntryAdapter();
        final Container root = mockery.mock(Container.class, "root" + mockId++);
        final Entry entry = mockery.mock(Entry.class, "entry" + mockId++);
        final OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        final ArtifactEntry artifactEntry = mockery.mock(ArtifactEntry.class, "artifactContainer" + mockId++);
        final NonPersistentCache nonPC = mockery.mock(NonPersistentCache.class, "nonPC" + mockId++);
        final ModuleInfo moduleInfo = mockery.mock(ModuleInfo.class, "moduleInfo" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(artifactEntry).getPath();
                will(returnValue("META-INF/validation.xml"));

                allowing(root).adapt(NonPersistentCache.class);
                will(returnValue(nonPC));
                allowing(nonPC).getFromCache(WebModuleInfo.class);
                will(returnValue(null));

                allowing(entry).getPath();
                will(returnValue("META-INF/validation.xml"));

                allowing(entry).adapt(InputStream.class);
                will(returnValue(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(ValidationConfig.class));
                will(returnValue(null));
                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(ValidationConfig.class), with(any(ValidationConfig.class)));

                if (notBvalXml) {
                    allowing(root).getName();
                    will(returnValue("testModule"));

                    allowing(nonPC).getFromCache(ModuleInfo.class);
                    will(returnValue(moduleInfo));

                    allowing(moduleInfo).getName();
                    will(returnValue("thisModule"));
                }
            }
        });

        return adapter.adapt(root, rootOverlay, artifactEntry, entry);
    }

    static final String validationConfig() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               " <validation-config" +
               " xmlns=\"http://jboss.org/xml/ns/javax/validation/configuration\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://jboss.org/xml/ns/javax/validation/configuration validation-configuration-1.0.xsd\"" +
               ">";
    }

    static final String validationConfig10() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               " <validation-config" +
               " xmlns=\"http://jboss.org/xml/ns/javax/validation/configuration\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://jboss.org/xml/ns/javax/validation/configuration validation-configuration-1.1.xsd\"" +
               " version=\"1.0\"" +
               ">";
    }

    static final String validationConfig11() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               " <validation-config" +
               " xmlns=\"http://jboss.org/xml/ns/javax/validation/configuration\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://jboss.org/xml/ns/javax/validation/configuration validation-configuration-1.1.xsd\"" +
               " version=\"1.1\"" +
               ">";
    }

    static final String notValidationConfig() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               " <not-validation-config><not-validation-config>";
    }
}
