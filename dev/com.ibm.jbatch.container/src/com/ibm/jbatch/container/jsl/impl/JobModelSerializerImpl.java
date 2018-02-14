/**
 * Copyright 2012 International Business Machines Corp.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.jbatch.container.jsl.impl;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

import com.ibm.jbatch.container.jsl.JSLValidationEventHandler;
import com.ibm.jbatch.container.jsl.ModelSerializer;
import com.ibm.jbatch.jsl.model.JSLJob;

public class JobModelSerializerImpl implements ModelSerializer<JSLJob> {

    @Override
    public String serializeModel(JSLJob model) {
        return marshalJSLJob(model);
    }

    @Override
    public String prettySerializeModel(JSLJob model) {
        String serializedModel = serializeModel(model);

        return formatXML(serializedModel);
    }

    private String marshalJSLJob(JSLJob job) {
        String resultXML = null;
        JSLValidationEventHandler handler = new JSLValidationEventHandler();
        try {
            ClassLoader currentClassLoader = AccessController.doPrivileged(
                                                                           new PrivilegedAction<ClassLoader>() {
                                                                               @Override
                                                                               public ClassLoader run() {
                                                                                   return JSLJob.class.getClassLoader();
                                                                               }
                                                                           });
            JAXBContext ctx = JAXBContext.newInstance("com.ibm.jbatch.jsl.model", currentClassLoader);
            Marshaller m = ctx.createMarshaller();
            m.setEventHandler(handler);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Let's invent a target NS for the purpose of this serialization, to be clear it's not part of the spec XSD.
            m.marshal(new JAXBElement(new QName("http://com.ibm.jbatch.model/serialization", "job"), JSLJob.class, job), baos);
            resultXML = baos.toString();
        } catch (Exception e) {
            throw new RuntimeException("Exception while marshalling JSLJob", e);
        }

        return resultXML;
    }

    private String formatXML(String input) {
        String returnString;
        try {
            final InputSource src = new InputSource(new StringReader(input));
            final Node document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src).getDocumentElement();
            final DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            final DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
            final LSSerializer writer = impl.createLSSerializer();
            writer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
            writer.getDomConfig().setParameter("xml-declaration", false); /* skip XML declare */
            returnString = writer.writeToString(document);
            return returnString;
        } catch (Exception e) {
            // Oh well, just return it as one line
            returnString = input;
        }
        return returnString;
    }

}
