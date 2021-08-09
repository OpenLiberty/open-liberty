/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.xml.internal.XMLConfigParser.MergeBehavior;

@Trivial
abstract class ConfigElement {

    public enum MERGE_OP {
        APPEND, SET
    };

    private final String nodeName;
    protected final GroupHashMap attributes;

    private int sequenceId;
    private String location;
    private String overrideLocation;
    private final Map<String, MERGE_OP> operations;
    private boolean isTextOnly = true; //assume non-nested config by default
    private String elementValue = "";

    private ConfigElement parent;
    private List<ConfigElement> children = new ArrayList<ConfigElement>();
    protected String childAttributeName;
    protected MergeBehavior mergeBehavior;

    private LinkedList<String> docLocationStack;
    private LinkedList<MergeBehavior> behaviorStack;

    /**
     * @return the elementValue
     */
    public String getElementValue() {
        return elementValue;
    }

    /**
     * @param elementValue the elementValue to set
     */
    public void setElementValue(String elementValue) {
        this.elementValue = elementValue;
    }

    /**
     * @return the isTextOnly
     */
    public boolean isTextOnly() {
        return isTextOnly;
    }

    /**
     * @param isTextOnly the isTextOnly to set
     */
    public void setTextOnly(boolean isTextOnly) {
        this.isTextOnly = isTextOnly;
    }

    protected ConfigElement(String nodeName) {
        if (nodeName == null) {
            throw new NullPointerException("Configuration id must be specified");
        }
        this.nodeName = nodeName;
        this.operations = new HashMap<String, MERGE_OP>();
        this.attributes = new GroupHashMap();
    }

    public ConfigElement(ConfigElement element) {
        this.nodeName = element.nodeName;
        this.location = element.location;
        this.operations = new HashMap<String, MERGE_OP>(element.operations);
        this.attributes = new GroupHashMap(element.attributes);
        this.parent = element.parent;
        this.children = element.children;
        for (ConfigElement child : children) {
            child.setParent(this);
        }
        this.childAttributeName = element.childAttributeName;
        this.mergeBehavior = element.mergeBehavior;
        this.docLocationStack = element.docLocationStack;
        this.behaviorStack = element.behaviorStack;

    }

    public String getNodeName() {
        return nodeName;
    }

    public abstract boolean isSimple();

    public abstract String getId();

    public void setSequenceId(int sequenceId) {
        this.sequenceId = sequenceId;
    }

    public int getSequenceId() {
        return sequenceId;
    }

    public void setDocumentLocation(String location) {
        this.location = location;
    }

    public String getDocumentLocation() {
        return location;
    }

    public String getMergedLocation() {
        if (overrideLocation == null || overrideLocation.equals("")) {
            return location;
        } else {
            return overrideLocation;
        }
    }

    public boolean hasNestedElements() {
        return !children.isEmpty();
    }

    public String getRefAttr() {
        Object attribute = attributes.get(XMLConfigConstants.CFG_CONFIG_REF);
        if (attribute != null && attribute instanceof String) {
            return (String) attribute;
        } else {
            return null;
        }
    }

    /**
     *
     * @return If id is null it returns nodeName, or if id is not null, it returns nodeName with id in the end.
     */
    public String getFullId() {
        return getConfigID().toString();
    }

    public ConfigID getConfigID() {
        if (getParent() == null)
            return new ConfigID(nodeName, getId());
        else
            return new ConfigID(getParent().getConfigID(), nodeName, getId(), childAttributeName);
    }

    public void setMergeOperation(String name, MERGE_OP mergeOperation) {
        MERGE_OP operation = operations.put(name, mergeOperation);
        if (operation != mergeOperation) {
            // TODO: log warning: two different operations specified for the
            // same attribute
        }
    }

    private MERGE_OP getMergeOperation(String name) {
        return operations.get(name);
    }

