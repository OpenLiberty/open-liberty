/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
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

import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class ValidationConfigTestBase {
    private final Mockery mockery = new Mockery();
    private int mockId;

    ValidationConfig parse(String xml) throws Exception {
        return parse(xml, false);
    }

    @SuppressWarnings("deprecation")
    ValidationConfig parse(String xml, boolean notBvalXml) throws Exception {
        OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        ArtifactEntry artifactEntry = mockery.mock(ArtifactEntry.class, "artifactContainer" + mockId++);

        NonPersistentCache nonPC = mockery.mock(NonPersistentCache.class, "nonPC" + mockId++);
        Container moduleRoot = mockery.mock(Container.class, "moduleRoot" + mockId++);
        Entry ddEntry = mockery.mock(Entry.class, "ddEntry" + mockId++);

        ModuleInfo moduleInfo = ( notBvalXml ? mockery.mock(ModuleInfo.class, "moduleInfo" + mockId++) : null );

        mockery.checking(new Expectations() {
            {
                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(ValidationConfig.class));
                will(returnValue(null));
                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(ValidationConfig.class), with(any(ValidationConfig.class)));

                allowing(artifactEntry).getPath();
                will(returnValue("META-INF/validation.xml"));

                allowing(nonPC).getFromCache(WebModuleInfo.class);
                will(returnValue(null));
                allowing(moduleRoot).adapt(NonPersistentCache.class);
                will(returnValue(nonPC));
                allowing(moduleRoot).getPhysicalPath();
                will(returnValue("/root/wlp/usr/servers/server1/apps/ejbJar.jar"));
                allowing(moduleRoot).adapt(Entry.class);
                will(returnValue(null));
                allowing(moduleRoot).getPath();
                will(returnValue("/"));

                allowing(ddEntry).getRoot();
                will(returnValue(moduleRoot));
                allowing(ddEntry).getPath();
                will(returnValue("META-INF/validation.xml"));
                allowing(ddEntry).adapt(InputStream.class);
                will(returnValue(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

                if (notBvalXml) {
                    allowing(nonPC).getFromCache(ModuleInfo.class);
                    will(returnValue(moduleInfo));
                    allowing(moduleInfo).getName();
                    will(returnValue("thisModule"));

                    allowing(moduleRoot).getName();
                    will(returnValue("testModule"));
                }
            }
        });

        ValidationConfigEntryAdapter adapter = new ValidationConfigEntryAdapter();
        return adapter.adapt(moduleRoot, rootOverlay, artifactEntry, ddEntry);
    }

    protected static String validationConfigNoVersion() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<validation-config" +
                   " xmlns=\"http://jboss.org/xml/ns/javax/validation/configuration\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://jboss.org/xml/ns/javax/validation/configuration validation-configuration-1.0.xsd\"" +
               ">";
    }

    protected static String validationConfig10() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<validation-config" +
                   " xmlns=\"http://jboss.org/xml/ns/javax/validation/configuration\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://jboss.org/xml/ns/javax/validation/configuration validation-configuration-1.1.xsd\"" +
                   " version=\"1.0\"" +
               ">";
    }

    protected static String validationConfig11() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<validation-config" +
                   " xmlns=\"http://jboss.org/xml/ns/javax/validation/configuration\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://jboss.org/xml/ns/javax/validation/configuration validation-configuration-1.1.xsd\"" +
                   " version=\"1.1\"" +
               ">";
    }

    protected static String notValidationConfig() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<not-validation-config>" +
               "</not-validation-config>";
    }

    //
    
    public static void verifyMessage(Exception e, String altMessage, String... requiredMessages) {
        DDTestBase.verifyMessage(e, altMessage, requiredMessages);
    }
}
