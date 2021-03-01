/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jboss.shrinkwrap.api.asset.Asset;

/**
 * Shrinkwrap asset for a beans.xml file
 * <p>
 * Example usage, either using CDIArchiveHelper...
 *
 * <pre>
 * <code>
 * WebArchive war = ShrinkWrap.create(WebArchive.class)
 *              .addPackage(MyClass.class.getPackage());
 *
 * war = CDIArchiveHelper.addBeansXML(war, Mode.ALL);
 * </code>
 * </pre>
 *
 * ...or directly...
 *
 * <pre>
 * <code>
 * BeansAsset beans = new BeansAsset(Mode.ALL);
 *
 * WebArchive war = ShrinkWrap.create(WebArchive.class)
 *              .addPackage(MyClass.class.getPackage())
 *              .addAsWebInfResource(beans, "beans.xml");
 * </code>
 * </pre>
 *
 * In future it would be better to make use of the ShrinkWrap Descriptors lib instead
 * https://github.com/OpenLiberty/open-liberty/issues/16057
 */
public class BeansAsset implements Asset {

    public static enum DiscoveryMode {
        NONE, ALL, ANNOTATED
    };

    public static enum CDIVersion {
        CDI11, CDI20, CDI30
    };

    private static final String NEW_LINE = "\n";

    private static final String BEANS_START = "<beans ";
    private static final String BEANS_END = "</beans>";

    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String XML_SCHEMA = "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";

    //Java EE namespace
    private static final String JAVAEE_NS = "xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"";
    //Jakarta EE namespace
    private static final String JAKARTA_NS = "xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"";

    //CDI 1.1 namespaces and versions
    private static final String BEANS11_SCHEMA = "xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/beans_1_1.xsd\"";
    private static final String BEANS11_VERSION = "version=\"1.1\"";
    //CDI 2.0 namespaces and versions
    private static final String BEANS20_SCHEMA = "xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/beans_2_0.xsd\"";
    private static final String BEANS20_VERSION = "version=\"2.0\"";
    //CDI 3.0 namespaces and versions
    private static final String BEANS30_SCHEMA = "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\"";
    private static final String BEANS30_VERSION = "version=\"3.0\"";

    private static final String BEAN_DISCOVERY_MODE = "bean-discovery-mode=\"";

    private static final String TRIM = "<trim/>";

    private static final String PRE = XML + NEW_LINE +
                                      BEANS_START + XML_SCHEMA + NEW_LINE;

    private static final String BEANS11 = PRE + JAVAEE_NS + NEW_LINE +
                                          BEANS11_SCHEMA + NEW_LINE +
                                          BEANS11_VERSION + NEW_LINE;

    private static final String BEANS20 = PRE + JAVAEE_NS + NEW_LINE +
                                          BEANS20_SCHEMA + NEW_LINE +
                                          BEANS20_VERSION + NEW_LINE;

    private static final String BEANS30 = PRE + JAKARTA_NS + NEW_LINE +
                                          BEANS30_SCHEMA + NEW_LINE +
                                          BEANS30_VERSION + NEW_LINE;

    private DiscoveryMode mode = DiscoveryMode.ALL;
    private CDIVersion version = CDIVersion.CDI11;
    private boolean trim = false;

    public BeansAsset() {

    }

    public BeansAsset(DiscoveryMode mode) {
        this(mode, CDIVersion.CDI11);
    }

    public BeansAsset(DiscoveryMode mode, CDIVersion version) {
        this.mode = mode;
        this.version = version;
    }

    public BeansAsset setVersion(CDIVersion version) {
        this.version = version;
        return this;
    }

    public BeansAsset setMode(DiscoveryMode mode) {
        this.mode = mode;
        return this;
    }

    public BeansAsset setTrim(boolean trim) {
        this.trim = trim;
        return this;
    }

    @Override
    public InputStream openStream() {

        StringBuilder output = new StringBuilder();
        if (this.version == CDIVersion.CDI11) {
            output.append(BEANS11);
        } else if (this.version == CDIVersion.CDI20) {
            output.append(BEANS20);
        } else {
            output.append(BEANS30);
        }
        output.append(BEAN_DISCOVERY_MODE);
        output.append(mode.toString().toLowerCase());
        output.append("\">");
        output.append(NEW_LINE);

        if (this.version != CDIVersion.CDI11 && trim) {
            output.append(TRIM);
            output.append(NEW_LINE);
        }
        output.append(BEANS_END);

        try {
            return new ByteArrayInputStream(output.toString().getBytes("UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException("Unable to create properties asset", e);
        }
    }

}
