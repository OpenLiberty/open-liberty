/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.osgi.internal;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jpa.management.JPAApplInfo;
import com.ibm.ws.jpa.management.JPAPXml;
import com.ibm.ws.jpa.management.JPAPuScope;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

@Trivial
public class OSGiJPAPXml extends JPAPXml {
    private static final TraceComponent tc = Tr.register(OSGiJPAPXml.class);

    private final Entry ivPxml;

    /**
     * @param pxmlUrl
     */
    OSGiJPAPXml(JPAApplInfo applInfo, String archiveName, JPAPuScope puScope, URL puRoot, ClassLoader classloader, Entry pxml) {
        super(applInfo, archiveName, puScope, puRoot, classloader);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init> : " + pxml);
        ivPxml = pxml;
    }

    /** {@inheritDoc} */
    @Override
    protected InputStream openStream() throws IOException {
        try {
            return ivPxml.adapt(InputStream.class);
        } catch (UnableToAdaptException ex) {
            throw new IOException(ivPxml.toString(), ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Schema newSchema(String xsdName) throws SAXException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);

        // Obtain the xsd file from the jpa container bundle
        final boolean isJavaEE = (xsdName != null && (xsdName.endsWith("1_0.xsd") || xsdName.endsWith("2_0.xsd")
                                                      || xsdName.endsWith("2_1.xsd") || xsdName.endsWith("2_2.xsd")));
        String resName = "com/ibm/ws/jpa/schemas/" + ((isJavaEE) ? "javaee/" : "jakartaee/") + xsdName;
        URL xsdUrl = JPAPXml.class.getClassLoader().getResource(resName);
        if (xsdUrl == null) {
            throw new RuntimeException(resName + " not found");
        }

        Schema schema = schemaFactory.newSchema(xsdUrl);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "newSchema : " + Util.identity(schema));

        return schema;
    }

    /**
     * Returns the parent of the container that holds the persistence.xml (META-INF)
     */
    Container getPuRootContainer() {
        return ivPxml.getEnclosingContainer();
    }
}
