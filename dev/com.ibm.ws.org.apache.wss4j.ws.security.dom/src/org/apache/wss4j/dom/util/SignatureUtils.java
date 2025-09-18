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

package org.apache.wss4j.dom.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * WS-Security Utility methods. <p/>
 */
public final class SignatureUtils {

    private SignatureUtils() {
        // Complete
    }

    public static void verifySignedElement(Element elem, WSDocInfo wsDocInfo)
        throws WSSecurityException {
        verifySignedElement(elem, wsDocInfo.getResultsByTag(WSConstants.SIGN));
    }

    public static void verifySignedElement(Element elem, List<WSSecurityEngineResult> signedResults)
        throws WSSecurityException {
        if (signedResults != null) {
            for (WSSecurityEngineResult signedResult : signedResults) {
                @SuppressWarnings("unchecked")
                List<WSDataRef> dataRefs =
                    (List<WSDataRef>)signedResult.get(WSSecurityEngineResult.TAG_DATA_REF_URIS);
                if (dataRefs != null) {
                    for (WSDataRef dataRef : dataRefs) {
                        if (isElementOrAncestorSigned(elem, dataRef.getProtectedElement())) {
                            return;
                        }
                    }
                }
            }
        }

        throw new WSSecurityException(
            WSSecurityException.ErrorCode.FAILED_CHECK, "elementNotSigned",
            new Object[] {elem});
    }

    /**
     * Get the List of inclusive prefixes from the DOM Element argument
     */
    public static List<String> getInclusivePrefixes(Element target, boolean excludeVisible) {
        Set<String> result = new LinkedHashSet<>();
        Node parent = target;
        while (parent.getParentNode() != null
            && !(Node.DOCUMENT_NODE == parent.getParentNode().getNodeType())) {
            parent = parent.getParentNode();
            NamedNodeMap attributes = parent.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                if (WSConstants.XMLNS_NS.equals(attribute.getNamespaceURI())) {
                    if ("xmlns".equals(attribute.getNodeName())) {
                        result.add("#default");
                    } else {
                        result.add(attribute.getLocalName());
                    }
                }
            }
        }

        if (excludeVisible) {
            NamedNodeMap attributes = target.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                if (WSConstants.XMLNS_NS.equals(attribute.getNamespaceURI())) {
                    if ("xmlns".equals(attribute.getNodeName())) {
                        result.remove("#default");
                    } else {
                        result.remove(attribute.getLocalName());
                    }
                }
                if (attribute.getPrefix() != null) {
                    result.remove(attribute.getPrefix());
                }
            }

            if (target.getPrefix() == null) {
                result.remove("#default");
            } else {
                result.remove(target.getPrefix());
            }
        }

        return new ArrayList<String>(result);
    }

    /**
     * Does the current element or some ancestor of it correspond to the known "signedElement"?
     */
    private static boolean isElementOrAncestorSigned(Element elem, Element signedElement)
        throws WSSecurityException {
        final Element envelope = elem.getOwnerDocument().getDocumentElement();
        Node cur = elem;
        while (!cur.isSameNode(envelope)) {
            if (cur.getNodeType() == Node.ELEMENT_NODE && cur.equals(signedElement)) {
                return true;
            }
            cur = cur.getParentNode();
        }

        return false;
    }

}
