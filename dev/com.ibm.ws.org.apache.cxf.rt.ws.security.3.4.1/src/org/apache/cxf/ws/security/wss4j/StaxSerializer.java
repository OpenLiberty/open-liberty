/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.ws.security.wss4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.SequenceInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.xml.namespace.NamespaceContext;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;

import org.apache.cxf.binding.soap.saaj.SAAJStreamWriter;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.staxutils.OverlayW3CDOMStreamWriter;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.apache.xml.security.encryption.AbstractSerializer;
import org.apache.xml.security.encryption.XMLEncryptionException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Converts <code>String</code>s into <code>Node</code>s and visa versa using CXF's StaxUtils
 */
//No Liberty Change, but needed to recompile due to Liberty change in MessageImpl.
public class StaxSerializer extends AbstractSerializer {
    private XMLInputFactory factory;
    private boolean validFactory;

    public StaxSerializer() throws InvalidCanonicalizerException {
        super(Canonicalizer.ALGO_ID_C14N_PHYSICAL, true);
    }

    /**
     * @param source
     * @param ctx
     * @return the Node resulting from the parse of the source
     * @throws XMLEncryptionException
     */
    @Override
    public Node deserialize(byte[] source, Node ctx) throws XMLEncryptionException {
        XMLStreamReader reader = createWstxReader(source, ctx);
        if (reader != null) {
            return deserialize(ctx, reader, false);
        }
        return deserialize(ctx, new InputSource(createStreamContext(source, ctx)));
    }

