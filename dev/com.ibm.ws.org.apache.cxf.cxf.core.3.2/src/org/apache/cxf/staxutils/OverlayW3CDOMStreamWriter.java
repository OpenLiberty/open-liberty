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
package org.apache.cxf.staxutils;


import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import org.apache.cxf.common.util.StringUtils;


/**
 * Special StreamWriter that will "overlay" any write events onto the DOM.
 * If the startElement ends up writing an element that already exists at that
 * location, it will just walk into it instead of creating a new element
 */
public class OverlayW3CDOMStreamWriter extends W3CDOMStreamWriter {
    protected boolean isOverlaid = true;

    List<Boolean> isOverlaidStack = new LinkedList<>();
    Boolean textOverlay;

    public OverlayW3CDOMStreamWriter(Document document) {
        super(document);
    }

    public OverlayW3CDOMStreamWriter(Element e) {
        super(e);
    }
    public OverlayW3CDOMStreamWriter(Document doc, Element e) {
        super(doc, e);
    }
    public OverlayW3CDOMStreamWriter(Document doc, DocumentFragment frag) {
        super(doc, frag);
        isOverlaid = false;
    }


    @Override
    protected void createAndAddElement(String prefix, String local, String namespace) {
        super.createAndAddElement(prefix, local, namespace);
        if (isOverlaid) {
            try {
                //mark this as new so we don't consider this for overlaying
                getCurrentNode().setUserData("new", "new", null);
            } catch (Throwable t) {
                //ignore
            }
        }
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        if (isOverlaid) {
            Node nd = getCurrentNode().getFirstChild();
            while (nd != null) {
                try {
                    getCurrentNode().setUserData("new", null, null);
                } catch (Throwable t) {
                    //ignore
                }
                nd = nd.getNextSibling();
            }
        }
        isOverlaid = isOverlaidStack.remove(0);
        super.writeEndElement();
        textOverlay = null;
    }
    public void writeStartElement(String local) throws XMLStreamException {
        isOverlaidStack.add(0, isOverlaid);
        if (isOverlaid) {
            Element nd = getCurrentNode();
            Node nd2;
            if (nd == null) {
                nd2 = getDocument().getDocumentElement();
            } else {
                nd2 = nd.getFirstChild();
            }
            while (nd2 != null) {
                Object userData = null;
                try {
                    userData = nd2.getUserData("new");
                } catch (Throwable t) {
                    //ignore - non DOM level 3
                }
                if (nd2.getNodeType() == Node.ELEMENT_NODE
                    && local.equals(nd2.getLocalName())
                    && StringUtils.isEmpty(nd2.getNamespaceURI())
                    && userData != null) {
                    adjustOverlaidNode(nd2, null);
                    setChild((Element)nd2, false);
                    if (nd2.getFirstChild() == null) {
                        //optimize a case where we KNOW anything added cannot be an overlay
                        isOverlaid = false;
                        textOverlay = null;
                    }
                    return;
                }
                nd2 = nd2.getNextSibling();
            }
        }
        super.writeStartElement(local);
        isOverlaid = false;
        textOverlay = Boolean.FALSE;
    }

    protected void adjustOverlaidNode(Node nd2, String pfx) {
    }

    public void writeStartElement(String namespace, String local) throws XMLStreamException {
        isOverlaidStack.add(0, isOverlaid);
        if (isOverlaid) {
            Element nd = getCurrentNode();
            Node nd2;
            if (nd == null) {
                nd2 = getDocument().getDocumentElement();
            } else {
                nd2 = nd.getFirstChild();
            }
            while (nd2 != null) {
                Object userData = null;
                try {
                    userData = nd2.getUserData("new");
                } catch (Throwable t) {
                    //ignore - non DOM level 3
                }
                if (nd2.getNodeType() == Node.ELEMENT_NODE
                    && local.equals(nd2.getLocalName())
                    && namespace.equals(nd2.getNamespaceURI())
                    && userData == null) {
                    adjustOverlaidNode(nd2, "");
                    setChild((Element)nd2, false);
                    if (nd2.getFirstChild() == null) {
                        //optimize a case where we KNOW anything added cannot be an overlay
                        isOverlaid = false;
                        textOverlay = null;
                    }
                    return;
                }
                nd2 = nd2.getNextSibling();
            }
        }
        super.writeStartElement(namespace, local);
        isOverlaid = false;
        textOverlay = false;
    }

    public void writeStartElement(String prefix, String local, String namespace) throws XMLStreamException {
        if (prefix == null || prefix.isEmpty()) {
            writeStartElement(namespace, local);
        } else {
            isOverlaidStack.add(0, isOverlaid);
            if (isOverlaid) {
                Element nd = getCurrentNode();
                Node nd2;
                if (nd == null) {
                    nd2 = getDocument().getDocumentElement();
                } else {
                    nd2 = nd.getFirstChild();
                }

                while (nd2 != null) {
                    Object userData = null;
                    try {
                        userData = nd2.getUserData("new");
                    } catch (Throwable t) {
                        //ignore - non DOM level 3
                    }

                    if (nd2.getNodeType() == Node.ELEMENT_NODE
                        && local.equals(nd2.getLocalName())
                        && namespace.equals(nd2.getNamespaceURI())
                        && userData == null) {
                        adjustOverlaidNode(nd2, prefix);
                        setChild((Element)nd2, false);
                        if (nd2.getFirstChild() == null) {
                            //optimize a case where we KNOW anything added cannot be an overlay
                            isOverlaid = false;
                            textOverlay = null;
                        }
                        return;
                    }
                    nd2 = nd2.getNextSibling();
                }
            }
            super.writeStartElement(prefix, local, namespace);
            isOverlaid = false;
            textOverlay = false;
        }
    }

    public void writeCharacters(String text) throws XMLStreamException {
        if (!isOverlaid || textOverlay == null) {
            super.writeCharacters(text);
        } else if (!textOverlay) {
            Element nd = getCurrentNode();
            Node txt = nd.getFirstChild();
            if (txt instanceof Text
                && ((Text)txt).getTextContent().startsWith(text)) {
                textOverlay = true;
                return;
            }
            super.writeCharacters(text);
        }

    }


}