    public boolean containsAttribute(String name) {
        return attributes.containsKey(name);
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    /**
     * Add a single-valued attribute.
     */
    public void addAttribute(String name, String value) {
        checkType(name, String.class);
        attributes.put(name, value);
    }

    /**
     * Add a multi-valued attribute.
     */
    public void addCollectionAttribute(String name, Object value) {
        List<Object> attributeValues = getCollectionAttribute(name);
        attributeValues.add(value);
    }

    private List<Object> getCollectionAttribute(String name) {
        checkType(name, List.class);
        @SuppressWarnings("unchecked")
        List<Object> attributeValues = (List<Object>) attributes.get(name);
        if (attributeValues == null) {
            attributeValues = new ArrayList<Object>(1);
            attributes.put(name, attributeValues);
        }
        return attributeValues;
    }

    public void addReference(String name, String id) {
        List<Object> attributeValues = getCollectionAttribute(name);
        attributeValues.add(new Reference(name, id));
    }

    public void addChildConfigElement(String name, SimpleElement configElement) {
        List<Object> attributeValues = getCollectionAttribute(name);
        attributeValues.add(configElement);
        children.add(configElement);
        configElement.setParent(this);
        configElement.setChildAttributeName(name);
    }

    /**
     * @param name
     */
    protected void setChildAttributeName(String name) {
        this.childAttributeName = name;

    }

    private void checkType(String name, Class<?> type) {
        Object attributeValue = attributes.get(name);
        if (attributeValue != null && !type.isInstance(attributeValue)) {
            throw new IllegalArgumentException("Configuration property '" + name + "' seems to be specified as an attribute and an element");
        }
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    protected void override(ConfigElement in) {
        MERGE_OP defaultOperation = MERGE_OP.APPEND;
        boolean sameLocation = in.getDocumentLocation().equals(getDocumentLocation());

        if (!sameLocation) {
            if (in.mergeBehavior == MergeBehavior.IGNORE) {
                return;
            } else if (in.mergeBehavior == MergeBehavior.REPLACE) {
                defaultOperation = MERGE_OP.SET;
            }
        }
        for (Map.Entry<String, Object> entry : in.attributes.entrySet()) {
            MERGE_OP operation = in.getMergeOperation(entry.getKey());
            if (operation == null)
                operation = defaultOperation;
            Object value = entry.getValue();
            if (operation == MERGE_OP.APPEND && value instanceof List) {
                List<Object> attributeValues = getCollectionAttribute(entry.getKey());
                @SuppressWarnings("unchecked")
                List<Object> values = (List<Object>) value;
                attributeValues.addAll(values);
            } else {
                setAttribute(entry.getKey(), value);
            }
        }

        children.addAll(in.getChildren());
        parent = in.getParent();
        childAttributeName = in.childAttributeName;
        overrideLocation = in.location;
    }

    public void setIdAttribute() {
        if (getId() != null) {
            if (getId().startsWith("default-")) {
                attributes.put(XMLConfigConstants.CFG_INSTANCE_ID, getFullId());
            } else {
                attributes.put(XMLConfigConstants.CFG_INSTANCE_ID, getId());
            }
        }
    }

    public boolean isEnabled() {
        Object enabled = getAttribute(XMLConfigConstants.CONFIG_ENABLED_ATTRIBUTE);
        if (enabled instanceof String && "false".equalsIgnoreCase((String) enabled)) {
            return false;
        } else {
            return true;
        }
    }

    boolean equalsIgnoreIdAttr(ConfigElement other) {
        if (this == other)
            return true;
        if ((elementValue == null && other.elementValue != null) || (elementValue != null && !elementValue.equals(other.elementValue)))
            return false;
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String key = entry.getKey();
            if ("id".equals(key))
                continue;
            Object value = entry.getValue();
            Object otherValue = other.attributes.get(key);
            if ((value == null && otherValue != null) || (value != null && !value.equals(otherValue)))
                return false;
        }
        for (Map.Entry<String, Object> entry : other.attributes.entrySet()) {
            String otherKey = entry.getKey();
            if ("id".equals(otherKey))
                continue;
            Object otherValue = entry.getValue();
            Object thisValue = attributes.get(otherKey);
            if ((otherValue == null && thisValue != null) || (otherValue != null && !otherValue.equals(thisValue)))
                return false;
        }
        return ((children == null && other.children == null) ||
                (children != null && children.equals(other.children)))
               &&
               ((getId() == null && other.getId() == null) || (getId() != null && getId().equals(other.getId()))) &&
               isTextOnly == other.isTextOnly &&
               ((location == null && other.location == null) || (location != null && location.equals(other.location))) &&
               ((nodeName == null && other.nodeName == null) || (nodeName != null && nodeName.equals(other.nodeName))) &&
               operations.equals(other.operations) && sequenceId == other.sequenceId;
    }

    static Comparator<String> CASE_INSENSITIVE = new Comparator<String>() {

        @Override
        @Trivial
        public int compare(String s1, String s2) {
            if (s1 == s2) {
                return 0;
            }
            if (s1 == null)
                return 1;

            if (s2 == null)
                return -1;

            return s1.compareToIgnoreCase(s2);
        }

    };

    @Trivial
    static class GroupHashMap extends TreeMap<String, Object> {
        private static final long serialVersionUID = 3632130989008067578L;

        public GroupHashMap() {
            super(CASE_INSENSITIVE);
        }

        public GroupHashMap(GroupHashMap map) {
            super(CASE_INSENSITIVE);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> values = (List<Object>) value;
                    value = new ArrayList<Object>(values);
                }
                put(entry.getKey(), value);
            }
        }
    }

    @Trivial
    public static class Reference extends ConfigID {

        private static final long serialVersionUID = 6931519568553134827L;

        public Reference(String pid, String id) {
            super(pid, id);
        }

    }

    @Trivial
    public static class ConfigElementComparator implements Comparator<ConfigElement> {

        public static final ConfigElementComparator INSTANCE = new ConfigElementComparator();

        @Override
        public int compare(ConfigElement object1, ConfigElement object2) {
            return object1.getSequenceId() - object2.getSequenceId();
        }

    }

    /**
     * @param configElement
     */
    void setParent(ConfigElement configElement) {
        this.parent = configElement;
    }

    public ConfigElement getParent() {
        return this.parent;
    }

    public List<ConfigElement> getChildren() {
        return this.children;
    }

    public String getDisplayId() {
        StringBuffer displayId = new StringBuffer();
        if (getParent() != null) {
            displayId.append(getParent().getDisplayId());
            displayId.append('/');
        }

        displayId.append(getNodeDisplayName());

        if (getId() != null) {
            displayId.append('[');
            displayId.append(getId());
            displayId.append(']');
        }

        return displayId.toString();
    }

    /**
     * This should match the original XML value. This is intended to be used in messages
     *
     * @return the node name to use as a display name
     */
    protected String getNodeDisplayName() {
        return this.nodeName;
    }

    /**
     * The logic for merge has, unfortunately, gotten a bit unwieldy. Previously we were able to simply override in order
     * of the sequence number to support a "last one wins" policy. We kept the same logic when we added onConflict values
     * of REPLACE/IGNORE. Everything was fine with that as long as you're only dealing with one level of includes. If you
     * had multiple levels, a nested 'REPLACE' or 'MERGE' could replace a top level element even though the nested include's
     * parent was included with 'IGNORE'.
     *
     * So, now we have complicated logic. First we flatten all conflict subtrees in the list of elements using the following rules:
     *
     * 1. If the element preceding the current element in the conflict list is from the same file, merge the current element with
     * the previous element.
     *
     * 2. If the element preceding the current element in the conflict list is the current element's direct parent, we use the merge
     * behavior specified on the element to emerge the previous and current elements.
     *
     * 3. If the preceding element is an ancestor of the current element, we use the merge behavior from the ancestor's child to merge the
     * current element with the preceding element.
     *
     * 4. Otherwise, the two elements are not related, so no flattening is done. We add the current element to the flattened list of elements.
     *
     * After everything has been flattened, we can go through each element in the list and call override.
     *
     * There are probably better ways to handle this, but they would require restructuring the way we parse and store configuration.
     */
    protected void merge(List<? extends ConfigElement> elements) {
        if (elements.size() > 1) {
            Collections.sort(elements, ConfigElementComparator.INSTANCE);
        }

        ConfigElement[] elementArray = elements.toArray(new ConfigElement[elements.size()]);

        LinkedList<ConfigElement> flattened = new LinkedList<ConfigElement>();
        flattened.add(elementArray[elements.size() - 1]);
        for (int i = (elements.size() - 1); i > 0; i--) {
            ConfigElement element = new SimpleElement(elementArray[i - 1]);
            ConfigElement previous = flattened.getLast();
            if (element.getDocumentLocation().equals(previous.getDocumentLocation())) {
                // Same document, just merge
                element.override(previous);
                flattened.removeLast();
                flattened.add(element);
            } else if (previous.getParentDocumentLocation() != null && previous.getParentDocumentLocation().equals(element.getDocumentLocation())) {
                // 'element' is in the immediate parent document of 'previous'. Just override using the merge behavior that's already specified
                // on 'previous'
                element.override(previous);
                flattened.removeLast();
                flattened.add(element);
            } else if (previous.docLocationStack.contains(element.getDocumentLocation())) {
                // If 'element' is in the hierarchy that included 'previous', 'previous' needs to use the merge behavior specified
                // in the include statement in the document that contains 'element'.
                int idx = previous.docLocationStack.indexOf(element.getDocumentLocation());
                previous.mergeBehavior = previous.behaviorStack.get(idx + 1);
                element.override(previous);
                flattened.removeLast();
                flattened.add(element);
            } else {
                // Conflicting element not in stack, add for later merging
                flattened.add(element);

            }
        }

        for (int i = flattened.size(); i > 0; i--) {
            override(flattened.get(i - 1));
        }

    }

    protected void setMergeBehavior(MergeBehavior mb) {
        this.mergeBehavior = mb;
    }

    /**
     * @return
     */
    private Object getParentDocumentLocation() {
        if (docLocationStack.size() == 1)
            return null;

        return docLocationStack.get(docLocationStack.size() - 2);
    }

    /**
     * @param clone
     */
    public void setBehaviorStack(LinkedList<MergeBehavior> clone) {
        this.behaviorStack = clone;

    }

    /**
     * @param clone
     */
    public void setDocLocationStack(LinkedList<String> clone) {
        this.docLocationStack = clone;

    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getName());
        builder.append("[").append(getFullId()).append("]");
        return builder.toString();
    }

}
