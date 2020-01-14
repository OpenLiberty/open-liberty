/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.wsspi.security.wim.exception.MissingInitPropertyException;

/**
 * Contains attributes information and other LDAP related information of a entity type,
 * like object class and the mapping between properties and attributes.
 */
@Trivial
public class LdapEntity {

    private String iQEntityType = null;

    private String[][] iWIMRDNProps = null;

    private String[][] iWIMRDNAttrs = null;

    private String[][] iRDNAttrs = null;

    private String[][] iRDNObjectClass = null;

    private List<String> iObjectClasses = null;

    private Attribute[] iObjectClassAttrs = null;

    private String[] iSearchBases = null;

    private boolean iSearchBaseConfigured = false;

    private List<String> iSearchBaseList = null;

    private String iSearchFilter = null;

    private Set<String> iProps = null;

    private Map<String, String> iPropToAttrMap = null;

    private Map<String, Set<String>> iAttrToPropMap = null;

    private String iExtId = null;

    private boolean iTranslatedRDN = false;

    private Set<String> iAttrs = null;
    private static final TraceComponent tc = Tr.register(LdapEntity.class);

    public LdapEntity(String entityType) {
        iQEntityType = entityType;
        iAttrToPropMap = new Hashtable<String, Set<String>>();
        iPropToAttrMap = new Hashtable<String, String>();
        iAttrs = new HashSet<String>();
        iProps = new HashSet<String>();
    }

    public LdapEntity(String entityType, String entityTypeSearchFilter, String[] objectClasses, List<Map<String, Object>> rdnConfigs) throws MissingInitPropertyException {
        iQEntityType = entityType;

        setRDNAttributes(rdnConfigs);

        if (objectClasses != null)
            setObjectClasses(Arrays.asList(objectClasses));

        if (objectClasses != null) {
            ArrayList<String> objClsCreate = new ArrayList<String>(objectClasses.length);
            for (int i = 0; i < objectClasses.length; i++)
                objClsCreate.add(objectClasses[i]);
            setObjectClassesForCreate(objClsCreate);
        }

        if (getSearchBases() == null)
            iSearchBaseConfigured = false;
        else
            iSearchBaseConfigured = true;
        setSearchFilter(entityTypeSearchFilter);
        iAttrToPropMap = new Hashtable<String, Set<String>>();
        iPropToAttrMap = new Hashtable<String, String>();
        iAttrs = new HashSet<String>();
        iProps = new HashSet<String>();
    }

    public Set<String> getAttributes() {
        return iAttrs;
    }

    public String[][] getWIMRDNProperties() {
        return iWIMRDNProps.clone();
    }

    public String[][] getRDNAttributes() {
        if (iRDNAttrs == null) {
            if (iWIMRDNAttrs != null)
                return iWIMRDNAttrs.clone();
            else
                return null;
        } else {
            return iRDNAttrs.clone();
        }
    }

    public String[][] getRDNObjectclasses() {
        return iRDNObjectClass.clone();
    }

    public String getName() {
        return iQEntityType;
    }

    public Collection<? extends String> getSearchBaseList() {
        return iSearchBaseList;
    }

    public String getSearchFilter() {
        return iSearchFilter;
    }

    public boolean startWithSameRDN(String dn) {
        dn = dn.toLowerCase();
        String[][] rdnAttrs = iRDNAttrs != null ? iRDNAttrs : iWIMRDNAttrs;
        for (int i = 0; i < rdnAttrs.length; i++) {
            String[] attrs = rdnAttrs[i];
            for (int j = 0; j < attrs.length; j++) {
                if (dn.startsWith(attrs[j])) {
                    return true;
                }
            }
        }
        return false;
    }

    public String[][] getWIMRDNAttributes() {
        return iWIMRDNAttrs;
    }

    public boolean isSearchBaseConfigured() {
        return iSearchBaseConfigured;
    }

    public String getAttribute(String propName) {
        Object attrName = iPropToAttrMap.get(propName);
        if (attrName != null) {
            return (String) attrName;
        } else {
            return null;
        }
    }

    public Set<String> getProperty(String attrName) {
        return iAttrToPropMap.get(attrName.toLowerCase());
    }

