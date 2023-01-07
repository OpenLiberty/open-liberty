/*******************************************************************************
 * Copyright (c) 2019,2021 IBM Corporation and others.
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
package com.ibm.ws.wsat.common.impl;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Assorted debug helpers
 */
@Trivial
public class DebugUtils {

    // Convert an EPR to readable XML
    public static String printEPR(EndpointReferenceType epr) {
        return printXML(EndpointReferenceUtils.convertToXML(epr));
    }

    private static String printXML(Source source) {
        String xml = null;
        try {
            StringWriter out = new StringWriter();
            getTransformer().transform(source, new StreamResult(out));
            xml = out.toString();
        } catch (Exception e) {
            xml = e.getMessage();
        }
        return xml;
    }

    private static Transformer getTransformer() throws TransformerConfigurationException, TransformerFactoryConfigurationError {
        Transformer xf = TransformerFactory.newInstance().newTransformer();
        xf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        xf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        xf.setOutputProperty(OutputKeys.INDENT, "yes");
        xf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        return xf;
    }
}
