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
package org.apache.cxf.databinding.source;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;

import com.ibm.websphere.ras.annotation.Trivial;

public class XMLStreamDataWriter implements DataWriter<XMLStreamWriter> {

    private static final Logger LOG = LogUtils.getL7dLogger(XMLStreamDataWriter.class);


    private Schema schema;

    public XMLStreamDataWriter() {

    }

    @Trivial  // Liberty change: line is added
    public void write(Object obj, MessagePartInfo part, XMLStreamWriter output) {
        LOG.entering("XMLStreamDataWriter", "write");   // Liberty change: line is added
        write(obj, output);
        LOG.exiting("XMLStreamDataWriter", "write");   // Liberty change: line is added
    }

    @Trivial  // Liberty change: line is added
    public void write(Object obj, XMLStreamWriter writer) {
        LOG.entering("XMLStreamDataWriter", "write");   // Liberty change: line is added
        try {
            if (obj instanceof DataSource) {
                DataSource ds = (DataSource)obj;
                if (schema != null) {
                    DOMSource domSource = new DOMSource(StaxUtils.read(ds.getInputStream()));
                    Validator schemaValidator = schema.newValidator();
                    schemaValidator.setErrorHandler(
                        new MtomValidationErrorHandler(schemaValidator.getErrorHandler(), domSource.getNode()));
                    schemaValidator.validate(domSource);
                    StaxUtils.copy(domSource, writer);
                } else {
                    XMLStreamReader reader = StaxUtils.createXMLStreamReader(ds.getInputStream());
                    StaxUtils.copy(reader, writer);
                    reader.close();
                }

            } else if (obj instanceof Node) {
                if (obj instanceof DocumentFragment) {
                    obj = org.apache.cxf.helpers.DOMUtils.getDomDocumentFragment((DocumentFragment)obj);
                }
                if (schema != null) {
                    Validator schemaValidator = schema.newValidator();
                    schemaValidator.setErrorHandler(
                        new MtomValidationErrorHandler(schemaValidator.getErrorHandler(), (Node)obj));
                    schemaValidator.validate(new DOMSource((Node)obj));
                }
                Node nd = (Node)obj;
                writeNode(nd, writer);
            } else {
                Source s = (Source) obj;
                if (schema != null) {
                    if (!(s instanceof DOMSource)) {
                        //make the source re-readable.
                        s = new DOMSource(StaxUtils.read(s));
                    }
                    Validator schemaValidator = schema.newValidator();
                    schemaValidator.setErrorHandler(
                        new MtomValidationErrorHandler(schemaValidator.getErrorHandler(), ((DOMSource)s).getNode()));
                    schemaValidator.validate(s);
                }
                if (s instanceof DOMSource
                    && ((DOMSource) s).getNode() == null) {
                    LOG.exiting("XMLStreamDataWriter", "write");  // Liberty change: line is added
                    return;
                }
                StaxUtils.copy(s, writer);
            }
        } catch (XMLStreamException e) {
            throw new Fault("COULD_NOT_WRITE_XML_STREAM_CAUSED_BY", LOG, e,
                            e.getClass().getCanonicalName(), e.getMessage());
        } catch (IOException e) {
            throw new Fault(new Message("COULD_NOT_WRITE_XML_STREAM", LOG), e);
        } catch (SAXException e) {
            throw new Fault("COULD_NOT_WRITE_XML_STREAM_CAUSED_BY", LOG, e,
                            e.getClass().getCanonicalName(), e.getMessage());
        }
        LOG.exiting("XMLStreamDataWriter", "write");   // Liberty change: line is added
    }

