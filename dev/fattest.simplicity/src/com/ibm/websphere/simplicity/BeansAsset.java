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
import java.io.ByteArrayOutputStream;
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
 * TODO make use of the ShrinkWrap Descriptors lib instead
 */
public class BeansAsset implements Asset {

    public static enum Mode {
        NONE, ALL, ANNOTATED
    };

    public static enum Version {
        CDI11, CDI30
    };

    private static final String NEW_LINE = "\n";

    private static final String BEANS_START = "<beans ";
    private static final String BEANS_END = ">\n</beans>";

    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String XML_SCHEMA = "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";

    //Java EE (CDI 1.1) namespaces and versions
    private static final String JAVAEE_NS = "xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"";
    private static final String JAVAEE_SCHEMA = "xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/beans_1_1.xsd\"";
    private static final String JAVAEE_VERSION = "version=\"1.1\"";

    //Jakarta EE (CDI 3.0) namespaces and versions
    private static final String JAKARTA_NS = "xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"";
    private static final String JAKARTA_SCHEMA = "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\"";
    private static final String JAKARTA_VERSION = "version=\"3.0\"";

    private static final String MODE = "[[MODE]]";
    private static final String BEAN_DISCOVERY_MODE = "bean-discovery-mode=\"" + MODE + "\"";

    private static final String PRE = XML + NEW_LINE +
                                      BEANS_START + XML_SCHEMA + NEW_LINE;

    private static final String BEANS11 = PRE + JAVAEE_NS + NEW_LINE +
                                          JAVAEE_SCHEMA + NEW_LINE +
                                          JAVAEE_VERSION + NEW_LINE +
                                          BEAN_DISCOVERY_MODE + BEANS_END;

    private static final String BEANS30 = PRE + JAKARTA_NS + NEW_LINE +
                                          JAKARTA_SCHEMA + NEW_LINE +
                                          JAKARTA_VERSION + NEW_LINE +
                                          BEAN_DISCOVERY_MODE + BEANS_END;

    private Mode mode = Mode.ALL;
    private Version version = Version.CDI11;

    public BeansAsset() {

    }

    public BeansAsset(Mode mode) {
        this(mode, Version.CDI11);
    }

    public BeansAsset(Mode mode, Version version) {
        this.mode = mode;
        this.version = version;
    }

    public BeansAsset setVersion(Version version) {
        this.version = version;
        return this;
    }

    public BeansAsset setMode(Mode mode) {
        this.mode = mode;
        return this;
    }

    @Override
    public InputStream openStream() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (this.version == Version.CDI11) {
                baos.write(BEANS11.replace(MODE, this.mode.toString().toLowerCase()).getBytes("UTF-8"));
            } else {
                baos.write(BEANS30.replace(MODE, this.mode.toString().toLowerCase()).getBytes("UTF-8"));
            }

            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Unable to create properties asset", e);
        }
    }

}
