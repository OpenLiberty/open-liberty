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
 * Example usage:
 *
 * <pre>
 * <code>
 * BeansAsset beans = new BeansAsset(Mode.ALL);
 *
 * WebArchive war = ShrinkWrap.create(WebArchive.class)
 *              .addPackage(MyClass.class.getPackage())
 *              .addAsResource(beans, "META-INF/beans.xml");
 * </code>
 * </pre>
 */
public class BeansAsset implements Asset {

    public static enum Mode {
        NONE, ALL, ANNOTATED
    };

    private static final String PRE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                      "\n" +
                                      "<beans xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"\n" +
                                      "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                      "xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/beans_1_1.xsd\" \n" +
                                      "bean-discovery-mode=\"";

    private static final String POST = "\"\n" +
                                       "       version=\"1.1\"> \n" +
                                       "</beans>";

    private Mode mode = Mode.ALL;

    public BeansAsset() {

    }

    public BeansAsset(Mode mode) {
        setMode(mode);
    }

    public BeansAsset setMode(Mode mode) {
        this.mode = mode;
        return this;
    }

    @Override
    public InputStream openStream() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(PRE.getBytes("UTF-8"));
            baos.write(mode.toString().toLowerCase().getBytes("UTF-8"));
            baos.write(POST.getBytes("UTF-8"));

            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Unable to create properties asset", e);
        }
    }

}