    public boolean needTranslateRDN() {
        return iTranslatedRDN;
    }

    public String getExtId() {
        return iExtId;
    }

    public List<String> getRDNAttributesList() {
        String[][] rdnAttrs = getRDNAttributes();
        List<String> rdnList = new ArrayList<String>();
        for (int i = 0; i < rdnAttrs.length; i++) {
            for (int j = 0; j < rdnAttrs[i].length; j++) {
                rdnList.add(rdnAttrs[i][j]);
            }
        }
        return rdnList;
    }

    /**
     * Returns all object object classes of this entity type.
     *
     * @return A set of of object classes of this entity type in lower case form.
     */
    public List<String> getObjectClasses() {
        return iObjectClasses;
    }

    /**
     * Gets all search bases for this member type.
     *
     * @return All search bases for this member type.
     */
    public String[] getSearchBases() {
        return iSearchBases;
    }

    public void setSearchBases(String[] searchBases) {
        iSearchBases = searchBases;
        iSearchBaseList = new ArrayList<String>(iSearchBases.length);
        for (int i = 0; i < iSearchBases.length; i++) {
            iSearchBaseList.add(iSearchBases[i]);
        }
        iSearchBaseConfigured = true;
    }

    /**
     * Sets a list of defining object classes for this entity type.
     * The object classes will be stored in lower case form for comparison.
     *
     * @param objectClasses A list of defining object class names to be added.
     *
     */
    public void setObjectClasses(List<String> objectClasses) {
        int size = objectClasses.size();
        iObjectClasses = new ArrayList<String>(objectClasses.size());
        for (int i = 0; i < size; i++) {
            String objectClass = objectClasses.get(i).toLowerCase();
            if (!iObjectClasses.contains(objectClass)) {
                iObjectClasses.add(objectClass);
            }
        }
    }

    /**
     * Sets the object classes attribute for creating.
     *
     * @param objectClasses A list of defining object class names to be added.
     *
     */
    public void setObjectClassesForCreate(List<String> objectClasses) {
        if (iRDNObjectClass != null && iRDNObjectClass.length > 1) {
            iObjectClassAttrs = new Attribute[iRDNObjectClass.length];
            for (int i = 0; i < iRDNObjectClass.length; i++) {
                iObjectClassAttrs[i] = new BasicAttribute(LdapConstants.LDAP_ATTR_OBJECTCLASS, iRDNObjectClass[i][0]);
                // Remove the object class for create
                objectClasses.remove(iRDNObjectClass[i][0]);
            }
            // Add rest auxliary object classes.
            for (int i = 0; i < iObjectClassAttrs.length; i++) {
                for (int j = 0; j < objectClasses.size(); j++) {
                    iObjectClassAttrs[i].add(objectClasses.get(j));
                }
            }
        } else {
            iObjectClassAttrs = new Attribute[1];
            iObjectClassAttrs[0] = new BasicAttribute(LdapConstants.LDAP_ATTR_OBJECTCLASS);
            if (objectClasses.size() > 0) {
                for (int i = 0; i < objectClasses.size(); i++) {
                    iObjectClassAttrs[0].add(objectClasses.get(i));
                }
            } else {
                // If create object class is not define, use object classes
                for (int i = 0; i < iObjectClasses.size(); i++) {
                    iObjectClassAttrs[0].add(iObjectClasses.get(i));
                }
            }
        }
    }

    public void setSearchFilter(String filter) {
        if (filter != null && filter.trim().length() > 0) {
            filter = filter.trim();
            if (filter.length() > 0) {
                if (filter.charAt(0) != '(' || filter.charAt(filter.length() - 1) != ')') {
                    filter = "(" + filter + ")";
                }
                iSearchFilter = filter;
            }
        } else {
            if (iObjectClasses != null) {
                // If search filter is not defined, construct it using object classes.
                int n = iObjectClasses.size();
                StringBuffer filterBuffer = new StringBuffer(n * LdapConstants.LDAP_OBJCLS_FILTER_ESTIMATED_SIZE);
                if (n > 1) {
                    filterBuffer.append("(|");
                }
                for (int i = 0; i < n; i++) {
                    filterBuffer.append("(").append(LdapConstants.LDAP_ATTR_OBJECTCLASS).append("=").append(
                                                                                                            iObjectClasses.get(i)).append(")");
                }
                if (n > 1) {
                    filterBuffer.append(")");
                }
                iSearchFilter = filterBuffer.toString();
            }
        }
    }