    @Override
    public byte[] serializeToByteArray(Element element) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(baos);
            StaxUtils.copy(element, writer);
            writer.close();
            return baos.toByteArray();
        }
    }

    private boolean addNamespaces(XMLStreamReader reader, Node ctx) {
        try {
            NamespaceContext nsctx = reader.getNamespaceContext();
            if (nsctx instanceof com.ctc.wstx.sr.InputElementStack) {
                com.ctc.wstx.sr.InputElementStack ies = (com.ctc.wstx.sr.InputElementStack)nsctx;
                com.ctc.wstx.util.InternCache ic = com.ctc.wstx.util.InternCache.getInstance();

                Map<String, String> storedNamespaces = new HashMap<>();
                Node wk = ctx;
                while (wk != null) {
                    NamedNodeMap atts = wk.getAttributes();
                    if (atts != null) {
                        for (int i = 0; i < atts.getLength(); ++i) {
                            Node att = atts.item(i);
                            String nodeName = att.getNodeName();
                            if (("xmlns".equals(nodeName) || nodeName.startsWith("xmlns:"))
                                && !storedNamespaces.containsKey(att.getNodeName())) {

                                String prefix = att.getLocalName();
                                if ("xmlns".equals(prefix)) {
                                    prefix = "";
                                }
                                prefix = ic.intern(prefix);
                                ies.addNsBinding(prefix, att.getNodeValue());
                                storedNamespaces.put(nodeName, att.getNodeValue());
                            }
                        }
                    }
                    wk = wk.getParentNode();
                }
            }
            return true;
        } catch (Throwable t) {
            //ignore, not much we can do but hope the decrypted XML is stand alone ok
        }
        return false;
    }

    private XMLStreamReader createWstxReader(byte[] source, Node ctx) throws XMLEncryptionException {
        try {
            if (factory == null) {
                factory = StaxUtils.createXMLInputFactory(true);
                try {
                    factory.setProperty("com.ctc.wstx.fragmentMode",
                                        com.ctc.wstx.api.WstxInputProperties.PARSING_MODE_FRAGMENT);
                    factory.setProperty(org.codehaus.stax2.XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE, Boolean.TRUE);
                    validFactory = true;
                } catch (Throwable t) {
                    //ignore
                    validFactory = false;
                }
            }
            if (validFactory) {
                XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(source));
                if (addNamespaces(reader, ctx)) {
                    return reader;
                }
            }
        } catch (Throwable e) {
            //ignore
        }
        return null;
    }

    private InputStream createStreamContext(byte[] source, Node ctx) throws XMLEncryptionException {
        Vector<InputStream> v = new Vector<>(2); //NOPMD

        LoadingByteArrayOutputStream byteArrayOutputStream = new LoadingByteArrayOutputStream();
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(byteArrayOutputStream, UTF_8);
            outputStreamWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><dummy");

            // Run through each node up to the document node and find any xmlns: nodes
            Map<String, String> storedNamespaces = new HashMap<>();
            Node wk = ctx;
            while (wk != null) {
                NamedNodeMap atts = wk.getAttributes();
                if (atts != null) {
                    for (int i = 0; i < atts.getLength(); ++i) {
                        Node att = atts.item(i);
                        String nodeName = att.getNodeName();
                        if (("xmlns".equals(nodeName) || nodeName.startsWith("xmlns:"))
                                && !storedNamespaces.containsKey(att.getNodeName())) {
                            outputStreamWriter.write(" ");
                            outputStreamWriter.write(nodeName);
                            outputStreamWriter.write("=\"");
                            outputStreamWriter.write(att.getNodeValue());
                            outputStreamWriter.write("\"");
                            storedNamespaces.put(nodeName, att.getNodeValue());
                        }
                    }
                }
                wk = wk.getParentNode();
            }
            outputStreamWriter.write(">");
            outputStreamWriter.close();
            v.add(byteArrayOutputStream.createInputStream());
            v.addElement(new ByteArrayInputStream(source));
            byteArrayOutputStream = new LoadingByteArrayOutputStream();
            outputStreamWriter = new OutputStreamWriter(byteArrayOutputStream, UTF_8);
            outputStreamWriter.write("</dummy>");
            outputStreamWriter.close();
            v.add(byteArrayOutputStream.createInputStream());
        } catch (IOException e) {
            throw new XMLEncryptionException(e);
        }
        return new SequenceInputStream(v.elements());
    }

    /**
     * @param ctx
     * @param inputSource
     * @return the Node resulting from the parse of the source
     * @throws XMLEncryptionException
     */
    private Node deserialize(Node ctx, InputSource inputSource) throws XMLEncryptionException {
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(inputSource);
        return deserialize(ctx, reader, true);
    }

    private Node deserialize(Node ctx, XMLStreamReader reader, boolean wrapped) throws XMLEncryptionException {
        Document contextDocument = null;
        if (Node.DOCUMENT_NODE == ctx.getNodeType()) {
            contextDocument = (Document)ctx;
        } else {
            contextDocument = ctx.getOwnerDocument();
        }

        XMLStreamWriter writer = null;
        try {
            if (ctx instanceof SOAPElement) {
                SOAPElement el = (SOAPElement)ctx;
                while (el != null && !(el instanceof SOAPEnvelope)) {
                    el = el.getParentElement();
                }
                //cannot load into fragment due to a ClassCastException within SAAJ addChildElement
                //which only checks for Document as parent, not DocumentFragment
                Element element = ctx.getOwnerDocument().createElementNS("dummy", "dummy");
                writer = new SAAJStreamWriter((SOAPEnvelope)el, element);
                return appendNewChild(reader, wrapped, contextDocument, writer, element);
            }
            if (DOMUtils.isJava9SAAJ()) {
                //cannot load into fragment due to a ClassCastException within SAAJ addChildElement
                //which only checks for Document as parent, not DocumentFragment
                Element element = ctx.getOwnerDocument().createElementNS("dummy", "dummy");
                writer = new OverlayW3CDOMStreamWriter(ctx.getOwnerDocument(), element);
                return appendNewChild(reader, wrapped, contextDocument, writer, element);
            }
            // Import to a dummy fragment
            DocumentFragment dummyFragment = contextDocument.createDocumentFragment();
            writer = StaxUtils.createXMLStreamWriter(new DOMResult(dummyFragment));
            StaxUtils.copy(reader, writer);

            // Remove the "dummy" wrapper

            if (wrapped) {
                DocumentFragment result = contextDocument.createDocumentFragment();
                Node child = dummyFragment.getFirstChild().getFirstChild();
                if (child != null && child.getNextSibling() == null) {
                    return child;
                }
                while (child != null) {
                    Node nextChild = child.getNextSibling();
                    result.appendChild(child);
                    child = nextChild;
                }
                dummyFragment = result;
            }
            return dummyFragment;
        } catch (XMLStreamException ex) {
            throw new XMLEncryptionException(ex);
        }
    }

    private Node appendNewChild(XMLStreamReader reader, boolean wrapped, Document contextDocument,
                                XMLStreamWriter writer, Element element) throws XMLStreamException {
        StaxUtils.copy(reader, writer);

        DocumentFragment result = contextDocument.createDocumentFragment();
        Node child = element.getFirstChild();
        if (wrapped) {
            child = child.getFirstChild();
        }
        if (child != null && child.getNextSibling() == null) {
            return child;
        }
        while (child != null) {
            Node nextChild = child.getNextSibling();
            result.appendChild(child);
            child = nextChild;
        }

        return result;
    }

}
