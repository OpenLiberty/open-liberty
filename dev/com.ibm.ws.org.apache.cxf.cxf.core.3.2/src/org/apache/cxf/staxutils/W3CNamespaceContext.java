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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.NamespaceContext;

import org.apache.cxf.common.logging.LogUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.ibm.websphere.ras.annotation.Trivial;

@Trivial  // Liberty change: line is added
public class W3CNamespaceContext implements NamespaceContext {

    private static final Logger LOG = LogUtils.getL7dLogger(W3CNamespaceContext.class); // Liberty change: line is added

    private Element currentNode;
    private NamespaceContext outNamespaceContext;

    public W3CNamespaceContext() {
    }
    public W3CNamespaceContext(Element el) {
        currentNode = el;
    }

    public void setOutNamespaceContext(NamespaceContext context) {
        outNamespaceContext = context;
    }

    // Liberty change: getOutNamespaceContext method below is added
    public NamespaceContext getOutNamespaceContext() {
        return outNamespaceContext;
    } // Liberty change: end

    public String getNamespaceURI(String prefix) {
        return getNamespaceURI(currentNode, !prefix.isEmpty() ? "xmlns:" + prefix : "xmlns");
    }

    private String getNamespaceURI(Element e, String name) {
        if (e == null) {
            return null;
        }
/*      Liberty change: if block below is removed
        // check the outside namespace URI
        if (outNamespaceContext != null) {
            String result = outNamespaceContext.getNamespaceURI(name);
            if (result != null) {
                return result;
            }
        } Liberty change: end */

        Attr attr = e.getAttributeNode(name);
        if (attr == null) {
            Node n = e.getParentNode();
            if (n instanceof Element && n != e) {
                return getNamespaceURI((Element)n, name);
            }
        } else {
            return attr.getValue();
        }

        return null;
    }

    public String getPrefix(String uri) {
        return getPrefix(currentNode, uri);
    }

    private String getPrefix(Element e, String uri) {
        if (e == null) {
            return null;
        }
        /*      Liberty change: if block below is removed
        // check the outside namespace URI
        if (outNamespaceContext != null) {
            String result = outNamespaceContext.getPrefix(uri);
            if (result != null) {
                return result;
            }
        } Liberty change: end */

        NamedNodeMap attributes = e.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr a = (Attr)attributes.item(i);

                String val = a.getValue();
                if (val != null && val.equals(uri)) {
                    String name = a.getNodeName(); // Liberty change: getLocalName is replaced by getNodeName
                    if ("xmlns".equals(name)) {
                        return "";
                    }
                    return name.substring(6); // Liberty change: name is replaced by name.substring(6)
                }
            }
        }

        Node n = e.getParentNode();
        if (n instanceof Element && n != e) {
            return getPrefix((Element)n, uri);
        }

        return null;
    }

    public Iterator<String> getPrefixes(String uri) {
        List<String> prefixes = new ArrayList<>();

        String prefix = getPrefix(uri);
        if (prefix != null) {
            prefixes.add(prefix);
        }

        return prefixes.iterator();
    }

    public Element getElement() {
        return currentNode;
    }

    public void setElement(Element node) {
        LOG.entering("W3CNamespaceContext", "setElement");  // Liberty change: line is added
        this.currentNode = node;
        LOG.exiting("W3CNamespaceContext", "setElement"); // Liberty change: line is added
    }
}
