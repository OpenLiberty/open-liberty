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

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Attr;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.TypeInfo;

import com.ibm.websphere.ras.annotation.Trivial;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;

@Trivial
public class W3CDOMStreamReader extends AbstractDOMStreamReader<Node, Node> {

    private static final Logger LOG = LogUtils.getL7dLogger(W3CDOMStreamReader.class);

    private Node content;

    private Document document;

    private W3CNamespaceContext context;
    
    private String sysId;

    /**
     * @param element
     */
    public W3CDOMStreamReader(Element element) {
        super(new ElementFrame<Node, Node>(element, null));
        LOG.entering("W3CDOMStreamReader", "W3CDOMStreamReader");
        content = element;
        newFrame(getCurrentFrame());
                
        this.document = element.getOwnerDocument();
        LOG.exiting("W3CDOMStreamReader", "W3CDOMStreamReader");
    }

    public W3CDOMStreamReader(Element element, String systemId) {
        this(element);
        LOG.entering("W3CDOMStreamReader", "W3CDOMStreamReader");
        sysId = systemId;
        LOG.exiting("W3CDOMStreamReader", "W3CDOMStreamReader");
    }

    public W3CDOMStreamReader(Document doc) {
        super(new ElementFrame<Node, Node>(doc, false) {
            public boolean isDocument() {
                return true;
            }
        });
        LOG.entering("W3CDOMStreamReader", "W3CDOMStreamReader");
        this.document = doc;
        LOG.exiting("W3CDOMStreamReader", "W3CDOMStreamReader");
    }

    public W3CDOMStreamReader(DocumentFragment docfrag) {
        super(new ElementFrame<Node, Node>(docfrag, true) {
            public boolean isDocumentFragment() {
                return true;
            }
        });
        LOG.entering("W3CDOMStreamReader", "W3CDOMStreamReader");
        this.document = docfrag.getOwnerDocument();
        LOG.exiting("W3CDOMStreamReader", "W3CDOMStreamReader");
    }

