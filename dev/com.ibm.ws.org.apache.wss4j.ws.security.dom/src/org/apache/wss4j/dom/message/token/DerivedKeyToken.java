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

package org.apache.wss4j.dom.message.token;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.bsp.BSPEnforcer;
import org.apache.wss4j.common.derivedKey.ConversationConstants;
import org.apache.wss4j.common.derivedKey.DerivedKeyUtils;
import org.apache.wss4j.common.principal.WSDerivedKeyTokenPrincipal;
import org.apache.wss4j.common.token.SecurityTokenReference;
import org.apache.wss4j.common.util.DOM2Writer;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 <DerivedKeyToken wsu:Id="..." wsc:Algorithm="...">
 <SecurityTokenReference>...</SecurityTokenReference>
 <Properties>...</Properties>
 <Generation>...</Generation>
 <Offset>...</Offset>
 <Length>...</Length>
 <Label>...</Label>
 <Nonce>...</Nonce>
 </DerivedKeyToken>
 */

public class DerivedKeyToken {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(DerivedKeyToken.class);

    // These are the elements that are used to create the SecurityContextToken
    private Element element;
    private Element elementSecurityTokenReference;
    private Element elementProperties;
    private Element elementGeneration;
    private Element elementOffset;
    private Element elementLength;
    private Element elementLabel;
    private Element elementNonce;
    private int length = 32;
    private int offset = 0;
    private int generation = -1;

    private String ns;

    private final BSPEnforcer bspEnforcer;

    /**
     * This will create an empty DerivedKeyToken
     *
     * @param doc The DOM document
     */
    public DerivedKeyToken(Document doc) throws WSSecurityException {
        this(ConversationConstants.DEFAULT_VERSION, doc);
    }

    /**
     * This will create an empty DerivedKeyToken
     *
     * @param doc The DOM document
     */
    public DerivedKeyToken(int version, Document doc) throws WSSecurityException {
        LOG.debug("DerivedKeyToken: created");

        ns = ConversationConstants.getWSCNs(version);
        element =
            doc.createElementNS(ns, ConversationConstants.WSC_PREFIX + ":"
                + ConversationConstants.DERIVED_KEY_TOKEN_LN);
        XMLUtils.setNamespace(element, ns, ConversationConstants.WSC_PREFIX);
        bspEnforcer = new BSPEnforcer();
    }

