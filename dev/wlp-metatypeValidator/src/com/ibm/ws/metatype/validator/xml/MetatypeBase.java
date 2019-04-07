/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metatype.validator.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.metatype.validator.MetatypeValidator.MetatypeOcdStats;
import com.ibm.ws.metatype.validator.MetatypeValidator.ValidityState;
import com.ibm.ws.metatype.validator.ValidatorMessage;
import com.ibm.ws.metatype.validator.ValidatorMessage.MessageType;

public abstract class MetatypeBase {
    public static final String NEW_LINE = System.getProperty("line.separator");
    protected static final String IBM_NAMESPACE = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0";
    protected static final String IBMUI_NAMESPACE = "http://www.ibm.com/xmlns/appservers/osgi/metatype/ui/v1.0.0";
    protected static final String IBMMVE_NAMESPACE = "http://www.ibm.com/xmlns/appservers/osgi/metatype/validatorExclusions/v1.0.0";

    protected static final HashMap<String, String> NAMESPACE_TO_PREFIX_MAP = new HashMap<String, String>();
    static {
        NAMESPACE_TO_PREFIX_MAP.put(IBM_NAMESPACE, "ibm");
        NAMESPACE_TO_PREFIX_MAP.put(IBMUI_NAMESPACE, "ibmui");
    }

    @XmlAnyElement
    private final List<Element> unknownElements = new LinkedList<Element>();
    @XmlAnyAttribute
    private final HashMap<QName, Object> unknownAttributes = new HashMap<QName, Object>();
    @XmlAttribute(name = "exclusions", namespace = IBMMVE_NAMESPACE)
    private String exclusions;
    private ValidityState validityState = ValidityState.NotValidated;
    private final List<ValidatorMessage> events = new LinkedList<ValidatorMessage>();
    private List<MetatypeOcdStats> ocdStats;
    private HashSet<String> nlsKeys;
    protected MetatypeRoot root;
    private HashSet<String> validatorKeys;

    protected void setRoot(MetatypeRoot root) {
        this.root = root;
    }

    public void setNlsKeys(HashSet<String> nlsKeys) {
        this.nlsKeys = nlsKeys;
    }

    protected HashSet<String> getNlsKeys() {
        return nlsKeys;
    }

    protected boolean isNlsKeyValid(String nlsKey) {
        if (nlsKeys == null)
            return false;

        return nlsKeys.contains(nlsKey);
    }

    public void setOcdStats(List<MetatypeOcdStats> ocdStats) {
        this.ocdStats = ocdStats;
    }

    protected List<MetatypeOcdStats> getOcdStats() {
        return ocdStats;
    }

    protected void logMsg(MessageType msgType, String msgId, Object... args) {
        logMsgWithContext(msgType, "", msgId, args);
    }

    protected void logMsgWithContext(MessageType msgType, String id, String msgId, Object... args) {
        ValidatorMessage event = new ValidatorMessage(msgType, id, msgId, args);
        if (!isKeyIgnored(event.getMsgKey())) {
            validationError(event, false);
        }
    }

    void validationError(ValidatorMessage event, boolean ignore) {
        if (!ignore) {
            events.add(event);
            if (event.getMsgType() == MessageType.Error)
                setValidityState(ValidityState.Failure);
            else if (event.getMsgType() == MessageType.Warning)
                setValidityState(ValidityState.Warning);
        }
    }

    boolean isKeyIgnored(String key) {
        if (exclusions == null)
            return false;

        if (validatorKeys == null) {
            validatorKeys = new HashSet<String>();

            String[] keys = exclusions.trim().split(";");
            for (String _key : keys)
                validatorKeys.add(_key.trim());
        }

        return validatorKeys.contains(key);
    }

    protected void setValidityState(ValidityState state) {
        if (state == ValidityState.Failure)
            validityState = ValidityState.Failure;
        else if (state == ValidityState.Warning && validityState == ValidityState.Pass)
            validityState = ValidityState.Warning;
        else if (state == ValidityState.Pass && validityState == ValidityState.NotValidated)
            validityState = ValidityState.Pass;
    }

    public List<ValidatorMessage> getMessages() {
        return events;
    }

    public List<ValidatorMessage> getErrorMessages() {
        List<ValidatorMessage> errorMessages = new LinkedList<ValidatorMessage>();

        for (ValidatorMessage message : events)
            if (message.getMsgType() == MessageType.Error)
                errorMessages.add(message);

        return errorMessages;
    }

    public List<ValidatorMessage> getWarningMessages() {
        List<ValidatorMessage> warningMessages = new LinkedList<ValidatorMessage>();

        for (ValidatorMessage message : events)
            if (message.getMsgType() == MessageType.Warning)
                warningMessages.add(message);

        return warningMessages;
    }

    public List<ValidatorMessage> getInfoMessages() {
        List<ValidatorMessage> infoMessages = new LinkedList<ValidatorMessage>();

        for (ValidatorMessage message : events)
            if (message.getMsgType() == MessageType.Info)
                infoMessages.add(message);

        return infoMessages;
    }