    /**
     * Get the document associated with this stream.
     * 
     * @return
     */
    public Document getDocument() {
        return document;
    }
    public String getSystemId() {
        return sysId == null ? document.getDocumentURI() : sysId;
    }
    /**
     * Find name spaces declaration in atrributes and move them to separate
     * collection.
     */
    @Override
    protected final void newFrame(ElementFrame<Node, Node> frame) {
        Node element = getCurrentNode();
        frame.uris = new ArrayList<String>();
        frame.prefixes = new ArrayList<String>();
        frame.attributes = new ArrayList<Object>();

        if (context == null) {
            context = new W3CNamespaceContext();
        }
        if (element instanceof Element) {
            context.setElement((Element)element);
        }

        NamedNodeMap nodes = element.getAttributes();

        String ePrefix = element.getPrefix();
        if (ePrefix == null) {
            ePrefix = "";
        }

        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                String prefix = node.getPrefix();
                String localName = node.getLocalName();
                String value = node.getNodeValue();
                String name = node.getNodeName();

                if (prefix == null) {
                    prefix = "";
                }

                if (name != null && "xmlns".equals(name)) {
                    frame.uris.add(value);
                    frame.prefixes.add("");
                } else if (prefix.length() > 0 && "xmlns".equals(prefix)) {
                    frame.uris.add(value);
                    frame.prefixes.add(localName);
                } else if (name.startsWith("xmlns:")) {
                    prefix = name.substring(6);
                    frame.uris.add(value);
                    frame.prefixes.add(prefix);
                } else {
                    frame.attributes.add(node);
                }
            }
        }
    }

    @Override
    protected void endElement() {
        super.endElement();
    }

    public final Node getCurrentNode() {
        return getCurrentFrame().element;
    }
    public final Element getCurrentElement() {
        return (Element)getCurrentFrame().element;
    }

    @Override
    protected ElementFrame<Node, Node> getChildFrame() {
        return new ElementFrame<Node, Node>(getCurrentFrame().currentChild, 
                                getCurrentFrame());
    }

    @Override
    protected boolean hasMoreChildren() {
        if (getCurrentFrame().currentChild == null) {
            return getCurrentNode().getFirstChild() != null;            
        }
        return getCurrentFrame().currentChild.getNextSibling() != null;
    }

    @Override
    protected int nextChild() {
        ElementFrame<Node, Node> frame = getCurrentFrame();
        if (frame.currentChild == null) {
            content = getCurrentNode().getFirstChild();            
        } else {
            content = frame.currentChild.getNextSibling();
        }
        
        frame.currentChild = content;
        switch (content.getNodeType()) {
        case Node.ELEMENT_NODE:
            return START_ELEMENT;
        case Node.TEXT_NODE:
            return CHARACTERS;
        case Node.COMMENT_NODE:
            return COMMENT;
        case Node.CDATA_SECTION_NODE:
            return CDATA;
        case Node.ENTITY_REFERENCE_NODE:
            return ENTITY_REFERENCE;
        default:
            throw new IllegalStateException("Found type: " + content.getClass().getName());
        }
    }

    @Override
    public String getElementText() throws XMLStreamException {
        String result = DOMUtils.getRawContent(content);

        ElementFrame<Node, Node> frame = getCurrentFrame();
        frame.ended = true;
        currentEvent = END_ELEMENT;
        endElement();

        // we should not return null according to the StAx API javadoc
        return result != null ? result : "";
    }

    @Override
    public String getNamespaceURI(String prefix) {
        ElementFrame<Node, Node> frame = getCurrentFrame();

        while (null != frame) {
            int index = frame.prefixes.indexOf(prefix);
            if (index != -1) {
                return frame.uris.get(index);
            }

            if (frame.parent == null && frame.getElement() instanceof Element) {
                return ((Element)frame.getElement()).lookupNamespaceURI(prefix);
            }
            frame = frame.parent;
        }

        return null;
    }

    public String getAttributeValue(String ns, String local) {
        Attr at;
        if (ns == null || ns.equals("")) {
            at = getCurrentElement().getAttributeNode(local);
        } else {
            at = getCurrentElement().getAttributeNodeNS(ns, local);
        }

        if (at == null) {
            return null;
        }
        return at.getNodeValue();
    }

    public int getAttributeCount() {
        return getCurrentFrame().attributes.size();
    }

    Attr getAttribute(int i) {
        return (Attr)getCurrentFrame().attributes.get(i);
    }

    private String getLocalName(Attr attr) {

        String name = attr.getLocalName();
        if (name == null) {
            name = attr.getNodeName();
        }
        return name;
    }

    public QName getAttributeName(int i) {
        Attr at = getAttribute(i);

        String prefix = at.getPrefix();
        String ln = getLocalName(at);
        // at.getNodeName();
        String ns = at.getNamespaceURI();

        if (prefix == null) {
            return new QName(ns, ln);
        } else {
            return new QName(ns, ln, prefix);
        }
    }

    public String getAttributeNamespace(int i) {
        return getAttribute(i).getNamespaceURI();
    }

    public String getAttributeLocalName(int i) {
        Attr attr = getAttribute(i);
        return getLocalName(attr);
    }

    public String getAttributePrefix(int i) {
        return getAttribute(i).getPrefix();
    }

    public String getAttributeType(int i) {
        Attr attr = getAttribute(i);
        if (attr.isId()) {
            return "ID";
        }
        TypeInfo schemaType = null;
        try {
            schemaType = attr.getSchemaTypeInfo();
        } catch (Throwable t) {
            //DOM level 2?
            schemaType = null;
        }
        return (schemaType == null) ? "CDATA" 
            : schemaType.getTypeName() == null ? "CDATA" : schemaType.getTypeName();
    }


    public String getAttributeValue(int i) {
        return getAttribute(i).getValue();
    }

    public boolean isAttributeSpecified(int i) {
        return getAttribute(i).getValue() != null;
    }

    public int getNamespaceCount() {
        return getCurrentFrame().prefixes.size();
    }

    public String getNamespacePrefix(int i) {
        return getCurrentFrame().prefixes.get(i);
    }

    public String getNamespaceURI(int i) {
        return getCurrentFrame().uris.get(i);
    }

    public NamespaceContext getNamespaceContext() {
        return context;
    }

    public String getText() {
        if (content instanceof Text) {
            return ((Text)content).getData();
        } else if (content instanceof Comment) {
            return ((Comment)content).getData();
        }
        return DOMUtils.getRawContent(getCurrentNode());
    }

    public char[] getTextCharacters() {
        return getText().toCharArray();
    }

    public int getTextStart() {
        return 0;
    }

    public int getTextLength() {
        return getText().length();
    }

    public String getEncoding() {
        return null;
    }

    public QName getName() {
        Node el = getCurrentNode();

        String prefix = getPrefix();
        String ln = getLocalName();

        if (prefix == null) {
            return new QName(el.getNamespaceURI(), ln);
        } else {
            return new QName(el.getNamespaceURI(), ln, prefix);
        }
    }

    public String getLocalName() {
        String ln = getCurrentNode().getLocalName();
        if (ln == null) {
            ln = getCurrentNode().getNodeName();
            if (ln.indexOf(":") != -1) {
                ln = ln.substring(ln.indexOf(":") + 1);
            }
        }
        return ln;
    }

    public String getNamespaceURI() {
        String ln = getCurrentNode().getLocalName();
        if (ln == null) {
            ln = getCurrentNode().getNodeName();
            if (ln.indexOf(":") == -1) {
                ln = getNamespaceURI("");
            } else {
                ln = getNamespaceURI(ln.substring(0, ln.indexOf(":")));
            }
            return ln;
        }
        return getCurrentNode().getNamespaceURI();
    }

    public String getPrefix() {
        String prefix = getCurrentNode().getPrefix();
        if (prefix == null) {
            String nodeName = getCurrentNode().getNodeName();
            if (nodeName.indexOf(":") != -1) {
                prefix = nodeName.substring(0, nodeName.indexOf(":"));
            }  else {
                prefix = "";
            }
        }
        return prefix;
    }

    public String getPITarget() {
        throw new UnsupportedOperationException();
    }

    public String getPIData() {
        throw new UnsupportedOperationException();
    }   
    public Location getLocation() {
        try {
            Object o = getCurrentNode().getUserData("location");
            if (o instanceof Location) { 
                return (Location)o;
            }
        } catch (Exception ex) {
            //ignore, probably not DOM level 3
        }
        return super.getLocation();
    }

    public String toString() {
        LOG.entering("W3CDOMStreamReader", "toString");
        if (document == null) {
            return "<null>";
        }
        if (document.getDocumentElement() == null) {
            return "<null document element>";
        }
        try {
            LOG.exiting("W3CDOMStreamReader", "toString");
            return StaxUtils.toString(document);
        } catch (XMLStreamException e) {
            LOG.exiting("W3CDOMStreamReader", "toString");
            return super.toString();
        } catch (Throwable t) {
            t.printStackTrace();
            LOG.exiting("W3CDOMStreamReader", "toString");
            return super.toString();
        }
    }

    
}
