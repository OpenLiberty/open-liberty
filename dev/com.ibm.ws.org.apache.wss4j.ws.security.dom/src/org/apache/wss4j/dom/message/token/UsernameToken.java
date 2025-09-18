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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wss4j.common.bsp.BSPEnforcer;
import org.apache.wss4j.common.bsp.BSPRule;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.WSUsernameTokenPrincipalImpl;
import org.apache.wss4j.common.util.DOM2Writer;
import org.apache.wss4j.common.util.DateUtil;
import org.apache.wss4j.common.util.UsernameTokenUtil;
import org.apache.wss4j.common.util.WSCurrentTimeSource;
import org.apache.wss4j.common.util.WSTimeSource;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * UsernameToken according to WS Security specifications, UsernameToken profile.
 *
 * Enhanced to support digest password type for username token signature
 * Enhanced to support passwordless usernametokens as allowed by spec.
 */
public class UsernameToken {
    public static final String BASE64_ENCODING = WSConstants.SOAPMESSAGE_NS + "#Base64Binary";
    public static final String PASSWORD_TYPE = "passwordType";
    public static final int DEFAULT_ITERATION = 1000;
    public static final QName TOKEN =
        new QName(WSConstants.WSSE_NS, WSConstants.USERNAME_TOKEN_LN);

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(UsernameToken.class);

    private Element element;
    private Element elementUsername;
    private Element elementPassword;
    private Element elementNonce;
    private Element elementCreated;
    private Element elementSalt;
    private Element elementIteration;
    private int iteration = DEFAULT_ITERATION;
    private String passwordType;
    private boolean hashed = true;
    private boolean passwordsAreEncoded;
    private Instant created;