    public ValidityState getValidityState() {
        return validityState;
    }

    protected void checkIfUnknownElementsPresent() {
        Iterator<Element> iterator = unknownElements.iterator();

        while (iterator.hasNext()) {
            Element element = iterator.next();
            if ("#text".equals(element.getNodeName()))
                iterator.remove();
            else {
                logMsg(MessageType.Error, "unknown.element", "<" + element.getNodeName() + " ... />");
            }
        }
    }

    protected void checkIfUnknownAttributesPresent() {
        Iterator<Entry<QName, Object>> iterator = unknownAttributes.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<QName, Object> attribute = iterator.next();
            QName attributeName = attribute.getKey();
            Object attributeValue = attribute.getValue();
            String namespace = attributeName.getNamespaceURI();
            StringBuilder sb = new StringBuilder();

            if (namespace != null && "http://www.w3.org/2000/xmlns/".equals(namespace))
                iterator.remove();
            else {
                if (namespace != null && !namespace.isEmpty()) {
                    String prefix = NAMESPACE_TO_PREFIX_MAP.get(namespace);
                    if (prefix != null)
                        sb.append(prefix).append(':');
                    else
                        sb.append('{').append(namespace).append("}:");
                }

                sb.append(attributeName.getLocalPart()).append("=\"").append(attributeValue).append("\"");
                logMsg(MessageType.Error, "unknown.attribute", sb.toString());
            }
        }
    }

    protected final List<String> getUnknownAttributes() {
        ArrayList<String> attributes = new ArrayList<String>(unknownAttributes.size());
        for (Entry<QName, Object> attribute : unknownAttributes.entrySet())
            attributes.add(new StringBuilder(attribute.getKey().toString()).append("=\"").append(attribute.getValue()).append("\"").toString());

        return attributes;
    }

    protected final List<String> getUnknownElements(boolean simple) {
        ArrayList<String> elements = new ArrayList<String>(unknownElements.size());

        Iterator<Element> iterator = unknownElements.iterator();
        while (iterator.hasNext()) {
            Element element = iterator.next();
            StringBuilder sb = new StringBuilder("<").append(element.getNodeName());

            if (simple) {
                sb.append(" ... />");
                elements.add(sb.toString());
            } else {
                // get element attributes
                if (element.hasAttributes()) {
                    NamedNodeMap attributes = element.getAttributes();
                    for (int i = 0; i < attributes.getLength(); ++i) {
                        Node attribute = attributes.item(i);
                        String namespace = attribute.getNamespaceURI();

                        if (namespace == null || !namespace.equals("http://www.w3.org/2000/xmlns/"))
                            sb.append(' ').append(attribute.getNodeName()).append("=\"").append(attribute.getNodeValue()).append('\"');
                    }
                }

                // get child elements
                if (element.hasChildNodes()) {
                    sb.append('>').append(NEW_LINE);
                    NodeList children = element.getChildNodes();
                    for (int i = 0; i < children.getLength(); ++i) {
                        Node child = children.item(i);

                        if (!child.getNodeName().equals("#text")) {
                            traverseUnknownNode(child, sb, 2);

                            if (i + 1 != children.getLength())
                                sb.append(NEW_LINE);
                        }
                    }
                    sb.append("</").append(element.getNodeName()).append('>');
                } else
                    sb.append("/>");

                elements.add(sb.toString());
            }
        }

        return elements;
    }

    private void traverseUnknownNode(Node node, StringBuilder sb, int padLength) {
        final String pad = getPadding(padLength);
        sb.append(pad).append('<').append(node.getNodeName());

        if (node.hasAttributes()) {
            NamedNodeMap attributes = node.getAttributes();
            for (int i = 0; i < attributes.getLength(); ++i) {
                Node attribute = attributes.item(i);
                String namespace = attribute.getNamespaceURI();

                if (namespace == null || !namespace.equals("http://www.w3.org/2000/xmlns/"))
                    sb.append(' ').append(attribute.getNodeName()).append("=\"").append(attribute.getNodeValue()).append('\"');
            }
        }

        if (node.hasChildNodes()) {
            sb.append('>').append(NEW_LINE);
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); ++i) {
                Node child = childNodes.item(i);
                if (!child.getNodeName().equals("#text")) {
                    traverseUnknownNode(child, sb, padLength + 2);
                    if (i + 1 != childNodes.getLength())
                        sb.append(NEW_LINE);
                }
            }

            sb.append(NEW_LINE).append(pad).append("</").append(node.getNodeName()).append('>');
        } else
            sb.append("/>");
    }

    protected static String getPadding(int length) {
        if (length < 1)
            return "";
        else if (length == 1)
            return " ";
        else {
            StringBuilder pad = new StringBuilder(length);
            while (length > 0) {
                pad.append(' ');
                --length;
            }

            return pad.toString();
        }
    }

    public abstract void validate(boolean validateRefs);
}
