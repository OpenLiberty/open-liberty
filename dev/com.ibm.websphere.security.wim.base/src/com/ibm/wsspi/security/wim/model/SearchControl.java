/*******************************************************************************
 * Copyright (c) 2017,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.wim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for SearchControl complex type.
 *
 * <p> The SearchControl object extends the {@link PropertyControl} object.
 *
 * <p>Below is a list of supported properties for {@link SearchControl}.
 *
 * <ul>
 * <li><b>countLimit</b>: used to specify the number of results to return from the search call. If the actual number of
 * search results is more than the <b>countLimit</b>, the <b>hasMoreResults</b> property in the {@link SearchResponseControl}
 * will be set to true.</li>
 * <li><b>searchLimit</b>: used to specify the maximum number of search results that may be returned by the search operation.</li>
 * <li><b>timeLimit</b>: specifies the maximum number of milliseconds the search is allowed to take if a repository
 * supports such a parameter.</li>
 * <li><b>expression</b>: the search expression in XPath format.</li>
 * <li><b>searchBases</b>: a list of the search bases to narrow down the search. For example, 'ou=Mahwah, o=mycompany, c=us'
 * could be specified in 'searchBases' to only search for users in the Mahwah division in the United States.</li>
 * <li><b>returnSubType</b>: whether to return sub-types of the any entity types specified in 'expression'.</li>
 * </ul>
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link PropertyControl} and its
 * super-classes are supported.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = SearchControl.TYPE_NAME, propOrder = {
                                                       "searchBases"
})
@XmlSeeAlso({
              LoginControl.class,
              ChangeControl.class,
              HierarchyControl.class
})
public class SearchControl extends PropertyControl {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "SearchControl";

    /** Property name constant for the <b>searchBases</b> property. */
    private static final String PROP_SEARCH_BASES = "searchBases";

    /** Property name constant for the <b>countLimit</b> property. */
    private static final String PROP_COUNT_LIMIT = "countLimit";

    /** Property name constant for the <b>searchLimit</b> property. */
    private static final String PROP_SEARCH_LIMIT = "searchLimit";

    /** Property name constant for the <b>timeLimit</b> property. */
    private static final String PROP_TIME_LIMIT = "timeLimit";

    /** Property name constant for the <b>expression</b> property. */
    private static final String PROP_EXPRESSION = "expression";

    /** Property name constant for the <b>returnSubType</b> property. */
    private static final String PROP_RETURN_SUB_TYPE = "returnSubType";

    /**
     * A list of the search bases to narrow down the search. For example, 'ou=Mahwah, o=mycompany, c=us'
     * could be specified in 'searchBases' to only search for users in the Mahwah division in the United States.
     */
    @XmlElement(name = PROP_SEARCH_BASES)
    protected List<String> searchBases;

    /**
     * The number of results to return from the search call. If the actual number of
     * search results is more than the <b>countLimit</b>, the <b>hasMoreResults</b>
     * property in the {@link SearchResponseControl} will be set to true.
     */
    @XmlAttribute(name = PROP_COUNT_LIMIT)
    protected Integer countLimit;

    /**
     * The maximum number of search results that may be returned by the search operation.
     */
    @XmlAttribute(name = PROP_SEARCH_LIMIT)
    protected Integer searchLimit;

    /**
     * The maximum number of milliseconds the search is allowed to take if a repository
     * supports such a parameter.
     */
    @XmlAttribute(name = PROP_TIME_LIMIT)
    protected Integer timeLimit;

    /**
     * The search expression in XPath format.
     */
    @XmlAttribute(name = PROP_EXPRESSION)
    protected String expression;

    /** Whether to return sub-types of the any entity types specified in 'expression'. */
    @XmlAttribute(name = PROP_RETURN_SUB_TYPE)
    protected Boolean returnSubType;

    /** The list of properties that comprise this entity. */
    private static List<String> propertyNames = null;

    /** A mapping of property names to data types. */
    private static HashMap<String, String> dataTypeMap = null;

    /** A list of super-types of this entity. */
    private static ArrayList<String> superTypeList = null;

    /** A set of sub-types of this entity. */
    private static HashSet<String> subTypeSet = null;

    static {
        setDataTypeMap();
        setSuperTypes();
        setSubTypes();
    }

    /**
     * Gets the value of the <b>searchBases</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>searchBases</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getSearchBases().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getSearchBases() {
        if (searchBases == null) {
            searchBases = new ArrayList<String>();
        }
        return this.searchBases;
    }

    /**
     * Returns true if the <b>searchBases</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetSearchBases() {
        return ((this.searchBases != null) && (!this.searchBases.isEmpty()));
    }

    /**
     * Unset the <b>searchBases</b> property.
     */
    public void unsetSearchBases() {
        this.searchBases = null;
    }

    /**
     * Gets the value of the <b>countLimit</b> property.
     *
     * @return
     *         possible object is {@link Integer }
     */
    public int getCountLimit() {
        if (countLimit == null) {
            return 0;
        } else {
            return countLimit;
        }
    }

    /**
     * Sets the value of the <b>countLimit</b> property.
     *
     * @param value
     *            allowed object is {@link Integer }
     */
    public void setCountLimit(int value) {
        this.countLimit = value;
    }

    /**
     * Returns true if the <b>countLimit</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetCountLimit() {
        return (this.countLimit != null);
    }

    /**
     * Unset the <b>countLimit</b> property.
     */
    public void unsetCountLimit() {
        this.countLimit = null;
    }

    /**
     * Gets the value of the <b>searchLimit</b> property.
     *
     * @return
     *         possible object is {@link Integer }
     */
    public int getSearchLimit() {
        if (searchLimit == null) {
            return 0;
        } else {
            return searchLimit;
        }
    }

    /**
     * Sets the value of the <b>searchLimit</b> property.
     *
     * @param value
     *            allowed object is {@link Integer }
     */
    public void setSearchLimit(int value) {
        this.searchLimit = value;
    }

    /**
     * Returns true if the <b>searchLimit</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetSearchLimit() {
        return (this.searchLimit != null);
    }

    /**
     * Unset the <b>searchLimit</b> property.
     */
    public void unsetSearchLimit() {
        this.searchLimit = null;
    }

    /**
     * Gets the value of the timeLimit property.
     *
     * @return
     *         possible object is {@link Integer }
     */
    public int getTimeLimit() {
        if (timeLimit == null) {
            return 0;
        } else {
            return timeLimit;
        }
    }

    /**
     * Sets the value of the timeLimit property.
     *
     * @param value
     *            allowed object is {@link Integer }
     */
    public void setTimeLimit(int value) {
        this.timeLimit = value;
    }

    /**
     * Returns true if the <b>timeLimit</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetTimeLimit() {
        return (this.timeLimit != null);
    }

    /**
     * Unset the <b>timeLimit</b> property.
     */
    public void unsetTimeLimit() {
        this.timeLimit = null;
    }

    /**
     * Gets the value of the <b>expression</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Sets the value of the <b>expression</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setExpression(String value) {
        this.expression = value;
    }

    /**
     * Returns true if the <b>expression</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetExpression() {
        return (this.expression != null);
    }

    /**
     * Gets the value of the <b>returnSubType</b> property.
     *
     * @return
     *         possible object is {@link Boolean }
     */
    public boolean isReturnSubType() {
        if (returnSubType == null) {
            return true;
        } else {
            return returnSubType;
        }
    }

    /**
     * Sets the value of the <b>returnSubType</b> property.
     *
     * @param value
     *            allowed object is {@link Boolean }
     */
    public void setReturnSubType(boolean value) {
        this.returnSubType = value;
    }

    /**
     * Returns true if the <b>returnSubType</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetReturnSubType() {
        return (this.returnSubType != null);
    }

    /**
     * Unset the <b>returnSubType</b> property.
     */
    public void unsetReturnSubType() {
        this.returnSubType = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals(PROP_SEARCH_BASES)) {
            return getSearchBases();
        }
        if (propName.equals(PROP_COUNT_LIMIT)) {
            return getCountLimit();
        }
        if (propName.equals(PROP_SEARCH_LIMIT)) {
            return getSearchLimit();
        }
        if (propName.equals(PROP_TIME_LIMIT)) {
            return getTimeLimit();
        }
        if (propName.equals(PROP_EXPRESSION)) {
            return getExpression();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_SEARCH_BASES)) {
            return isSetSearchBases();
        }
        if (propName.equals(PROP_COUNT_LIMIT)) {
            return isSetCountLimit();
        }
        if (propName.equals(PROP_SEARCH_LIMIT)) {
            return isSetSearchLimit();
        }
        if (propName.equals(PROP_TIME_LIMIT)) {
            return isSetTimeLimit();
        }
        if (propName.equals(PROP_EXPRESSION)) {
            return isSetExpression();
        }
        if (propName.equals(PROP_RETURN_SUB_TYPE)) {
            return isSetReturnSubType();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_SEARCH_BASES)) {
            getSearchBases().add(((String) value));
        }
        if (propName.equals(PROP_COUNT_LIMIT)) {
            setCountLimit(((Integer) value));
        }
        if (propName.equals(PROP_SEARCH_LIMIT)) {
            setSearchLimit(((Integer) value));
        }
        if (propName.equals(PROP_TIME_LIMIT)) {
            setTimeLimit(((Integer) value));
        }
        if (propName.equals(PROP_EXPRESSION)) {
            setExpression(((String) value));
        }
        if (propName.equals(PROP_RETURN_SUB_TYPE)) {
            setReturnSubType(((Boolean) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_SEARCH_BASES)) {
            unsetSearchBases();
        }
        if (propName.equals(PROP_COUNT_LIMIT)) {
            unsetCountLimit();
        }
        if (propName.equals(PROP_SEARCH_LIMIT)) {
            unsetSearchLimit();
        }
        if (propName.equals(PROP_TIME_LIMIT)) {
            unsetTimeLimit();
        }
        if (propName.equals(PROP_RETURN_SUB_TYPE)) {
            unsetReturnSubType();
        }
        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    /**
     * Gets a list of all supported properties for this type.
     *
     * @param entityTypeName
     *            allowed object is {@link String}
     * @return
     *         returned object is {@link List}
     */
    public static synchronized List<String> getPropertyNames(String entityTypeName) {
        if (propertyNames == null) {
            List<String> names = new ArrayList<String>();
            names.add(PROP_SEARCH_BASES);
            names.add(PROP_COUNT_LIMIT);
            names.add(PROP_SEARCH_LIMIT);
            names.add(PROP_TIME_LIMIT);
            names.add(PROP_EXPRESSION);
            names.add(PROP_RETURN_SUB_TYPE);
            names.addAll(PropertyControl.getPropertyNames(PropertyControl.TYPE_NAME));
            propertyNames = Collections.unmodifiableList(names);
        }
        return propertyNames;
    }

    /**
     * Create the property name to data type mapping.
     */
    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap<String, String>();
        }
        dataTypeMap.put(PROP_SEARCH_BASES, "String");
        dataTypeMap.put(PROP_COUNT_LIMIT, "Integer");
        dataTypeMap.put(PROP_SEARCH_LIMIT, "Integer");
        dataTypeMap.put(PROP_TIME_LIMIT, "Integer");
        dataTypeMap.put(PROP_EXPRESSION, "String");
        dataTypeMap.put(PROP_RETURN_SUB_TYPE, "Boolean");
    }

    @Override
    public String getDataType(String propName) {
        if (dataTypeMap.containsKey(propName)) {
            return (dataTypeMap.get(propName));
        } else {
            return super.getDataType(propName);
        }
    }

    /**
     * Create the list of super-types for this type.
     */
    private static synchronized void setSuperTypes() {
        if (superTypeList == null) {
            superTypeList = new ArrayList<String>();
        }
        superTypeList.add(PropertyControl.TYPE_NAME);
        superTypeList.add(Control.TYPE_NAME);
    }

    @Override
    public ArrayList<String> getSuperTypes() {
        if (superTypeList == null) {
            setSuperTypes();
        }
        return superTypeList;
    }

    @Override
    public boolean isSubType(String superTypeName) {
        return superTypeList.contains(superTypeName);
    }

    /**
     * Create the set of sub-types for this type.
     */
    private static synchronized void setSubTypes() {
        if (subTypeSet == null) {
            subTypeSet = new HashSet<String>();
        }
        subTypeSet.add(DescendantControl.TYPE_NAME);
        subTypeSet.add(GroupMemberControl.TYPE_NAME);
        subTypeSet.add(GroupMembershipControl.TYPE_NAME);
        subTypeSet.add(HierarchyControl.TYPE_NAME);
        subTypeSet.add(LoginControl.TYPE_NAME);
        subTypeSet.add(AncestorControl.TYPE_NAME);
        subTypeSet.add(ChangeControl.TYPE_NAME);
        subTypeSet.add(GroupControl.TYPE_NAME);
    }

    /**
     * Gets a set of any types which extend this type.
     *
     * @return
     *         returned object is {@link HashSet}
     */
    public static HashSet<String> getSubTypes() {
        if (subTypeSet == null) {
            setSubTypes();
        }
        return subTypeSet;
    }
}