    /**
     * This will create a DerivedKeyToken object with the given DerivedKeyToken element
     *
     * @param elem The DerivedKeyToken DOM element
     * @param bspEnforcer a BSPEnforcer instance to enforce BSP rules
     * @throws WSSecurityException If the element is not a derived key token
     */
    public DerivedKeyToken(Element elem, BSPEnforcer bspEnforcer) throws WSSecurityException {
        LOG.debug("DerivedKeyToken: created : element constructor");
        element = elem;
        this.bspEnforcer = bspEnforcer;
        QName el = new QName(element.getNamespaceURI(), element.getLocalName());

        if (!(el.equals(ConversationConstants.DERIVED_KEY_TOKEN_QNAME_05_02)
            || el.equals(ConversationConstants.DERIVED_KEY_TOKEN_QNAME_05_12))) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN
            );
        }
        elementSecurityTokenReference =
            XMLUtils.getDirectChildElement(
                element,
                ConversationConstants.SECURITY_TOKEN_REFERENCE_LN,
                WSConstants.WSSE_NS
            );

        ns = el.getNamespaceURI();

        elementProperties =
            XMLUtils.getDirectChildElement(
                element, ConversationConstants.PROPERTIES_LN, ns
            );
        elementGeneration =
            XMLUtils.getDirectChildElement(
                element, ConversationConstants.GENERATION_LN, ns
            );
        elementOffset =
            XMLUtils.getDirectChildElement(
                element, ConversationConstants.OFFSET_LN, ns
            );
        elementLength =
            XMLUtils.getDirectChildElement(
                element, ConversationConstants.LENGTH_LN, ns
            );
        elementLabel =
            XMLUtils.getDirectChildElement(
                element, ConversationConstants.LABEL_LN, ns
            );
        elementNonce =
            XMLUtils.getDirectChildElement(
                element, ConversationConstants.NONCE_LN, ns
            );

        if (elementLength != null) {
            Text text = getFirstNode(elementLength);
            if (text != null) {
                try {
                    length = Integer.parseInt(text.getData());
                } catch (NumberFormatException ex) {
                    throw new WSSecurityException(
                            WSSecurityException.ErrorCode.FAILURE, ex, "decoding.general"
                    );
                }
            }
        }

        if (elementOffset != null) {
            Text text = getFirstNode(elementOffset);
            if (text != null) {
                try {
                    offset = Integer.parseInt(text.getData());
                } catch (NumberFormatException ex) {
                    throw new WSSecurityException(
                            WSSecurityException.ErrorCode.FAILURE, ex, "decoding.general"
                    );
                }
            }
        }

        if (elementGeneration != null) {
            Text text = getFirstNode(elementGeneration);
            if (text != null) {
                try {
                    generation = Integer.parseInt(text.getData());
                } catch (NumberFormatException ex) {
                    throw new WSSecurityException(
                            WSSecurityException.ErrorCode.FAILURE, ex, "decoding.general"
                    );
                }
            }
        }
    }

    /**
     * Add the WSU Namespace to this DKT. The namespace is not added by default for
     * efficiency purposes.
     */
    public void addWSUNamespace() {
        element.setAttributeNS(XMLUtils.XMLNS_NS, "xmlns:" + WSConstants.WSU_PREFIX, WSConstants.WSU_NS);
    }

    /**
     * Sets the security token reference of the derived key token
     * This is the reference to the shared secret used in the conversation/context
     *
     * @param ref Security token reference
     */
    public void setSecurityTokenReference(SecurityTokenReference ref) {
        elementSecurityTokenReference = ref.getElement();
        WSSecurityUtil.prependChildElement(element, ref.getElement());
    }

    public void setSecurityTokenReference(Element elem) {
        elementSecurityTokenReference = elem;
        WSSecurityUtil.prependChildElement(element, elem);
    }

    /**
     * Returns the SecurityTokenReference of the derived key token
     *
     * @return the Security Token Reference of the derived key token
     * @throws WSSecurityException
     */
    public SecurityTokenReference getSecurityTokenReference() throws WSSecurityException {
        if (elementSecurityTokenReference != null) {
            return new SecurityTokenReference(elementSecurityTokenReference, bspEnforcer);
        }
        return null;
    }

    /**
     * Returns the SecurityTokenReference element of the derived key token
     *
     * @return the Security Token Reference element of the derived key token
     */
    public Element getSecurityTokenReferenceElement() {
        return elementSecurityTokenReference;
    }

    /**
     * This adds a property into
     * /DerivedKeyToken/Properties
     *
     * @param propName  Name of the property
     * @param propValue Value of the property
     */
    private void addProperty(String propName, String propValue) {
        if (elementProperties == null) { //Create the properties element if it is not there
            elementProperties =
                element.getOwnerDocument().createElementNS(
                    ns, ConversationConstants.WSC_PREFIX + ":" + ConversationConstants.PROPERTIES_LN
                );
            element.appendChild(elementProperties);
        }
        Element tempElement =
            element.getOwnerDocument().createElementNS(ns, ConversationConstants.WSC_PREFIX + ":"
                + propName);
        tempElement.appendChild(element.getOwnerDocument().createTextNode(propValue));

        elementProperties.appendChild(tempElement);
    }

    /**
     * This is used to set the Name, Label and Nonce element values in the properties element
     * <b>At this point I'm not sure if these are the only properties that will appear in the
     * <code>Properties</code> element. There fore this method is provided
     * If this is not required feel free to remove this :D
     * </b>
     *
     * @param name  Value of the Properties/Name element
     * @param label Value of the Properties/Label element
     * @param nonce Value of the Properties/Nonce element
     */
    public void setProperties(String name, String label, String nonce) {
        Map<String, String> table = new HashMap<>();
        table.put("Name", name);
        table.put("Label", label);
        table.put("Nonce", nonce);
        setProperties(table);
    }

    /**
     * If there are other types of properties other than Name, Label and Nonce
     * This is provided for extensibility purposes
     *
     * @param properties The properties and values in a Map
     */
    public void setProperties(Map<String, String> properties) {
        if (properties != null && !properties.isEmpty()) {
            for (Entry<String, String> entry : properties.entrySet()) {
                String propertyName = entry.getValue();
                //Check whether this property is already there
                //If so change the value
                Element node =
                    XMLUtils.findElement(elementProperties, propertyName, ns);
                if (node != null) { //If the node is not null
                    Text node1 = getFirstNode(node);
                    if (node1 != null) {
                        node1.setData(properties.get(propertyName));
                    }
                } else {
                    addProperty(propertyName, properties.get(propertyName));
                }
            }
        }
    }

    public Map<String, String> getProperties() {
        if (elementProperties != null) {
            Map<String, String> table = new HashMap<>();
            Node node = elementProperties.getFirstChild();
            while (node != null) {
                if (Node.ELEMENT_NODE == node.getNodeType()) {
                    Text text = getFirstNode((Element) node);
                    if (text != null) {
                        table.put(node.getNodeName(), text.getData());
                    }
                }
                node = node.getNextSibling();
            }
            return table;
        }
        return null;
    }

    /**
     * Sets the length of the derived key
     *
     * @param length The length of the derived key as a long
     */
    public void setLength(int length) {
        elementLength =
            element.getOwnerDocument().createElementNS(
                ns, ConversationConstants.WSC_PREFIX + ":" + ConversationConstants.LENGTH_LN
            );
        elementLength.appendChild(
            element.getOwnerDocument().createTextNode(Long.toString(length))
        );
        element.appendChild(elementLength);
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    /**
     * Sets the offset
     *
     * @param offset The offset value as an integer
     */
    public void setOffset(int offset) throws WSSecurityException {
        //This element MUST NOT be used if the <Generation> element is specified
        if (elementGeneration == null) {
            elementOffset =
                element.getOwnerDocument().createElementNS(
                    ns, ConversationConstants.WSC_PREFIX + ":" + ConversationConstants.OFFSET_LN
                );
            elementOffset.appendChild(
                element.getOwnerDocument().createTextNode(Integer.toString(offset))
            );
            element.appendChild(elementOffset);
        } else {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "offsetError");
        }
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    /**
     * Sets the generation of the derived key
     *
     * @param generation generation value as an integer
     */
    public void setGeneration(int generation) throws WSSecurityException {
        //This element MUST NOT be used if the <Offset> element is specified
        if (elementOffset == null) {
            elementGeneration =
                element.getOwnerDocument().createElementNS(
                    ns, ConversationConstants.WSC_PREFIX + ":" + ConversationConstants.GENERATION_LN
                );
            elementGeneration.appendChild(
                element.getOwnerDocument().createTextNode(Integer.toString(generation))
            );
            element.appendChild(elementGeneration);
        } else {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "offsetError");
        }
        this.generation = generation;
    }

    public int getGeneration() {
        return generation;
    }

    /**
     * Sets the label of the derived key
     *
     * @param label Label value as a string
     */
    public void setLabel(String label) {
        elementLabel =
            element.getOwnerDocument().createElementNS(
                ns, ConversationConstants.WSC_PREFIX + ":" + ConversationConstants.LABEL_LN
            );
        elementLabel.appendChild(element.getOwnerDocument().createTextNode(label));
        element.appendChild(elementLabel);
    }

    /**
     * Sets the nonce value of the derived key
     *
     * @param nonce Nonce value as a string
     */
    public void setNonce(String nonce) {
        elementNonce =
            element.getOwnerDocument().createElementNS(
                ns, ConversationConstants.WSC_PREFIX + ":" + ConversationConstants.NONCE_LN
            );
        elementNonce.appendChild(element.getOwnerDocument().createTextNode(nonce));
        element.appendChild(elementNonce);
    }

    /**
     * Returns the label of the derived key token
     *
     * @return Label of the derived key token
     */
    public String getLabel() {
        if (elementLabel != null) {
            Text text = getFirstNode(elementLabel);
            if (text != null) {
                return text.getData();
            }
        }
        return null;
    }

    /**
     * Return the nonce of the derived key token
     *
     * @return Nonce of the derived key token
     */
    public String getNonce() {
        if (elementNonce != null) {
            Text text = getFirstNode(elementNonce);
            if (text != null) {
                return text.getData();
            }
        }
        return null;
    }

    /**
     * Returns the first text node of an element.
     *
     * @param e the element to get the node from
     * @return the first text node or <code>null</code> if node
     *         is null or is not a text node
     */
    private Text getFirstNode(Element e) {
        Node node = e.getFirstChild();
        return node != null && Node.TEXT_NODE == node.getNodeType() ? (Text) node : null;
    }

    /**
     * Returns the dom element of this <code>SecurityContextToken</code> object.
     *
     * @return the DerivedKeyToken element
     */
    public Element getElement() {
        return element;
    }

    /**
     * Returns the string representation of the token.
     *
     * @return a XML string representation
     */
    public String toString() {
        return DOM2Writer.nodeToString(element);
    }

    /**
     * Gets the id.
     *
     * @return the value of the <code>wsu:Id</code> attribute of this
     *         DerivedKeyToken
     */
    public String getID() {
        return element.getAttributeNS(WSConstants.WSU_NS, "Id");
    }

    /**
     * Set the id of this derived key token.
     *
     * @param id the value for the <code>wsu:Id</code> attribute of this
     *           DerivedKeyToken
     */
    public void setID(String id) {
        element.setAttributeNS(WSConstants.WSU_NS, WSConstants.WSU_PREFIX + ":Id", id);
    }

    /**
     * Gets the derivation algorithm
     *
     * @return the value of the <code>wsc:Algorithm</code> attribute of this
     *         DerivedKeyToken
     */
    public String getAlgorithm() {
        String algo = element.getAttributeNS(ns, "Algorithm");
        if (algo.length() == 0) {
            return ConversationConstants.DerivationAlgorithm.P_SHA_1;
        } else {
            return algo;
        }
    }

    /**
     * Create a WSDerivedKeyTokenPrincipal from this DerivedKeyToken object
     */
    public Principal createPrincipal() throws WSSecurityException {
        WSDerivedKeyTokenPrincipal principal = new WSDerivedKeyTokenPrincipal(getID());
        principal.setNonce(getNonce());
        principal.setLabel(getLabel());
        principal.setLength(getLength());
        principal.setOffset(getOffset());
        principal.setAlgorithm(getAlgorithm());

        String basetokenId = null;
        SecurityTokenReference securityTokenReference = getSecurityTokenReference();
        if (securityTokenReference != null && securityTokenReference.getReference() != null) {
            basetokenId = securityTokenReference.getReference().getURI();
            basetokenId = XMLUtils.getIDFromReference(basetokenId);
        } else if (securityTokenReference != null) {
            // KeyIdentifier
            basetokenId = securityTokenReference.getKeyIdentifierValue();
        }
        principal.setBasetokenId(basetokenId);

        return principal;
    }

    /**
     * Set the derivation algorithm of this derived key token.
     *
     * @param algo the value for the <code>Algorithm</code> attribute of this
     *             DerivedKeyToken
     */
    public void setAlgorithm(String algo) {
        if (algo != null) {
            element.setAttributeNS(ns, "Algorithm", algo);
        }
    }

    /**
     * Derive a key from this DerivedKeyToken instance
     * @param length
     * @param secret
     * @throws WSSecurityException
     */
    public byte[] deriveKey(int length, byte[] secret) throws WSSecurityException {
        try {
            byte[] nonce = org.apache.xml.security.utils.XMLUtils.decode(getNonce());
            return DerivedKeyUtils.deriveKey(getAlgorithm(), getLabel(), length, secret, nonce, getOffset());
        } catch (Exception e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILURE, e
            );
        }
    }

    @Override
    public int hashCode() {
        int result = 17;
        String algorithm = getAlgorithm();
        if (algorithm != null) {
            result = 31 * result + algorithm.hashCode();
        }
        try {
            SecurityTokenReference tokenReference = getSecurityTokenReference();
            if (tokenReference != null) {
                result = 31 * result + tokenReference.hashCode();
            }
        } catch (WSSecurityException e) {
            LOG.error(e.getMessage(), e);
        }

        Map<String, String> properties = getProperties();
        if (properties != null) {
            result = 31 * result + properties.hashCode();
        }
        int generation = getGeneration();
        if (generation != -1) {
            result = 31 * result + generation;
        }
        int offset = getOffset();
        if (offset != -1) {
            result = 31 * result + offset;
        }
        int length = getLength();
        if (length != -1) {
            result = 31 * result + length;
        }
        String label = getLabel();
        if (label != null) {
            result = 31 * result + label.hashCode();
        }
        String nonce = getNonce();
        if (nonce != null) {
            result = 31 * result + nonce.hashCode();
        }

        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DerivedKeyToken)) {
            return false;
        }
        DerivedKeyToken token = (DerivedKeyToken)object;
        if (!compare(getAlgorithm(), token.getAlgorithm())) {
            return false;
        }
        try {
            if (getSecurityTokenReference() != null
                && !getSecurityTokenReference().equals(token.getSecurityTokenReference())
                || getSecurityTokenReference() == null && token.getSecurityTokenReference() != null) {
                return false;
            }
        } catch (WSSecurityException e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
        if (!compare(getProperties(), token.getProperties())) {
            return false;
        }
        if (getGeneration() != token.getGeneration()) {
            return false;
        }
        if (getOffset() != token.getOffset()) {
            return false;
        }
        if (getLength() != token.getLength()) {
            return false;
        }
        if (!compare(getLabel(), token.getLabel())) {
            return false;
        }
        if (!compare(getNonce(), token.getNonce())) {
            return false;
        }
        return true;
    }

    private boolean compare(String item1, String item2) {
        if (item1 == null && item2 != null) {
            return false;
        } else if (item1 != null && !item1.equals(item2)) {
            return false;
        }
        return true;
    }

    private boolean compare(Map<String, String> item1, Map<String, String> item2) {
        if (item1 == null && item2 != null) {
            return false;
        } else if (item1 != null && !item1.equals(item2)) {
            return false;
        }
        return true;
    }
}
