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
package org.apache.cxf.binding.soap.saaj;

import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamException;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.staxutils.OverlayW3CDOMStreamWriter;
import org.apache.cxf.staxutils.W3CNamespaceContext;

import static org.apache.cxf.binding.soap.saaj.SAAJUtils.adjustPrefix;


public final class SAAJStreamWriter extends OverlayW3CDOMStreamWriter {
    private final SOAPPart part;
    private final SOAPEnvelope envelope;
    private String uri;


    public SAAJStreamWriter(SOAPPart part) {
        super(part);
        this.part = part;
        Node nd = part.getFirstChild();
        if (nd == null) {
            isOverlaid = false;
        }
        envelope = null;
    }
    public SAAJStreamWriter(SOAPPart part, Element current) {
        super(part, current);
        this.part = part;
        envelope = null;
    }
    public SAAJStreamWriter(SOAPEnvelope env, DocumentFragment frag) {
        super(env.getOwnerDocument(), frag);
        this.part = null;
        this.envelope = env;
        isOverlaid = false;
    }
    public SAAJStreamWriter(SOAPEnvelope env, Element cur) {
        super(env.getOwnerDocument(), cur);
        this.part = null;
        this.envelope = env;
        isOverlaid = false;
    }

    @Override
    public String getPrefix(String nsuri) throws XMLStreamException {
        if (isOverlaid && part != null && getCurrentNode() == null) {
            Node nd = part.getFirstChild();
            while (nd != null) {
                if (nd instanceof Element) {
                    Iterator<String> it = new W3CNamespaceContext((Element)nd).getPrefixes(nsuri);
                    if (it.hasNext()) {
                        return it.next();
                    }
                    nd = null;
                } else {
                    nd = nd.getNextSibling();
                }
            }
        }
        return super.getPrefix(nsuri);
    }
    private String getEnvelopeURI() throws SOAPException {
        if (uri == null) {
            uri = getEnvelope().getElementName().getURI();
        }
        return uri;
    }
    private SOAPEnvelope getEnvelope() throws SOAPException {
        if (envelope == null) {
            return part.getEnvelope();
        }
        return envelope;
    }

    protected void adjustOverlaidNode(Node nd2, String pfx) {
        String namespace = nd2.getNamespaceURI();
        try {
            if (namespace != null
                && namespace.equals(getEnvelopeURI())) {
                adjustPrefix((Element)nd2, pfx);
                if ("Envelope".equals(nd2.getLocalName())) {
                    adjustPrefix(getEnvelope().getHeader(), pfx);
                }
            }
        } catch (SOAPException e) {
            //ignore, fallback
        }
        super.adjustOverlaidNode(nd2, pfx);
    }

    protected void createAndAddElement(String prefix, String local, String namespace) {
        if (part == null) {
            super.createAndAddElement(prefix, local, namespace);
            return;
        }
        try {
            if (namespace != null
                && namespace.equals(getEnvelopeURI())) {
                if ("Envelope".equals(local)) {
                    setChild(adjustPrefix(getEnvelope(), prefix), false);
                    adjustPrefix(getEnvelope().getHeader(), prefix);
                    adjustPrefix(getEnvelope().getBody(), prefix);
                    getEnvelope().removeChild(getEnvelope().getHeader());
                    getEnvelope().removeChild(getEnvelope().getBody());
                    return;
                } else if ("Body".equals(local)) {
                    if (getEnvelope().getBody() == null) {
                        getEnvelope().addBody();
                    }
                    setChild(adjustPrefix(getEnvelope().getBody(), prefix), false);
                    return;
                } else if ("Header".equals(local)) {
                    if (getEnvelope().getHeader() == null) {
                        getEnvelope().addHeader();
                    }
                    setChild(adjustPrefix(getEnvelope().getHeader(), prefix), false);
                    return;
                } else if ("Fault".equals(local)) {
                    SOAPFault f = getEnvelope().getBody().getFault();
                    if (f == null) {
                        Element el = getDocument().createElementNS(namespace,
                                             StringUtils.isEmpty(prefix) ? local : prefix + ":" + local);
                        getEnvelope().getBody().appendChild(el);
                        f = getEnvelope().getBody().getFault();
                        if (f == null) {
                            f = getEnvelope().getBody().addFault();
                        }
                    }
                    setChild(adjustPrefix(f, prefix), false);
                    return;
                }
            } else if (getCurrentNode() instanceof SOAPFault) {
                SOAPFault f = (SOAPFault)getCurrentNode();
                Node nd = f.getFirstChild();
                while (nd != null) {
                    if (nd instanceof Element) {
                        Element el = (Element)nd;
                        if (local.equals(nd.getLocalName())) {
                            setChild(el, false);
                            nd = el.getFirstChild();
                            while (nd != null) {
                                el.removeChild(nd);
                                nd = el.getFirstChild();
                            }
                            return;
                        }
                    }
                    nd = nd.getNextSibling();
                }
            }
        } catch (SOAPException e) {
            //ignore, fallback
        }
        super.createAndAddElement(prefix, local, namespace);
    }

    @Override
    protected Element createElementNS(String ns, String pfx, String local) {
        Element cur = getCurrentNode();
        if (cur instanceof SOAPBody) {
            try {
                if (StringUtils.isEmpty(pfx) && StringUtils.isEmpty(ns)) {
                    Element el = ((SOAPBody)cur).addBodyElement(new QName(local));
                    cur.removeChild(el);
                    return el;
                }
                Element el = ((SOAPBody)cur).addBodyElement(new QName(ns, local,  pfx == null ? "" : pfx));
                cur.removeChild(el);
                return el;
            } catch (SOAPException e) {
                //ignore
            }
        } else if (cur instanceof SOAPHeader) {
            try {
                Element el = ((SOAPHeader)cur).addHeaderElement(new QName(ns, local, pfx == null ? "" : pfx));
                cur.removeChild(el);
                return el;
            } catch (SOAPException e) {
                //ignore
            }
        } else if (cur instanceof SOAPElement) {
            try {
                Element el = null;
                if (StringUtils.isEmpty(pfx) && StringUtils.isEmpty(ns)) {
                    el = ((SOAPElement)cur).addChildElement(local, "", "");
                } else {
                    el = ((SOAPElement)cur).addChildElement(local, pfx == null ? "" : pfx, ns);
                    adjustPrefix(el, pfx);
                }
                cur.removeChild(el);
                return el;
            } catch (SOAPException e) {
                //ignore
            }
        }
        return super.createElementNS(ns, pfx, local);
    }

}