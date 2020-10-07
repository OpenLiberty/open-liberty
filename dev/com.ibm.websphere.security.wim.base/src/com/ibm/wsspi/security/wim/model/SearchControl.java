/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for SearchControl complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SearchControl">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}PropertyControl">
 * &lt;sequence>
 * &lt;element name="searchBases" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;/sequence>
 * &lt;attribute name="countLimit" type="{http://www.w3.org/2001/XMLSchema}int" default="0" />
 * &lt;attribute name="searchLimit" type="{http://www.w3.org/2001/XMLSchema}int" default="0" />
 * &lt;attribute name="timeLimit" type="{http://www.w3.org/2001/XMLSchema}int" default="0" />
 * &lt;attribute name="expression" type="{http://www.w3.org/2001/XMLSchema}string" />
 * &lt;attribute name="returnSubType" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The SearchControl object extends the PropertyControl object.
 * 
 * <p> It is possible to provide a list of the search bases to narrow down the search by specifying the <b>searchBases</b> property
 * in the SearchControl. For example, 'ou=Mahwah, o=mycompany, c=us' could be specified in 'searchBases' to only search for users
 * in the Mahwah division in the United States.
 * 
 * <ul>
 * <li><b>countLimit</b>: used to specify the number of results to return from the search call. If the actual number of
 * search results is more than the <b>countLimit</b>, the <b>hasMoreResults</b> property in the SearchResponseControl will be set to true.</li>
 * 
 * <li><b>searchLimit</b>: used to specify the maximum number of search results that may be returned by the search operation.</li>
 * 
 * <li><b>timeLimit</b>: specifies the maximum number of milliseconds the search is allowed to take if a repository
 * supports such a parameter.</li>
 * 
 * <li><b>expression</b>: the search expression in XPath format.</li>
 * 
 * </ul>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SearchControl", propOrder = {
                                              "searchBases"
})
@XmlSeeAlso({
             LoginControl.class,
             ChangeControl.class,
             HierarchyControl.class
})
@Trivial
public class SearchControl
                extends PropertyControl
{

    protected List<String> searchBases;
    @XmlAttribute(name = "countLimit")
    protected Integer countLimit;
    @XmlAttribute(name = "searchLimit")
    protected Integer searchLimit;
    @XmlAttribute(name = "timeLimit")
    protected Integer timeLimit;
    @XmlAttribute(name = "expression")
    protected String expression;
    @XmlAttribute(name = "returnSubType")
    protected Boolean returnSubType;
    private static List propertyNames = null;
    private static HashMap dataTypeMap = null;
    private static ArrayList superTypeList = null;
    private static HashSet subTypeList = null;

    static {
        setDataTypeMap();
        setSuperTypes();
        setSubTypes();
    }

    /**
     * Gets the value of the searchBases property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the searchBases property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getSearchBases().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getSearchBases() {
        if (searchBases == null) {
            searchBases = new ArrayList<String>();
        }
        return this.searchBases;
    }

    public boolean isSetSearchBases() {
        return ((this.searchBases != null) && (!this.searchBases.isEmpty()));
    }

    public void unsetSearchBases() {
        this.searchBases = null;
    }

    /**
     * Gets the value of the countLimit property.
     * 
     * @return
     *         possible object is {@link Integer }
     * 
     */
    public int getCountLimit() {
        if (countLimit == null) {
            return 0;
        } else {
            return countLimit;
        }
    }

    /**
     * Sets the value of the countLimit property.
     * 
     * @param value
     *            allowed object is {@link Integer }
     * 
     */
    public void setCountLimit(int value) {
        this.countLimit = value;
    }

    public boolean isSetCountLimit() {
        return (this.countLimit != null);
    }

    public void unsetCountLimit() {
        this.countLimit = null;
    }

    /**
     * Gets the value of the searchLimit property.
     * 
     * @return
     *         possible object is {@link Integer }
     * 
     */
    public int getSearchLimit() {
        if (searchLimit == null) {
            return 0;
        } else {
            return searchLimit;
        }
    }

    /**
     * Sets the value of the searchLimit property.
     * 
     * @param value
     *            allowed object is {@link Integer }
     * 
     */
    public void setSearchLimit(int value) {
        this.searchLimit = value;
    }

    public boolean isSetSearchLimit() {
        return (this.searchLimit != null);
    }

    public void unsetSearchLimit() {
        this.searchLimit = null;
    }

    /**
     * Gets the value of the timeLimit property.
     * 
     * @return
     *         possible object is {@link Integer }
     * 
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
     * 
     */
    public void setTimeLimit(int value) {
        this.timeLimit = value;
    }

    public boolean isSetTimeLimit() {
        return (this.timeLimit != null);
    }

    public void unsetTimeLimit() {
        this.timeLimit = null;
    }

    /**
     * Gets the value of the expression property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Sets the value of the expression property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setExpression(String value) {
        this.expression = value;
    }

    public boolean isSetExpression() {
        return (this.expression != null);
    }

    /**
     * Gets the value of the returnSubType property.
     * 
     * @return
     *         possible object is {@link Boolean }
     * 
     */
    public boolean isReturnSubType() {
        if (returnSubType == null) {
            return true;
        } else {
            return returnSubType;
        }
    }

    /**
     * Sets the value of the returnSubType property.
     * 
     * @param value
     *            allowed object is {@link Boolean }
     * 
     */
    public void setReturnSubType(boolean value) {
        this.returnSubType = value;
    }

    public boolean isSetReturnSubType() {
        return (this.returnSubType != null);
    }

    public void unsetReturnSubType() {
        this.returnSubType = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals("searchBases")) {
            return getSearchBases();
        }
        if (propName.equals("countLimit")) {
            return getCountLimit();
        }
        if (propName.equals("searchLimit")) {
            return getSearchLimit();
        }
        if (propName.equals("timeLimit")) {
            return getTimeLimit();
        }
        if (propName.equals("expression")) {
            return getExpression();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals("searchBases")) {
            return isSetSearchBases();
        }
        if (propName.equals("countLimit")) {
            return isSetCountLimit();
        }
        if (propName.equals("searchLimit")) {
            return isSetSearchLimit();
        }
        if (propName.equals("timeLimit")) {
            return isSetTimeLimit();
        }
        if (propName.equals("expression")) {
            return isSetExpression();
        }
        if (propName.equals("returnSubType")) {
            return isSetReturnSubType();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals("searchBases")) {
            getSearchBases().add(((String) value));
        }
        if (propName.equals("countLimit")) {
            setCountLimit(((Integer) value));
        }
        if (propName.equals("searchLimit")) {
            setSearchLimit(((Integer) value));
        }
        if (propName.equals("timeLimit")) {
            setTimeLimit(((Integer) value));
        }
        if (propName.equals("expression")) {
            setExpression(((String) value));
        }
        if (propName.equals("returnSubType")) {
            setReturnSubType(((Boolean) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals("searchBases")) {
            unsetSearchBases();
        }
        if (propName.equals("countLimit")) {
            unsetCountLimit();
        }
        if (propName.equals("searchLimit")) {
            unsetSearchLimit();
        }
        if (propName.equals("timeLimit")) {
            unsetTimeLimit();
        }
        if (propName.equals("returnSubType")) {
            unsetReturnSubType();
        }
        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return "SearchControl";
    }

    public static synchronized List getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.add("searchBases");
                names.add("countLimit");
                names.add("searchLimit");
                names.add("timeLimit");
                names.add("expression");
                names.add("returnSubType");
                names.addAll(PropertyControl.getPropertyNames("PropertyControl"));
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put("searchBases", "String");
        dataTypeMap.put("countLimit", "Integer");
        dataTypeMap.put("searchLimit", "Integer");
        dataTypeMap.put("timeLimit", "Integer");
        dataTypeMap.put("expression", "String");
        dataTypeMap.put("returnSubType", "Boolean");
    }

    @Override
    public String getDataType(String propName) {
        if (dataTypeMap.containsKey(propName)) {
            return ((String) dataTypeMap.get(propName));
        } else {
            return super.getDataType(propName);
        }
    }

    private static synchronized void setSuperTypes() {
        if (superTypeList == null) {
            superTypeList = new ArrayList();
        }
        superTypeList.add("PropertyControl");
        superTypeList.add("Control");
    }

    @Override
    public ArrayList getSuperTypes() {
        if (superTypeList == null) {
            setSuperTypes();
        }
        return superTypeList;
    }

    @Override
    public boolean isSubType(String superTypeName) {
        return superTypeList.contains(superTypeName);
    }

    private static synchronized void setSubTypes() {
        if (subTypeList == null) {
            subTypeList = new HashSet();
        }
        subTypeList.add("DescendantControl");
        subTypeList.add("GroupMemberControl");
        subTypeList.add("GroupMembershipControl");
        subTypeList.add("HierarchyControl");
        subTypeList.add("LoginControl");
        subTypeList.add("AncestorControl");
        subTypeList.add("ChangeControl");
        subTypeList.add("GroupControl");
    }

    public static HashSet getSubTypes() {
        if (subTypeList == null) {
            setSubTypes();
        }
        return subTypeList;
    }

    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