    public void setRDNAttributes(String[][] RDNAttrs, String[][] RDNObjCls) {
        iRDNAttrs = RDNAttrs;
        iRDNObjectClass = RDNObjCls;
    }

    public void addPropertyAttributeMap(String propName, String attrName) {
        String realPropName = propName;
        if (propName.equalsIgnoreCase("ibm-primaryEmail")) {
            realPropName = "ibmPrimaryEmail";
        } else if (propName.equalsIgnoreCase("ibm-jobTitle")) {
            realPropName = "ibmJobTitle";
        }
        if ((propName.equalsIgnoreCase("ibmPrimaryEmail")) && (attrName.equalsIgnoreCase("ibmPrimaryEmail"))) {
            attrName = "ibm-primaryEmail";
        }
        if ((propName.equalsIgnoreCase("ibmJobTitle")) && (attrName.equalsIgnoreCase("ibmJobTitle"))) {
            attrName = "ibm-jobTitle";
        }
        if (propName.equalsIgnoreCase("ibm-primaryEmail") || (propName.equalsIgnoreCase("ibm-jobTitle"))) {
            iPropToAttrMap.put(realPropName, attrName);
            iPropToAttrMap.put(propName, attrName);
        } else {
            iPropToAttrMap.put(realPropName, attrName);
        }
        String attrKey = attrName.toLowerCase();
        Set<String> propSet = iAttrToPropMap.get(attrKey);
        if (propSet == null) {
            propSet = new HashSet<String>();
            iAttrToPropMap.put(attrKey, propSet);
        }
        if (propName.equalsIgnoreCase("ibm-primaryEmail") || (propName.equalsIgnoreCase("ibm-jobTitle"))) {
            propSet.add(realPropName);
            propSet.add(propName);
            addProperty(propName);
            addProperty(realPropName);
        } else {
            propSet.add(realPropName);
            addProperty(realPropName);
        }
        addAttribute(attrName);

    }

    private void addProperty(String propName) {
        iProps.add(propName);
    }

    private void addAttribute(String attrName) {
        iAttrs.add(attrName);
    }

    public void setExtId(String attrName) {
        iExtId = attrName;
    }

    public void addObjectClass(String objectClass) {
        objectClass = objectClass.toLowerCase();
        if (!iObjectClasses.contains(objectClass)) {
            iObjectClasses.add(objectClass);
        }
    }

    public void setRDNProperties(String[][] rdnProps, String[][] rdnAttrs) {
        iWIMRDNAttrs = rdnAttrs;
        iWIMRDNProps = rdnProps;
        if (iRDNAttrs != null) {
            if (iRDNAttrs.length != iWIMRDNAttrs.length) {
                iTranslatedRDN = true;
                return;
            }
            for (int i = 0; i < iRDNAttrs.length; i++) {
                if (!Arrays.equals(iRDNAttrs[i], iWIMRDNAttrs[i])) {
                    iTranslatedRDN = true;
                    return;
                }
            }
        } else {
            for (int i = 0; i < iWIMRDNAttrs.length; i++) {
                if (!Arrays.equals(iWIMRDNAttrs[i], iWIMRDNProps[i])) {
                    iTranslatedRDN = true;
                    return;
                }
            }
        }
    }