    /**
     * Constructs a <code>UsernameToken</code> object and parses the
     * <code>wsse:UsernameToken</code> element to initialize it.
     *
     * @param elem the <code>wsse:UsernameToken</code> element that contains
     *             the UsernameToken data
     * @param allowNamespaceQualifiedPasswordTypes whether to allow (wsse)
     *        namespace qualified password types or not (for interop with WCF)
     * @param bspEnforcer a BSPEnforcer instance to enforce BSP rules
     * @throws WSSecurityException
     */
    public UsernameToken(
        Element elem,
        boolean allowNamespaceQualifiedPasswordTypes,
        BSPEnforcer bspEnforcer
    ) throws WSSecurityException {
        element = elem;
        QName el = new QName(element.getNamespaceURI(), element.getLocalName());
        if (!el.equals(TOKEN)) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN,
                "badElement",
                new Object[] {TOKEN, el}
            );
        }
        elementUsername =
            XMLUtils.getDirectChildElement(
                element, WSConstants.USERNAME_LN, WSConstants.WSSE_NS
            );
        elementPassword =
            XMLUtils.getDirectChildElement(
                element, WSConstants.PASSWORD_LN, WSConstants.WSSE_NS
            );
        elementNonce =
            XMLUtils.getDirectChildElement(
                element, WSConstants.NONCE_LN, WSConstants.WSSE_NS
            );
        elementCreated =
            XMLUtils.getDirectChildElement(
                element, WSConstants.CREATED_LN, WSConstants.WSU_NS
            );
        elementSalt =
            XMLUtils.getDirectChildElement(
                element, WSConstants.SALT_LN, WSConstants.WSSE11_NS
            );
        elementIteration =
            XMLUtils.getDirectChildElement(
                element, WSConstants.ITERATION_LN, WSConstants.WSSE11_NS
            );
        if (elementUsername == null) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN,
                "badUsernameToken",
                new Object[] {"Username is missing"}
            );
        }
        checkBSPCompliance(bspEnforcer);
        hashed = false;
        if (elementSalt != null && (elementPassword != null || elementIteration == null)) {
            //
            // If the UsernameToken is to be used for key derivation, the (1.1)
            // spec says that it cannot contain a password, and it must contain
            // an Iteration element
            //
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN,
                "badUsernameToken",
                new Object[] {"Password is missing"}
            );
        }

        // Guard against a malicious user sending a bogus iteration value
        if (elementIteration != null) {
            String iter = XMLUtils.getElementText(elementIteration);
            if (iter != null) {
                try {
                    iteration = Integer.parseInt(iter);
                    if (iteration < 0 || iteration > 10000) {
                        throw new WSSecurityException(
                            WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN,
                            "badUsernameToken",
                            new Object[] {"Iteration is missing"}
                        );
                    }
                } catch (NumberFormatException ex) {
                    throw new WSSecurityException(
                            WSSecurityException.ErrorCode.FAILURE, ex, "decoding.general"
                    );
                }
            }
        }

        if (elementPassword != null) {
            if (elementPassword.hasAttributeNS(null, WSConstants.PASSWORD_TYPE_ATTR)) {
                passwordType = elementPassword.getAttributeNS(null, WSConstants.PASSWORD_TYPE_ATTR);
            } else if (elementPassword.hasAttributeNS(
                WSConstants.WSSE_NS, WSConstants.PASSWORD_TYPE_ATTR)
            ) {
                if (allowNamespaceQualifiedPasswordTypes) {
                    passwordType =
                        elementPassword.getAttributeNS(
                            WSConstants.WSSE_NS, WSConstants.PASSWORD_TYPE_ATTR
                        );
                } else {
                    throw new WSSecurityException(
                        WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN,
                        "badUsernameToken",
                        new Object[] {"The Password Type is not allowed to be namespace qualified"}
                    );
                }
            }

        }
        if (WSConstants.PASSWORD_DIGEST.equals(passwordType)) {
            hashed = true;
            if (elementNonce == null || elementCreated == null) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN,
                    "badUsernameToken",
                    new Object[] {"Nonce or Created is missing"}
                );
            }
        }

        if (elementCreated != null) {
            String createdString = getCreated();
            if (createdString != null && createdString.length() != 0) {
                try {
                    created = ZonedDateTime.parse(createdString).toInstant();
                } catch (DateTimeParseException e) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, e);
                }
            }
        }
    }

    /**
     * Constructs a <code>UsernameToken</code> object according to the defined
     * parameters. <p/> This constructs set the password encoding to
     * {@link WSConstants#PASSWORD_DIGEST}
     *
     * @param doc the SOAP envelope as <code>Document</code>
     */
    public UsernameToken(boolean milliseconds, Document doc) {
        this(milliseconds, doc, WSConstants.PASSWORD_DIGEST);
    }

    /**
     * Constructs a <code>UsernameToken</code> object according to the defined
     * parameters.
     *
     * @param doc the SOAP envelope as <code>Document</code>
     * @param pwType the required password encoding, either
     *               {@link WSConstants#PASSWORD_DIGEST} or
     *               {@link WSConstants#PASSWORD_TEXT} or
     *               {@link WSConstants#PW_NONE} <code>null</code> if no
     *               password required
     */
    public UsernameToken(boolean milliseconds, Document doc, String pwType) {
        this(milliseconds, doc, new WSCurrentTimeSource(), pwType);
    }

    public UsernameToken(boolean milliseconds, Document doc, WSTimeSource timeSource, String pwType) {
        element =
            doc.createElementNS(WSConstants.WSSE_NS, "wsse:" + WSConstants.USERNAME_TOKEN_LN);

        elementUsername =
            doc.createElementNS(WSConstants.WSSE_NS, "wsse:" + WSConstants.USERNAME_LN);
        elementUsername.appendChild(doc.createTextNode(""));
        element.appendChild(elementUsername);

        if (pwType != null) {
            elementPassword =
                doc.createElementNS(WSConstants.WSSE_NS, "wsse:" + WSConstants.PASSWORD_LN);
            elementPassword.appendChild(doc.createTextNode(""));
            element.appendChild(elementPassword);

            passwordType = pwType;
            if (passwordType.equals(WSConstants.PASSWORD_DIGEST)) {
                addNonce(doc);
                addCreated(milliseconds, timeSource, doc);
            } else {
                hashed = false;
            }
        }
    }

    /**
     * Add the WSSE Namespace to this UT. The namespace is not added by default for
     * efficiency purposes.
     */
    public void addWSSENamespace() {
        XMLUtils.setNamespace(element, WSConstants.WSSE_NS, WSConstants.WSSE_PREFIX);
    }

    /**
     * Add the WSU Namespace to this UT. The namespace is not added by default for
     * efficiency purposes.
     */
    public void addWSUNamespace() {
        element.setAttributeNS(XMLUtils.XMLNS_NS, "xmlns:" + WSConstants.WSU_PREFIX, WSConstants.WSU_NS);
    }

    /**
     * Creates and adds a Nonce element to this UsernameToken
     */
    public void addNonce(Document doc) {
        if (elementNonce != null) {
            return;
        }
        byte[] nonceValue = null;
        try {
            nonceValue = WSSecurityUtil.generateNonce(16);
        } catch (WSSecurityException ex) {
            LOG.debug(ex.getMessage(), ex);
            return;
        }
        elementNonce = doc.createElementNS(WSConstants.WSSE_NS, "wsse:" + WSConstants.NONCE_LN);
        elementNonce.appendChild(doc.createTextNode(org.apache.xml.security.utils.XMLUtils.encodeToString(nonceValue)));
        elementNonce.setAttributeNS(null, "EncodingType", BASE64_ENCODING);
        element.appendChild(elementNonce);
    }

    /**
     * Creates and adds a Created element to this UsernameToken
     */
    public void addCreated(boolean milliseconds, Document doc) {
        addCreated(milliseconds, new WSCurrentTimeSource(), doc);
    }

    /**
     * Creates and adds a Created element to this UsernameToken
     */
    public void addCreated(boolean milliseconds, WSTimeSource timeSource, Document doc) {
        if (elementCreated != null) {
            return;
        }
        elementCreated =
            doc.createElementNS(
                WSConstants.WSU_NS, WSConstants.WSU_PREFIX + ":" + WSConstants.CREATED_LN
            );
        Instant currentTime = timeSource.now();

        DateTimeFormatter formatter = DateUtil.getDateTimeFormatter(milliseconds);
        elementCreated.appendChild(doc.createTextNode(currentTime.atZone(ZoneOffset.UTC).format(formatter)));
        element.appendChild(elementCreated);
    }

    /**
     * Adds a Salt element to this UsernameToken.
     *
     * @param doc The Document for the UsernameToken
     * @param saltValue The salt to add.
     * @param mac If <code>true</code> then an optionally generated value is
     *            usable for a MAC
     */
    public void addSalt(Document doc, byte[] saltValue, boolean mac) {
        elementSalt =
            doc.createElementNS(
                WSConstants.WSSE11_NS, WSConstants.WSSE11_PREFIX + ":" + WSConstants.SALT_LN
            );
        XMLUtils.setNamespace(element, WSConstants.WSSE11_NS, WSConstants.WSSE11_PREFIX);
        elementSalt.appendChild(doc.createTextNode(org.apache.xml.security.utils.XMLUtils.encodeToString(saltValue)));
        element.appendChild(elementSalt);
    }

    /**
     * Creates and adds a Iteration element to this UsernameToken
     */
    public void addIteration(Document doc, int iteration) {
        String text = "" + iteration;
        elementIteration =
            doc.createElementNS(
                WSConstants.WSSE11_NS, WSConstants.WSSE11_PREFIX + ":" + WSConstants.ITERATION_LN
            );
        XMLUtils.setNamespace(element, WSConstants.WSSE11_NS, WSConstants.WSSE11_PREFIX);
        elementIteration.appendChild(doc.createTextNode(text));
        element.appendChild(elementIteration);
        this.iteration = iteration;
    }

    /**
     * Get the user name.
     *
     * @return the data from the user name element.
     */
    public String getName() {
        return XMLUtils.getElementText(elementUsername);
    }

    /**
     * Set the user name.
     *
     * @param name sets a text node containing the use name into the user name
     *             element.
     */
    public void setName(String name) {
        Text node = getFirstNode(elementUsername);
        node.setData(name);
    }

    /**
     * Get the nonce.
     *
     * @return the data from the nonce element.
     */
    public String getNonce() {
        return XMLUtils.getElementText(elementNonce);
    }

    /**
     * Get the created timestamp.
     *
     * @return the data from the created time element.
     */
    public String getCreated() {
        return XMLUtils.getElementText(elementCreated);
    }

    /**
     * Return the Created Element as a Date object
     * @return the Created Date
     */
    public Instant getCreatedDate() {
        return created;
    }

    /**
     * Gets the password string. This is the password as it is in the password
     * element of a username token. Thus it can be either plain text or the
     * password digest value.
     *
     * @return the password string or <code>null</code> if no such node exists.
     */
    public String getPassword() {
        String password = XMLUtils.getElementText(elementPassword);
        // See WSS-219
        if (password == null && elementPassword != null) {
            return "";
        }
        return password;
    }

    /**
     * Return true if this UsernameToken contains a Password element
     */
    public boolean containsPasswordElement() {
        return elementPassword != null;
    }

    /**
     * Get the Salt value of this UsernameToken.
     *
     * @return Returns the binary Salt value or <code>null</code> if no Salt
     *         value is available in the username token.
     * @throws WSSecurityException
     */
    public byte[] getSalt() throws WSSecurityException {
        String salt = XMLUtils.getElementText(elementSalt);
        if (salt != null) {
            return org.apache.xml.security.utils.XMLUtils.decode(salt);
        }
        return null;
    }

    /**
     * Get the Iteration value of this UsernameToken.
     *
     * @return Returns the Iteration value. If no Iteration was specified in the
     *         username token the default value according to the specification
     *         is returned.
     */
    public int getIteration() {
        return iteration;
    }

    /**
     * Get the hashed indicator. If the indicator is <code>true> the password of the
     * <code>UsernameToken</code> was encoded using {@link WSConstants#PASSWORD_DIGEST}
     *
     * @return the hashed indicator.
     */
    public boolean isHashed() {
        return hashed;
    }

    /**
     * @return Returns the passwordType.
     */
    public String getPasswordType() {
        return passwordType;
    }

    /**
     * Sets the password string. This function sets the password in the
     * <code>UsernameToken</code> either as plain text or encodes the password
     * according to the WS Security specifications, UsernameToken profile, into
     * a password digest.
     *
     * @param pwd the password to use
     */
    public void setPassword(String pwd) {
        if (pwd == null) {
            if (passwordType != null) {
                throw new IllegalArgumentException("pwd == null but a password is needed");
            } else {
                // Ignore setting the password.
                return;
            }
        }

        Text node = getFirstNode(elementPassword);
        try {
            if (hashed) {
                byte[] decodedNonce = org.apache.xml.security.utils.XMLUtils.decode(getNonce());
                if (passwordsAreEncoded) {
                    node.setData(UsernameTokenUtil.doPasswordDigest(decodedNonce, getCreated(),
                                                  org.apache.xml.security.utils.XMLUtils.decode(pwd)));
                } else {
                    node.setData(UsernameTokenUtil.doPasswordDigest(decodedNonce, getCreated(), pwd));
                }
            } else {
                node.setData(pwd);
            }
            if (passwordType != null) {
                elementPassword.setAttributeNS(null, "Type", passwordType);
            }
        } catch (Exception e) {
            LOG.debug(e.getMessage(), e);
        }
    }

    /**
     * @param passwordsAreEncoded whether passwords are encoded
     */
    public void setPasswordsAreEncoded(boolean passwordsAreEncoded) {
        this.passwordsAreEncoded = passwordsAreEncoded;
    }

    /**
     * @return whether passwords are encoded
     */
    public boolean getPasswordsAreEncoded() {
        return passwordsAreEncoded;
    }

    /**
     * Returns the first text node of an element.
     *
     * @param e the element to get the node from
     * @return the first text node or <code>null</code> if node is null or is
     *         not a text node
     */
    private Text getFirstNode(Element e) {
        Node node = e.getFirstChild();
        return node != null && Node.TEXT_NODE == node.getNodeType() ? (Text) node : null;
    }

    /**
     * Returns the dom element of this <code>UsernameToken</code> object.
     *
     * @return the <code>wsse:UsernameToken</code> element
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
     * @return the value of the <code>wsu:Id</code> attribute of this username
     *         token
     */
    public String getID() {
        return element.getAttributeNS(WSConstants.WSU_NS, "Id");
    }

    /**
     * Set the id of this username token.
     *
     * @param id
     *            the value for the <code>wsu:Id</code> attribute of this
     *            username token
     */
    public void setID(String id) {
        element.setAttributeNS(WSConstants.WSU_NS, WSConstants.WSU_PREFIX + ":Id", id);
    }

    /**
     * This method gets a derived key as defined in WSS Username Token Profile.
     *
     * @param rawPassword The raw password to use to derive the key
     * @return Returns the derived key as a byte array
     * @throws WSSecurityException
     */
    public byte[] getDerivedKey(BSPEnforcer bspEnforcer, String rawPassword) throws WSSecurityException {
        if (rawPassword == null) {
            LOG.warn("The raw password was null");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }

        if (elementSalt == null) {
            // We must have a salt element to use this token for a derived key
            bspEnforcer.handleBSPRule(BSPRule.R4217);
        }
        if (elementIteration == null) {
            // we must have an iteration element to use this token for a derived key
            bspEnforcer.handleBSPRule(BSPRule.R4218);
        } else {
            String iter = XMLUtils.getElementText(elementIteration);
            try {
                if (iter == null || Integer.parseInt(iter) < 1000) {
                    bspEnforcer.handleBSPRule(BSPRule.R4218);
                }
            } catch (NumberFormatException ex) {
                throw new WSSecurityException(
                        WSSecurityException.ErrorCode.FAILURE, ex, "decoding.general"
                );
            }
        }

        int iteration = getIteration();
        byte[] salt = getSalt();
        if (passwordsAreEncoded) {
            return UsernameTokenUtil.generateDerivedKey(org.apache.xml.security.utils.XMLUtils.decode(rawPassword),
                                                        salt, iteration);
        } else {
            return UsernameTokenUtil.generateDerivedKey(rawPassword, salt, iteration);
        }
    }

    /**
     * Return whether the UsernameToken represented by this class is to be used
     * for key derivation as per the UsernameToken Profile 1.1. It does this by
     * checking that the username token has salt and iteration values.
     *
     * @throws WSSecurityException
     */
    public boolean isDerivedKey() throws WSSecurityException {
        if (elementSalt != null && elementIteration != null) {
            return true;
        }
        return false;
    }

    /**
     * Create a WSUsernameTokenPrincipal from this UsernameToken object
     */
    public Principal createPrincipal() throws WSSecurityException {
        WSUsernameTokenPrincipalImpl principal =
            new WSUsernameTokenPrincipalImpl(getName(), isHashed());
        String nonce = getNonce();
        if (nonce != null) {
            principal.setNonce(org.apache.xml.security.utils.XMLUtils.decode(nonce));
        }
        principal.setPassword(getPassword());
        principal.setCreatedTime(getCreated());
        return principal;
    }

    /**
     * Return true if the "Created" value is before the current time minus the timeToLive
     * argument, and if the Created value is not "in the future".
     *
     * @param timeToLive the value in seconds for the validity of the Created time
     * @param futureTimeToLive the value in seconds for the future validity of the Created time
     * @return true if the UsernameToken is before (now-timeToLive), false otherwise
     */
    public boolean verifyCreated(
        int timeToLive,
        int futureTimeToLive
    ) {
        return DateUtil.verifyCreated(created, timeToLive, futureTimeToLive);
    }

    @Override
    public int hashCode() {
        int result = 17;
        String username = getName();
        if (username != null) {
            result = 31 * result + username.hashCode();
        }
        String password = getPassword();
        if (password != null) {
            result = 31 * result + password.hashCode();
        }
        String passwordType = getPasswordType();
        if (passwordType != null) {
            result = 31 * result + passwordType.hashCode();
        }
        String nonce = getNonce();
        if (nonce != null) {
            result = 31 * result + nonce.hashCode();
        }
        String created = getCreated();
        if (created != null) {
            result = 31 * result + created.hashCode();
        }
        try {
            byte[] salt = getSalt();
            if (salt != null) {
                result = 31 * result + Arrays.hashCode(salt);
            }
        } catch (WSSecurityException ex) {
            LOG.debug(ex.getMessage(), ex);
        }
        result = 31 * result + Integer.valueOf(getIteration()).hashCode();

        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UsernameToken)) {
            return false;
        }
        UsernameToken usernameToken = (UsernameToken)object;
        if (!compare(usernameToken.getName(), getName())) {
            return false;
        }
        if (!compare(usernameToken.getPassword(), getPassword())) {
            return false;
        }
        if (!compare(usernameToken.getPasswordType(), getPasswordType())) {
            return false;
        }
        if (!compare(usernameToken.getNonce(), getNonce())) {
            return false;
        }
        if (!compare(usernameToken.getCreated(), getCreated())) {
            return false;
        }
        try {
            byte[] salt = usernameToken.getSalt();
            if (!Arrays.equals(salt, getSalt())) {
                return false;
            }
        } catch (WSSecurityException ex) {
            LOG.debug(ex.getMessage(), ex);
        }
        int iteration = usernameToken.getIteration();
        if (iteration != getIteration()) {
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

    /**
     * A method to check that the UsernameToken is compliant with the BSP spec.
     * @throws WSSecurityException
     */
    private void checkBSPCompliance(BSPEnforcer bspEnforcer) throws WSSecurityException {
        List<Element> passwordElements =
            WSSecurityUtil.getDirectChildElements(
                element, WSConstants.PASSWORD_LN, WSConstants.WSSE_NS
            );
        // We can only have one password element
        if (passwordElements.size() > 1) {
            LOG.debug("The Username Token had more than one password element");
            bspEnforcer.handleBSPRule(BSPRule.R4222);
        }

        // We must have a password type
        if (passwordElements.size() == 1) {
            Element passwordChild = passwordElements.get(0);
            String type = passwordChild.getAttributeNS(null, WSConstants.PASSWORD_TYPE_ATTR);
            if (type == null || type.length() == 0) {
                LOG.debug("The Username Token password does not have a Type attribute");
                bspEnforcer.handleBSPRule(BSPRule.R4201);
            }
        }

        List<Element> createdElements =
            WSSecurityUtil.getDirectChildElements(
                element, WSConstants.CREATED_LN, WSConstants.WSU_NS
            );
        // We can only have one created element
        if (createdElements.size() > 1) {
            LOG.debug("The Username Token has more than one created element");
            bspEnforcer.handleBSPRule(BSPRule.R4223);
        }

        List<Element> nonceElements =
            WSSecurityUtil.getDirectChildElements(
                element, WSConstants.NONCE_LN, WSConstants.WSSE_NS
            );
        // We can only have one nonce element
        if (nonceElements.size() > 1) {
            LOG.debug("The Username Token has more than one nonce element");
            bspEnforcer.handleBSPRule(BSPRule.R4225);
        }

        if (nonceElements.size() == 1) {
            Element nonce = nonceElements.get(0);
            String encodingType = nonce.getAttributeNS(null, "EncodingType");
            // Encoding Type must be equal to Base64Binary
            if (encodingType == null || encodingType.length() == 0) {
                bspEnforcer.handleBSPRule(BSPRule.R4220);
            } else if (!WSConstants.BASE64_ENCODING.equals(encodingType)) {
                LOG.debug("The Username Token's nonce element has a bad encoding type");
                bspEnforcer.handleBSPRule(BSPRule.R4221);
            }
        }
    }
}