    @Trivial  // Liberty change: line is added
    private void writeNode(Node nd, XMLStreamWriter writer) throws XMLStreamException {
        LOG.entering("XMLStreamDataWriter", "writeNode");   // Liberty change: line is added
        if (writer instanceof W3CDOMStreamWriter) {
            W3CDOMStreamWriter dw = (W3CDOMStreamWriter)writer;

            if (dw.getCurrentNode() != null) {
                if (nd instanceof DocumentFragment
                    && nd.getOwnerDocument() == dw.getCurrentNode().getOwnerDocument()) {
                    Node ch = nd.getFirstChild();
                    while (ch != null) {
                        nd.removeChild(ch);
                        dw.getCurrentNode().appendChild(org.apache.cxf.helpers.DOMUtils.getDomElement(ch));
                        ch = nd.getFirstChild();
                    }
                } else if (nd.getOwnerDocument() == dw.getCurrentNode().getOwnerDocument()) {
                    dw.getCurrentNode().appendChild(nd);
                    LOG.exiting("XMLStreamDataWriter", "writeNode");   // Liberty change: line is added
                    return;
                } else if (nd instanceof DocumentFragment) {
                    nd = dw.getDocument().importNode(nd, true);
                    dw.getCurrentNode().appendChild(nd);
                    LOG.exiting("XMLStreamDataWriter", "writeNode");   // Liberty change: line is added
                    return;
                }
            } else if (dw.getCurrentFragment() != null) {
                if (nd.getOwnerDocument() == dw.getCurrentFragment().getOwnerDocument()) {
                    dw.getCurrentFragment().appendChild(nd);
                    LOG.exiting("XMLStreamDataWriter", "writeNode");   // Liberty change: line is added
                    return;
                } else if (nd instanceof DocumentFragment) {
                    nd = dw.getDocument().importNode(nd, true);
                    dw.getCurrentFragment().appendChild(nd);
                    LOG.exiting("XMLStreamDataWriter", "writeNode");   // Liberty change: line is added
                    return;
                }
            }
        }
        if (nd instanceof Document) {
            StaxUtils.writeDocument((Document)nd,
                                    writer, false, true);
        } else {
            StaxUtils.writeNode(nd, writer, true);
        }
        LOG.exiting("XMLStreamDataWriter", "writeNode");   // Liberty change: line is added
    }

    public void setSchema(Schema s) {
        this.schema = s;
    }

    public void setAttachments(Collection<Attachment> attachments) {

    }

    public void setProperty(String key, Object value) {
    }

    private static class MtomValidationErrorHandler implements ErrorHandler {
        private ErrorHandler origErrorHandler;
        private Node node;

        MtomValidationErrorHandler(ErrorHandler origErrorHandler, Node node) {
            this.origErrorHandler = origErrorHandler;
            this.node = node;
        }


        @Override
        public void warning(SAXParseException exception) throws SAXException {

            if (this.origErrorHandler != null) {
                this.origErrorHandler.warning(exception);
            } else {
                // do nothing
            }
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            if (this.isCVC312Exception(exception)) {
                String elementName = this.getAttachmentElementName(exception);
                if (node != null && this.findIncludeNode(node, elementName)) {
                    return;
                }
            }

            if (this.origErrorHandler != null) {
                this.origErrorHandler.error(exception);
            } else {
                throw exception;
            }

        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            if (this.origErrorHandler != null) {
                this.origErrorHandler.fatalError(exception);
            } else {
                throw exception;
            }

        }

        private boolean isCVC312Exception(SAXParseException exception) {
            String msg = exception.getMessage();
            return (msg.startsWith("cvc-type.3.1.2") || msg.startsWith("cvc-complex-type.2.2"))
                && msg.endsWith("is a simple type, so it must have no element information item [children].");


        }

        private String getAttachmentElementName(SAXParseException exception) {
            String msg = exception.getMessage();
            String[] str = msg.split("'");
            return str[1];
        }

        private boolean findIncludeNode(Node checkNode, String mtomElement) {
            boolean ret = false;
            NodeList nList = checkNode.getChildNodes();
            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);
                if (nNode.getLocalName() != null
                    && nNode.getLocalName().equals(mtomElement)) {
                    NodeList subNodeList = nNode.getChildNodes();
                    for (int j = 0; j < subNodeList.getLength(); j++) {
                        Node subNode = subNodeList.item(j);
                        if ("http://www.w3.org/2004/08/xop/include".equals(subNode.getNamespaceURI())
                            && "Include".equals(subNode.getLocalName())) {
                            // This is the Mtom element which break the SchemaValidation so ignore this
                            return true;
                        }
                    }
                } else {
                    ret = findIncludeNode(nNode, mtomElement);
                }
            }
            return ret;
        }
    }

}