    /**
     * Sets the RDN attribute types of this member type.
     * RDN attribute types will be converted to lower case.
     *
     * @param The RDN attribute types of this member type.
     */
    public void setRDNAttributes(List<Map<String, Object>> rdnAttrList) throws MissingInitPropertyException {
        int size = rdnAttrList.size();
        // if size = 0, No RDN attributes defined. Same as RDN properties.
        if (size > 0) {
            iRDNAttrs = new String[size][];
            iRDNObjectClass = new String[size][];
            if (size == 1) {
                Map<String, Object> rdnAttr = rdnAttrList.get(0);
                String[] rdns = LdapHelper.getRDNs((String) rdnAttr.get(LdapConstants.CONFIG_PROP_NAME));
                iRDNAttrs[0] = rdns;
            } else {
                int i = 0;
                for (Map<String, Object> rdnAttr : rdnAttrList) {
                    String name = (String) rdnAttr.get(LdapConstants.CONFIG_PROP_NAME);
                    String[] rdns = LdapHelper.getRDNs(name);
                    iRDNAttrs[i] = rdns;
                    String[] objCls = (String[]) rdnAttr.get(LdapConstants.CONFIG_DO_OBJECTCLASS);
                    if (objCls == null) {
                        throw new MissingInitPropertyException(WIMMessageKey.MISSING_INI_PROPERTY, Tr.formatMessage(
                                                                                                                    tc,
                                                                                                                    WIMMessageKey.MISSING_INI_PROPERTY,
                                                                                                                    WIMMessageHelper.generateMsgParms(LdapConstants.CONFIG_DO_OBJECTCLASS)));
                    } else {
                        iRDNObjectClass[i] = objCls;
                    }
                    i++;
                }
            }
        }
    }

    /**
     * Gets the LDAP object class attribute that contains all object classes needed to create the member on LDAP server.
     *
     * @return The LDAP object class attribute of this member type.
     */
    public Attribute getObjectClassAttribute(String dn) {
        if (iObjectClassAttrs.length == 1 || dn == null) {
            return iObjectClassAttrs[0];
        } else {

            String[] rdns = LdapHelper.getRDNAttributes(dn);

            for (int i = 0; i < iRDNAttrs.length; i++) {
                String[] attrs = iRDNAttrs[i];
                for (int j = 0; j < attrs.length; j++) {
                    for (int k = 0; k < attrs.length; k++) {
                        if (attrs[j].equals(rdns[k])) {
                            return iObjectClassAttrs[i];
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getName()).append(":{");
        sb.append("EntityType=").append(iQEntityType);
        if (iExtId != null) {
            sb.append(", ExternalID=").append(iExtId);
        }
        if (iAttrs != null && !iAttrs.isEmpty()) {
            sb.append(", Attributes=").append(iAttrs);
        }
        if (iAttrToPropMap != null && !iAttrToPropMap.isEmpty()) {
            sb.append(", AttributeToPropertyMap=").append(iAttrToPropMap);
        }
        if (iProps != null && !iProps.isEmpty()) {
            sb.append(", Properties=").append(iProps);
        }
        if (iPropToAttrMap != null && !iPropToAttrMap.isEmpty()) {
            sb.append(", PropertyToAttributeMap=").append(iPropToAttrMap);
        }
        if (iObjectClassAttrs != null && iObjectClassAttrs.length != 0) {
            sb.append(", ObjectClassAttributes=").append(Arrays.toString(iObjectClassAttrs));
        }
        if (iObjectClasses != null && !iObjectClasses.isEmpty()) {
            sb.append(", ObjectClasses=").append(iObjectClasses);
        }

        sb.append(", iSearchBaseConfigured=").append(iSearchBaseConfigured);
        if (iSearchBaseList != null && !iSearchBaseList.isEmpty()) {
            sb.append(", SearchBases=").append(iSearchBaseList);
        }
        if (iSearchFilter != null) {
            sb.append(", SearchFilter=").append(iSearchFilter);
        }

        sb.append(", iTranslatedRDN=").append(iTranslatedRDN);
        if (iRDNAttrs != null && iRDNAttrs.length != 0) {
            sb.append(", RDNAttributes=").append(Arrays.deepToString(iRDNAttrs));
        }
        if (iRDNObjectClass != null && iRDNObjectClass.length != 0) {
            sb.append(", RDNObjectClass=").append(Arrays.deepToString(iRDNObjectClass));
        }
        if (iWIMRDNAttrs != null && iWIMRDNAttrs.length != 0) {
            sb.append(", WIMRDNAttrs=").append(Arrays.deepToString(iWIMRDNAttrs));
        }
        if (iWIMRDNProps != null && iWIMRDNProps.length != 0) {
            sb.append(", WIMRDNProps=").append(Arrays.deepToString(iWIMRDNProps));
        }
        sb.append("}");
        return sb.toString();
    }
}
