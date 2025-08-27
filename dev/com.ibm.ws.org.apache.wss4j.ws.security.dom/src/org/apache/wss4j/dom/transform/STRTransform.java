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

package org.apache.wss4j.dom.transform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Iterator;

import javax.xml.crypto.Data;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.NodeSetData;
import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMCryptoContext;
import javax.xml.crypto.dsig.TransformException;
import javax.xml.crypto.dsig.TransformService;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

import org.apache.wss4j.common.bsp.BSPEnforcer;
import org.apache.wss4j.common.token.PKIPathSecurity;
import org.apache.wss4j.common.token.SecurityTokenReference;
import org.apache.wss4j.common.token.X509Security;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.signature.XMLSignatureStreamInput;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * Class STRTransform.
 */
public class STRTransform extends TransformService {

    public static final String TRANSFORM_URI =
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#STR-Transform";

    public static final String TRANSFORM_WS_DOC_INFO = "transform_ws_doc_info";

    private TransformParameterSpec params;

    private Element transformElement;

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(STRTransform.class);

    public final AlgorithmParameterSpec getParameterSpec() {
        return params;
    }

    public void init(TransformParameterSpec params)
        throws InvalidAlgorithmParameterException {
        this.params = params;
    }

    public void init(XMLStructure parent, XMLCryptoContext context)
    throws InvalidAlgorithmParameterException {
        if (context != null && !(context instanceof DOMCryptoContext)) {
            throw new ClassCastException("context must be of type DOMCryptoContext");
        }
        if (!(parent instanceof javax.xml.crypto.dom.DOMStructure)) {
            throw new ClassCastException("parent must be of type DOMStructure");
        }
        transformElement = (Element)
            ((javax.xml.crypto.dom.DOMStructure) parent).getNode();
    }

    public void marshalParams(XMLStructure parent, XMLCryptoContext context)
    throws MarshalException {
        if (context != null && !(context instanceof DOMCryptoContext)) {
            throw new ClassCastException("context must be of type DOMCryptoContext");
        }
        if (!(parent instanceof javax.xml.crypto.dom.DOMStructure)) {
            throw new ClassCastException("parent must be of type DOMStructure");
        }
        Element transformElement2 = (Element)
            ((javax.xml.crypto.dom.DOMStructure) parent).getNode();
        appendChild(transformElement2, transformElement);
        transformElement = transformElement2;
    }


    public Data transform(Data data, XMLCryptoContext xc)
        throws TransformException {
        if (data == null) {
            throw new NullPointerException("data must not be null");
        }
        return transformIt(data, xc, null);
    }

    public Data transform(Data data, XMLCryptoContext xc, OutputStream os)
        throws TransformException {
        if (data == null) {
            throw new NullPointerException("data must not be null");
        }
        if (os == null) {
            throw new NullPointerException("output stream must not be null");
        }
        return transformIt(data, xc, os);
    }


    private Data transformIt(Data data, XMLCryptoContext xc, OutputStream os)
        throws TransformException {

        //
        // First step: Get the required c14n argument and get the specified
        // Canonicalizer
        //
        String canonAlgo = null;
        Element transformParams = XMLUtils.getDirectChildElement(
            transformElement, "TransformationParameters", WSConstants.WSSE_NS
        );
        if (transformParams != null) {
            Element canonElem =
                XMLUtils.getDirectChildElement(
                    transformParams, "CanonicalizationMethod", WSConstants.SIG_NS
                );
            canonAlgo = canonElem.getAttributeNS(null, "Algorithm");
        }
        try {
            //
            // Get the input (node) to transform.
            //
            Element str = null;
            if (data instanceof NodeSetData) {
                NodeSetData nodeSetData = (NodeSetData)data;
                Iterator<?> iterator = nodeSetData.iterator();
                while (iterator.hasNext()) {
                    Node node = (Node)iterator.next();
                    if (node instanceof Element && "SecurityTokenReference".equals(node.getLocalName())) {
                        str = (Element)node;
                        break;
                    }
                }
            } else {
                try {
                    XMLSignatureInput xmlSignatureInput = //NOPMD
                        new XMLSignatureStreamInput(((OctetStreamData)data).getOctetStream());
                    str = (Element)xmlSignatureInput.getSubNode();
                } catch (Exception ex) {
                    throw new TransformException(ex);
                }
            }
            if (str == null) {
                throw new TransformException("No SecurityTokenReference found");
            }
            //
            // The element to transform MUST be a SecurityTokenReference
            // element.
            //
            SecurityTokenReference secRef = new SecurityTokenReference(str, new BSPEnforcer());

            Canonicalizer canon = Canonicalizer.getInstance(canonAlgo);

            //
            // Third and fourth step are performed by dereferenceSTR()
            //
            Object wsDocInfoObject = xc.getProperty(TRANSFORM_WS_DOC_INFO);
            WSDocInfo wsDocInfo = null;
            if (wsDocInfoObject instanceof WSDocInfo) {
                wsDocInfo = (WSDocInfo)wsDocInfoObject;
            }
            if (wsDocInfo == null) {
                LOG.debug("STRTransform: no WSDocInfo found");
            }

            Document doc = str.getOwnerDocument();
            Element dereferencedToken =
                STRTransformUtil.dereferenceSTR(doc, secRef, wsDocInfo);

            if (dereferencedToken != null) {
                String type = dereferencedToken.getAttributeNS(null, "ValueType");
                if (X509Security.X509_V3_TYPE.equals(type)
                    || PKIPathSecurity.getType().equals(type)) {
                    //
                    // Add the WSSE/WSU namespaces to the element for C14n
                    //
                    XMLUtils.setNamespace(
                        dereferencedToken, WSConstants.WSSE_NS, WSConstants.WSSE_PREFIX
                    );
                    XMLUtils.setNamespace(
                        dereferencedToken, WSConstants.WSU_NS, WSConstants.WSU_PREFIX
                    );
                }
            }

            //
            // C14n with specified algorithm. According to WSS Specification.
            //
            if (os != null) {
                canon.canonicalizeSubtree(dereferencedToken, "#default", true, os);
                return null;
            }

            try (ByteArrayOutputStream writer = new ByteArrayOutputStream()) {
                canon.canonicalizeSubtree(dereferencedToken, "#default", true, writer);
                return new OctetStreamData(new ByteArrayInputStream(writer.toByteArray()));
            }
        } catch (Exception ex) {
            throw new TransformException(ex);
        }
    }


    public final boolean isFeatureSupported(String feature) {
        if (feature == null) {
            throw new NullPointerException();
        } else {
            return false;
        }
    }

    private static void appendChild(Node parent, Node child) {
        Document ownerDoc = null;
        if (parent.getNodeType() == Node.DOCUMENT_NODE) {
            ownerDoc = (Document)parent;
        } else {
            ownerDoc = parent.getOwnerDocument();
        }
        if (child.getOwnerDocument() != ownerDoc) {
            parent.appendChild(ownerDoc.importNode(child, true));
        } else {
            parent.appendChild(child);
        }
    }

}
