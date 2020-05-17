/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap;

import java.io.UnsupportedEncodingException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;
import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.wim.AccessControllerHelper;
import com.ibm.ws.security.wim.util.NodeHelper;
import com.ibm.ws.security.wim.util.UniqueNameHelper;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.CertificateMapperException;
import com.ibm.wsspi.security.wim.exception.EntityTypeNotSupportedException;
import com.ibm.wsspi.security.wim.exception.InitializationException;
import com.ibm.wsspi.security.wim.exception.InvalidInitPropertyException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.exception.WIMSystemException;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.Group;
import com.ibm.wsspi.security.wim.model.LangType;
import com.ibm.wsspi.security.wim.model.Person;
import com.ibm.wsspi.security.wim.model.PersonAccount;

@SuppressWarnings("restriction")
public class LdapConfigManager {

    /**  */
    private static final String GROUP_PROPERTIES = "groupProperties";

    /**  */
    private static final String LDAP_ENTITY_TYPE = "ldapEntityType";

    /**
     * Register the class to trace service.
     */
    private final static TraceComponent tc = Tr.register(LdapConfigManager.class);

    /**
     * Constant for Base Entry Name
     */
    static final String BASE_ENTRY_NAME = "name";

    /**
     * Constant for "registryBaseEntry"
     */
    static final String BASE_ENTRY = "registryBaseEntry";

    /**
     * Constant for Base DN
     */
    static final String BASE_DN = "baseDN";

    /**
     * Pattern to determine if the attribute is in the format of "&lt;attribute name&gt;:&lt;matching rule OID&gt;:=&lt;value&gt;".
     */
    private static final Pattern PATTERN_EXTENSIBLE_MATCH_FILTER = Pattern.compile("(.+):(.+):(.*)");

    /**
     * A array of <code>MessageFormat</code> objects that are used to build LDAP search filter.
     * The index of the array indicates the operator defined in <code>com.ibm.websphere.wmm.datatype.SearchCondition</code>
     * <ul>
     * <li>0 - OPERATOR_EQ
     * <li>1 - OPERATOR_NE
     * <li>2 - OPERATOR_GT
     * <li>3 - OPERATOR_LT
     * <li>4 - OPERATOR_GE
     * <li>5 - OPERATOR_LE
     * </ul>
     */
    public MessageFormat[] CONDITION_FORMATS = {
                                                 new MessageFormat("({0}={1})"),
                                                 new MessageFormat("(!({0}={1}))"),
                                                 new MessageFormat("(&({0}>={1})(!({0}={1})))"),
                                                 new MessageFormat("(&({0}<={1})(!({0}={1})))"),
                                                 new MessageFormat("({0}>={1})"),
                                                 new MessageFormat("({0}<={1})"),
    };

    /**
     * The name of the configured membership attribute
     */
    private String iMembershipAttrName;

    /**
     * Whether or not use default member attribute in runtime
     */
    private boolean iUseDefaultMbrAttr = false;

    /**
     * The Ldap Type
     */
    private String iLdapType = null;

    /**
     * Store the scope of the memberOf attribute. By default is 'direct'.
     */
    private short iMembershipAttrScope = LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP;

    /**
     * Stores the names of group member URL attribute names (like memberULR)
     */
    private String[] iDynaMbrAttrs = null;

    /**
     * Supported LDAP entity types in qualified name.
     */
    private List<String> iLdapEntityTypeList = null;

    /**
     * WIM property name to LDAP attribute name map
     */
    private Map<String, String> iPropToAttrMap = null;

    /**
     * LDAP attribute (lowercase) to Set of WIM property name name map
     */
    private Map<String, Set<String>> iAttrToPropMap = null;

    /**
     * Lower case LDAP attribute name to LDAPAttribute object map.
     */
    private Map<String, LdapAttribute> iAttrNameToAttrMap = null;

    /**
     * LDAP entities corresponding to the iLdapEntityTypes.
     */
    private List<LdapEntity> iLdapEntities = null;

    /**
     * List of Person Account Entity Types
     */
    private List<String> iPersonAccountTypes = null;

    /**
     * List of Person Entity Types
     */
    private List<String> iPersonTypes = null;

    /**
     * List of Group Entity Types
     */
    private List<String> iGroupTypes = null;

    /**
     * Array of Special characters
     */
    private final char[] iSpecialChars = { '(', ')', '\\' };

    /**
     * Flag to indicate whether user wants to encode characters while creating search expression.
     */
    private String useEncodingInSearchExpression = null;

    /**
     * Group Member Filter
     */
    private String iGrpMbrFilter = null;

    /**
     * List of Search bases to be used for groups.
     */
    private String[] iGroupSearchBases = null;

    /**
     * ldap node with descending length order for switching iNodes.
     */
    private String[] iNodesForCompare = null;

    /**
     * lower case wim node with descending length order for switching iNodes.
     */
    private String[] iLdapNodes = null;

    /**
     * Whether or not need to switch iNodes when converting between unique name and DN.
     */
    private boolean iNeedSwitchNode = false;

    private boolean iNeedTranslateRDN = false;

    /**
     * Stores the group object class (lower case) to group member URL attribute mapping.
     */
    private Map<String, String> iDynaMbrAttrMap = null;

    /**
     * Stores a list of object classes for Dynamic group member
     */
    private final List<String> iDynaMbrObjectClass = new ArrayList<String>();

    /**
     * Whether or not all member attributes are nested scope.
     */
    private boolean iMbrAttrsNestedScope = false;

    /**
     * Whether or not all member attributes are all scope.
     */
    private boolean iMbrAttrsAllScope = false;

    /**
     * top level ldap iNodes in lower cases for search purpose
     */
    private String[] iTopLdapNodes = null;

    /**
     * Stores the names of group member attribute names (like member, uniqueMember).
     */
    private String[] iMbrAttrs = null;

    /**
     * Stores the group object class (lower case) to group member attribute mapping.
     */
    private Map<String, String> iMbrAttrMap = null;

    /**
     * Flag indicating whether user wants to include groups as entity types in search.
     */
    private final boolean includeGroupInSearchEntityTypes = false;

    private Map<String, String> iDummyMbrMap;

    /**
     * The attributes used for login.
     */
    private List<String> iLoginAttrs = null;

    /**
     * The properties used for login.
     */
    private List<String> iLoginProps = null;

    /**
     * Certificate Mapping mode
     */
    private String iCertMapMode = null;

    /**
     * Array of tokens parsed from Certificate Filter.
     */
    private String[] iCertFilterEles = null;

    /**
     * Flag indicating whether user wants to use input principal name for login.
     */
    private final boolean usePrincipalNameForLogin = false;

    /**
     * All supported LDAP attributes names (not include operational).
     */
    private Set<String> iAttrs = null;

    /**
     * All supported operational attributes
     */
    private Set<String> iOperAttrs = null;

    /**
     * The lower case names of attributes which contains the default value.
     */
    private Set<String> iDefaultValueAttrs = null;

    /**
     * The lower case names of attributes which contains the default attribute.
     */
    private Set<String> iDefaultAttrAttrs = null;

    /**
     * List of datatypes
     */
    private List<String> iDataTypes = null;

    /**
     * used to store the properties with entity type
     */
    private final List<String> entityTypeProps = new ArrayList<String>();

    private Set<String> iExtIds = null;

    private Set<String> iConAttrs = null;

    private boolean isAnyExtIdDN = false;

    /**
     * lower case ldap node with descending length order for switching iNodes.
     */
    private String[] iLdapNodesForCompare = null;

    /**
     * wim node with descending length order for switching iNodes.
     */
    private String[] iNodes = null;

    /**
     * The filter for searching all dynamic groups.
     */
    private String iDynaGrpFilter = null;

    /**
     * Stores the group member attribute scope.
     */
    private short[] iMbrAttrScope;

    /**
     * Supported properties for Group
     */
    private final String[] iGroupSupportedProps = (String[]) Group.getPropertyNames(SchemaConstants.DO_GROUP).toArray(new String[0]);

    /**
     * Supported properties for Person
     */
    private final String[] iPersonSupportedProps = (String[]) PersonAccount.getPropertyNames(SchemaConstants.DO_PERSON_ACCOUNT).toArray(new String[0]);

    /**
     * The user filter
     */
    private String iUserFilter = null;

    /**
     * The Filter object for userFilter
     */
    private Filter userFilter = null;

    /**
     * The Group filter
     */
    private String iGroupFilter = null;

    /**
     * The Filter object for groupFilter
     */
    private Filter groupFilter = null;

    /**
     * The user id map
     */
    private String iUserIdMap = null;

    /**
     * The group id map
     */
    private String iGroupIdMap = null;

    /**
     * The group member id map
     */
    private String iGroupMemberIdMap = null;

    /**
     * Weather or not recursive search should be done for group members
     */
    private boolean iRecursiveSearch = false;

    /**
     * Constant for ibm-allGroups
     */
    private final String IBM_ALL_GROUPS = "ibm-allGroups";

    /**
     * Weather or not LDAP operation attr is set in groupMemberIdMap
     */
    private boolean iLdapOperationalAttr = false;

    private boolean isVMMRdnPropertiesDefined = false;

    /**
     * Flag to determine if the configured LDAP is a RACF(SDBM) system
     */
    private boolean isRacf = false;

    private String timestampFormat;

    /**
     * Flag to determine whether to use membership (memberOf) attribute or member attribute.
     * True means membership was not set, so the default is used. False means the user set the membership attribute.
     * If both member and membership attributes are set in groupProperties, member will take precedence.
     */
    private boolean iDefaultMembershipAttr = true;

    /**
     * Refreshes the caches using the given configuration data object.
     * This method should be called when there are changes in configuration and schema.
     *
     * @param reposConfig the data object containing configuration information of the repository.
     *
     * @throws WIMException
     */
    public void initialize(Map<String, Object> configProps) throws WIMException {
        final String METHODNAME = "initialize(configProps)";
        iLdapType = (String) configProps.get(LdapConstants.CONFIG_PROP_LDAP_SERVER_TYPE);
        if (iLdapType == null) {
            iLdapType = LdapConstants.CONFIG_LDAP_IDS52;
        } else {
            iLdapType = iLdapType.toUpperCase();
        }

        /*
         * Initialize certificate mapping.
         */
        setCertificateMapMode((String) configProps.get(LdapConstants.CONFIG_PROP_CERTIFICATE_MAP_MODE));
        if (LdapConstants.CONFIG_VALUE_FILTER_DESCRIPTOR_MODE.equalsIgnoreCase(getCertificateMapMode())) {

            setCertificateFilter((String) configProps.get(LdapConstants.CONFIG_PROP_CERTIFICATE_FILTER));

        }

        List<HashMap<String, String>> baseEntries = new ArrayList<HashMap<String, String>>();

        // Add the default (first base entry)
        String baseDN = (String) configProps.get(BASE_DN);
        String name = (String) configProps.get(BASE_ENTRY_NAME);
        HashMap<String, String> baseEntryMap = new HashMap<String, String>();
        if (name != null)
            baseEntryMap.put(name, baseDN);
        else
            baseEntryMap.put(baseDN, baseDN);

        baseEntries.add(baseEntryMap);

        Map<String, List<Map<String, Object>>> configMap = Nester.nest(configProps, BASE_ENTRY,
                                                                       LDAP_ENTITY_TYPE,
                                                                       GROUP_PROPERTIES);

        // Add any additional base entries configured.
        for (Map<String, Object> entry : configMap.get(BASE_ENTRY)) {
            baseDN = (String) entry.get(BASE_DN);
            name = (String) entry.get(BASE_ENTRY_NAME);
            if (baseDN == null || baseDN.trim().length() == 0) {
                //TODO correct error?
                Tr.error(tc, WIMMessageKey.INVALID_BASE_ENTRY_DEFINITION, name);
            } else {
                baseEntryMap = new HashMap<String, String>();
                if (name != null) {
                    baseEntryMap.put(name, baseDN);
                } else {
                    baseEntryMap.put(baseDN, baseDN);
                }
                baseEntries.add(baseEntryMap);
            }
        }

        // Set the timestampFormat
        if (configProps.containsKey(LdapConstants.TIMESTAMP_FORMAT))
            timestampFormat = (String) configProps.get(LdapConstants.TIMESTAMP_FORMAT);
        else
            timestampFormat = null;

        setNodes(baseEntries);
        setLDAPEntities(configMap.get(LDAP_ENTITY_TYPE), baseEntries);

        List<Map<String, Object>> groupPropList = configMap.get(GROUP_PROPERTIES);
        Map<String, Object> groupProps = groupPropList.isEmpty() ? Collections.<String, Object> emptyMap() : groupPropList.get(0);
        // TODO:: This seems to have been taken out.
        // setUpdateGroupMembership(configProps, configAdmin);
        setMemberAttributes(groupProps);
        setMembershipAttribute(groupProps);
        setDynaMemberAttributes(groupProps);
        setGroupSearchScope(configProps);

        setAttributes(configProps);
        setExtIdAttributes(configProps);
//        setRDNProperties(configProps);
        setConfidentialAttributes();
        //setGroupMemberFilter();
        setLoginProperties((String) configProps.get(LdapConstants.CONFIG_PROP_LOGIN_PROPERTIES));
        setFilters(configProps);
        setGroupMemberFilter();

        // TODO:: This is also deleted?
        // iNeedTranslateRDN = reposConfig.getBoolean(LdapConstants.CONFIG_PROP_TRANSLATE_RDN);

        // CUSTOM PROPERTY
        useEncodingInSearchExpression = AccessControllerHelper.getSystemProperty(LdapConstants.CONFIG_CUSTOM_PROP_USE_ENCODING_IN_SEARCH_EXPRESSION);
        if (useEncodingInSearchExpression != null) {
            try {
                "string to test encoding".getBytes(useEncodingInSearchExpression);
            } catch (UnsupportedEncodingException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, METHODNAME + " java.io.UnsupportedEncodingException: " + e.getMessage());
                useEncodingInSearchExpression = "ISO8859_1"; // Default
            }
        }
        entityTypeProps.add("parent");
        entityTypeProps.add("children");
        entityTypeProps.add("members");

        if (tc.isDebugEnabled()) {
            StringBuffer strBuf = new StringBuffer();
            strBuf.append("\n\nLDAPServerType: ").append(iLdapType).append("\n");
            strBuf.append("Nodes: ").append(WIMTraceHelper.printObjectArray(iNodes)).append("\n");
            strBuf.append("ReposNodes: ").append(WIMTraceHelper.printObjectArray(iLdapNodes)).append("\n");
            strBuf.append("TopReposNodes: ").append(WIMTraceHelper.printObjectArray(iTopLdapNodes)).append("\n");
            strBuf.append("NeedSwitchNode: ").append(iNeedSwitchNode).append("\n");
            strBuf.append("LDAPEntities: ").append("\n");
            for (int i = 0; i < iLdapEntities.size(); i++) {
                strBuf.append("   " + iLdapEntities.get(i).toString()).append("\n");
            }

            strBuf.append("GroupMemberAttrs: ").append(iMbrAttrMap).append("\n");
            strBuf.append("   memberAttrs: ").append(WIMTraceHelper.printObjectArray(iMbrAttrs)).append("\n");
            strBuf.append("   scopes: ").append(WIMTraceHelper.printPrimitiveArray(iMbrAttrScope)).append("\n");
            strBuf.append("GroupMemberFilter: ").append(iGrpMbrFilter).append("\n");
            strBuf.append("GroupDynaMemberAttrs: ").append(iDynaMbrAttrMap).append("\n");
            strBuf.append("DynaGroupFilter: ").append(iDynaGrpFilter).append("\n");
            strBuf.append("GroupMembershipAttrs: ").append(iMembershipAttrName).append("\n");
            strBuf.append("   scope: ").append(iMembershipAttrScope).append("\n");

            strBuf.append("PropToAttrMap: ").append(iPropToAttrMap).append("\n");
            strBuf.append("AttrToPropMap: ").append(iAttrToPropMap).append("\n");
            strBuf.append("ExtIds: ").append(iExtIds).append("\n");
            strBuf.append("AllAttrs: ").append(iAttrs).append("\n");
            strBuf.append("LoginAttrs: ").append(iLoginAttrs).append("\n");
            strBuf.append("iUserFilter: ").append(iUserFilter).append("\n");
            strBuf.append("iGroupFilter: ").append(iGroupFilter).append("\n");
            strBuf.append("iUserIdMap: ").append(iUserIdMap).append("\n");
            strBuf.append("iGroupIdMap: ").append(iGroupIdMap).append("\n");
            strBuf.append("iGroupMemberIdMap: ").append(iGroupMemberIdMap).append("\n");
            Tr.debug(tc, METHODNAME + strBuf.toString());
        }
    }

    /**
     * Set the filters depending on the ldap type selected.
     *
     * @param configProps
     */
    private void setFilters(Map<String, Object> configProps) throws WIMSystemException {
        // If no ldap type configured, return
        if (iLdapType == null)
            return;

        // Determine the Ldap type configured.
        //name under which filters are put
        String key = null;
        if (iLdapType.equalsIgnoreCase(LdapConstants.AD_LDAP_SERVER)) {
            key = LdapConstants.CONFIG_ACTIVE_DIRECTORY_FILTERS;
        } else if (iLdapType.equalsIgnoreCase(LdapConstants.CUSTOM_LDAP_SERVER)) {
            key = LdapConstants.CONFIG_CUSTOM_FILTERS;
        } else if (iLdapType.equalsIgnoreCase(LdapConstants.DOMINO_LDAP_SERVER)) {
            key = LdapConstants.CONFIG_DOMINO_FILTERS;
        } else if (iLdapType.equalsIgnoreCase(LdapConstants.NOVELL_LDAP_SERVER)) {
            key = LdapConstants.CONFIG_NOVELL_DIRECTORY_FILTERS;
        } else if (iLdapType.equalsIgnoreCase(LdapConstants.IDS_LDAP_SERVER)) {
            key = LdapConstants.CONFIG_TDS_FILTERS;
        } else if (iLdapType.equalsIgnoreCase(LdapConstants.SUN_LDAP_SERVER)) {
            key = LdapConstants.CONFIG_SUN_DIRECTORY_FILTERS;
        } else if (iLdapType.equalsIgnoreCase(LdapConstants.NETSCAPE_LDAP_SERVER)) {
            key = LdapConstants.CONFIG_NETSCAPE_DIRECTORY_FILTERS;
        } else if (iLdapType.equalsIgnoreCase(LdapConstants.SECUREWAY_LDAP_SERVER)) {
            key = LdapConstants.CONFIG_SECUREWAY_DIRECTORY_FILTERS;
        } else {
            return;
        }

        List<Map<String, Object>> filterList = Nester.nest(key, configProps);

        // If config is found,
        if (!filterList.isEmpty()) {
            Map<String, Object> props = filterList.get(0);

            if (props.get(LdapConstants.CONFIG_USER_FILTER) != null)
                iUserFilter = (String) props.get(LdapConstants.CONFIG_USER_FILTER);
            if (props.get(LdapConstants.CONFIG_GROUP_FILTER) != null)
                iGroupFilter = (String) props.get(LdapConstants.CONFIG_GROUP_FILTER);
            if (props.get(LdapConstants.CONFIG_USER_ID_FILTER) != null)
                iUserIdMap = (String) props.get(LdapConstants.CONFIG_USER_ID_FILTER);
            if (props.get(LdapConstants.CONFIG_GROUP_ID_FILTER) != null)
                iGroupIdMap = (String) props.get(LdapConstants.CONFIG_GROUP_ID_FILTER);
            if (props.get(LdapConstants.CONFIG_GROUP_MEMBER_ID_FILTER) != null)
                iGroupMemberIdMap = (String) props.get(LdapConstants.CONFIG_GROUP_MEMBER_ID_FILTER);
            // Update the Ldap entities with search filters.
            String objectClassStr = "objectclass=";
            if (iLdapType.equalsIgnoreCase(LdapConstants.AD_LDAP_SERVER))
                objectClassStr = "objectcategory=";

            int length = objectClassStr.length();

            // Parse the User filter and extract the applicable objectclass names
            if (iUserFilter != null) {
                LdapEntity ldapEntity = getLdapEntity(SchemaConstants.DO_PERSON_ACCOUNT);
                if (ldapEntity != null) {
                    Set<String> objClsSet = new HashSet<String>();
                    int index = iUserFilter.indexOf(objectClassStr);
                    while (index > -1) {
                        int endIndex = iUserFilter.indexOf(")", index);
                        String objectClass = iUserFilter.substring(index + length, endIndex);
                        objClsSet.add(objectClass);

                        index = endIndex + 1;
                        index = iUserFilter.indexOf(objectClassStr, endIndex);
                    }
                    if (objClsSet.size() > 0) {
                        ldapEntity.getObjectClasses().clear();
                        ldapEntity.getObjectClasses().addAll(objClsSet);
                    }
                }

                // Set the login property.
                // Remove the default uid login Property if userFilter is configured.
                if (iLoginAttrs != null)
                    iLoginAttrs.remove(0);
                if (iLoginProps != null)
                    iLoginProps.remove(0);

                String pattern = "=%v";
                int startIndex = 0;
                boolean hasLoginProperties = true;
                LdapEntity acct = getLdapEntity(iPersonAccountTypes.get(iPersonAccountTypes.size() - 1));
                while (hasLoginProperties) {
                    int index = iUserFilter.indexOf(pattern, startIndex);
                    int beginIndex = index;
                    if (index > -1) {
                        for (; beginIndex > 0; beginIndex--) {
                            if (iUserFilter.charAt(beginIndex) == ' ' || iUserFilter.charAt(beginIndex) == '(')
                                break;
                        }

                        String propName = iUserFilter.substring(beginIndex + 1, index);
                        if (iLoginAttrs != null && !iLoginAttrs.contains(propName)) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Setting login property from UserFilter as [" + propName + "]");
                            //iLoginAttrs.add(propName);
                            iLoginAttrs.add(getAttributeName(acct, propName));
                            iLoginProps.add(propName);
                        }
                        startIndex = index + 1;
                    } else
                        hasLoginProperties = false;
                }
                if (ldapEntity != null) {
                    try {
                        ldapEntity.addPropertyAttributeMap(SchemaConstants.PROP_PRINCIPAL_NAME, iLoginAttrs.get(0));
                    } catch (IndexOutOfBoundsException e) {
                        throw new WIMSystemException(WIMMessageKey.MALFORMED_SEARCH_EXPRESSION, Tr.formatMessage(
                                                                                                                 tc,
                                                                                                                 WIMMessageKey.MALFORMED_SEARCH_EXPRESSION,
                                                                                                                 WIMMessageHelper.generateMsgParms(e.toString())));
                    }
                }
            }

            // Parse the Group filter and extract the applicable objectclass names
            if (iGroupFilter != null) {
                LdapEntity ldapEntity = getLdapEntity(SchemaConstants.DO_GROUP);
                if (ldapEntity != null) {
                    Set<String> objClsSet = new HashSet<String>();
                    int index = iGroupFilter.indexOf(objectClassStr);
                    while (index > -1) {
                        int endIndex = iGroupFilter.indexOf(")", index);
                        String objectClass = iGroupFilter.substring(index + length, endIndex);
                        objClsSet.add(objectClass);

                        index = endIndex + 1;
                        index = iGroupFilter.indexOf(objectClassStr, endIndex);
                    }
                    if (objClsSet.size() > 0) {
                        ldapEntity.getObjectClasses().clear();
                        ldapEntity.getObjectClasses().addAll(objClsSet);
                    }
                }
            }

            // Parse the user id map
            if (iUserIdMap != null) {
                StringTokenizer strtok = new StringTokenizer(iUserIdMap, ":;");
                LdapEntity ldapEntity = getLdapEntity(SchemaConstants.DO_PERSON_ACCOUNT);
                if (ldapEntity != null) {
                    List<String> rdnPropList = new ArrayList<String>();
                    List<String> objClsList = new ArrayList<String>();
                    while (strtok.hasMoreTokens()) {
                        String objectClass = strtok.nextToken();
                        String attribute = strtok.nextToken();
                        // Handle samAccountName.
                        Set<String> propNames = null;

                        if (LdapConstants.LDAP_ATTR_SAM_ACCOUNT_NAME.equalsIgnoreCase(attribute)) {
                            propNames = getPropertyName(ldapEntity, "cn");
                            // propNames.addAll(getPropertyName(ldapEntity, attribute));
                        } else
                            propNames = getPropertyName(ldapEntity, attribute);

                        rdnPropList.add(propNames.iterator().next());

                        if (!SchemaConstants.VALUE_ALL_PROPERTIES.equalsIgnoreCase(objectClass))
                            objClsList.add(objectClass);
                    }
                    if (rdnPropList.size() > 0) {
                        String[][] rdnProps = new String[rdnPropList.size()][];
                        String[][] rdnAttrs = new String[rdnPropList.size()][];
                        String rdnObjCls[][] = new String[objClsList.size()][];
                        String objCls[] = new String[objClsList.size()];

                        objCls = objClsList.toArray(objCls);
                        for (int j = 0; j < rdnPropList.size(); j++) {
                            rdnProps[j] = LdapHelper.getRDNs(rdnPropList.get(j));
                            rdnAttrs[j] = new String[rdnProps[j].length];
                            for (int k = 0; k < rdnProps[j].length; k++) {
                                String rdnProp = rdnProps[j][k];
                                rdnAttrs[j][k] = getAttributeName(ldapEntity, rdnProp);
                            }
                            if (objCls.length > 0) {
                                rdnObjCls[j] = new String[objCls.length];
                                rdnObjCls[j][0] = objCls[j];
                            }
                        }
                        ldapEntity.setRDNProperties(rdnProps, rdnAttrs);
                        if (isVMMRdnPropertiesDefined) {
                            String updatedRdnAttrs[][] = null;
                            String updatedRdnObjCls[][] = null;
                            if (ldapEntity.getRDNAttributes().length > 0) {
                                String orgRdnAttr[][] = ldapEntity.getRDNAttributes();
                                updatedRdnAttrs = new String[orgRdnAttr.length + rdnAttrs.length][];
                                for (int i = 0; i < orgRdnAttr.length; i++) {
                                    updatedRdnAttrs[i] = new String[orgRdnAttr[i].length];
                                    for (int j = 0; j < orgRdnAttr[i].length; j++)
                                        updatedRdnAttrs[i][j] = orgRdnAttr[i][j];
                                }
                                int len = orgRdnAttr.length;
                                for (int i = 0; i < rdnAttrs.length; i++) {
                                    updatedRdnAttrs[len] = new String[rdnAttrs[i].length];
                                    for (int j = 0; j < rdnAttrs[i].length; j++)
                                        updatedRdnAttrs[len][j] = rdnAttrs[i][j];
                                    len++;
                                }
                            }
                            if (ldapEntity.getRDNObjectclasses().length > 0) {
                                String orgRdnObjCls[][] = ldapEntity.getRDNObjectclasses();
                                updatedRdnObjCls = new String[orgRdnObjCls.length + rdnObjCls.length][];
                                for (int i = 0; i < orgRdnObjCls.length; i++) {
                                    updatedRdnObjCls[i] = new String[orgRdnObjCls[i].length];
                                    for (int j = 0; j < orgRdnObjCls[i].length; j++)
                                        updatedRdnObjCls[i][j] = orgRdnObjCls[i][j];
                                }
                                int len = orgRdnObjCls.length;
                                for (int i = 0; i < rdnObjCls.length; i++) {
                                    updatedRdnObjCls[len] = new String[rdnObjCls[i].length];
                                    for (int j = 0; j < rdnObjCls[i].length; j++)
                                        updatedRdnObjCls[len][j] = rdnObjCls[i][j];
                                    len++;
                                }

                            }
                            ldapEntity.setRDNAttributes(updatedRdnAttrs, updatedRdnObjCls);
                        } else {
                            ldapEntity.setRDNAttributes(rdnAttrs, rdnObjCls);
                        }
                        if (ldapEntity.needTranslateRDN()) {
                            iNeedTranslateRDN = true;
                        }
                    }
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Could not find entity for person account!!");
                }
            }

            // Parse the group id map
            if (iGroupIdMap != null) {
                StringTokenizer strtok = new StringTokenizer(iGroupIdMap, ":;");
                LdapEntity ldapEntity = getLdapEntity(SchemaConstants.DO_GROUP);
                if (ldapEntity != null) {
                    List<String> rdnPropList = new ArrayList<String>();
                    Set<String> objClsSet = new HashSet<String>();
                    while (strtok.hasMoreTokens()) {
                        String objectClass = strtok.nextToken();
                        String attribute = strtok.nextToken();

                        Set<String> propNames = getPropertyName(ldapEntity, attribute);
                        rdnPropList.add(propNames.iterator().next());

                        if (!SchemaConstants.VALUE_ALL_PROPERTIES.equalsIgnoreCase(objectClass))
                            objClsSet.add(objectClass);
                    }
                    if (rdnPropList.size() > 0) {
                        String[][] rdnProps = new String[rdnPropList.size()][];
                        String[][] rdnAttrs = new String[rdnPropList.size()][];
                        for (int j = 0; j < rdnPropList.size(); j++) {
                            rdnProps[j] = LdapHelper.getRDNs(rdnPropList.get(j));
                            rdnAttrs[j] = new String[rdnProps[j].length];
                            for (int k = 0; k < rdnProps[j].length; k++) {
                                String rdnProp = rdnProps[j][k];
                                rdnAttrs[j][k] = getAttributeName(ldapEntity, rdnProp);
                            }
                        }
                        ldapEntity.setRDNProperties(rdnProps, rdnAttrs);
                        if (ldapEntity.needTranslateRDN()) {
                            iNeedTranslateRDN = true;
                        }
                    }
                } else if (tc.isDebugEnabled())
                    Tr.debug(tc, "Could not find entity for Group!!");
            }

            // Parse the group member id map
            if (iGroupMemberIdMap != null) {
                // Check if iGroupMemberIdMap have ibm-allGroups , if true then do the nested search for group members
                iLdapOperationalAttr = iGroupMemberIdMap.toLowerCase().contains(IBM_ALL_GROUPS.toLowerCase());

                // Clear the membership attribute if we were using the default. Otherwise keep it, as it was explicitly set
                if (iDefaultMembershipAttr) {
                    iMembershipAttrName = null;
                }
                LdapEntity ldapEntity = null;
                List<String> grpTypes = getGroupTypes();
                List<String> objectClasses = new ArrayList<String>();
                for (int i = 0; i < grpTypes.size(); i++) {
                    ldapEntity = getLdapEntity(grpTypes.get(i));
                    List<String> objClses = ldapEntity.getObjectClasses();
                    for (int j = 0; j < objClses.size(); j++) {
                        String objCls = objClses.get(j);
                        objectClasses.add(objCls);
                    }
                }

                List<String> attrScopes = new ArrayList<String>();
                List<String> attrNames = new ArrayList<String>();

                StringTokenizer strtok = new StringTokenizer(iGroupMemberIdMap, ":;");
                if (ldapEntity != null) {
                    while (strtok.hasMoreTokens()) {
                        String objectClass = strtok.nextToken();
                        String attribute = strtok.nextToken();
                        String scope = LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP_STRING;
                        if (SchemaConstants.VALUE_ALL_PROPERTIES.equalsIgnoreCase(objectClass)) {
                            for (int j = 0; j < objectClasses.size(); j++) {
                                iMbrAttrMap.put(objectClasses.get(j), attribute);
                            }
                        } else {
                            iMbrAttrMap.put(objectClass.toLowerCase(), attribute);
                        }
                        if (!attrNames.contains(attribute)) {
                            attrNames.add(attribute);
                            attrScopes.add(scope);
                        }
                        if (objectClass != null && !objectClasses.contains(objectClass.toLowerCase())
                            && (getGroupTypes() != null && getGroupTypes().size() > 0)
                            && !SchemaConstants.VALUE_ALL_PROPERTIES.equals(objectClass)) {
                            getLdapEntity(getGroupTypes().get(0)).addObjectClass(objectClass);
                        }
                    }
                    iMbrAttrs = attrNames.toArray(new String[0]);
                    iMbrAttrScope = new short[iMbrAttrs.length];

                    iMbrAttrsAllScope = true;
                    iMbrAttrsNestedScope = true;
                    for (int i = 0; i < attrScopes.size(); i++) {
                        iMbrAttrScope[i] = LdapHelper.getMembershipScope(attrScopes.get(i));
                        if (iMbrAttrScope[i] == LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP) {
                            iMbrAttrsAllScope = false;
                            iMbrAttrsNestedScope = false;
                        } else if (iMbrAttrScope[i] == LdapConstants.LDAP_NESTED_GROUP_MEMBERSHIP) {
                            iMbrAttrsAllScope = false;
                        }
                    }
                } else if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Could not find entity for Group!!");
                }

            }

            // check if this is a RACF configuration
            if (checkIfRacf()) {
                initializeRacfFilters();
            }

            resetEntitySearchFilters();
        }
    }

    /**
     * @return
     */
    private boolean checkIfRacf() {
        if (iUserFilter != null && iUserFilter.toLowerCase().contains("racfid=%v") &&
            iGroupFilter != null && iGroupFilter.toLowerCase().contains("racfid=%v") &&
            iUserIdMap != null && iUserIdMap.equalsIgnoreCase("*:racfid") &&
            iGroupIdMap != null && iGroupIdMap.equalsIgnoreCase("*:racfid") &&
            iGroupMemberIdMap != null && iGroupMemberIdMap.toLowerCase().contains("racfconnectgroupname:racfgroupuserids"))
            return true;
        else
            return false;
    }

    /**
     *
     */
    private void initializeRacfFilters() {
        isRacf = true;
        iMembershipAttrName = LdapConstants.LDAP_ATTR_RACF_CONNECT_GROUP_NAME;
        iMembershipAttrScope = LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP;

        List<String> grpTypes = getGroupTypes();
        for (int i = 0; i < grpTypes.size(); i++) {
            LdapEntity ldapEntity = getLdapEntity(grpTypes.get(i));
            List<String> objClses = ldapEntity.getObjectClasses();
            for (int j = 0; j < objClses.size(); j++) {
                String objCls = objClses.get(j);
                iMbrAttrMap.put(objCls, LdapConstants.RACF_GROUP_USER_ID);
            }
        }

        iMbrAttrs = new String[1];
        iMbrAttrs[0] = LdapConstants.RACF_GROUP_USER_ID;
        iMbrAttrScope = new short[1];
        iMbrAttrScope[0] = LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP;
        iMbrAttrsAllScope = false;
        iMbrAttrsNestedScope = false;

        LdapEntity ldapEntity = getLdapEntity(SchemaConstants.DO_GROUP);
        ldapEntity.addPropertyAttributeMap("cn", LdapConstants.LDAP_ATTR_RACF_ID);
    }

    /**
     *
     */
    private void resetEntitySearchFilters() {
        // Re-create all the required ldapEntity filters
        LdapEntity entity = getLdapEntity(SchemaConstants.DO_PERSON_ACCOUNT);
        if (entity != null)
            entity.setSearchFilter(null);

        entity = getLdapEntity(SchemaConstants.DO_GROUP);
        if (entity != null)
            entity.setSearchFilter(null);
    }

    public Filter getUserFilter() {
        // If there is a filter defined but not initialized, then initialize it.
        if ((iUserFilter != null) && (userFilter == null)) {
            userFilter = new Filter(iUserFilter);
        }

        return userFilter;
    }

    public Filter getGroupFilter() {
        // If there is a filter defined but not initialized, then initialize it.
        if ((iGroupFilter != null) && (groupFilter == null)) {
            groupFilter = new Filter(iGroupFilter);
        }

        return groupFilter;
    }

    /*
     * private void setRDNProperties(Map<String, Object> configProps, Set<String> pids, ConfigurationAdmin configAdmin) {
     * List<String> typeList = getSupportedEntityTypes();
     * for (int i = 0; i < typeList.size(); i++) {
     * String typeName = typeList.get(i);
     * LdapEntity ldapEntity = getLdapEntity(typeName);
     * if (ldapEntity != null) {
     * List<String> rdnPropList = getRDNProperties(typeName, configProps, pids, configAdmin);
     * if (rdnPropList != null) {
     * String[][] rdnProps = new String[rdnPropList.size()][];
     * String[][] rdnAttrs = new String[rdnPropList.size()][];
     * for (int j = 0; j < rdnPropList.size(); j++) {
     * rdnProps[j] = LdapHelper.getRDNs(rdnPropList.get(j));
     * rdnAttrs[j] = new String[rdnProps[j].length];
     * for (int k = 0; k < rdnProps[j].length; k++) {
     * String rdnProp = rdnProps[j][k];
     * rdnAttrs[j][k] = getAttributeName(ldapEntity, rdnProp);
     * }
     * }
     * ldapEntity.setRDNProperties(rdnProps, rdnAttrs);
     * if (ldapEntity.needTranslateRDN()) {
     * iNeedTranslateRDN = true;
     * }
     * }
     * }
     * }
     * }
     */
    private void setConfidentialAttributes() {
        iConAttrs = new HashSet<String>();
        iConAttrs.add(LdapConstants.LDAP_ATTR_UNICODEPWD);
        iConAttrs.add(LdapConstants.LDAP_ATTR_USER_PASSWORD);
    }

    private void setGroupMemberFilter() {
        LdapEntity grpEntity = getLdapEntity(SchemaConstants.DO_GROUP);
        String grpSearchFilter = grpEntity.getSearchFilter();
        StringBuffer filterBuffer = new StringBuffer("(&");
        filterBuffer.append(grpSearchFilter);
        if (iMbrAttrs.length == 1) {
            filterBuffer.append("(").append(iMbrAttrs[0]).append("={0}))");
        } else {
            filterBuffer.append("(|");
            for (int i = 0; i < iMbrAttrs.length; i++) {
                filterBuffer.append("(").append(iMbrAttrs[i]).append("={0})");
            }
            filterBuffer.append("))");
        }
        iGrpMbrFilter = filterBuffer.toString();

    }

    private void setLoginProperties(String loginProps) {
        if (iPersonAccountTypes.size() > 0) {
            LdapEntity acct = getLdapEntity(iPersonAccountTypes.get(iPersonAccountTypes.size() - 1));
            iLoginAttrs = new ArrayList<String>();
            iLoginProps = new ArrayList<String>();
            if (loginProps != null) {
                String[] loginProperties = loginProps.split(";");
                for (int i = 0; i < loginProperties.length; i++) {
                    String propName = loginProperties[i];
                    iLoginAttrs.add(getAttributeName(acct, propName));
                    iLoginProps.add(propName);
                }
                // iLoginAttrs.add(getAttributeName(acct, loginProps));
            }
            // If no login properties specified, the default RDN property will be used
            if (iLoginAttrs.size() == 0) {
                String[][] rdns = acct.getRDNAttributes();
                if (rdns != null && rdns.length > 0) {
                    iLoginAttrs.add(rdns[0][0]);
                    iLoginProps.add(rdns[0][0]);
                } else {
                    iLoginAttrs.add("uid");
                    iLoginProps.add("uid");
                }
            }
            for (int i = 0; i < iPersonAccountTypes.size(); i++) {
                String acctType = iPersonAccountTypes.get(i);
                LdapEntity acctEntity = getLdapEntity(acctType);
                String firstLoginAttr = iLoginAttrs.get(0);
                acctEntity.addPropertyAttributeMap(SchemaConstants.PROP_PRINCIPAL_NAME, firstLoginAttr);
                //PM55970 If login property is uid and it is mapped to cn attribute and
                //now cn property is mapped to sn attribute, then mapping of cn property is
                //not correct, it is mapped to cn(firstLoginAttr) instead of sn... so commenting this line.
                //acctEntity.addPropertyAttributeMap(firstLoginAttr, firstLoginAttr);
            }
        }
    }

    private void setAttributes(Map<String, Object> configProps) {
        iAttrs = new HashSet<String>();
        iOperAttrs = new HashSet<String>();
        iAttrNameToAttrMap = new Hashtable<String, LdapAttribute>();
        iPropToAttrMap = new Hashtable<String, String>();
        iAttrToPropMap = new Hashtable<String, Set<String>>();
        iDefaultValueAttrs = new HashSet<String>();
        iDefaultAttrAttrs = new HashSet<String>();
        iDataTypes = new ArrayList<String>();

        Set<String> unProps = new HashSet<String>();
        Map<String, Set<String>> entityUnProps = new HashMap<String, Set<String>>();
        Map<String, List<Map<String, Object>>> propMap = Nester.nest(configProps, LdapConstants.CONFIG_DO_ATTRIBUTE_CONFIGUARTION);
        List<Map<String, Object>> attrConfigList = propMap.get(LdapConstants.CONFIG_DO_ATTRIBUTE_CONFIGUARTION);

        // Set the default attributes and then override if applicable.
        setDefaultAttributes();

        if (!attrConfigList.isEmpty()) {
            Map<String, Object> attrConfig = attrConfigList.get(0);

            List<Map<String, Object>> attrDOs = Nester.nest(LdapConstants.CONFIG_DO_ATTRIBUTES, attrConfig);
            for (Map<String, Object> attrDO : attrDOs) {
                addAttribute(attrDO);
            }

            // Read properties not supported by the LDAP repository
            List<Map<String, Object>> propNotSupported = Nester.nest(LdapConstants.CONFIG_DO_PROPERTIES_NOT_SUPPORTED, attrConfig);
            for (Map<String, Object> uPropDO : propNotSupported) {
                String unPropName = (String) uPropDO.get(LdapConstants.CONFIG_PROP_PROPERTY_NAME);
                List<String> entityTypes = (List<String>) uPropDO.get(LdapConstants.CONFIG_PROP_ENTITY_TYPES);
                if (entityTypes == null || entityTypes.size() == 0) {
                    // apply to all entity types.
                    unProps.add(unPropName);
                    if (unPropName.equals("ibm-primaryEmail"))
                        unProps.add("ibmPrimaryEmail");
                    else if (unPropName.equals("ibm-jobTitle"))
                        unProps.add("ibmJobTitle");
                } else {
                    // apply to different entity types.
                    for (int j = 0; j < entityTypes.size(); j++) {
                        String qTypeName = entityTypes.get(j);
                        Set<String> props = entityUnProps.get(qTypeName);
                        if (props == null) {
                            props = new HashSet<String>();
                            entityUnProps.put(qTypeName, props);
                        }
                        props.add(unPropName);
                        if (unPropName.equals("ibm-primaryEmail"))
                            props.add("ibmPrimaryEmail");
                        else if (unPropName.equals("ibm-jobTitle"))
                            props.add("ibmJobTitle");
                    }
                }
            }
        } else {
            unProps.add("homeAddress");
            unProps.add("businessAddress");
            if (iLdapType.startsWith(LdapConstants.AD_LDAP_SERVER)) {
                unProps.add("description");
                unProps.add("jpegPhoto");
                unProps.add("labeledURI");
                unProps.add("carLicense");
                unProps.add("pager");
                unProps.add("roomNumber");
                unProps.add("localityName");
                unProps.add("stateOrProvinceName");
                unProps.add("countryName");
                unProps.add("employeeNumber");
                unProps.add("employeeType");
                unProps.add("businessCategory");
                unProps.add("departmentNumber");
            }
        }

        // Hardcode operational attrs for now
        iOperAttrs.add(SchemaConstants.PROP_CREATE_TIMESTAMP);
        iOperAttrs.add(SchemaConstants.PROP_MODIFY_TIMESTAMP);

        // Set supported attributes
        // New:: Modified the supported properties code.
        for (int i = 0; i < iLdapEntities.size(); i++) {
            LdapEntity ldapEntity = iLdapEntities.get(i);
            String qName = ldapEntity.getName();

            // If the entity name is null OR
            // It is neither a Person or a Group, skip the entity.
            if (qName == null ||
                ((!qName.equalsIgnoreCase(SchemaConstants.DO_PERSON)) &&
                 (!qName.equalsIgnoreCase(SchemaConstants.DO_PERSON_ACCOUNT)) &&
                 (!qName.equalsIgnoreCase(SchemaConstants.DO_GROUP))))
                continue;

            Set<String> notSupportedProps = entityUnProps.get(qName);
            Set<String> totalUnSupportedProp = unProps;
            if (notSupportedProps != null) {
                totalUnSupportedProp.addAll(notSupportedProps);
            }

            String[] supportedProps = null;
            Entity entity = null;

            if (qName.equalsIgnoreCase(SchemaConstants.DO_GROUP)) {
                supportedProps = iGroupSupportedProps;
                entity = new Group();
            } else {
                supportedProps = iPersonSupportedProps;
                entity = new Person();
            }

            for (int j = 0; j < supportedProps.length; j++) {
                String qPropName = supportedProps[j];
                if (!totalUnSupportedProp.contains(qPropName)) {
                    String attrName = getAttributeName(ldapEntity, qPropName);
                    String dataType = entity.getDataType(qPropName);
                    if (!iDataTypes.contains(dataType)) {
                        iDataTypes.add(dataType);
                    }
                    if (!iOperAttrs.contains(attrName)) {
                        iAttrs.add(attrName);
                    }
                    if (ldapEntity.getAttribute(qPropName) == null) {
                        ldapEntity.addPropertyAttributeMap(qPropName, attrName);
                    }
                }
            }
        }
    }

    private void setDefaultAttributes() {
        iDataTypes.add(SchemaConstants.DATA_TYPE_STRING);
        iDataTypes.add(SchemaConstants.DATA_TYPE_BASE_64_BINARY);
        if (iLdapType.equals("ADAM")) {
            // unicodePwd
            LdapAttribute ldapAttr = new LdapAttribute(LdapConstants.LDAP_ATTR_UNICODEPWD);
            ldapAttr.setSyntax(LdapConstants.LDAP_ATTR_SYNTAX_UNICODEPWD);
            iAttrNameToAttrMap.put(LdapConstants.LDAP_ATTR_UNICODEPWD.toLowerCase(), ldapAttr);
            iPropToAttrMap.put(SchemaConstants.PROP_PASSWORD, LdapConstants.LDAP_ATTR_UNICODEPWD);
            Set<String> propSet = new HashSet<String>();
            propSet.add(SchemaConstants.PROP_PASSWORD);
            iAttrToPropMap.put(LdapConstants.LDAP_ATTR_UNICODEPWD.toLowerCase(), propSet);

            //groupType
            ldapAttr = new LdapAttribute(LdapConstants.LDAP_ATTR_GROUP_TYPE);
            iDefaultValueAttrs.add(LdapConstants.LDAP_ATTR_GROUP_TYPE.toLowerCase());
            ldapAttr.setDefaultValue(SchemaConstants.DO_GROUP, "8");
            iAttrNameToAttrMap.put(LdapConstants.LDAP_ATTR_GROUP_TYPE.toLowerCase(), ldapAttr);
        } else if (iLdapType.startsWith(LdapConstants.AD_LDAP_SERVER)) {
            // samAccountName
            LdapAttribute ldapAttr = new LdapAttribute(LdapConstants.LDAP_ATTR_SAM_ACCOUNT_NAME);
            LdapEntity ldapEntity = getLdapEntity(SchemaConstants.DO_PERSON_ACCOUNT);
            ldapEntity.addPropertyAttributeMap("uid", LdapConstants.LDAP_ATTR_SAM_ACCOUNT_NAME);

            //ldapEntity = getLdapEntity(DO_GROUP);
            iDefaultAttrAttrs.add(LdapConstants.LDAP_ATTR_SAM_ACCOUNT_NAME.toLowerCase());
            ldapAttr.addEntityType(SchemaConstants.DO_GROUP);
            ldapAttr.setDefaultAttribute(SchemaConstants.DO_GROUP, "cn");

            // unicodePwd
            ldapAttr = new LdapAttribute(LdapConstants.LDAP_ATTR_UNICODEPWD);
            ldapAttr.setSyntax(LdapConstants.LDAP_ATTR_SYNTAX_UNICODEPWD);
            iAttrNameToAttrMap.put(LdapConstants.LDAP_ATTR_UNICODEPWD.toLowerCase(), ldapAttr);
            iPropToAttrMap.put(SchemaConstants.PROP_PASSWORD, LdapConstants.LDAP_ATTR_UNICODEPWD);
            Set<String> propSet = new HashSet<String>();
            propSet.add(SchemaConstants.PROP_PASSWORD);
            iAttrToPropMap.put(LdapConstants.LDAP_ATTR_UNICODEPWD.toLowerCase(), propSet);

            //userAccountControl
            ldapAttr = new LdapAttribute(LdapConstants.LDAP_ATTR_USER_ACCOUNT_CONTROL);
            iDefaultValueAttrs.add(LdapConstants.LDAP_ATTR_USER_ACCOUNT_CONTROL.toLowerCase());
            ldapAttr.setDefaultValue(SchemaConstants.DO_PERSON_ACCOUNT, "544");
            iAttrNameToAttrMap.put(LdapConstants.LDAP_ATTR_USER_ACCOUNT_CONTROL.toLowerCase(), ldapAttr);

            //groupType
            ldapAttr = new LdapAttribute(LdapConstants.LDAP_ATTR_GROUP_TYPE);
            iDefaultValueAttrs.add(LdapConstants.LDAP_ATTR_GROUP_TYPE.toLowerCase());
            ldapAttr.setDefaultValue(SchemaConstants.DO_GROUP, "8");
            iAttrNameToAttrMap.put(LdapConstants.LDAP_ATTR_GROUP_TYPE.toLowerCase(), ldapAttr);
        } else {
            LdapAttribute ldapAttr = new LdapAttribute(LdapConstants.LDAP_ATTR_USER_PASSWORD);
            iAttrNameToAttrMap.put(LdapConstants.LDAP_ATTR_USER_PASSWORD.toLowerCase(), ldapAttr);
            iPropToAttrMap.put(SchemaConstants.PROP_PASSWORD, LdapConstants.LDAP_ATTR_USER_PASSWORD);
            Set<String> propSet = new HashSet<String>();
            propSet.add(SchemaConstants.PROP_PASSWORD);
            iAttrToPropMap.put(LdapConstants.LDAP_ATTR_USER_PASSWORD.toLowerCase(), propSet);
        }
    }

    private void addAttribute(Map<String, Object> attrDO) {
        String attrName = (String) attrDO.get(LdapConstants.CONFIG_PROP_NAME);
        String attrKey = attrName.toLowerCase();

        LdapAttribute ldapAttr = iAttrNameToAttrMap.get(attrKey);
        if (ldapAttr == null) {
            ldapAttr = new LdapAttribute(attrName);
            iAttrNameToAttrMap.put(attrKey, ldapAttr);
        }
        ldapAttr.setSyntax((String) attrDO.get(LdapConstants.CONFIG_PROP_SYNTAX));

        String propName = (String) attrDO.get(LdapConstants.CONFIG_PROP_PROPERTY_NAME);
        String entityType = (String) attrDO.get(LdapConstants.CONFIG_PROP_ENTITY_TYPE);
        if (entityType == null || entityType.length() == 0) {
            if (propName != null) {
                // apply to all entity types.
                iPropToAttrMap.put(propName, attrName);
                Set<String> propSet = iAttrToPropMap.get(attrKey);
                if (propSet == null) {
                    propSet = new HashSet<String>();
                    iAttrToPropMap.put(attrKey, propSet);
                }
                propSet.add(propName);
            }
            if (attrDO.get(LdapConstants.CONFIG_PROP_DEFAULT_VALUE) != null) {
                iDefaultValueAttrs.add(attrKey);
                for (int j = 0; j < iLdapEntityTypeList.size(); j++) {
                    ldapAttr.setDefaultValue(iLdapEntityTypeList.get(j),
                                             (String) attrDO.get(LdapConstants.CONFIG_PROP_DEFAULT_VALUE));
                }
            }
            if (attrDO.get(LdapConstants.CONFIG_PROP_DEFAULT_ATTRIBUTE) != null) {
                iDefaultAttrAttrs.add(attrKey);
                for (int j = 0; j < iLdapEntityTypeList.size(); j++) {
                    ldapAttr.setDefaultAttribute(iLdapEntityTypeList.get(j),
                                                 (String) attrDO.get(LdapConstants.CONFIG_PROP_DEFAULT_ATTRIBUTE));
                }
            }
            if (propName != null) {
                for (int i = 0; i < iLdapEntities.size(); i++) {
                    if (iLdapEntities.get(i).getProperty(propName) != null) {
                        iLdapEntities.get(i).addPropertyAttributeMap(propName, attrName);
                    }
                }
            }
        } else {
            // apply to specified entity type.
            String qTypeName = entityType;
            if (propName != null) {
                LdapEntity ldapEntity = getLdapEntity(qTypeName);
                ldapEntity.addPropertyAttributeMap(propName, attrName);
            }
            ldapAttr.addEntityType(qTypeName);
            if (attrDO.get(LdapConstants.CONFIG_PROP_DEFAULT_VALUE) != null) {
                iDefaultValueAttrs.add(attrKey);
                ldapAttr.setDefaultValue(qTypeName, (String) attrDO.get(LdapConstants.CONFIG_PROP_DEFAULT_VALUE));
            }
            if (attrDO.get(LdapConstants.CONFIG_PROP_DEFAULT_ATTRIBUTE) != null) {
                iDefaultAttrAttrs.add(attrKey);
                ldapAttr.setDefaultAttribute(qTypeName, (String) attrDO.get(LdapConstants.CONFIG_PROP_DEFAULT_ATTRIBUTE));
            }
        }
    }

    private void setExtIdAttributes(Map<String, Object> configProps) {
        iExtIds = new HashSet<String>();
        List<Map<String, Object>> attrConfigList = Nester.nest(LdapConstants.CONFIG_DO_ATTRIBUTE_CONFIGUARTION, configProps);

        if (!attrConfigList.isEmpty()) {
            Map<String, Object> attrConfig = attrConfigList.get(0);

            List<Map<String, Object>> extIds = Nester.nest(LdapConstants.CONFIG_DO_EXTERNAL_ID_ATTRIBUTE, attrConfig);

            for (Map<String, Object> extIdDO : extIds) {

                String attrName = (String) extIdDO.get(LdapConstants.CONFIG_PROP_NAME);
                LdapAttribute ldapAttr = new LdapAttribute(attrName);
                ldapAttr.setSyntax((String) extIdDO.get(LdapConstants.CONFIG_PROP_SYNTAX));
                ldapAttr.setWIMGenerate((Boolean) extIdDO.get(LdapConstants.CONFIG_PROP_AUTO_GENERATE));
                iAttrNameToAttrMap.put(attrName.toLowerCase(), ldapAttr);
                String entityType = (String) extIdDO.get(LdapConstants.CONFIG_PROP_ENTITY_TYPE);
                if (entityType == null) {
                    for (int j = 0; j < iLdapEntities.size(); j++) {
                        iLdapEntities.get(j).setExtId(attrName);
                    }
                } else {
                    LdapEntity ldapEntity = getLdapEntity(entityType);
                    ldapEntity.setExtId(attrName);
                }
                if (attrName.equalsIgnoreCase(LdapConstants.LDAP_DISTINGUISHED_NAME)) { //Ranjan
                    isAnyExtIdDN = true;
                }
                iExtIds.add(attrName.toLowerCase());
            }
        }
        // Default extId
        String extId = null;
        String syntax = LdapConstants.LDAP_ATTR_SYNTAX_STRING;
        if (iLdapType.startsWith(LdapConstants.IDS_LDAP_SERVER) && !iLdapType.startsWith(LdapConstants.CONFIG_LDAP_IDS4)) {
            extId = LdapConstants.LDAP_ATTR_IBMENTRYUUID;
        } else if (iLdapType.startsWith(LdapConstants.AD_LDAP_SERVER)) {
            extId = LdapConstants.LDAP_ATTR_OBJECTGUID;
            syntax = LdapConstants.LDAP_ATTR_SYNTAX_OCTETSTRING;
        } else if (iLdapType.startsWith(LdapConstants.SUN_LDAP_SERVER)) {
            extId = LdapConstants.LDAP_ATTR_NSUNIQUEID;
        } else if (iLdapType.startsWith(LdapConstants.NOVELL_LDAP_SERVER)) {
            extId = LdapConstants.LDAP_ATTR_GUID;
            syntax = LdapConstants.LDAP_ATTR_SYNTAX_OCTETSTRING;
        } else {
            extId = LdapConstants.LDAP_DISTINGUISHED_NAME;
        }
        boolean useDefault = false;
        for (int i = 0; i < iLdapEntities.size(); i++) {
            if (iLdapEntities.get(i).getExtId() == null) {
                iLdapEntities.get(i).setExtId(extId);
                useDefault = true;
            }
        }
        if (useDefault) {
            if (!extId.equalsIgnoreCase(LdapConstants.LDAP_DISTINGUISHED_NAME)) {
                iExtIds.add(extId.toLowerCase());
                LdapAttribute ldapAttr = new LdapAttribute(extId);
                ldapAttr.setSyntax(syntax);
                iAttrNameToAttrMap.put(extId.toLowerCase(), ldapAttr);
            } else {
                isAnyExtIdDN = true;
            }
        }
    }

    private void setMembershipAttribute(Map<String, Object> groupConfig) {
        List<Map<String, Object>> membershipPropList = Nester.nest(LdapConstants.CONFIG_DO_MEMBERSHIP_ATTRIBUTES, groupConfig);

        if (membershipPropList.isEmpty()) {
            if (iLdapType.startsWith(LdapConstants.AD_LDAP_SERVER)) {
                iMembershipAttrName = LdapConstants.LDAP_ATTR_MEMBER_OF;
                iMembershipAttrScope = LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP;
            }
            if (iLdapType.startsWith(LdapConstants.SUN_LDAP_SERVER)) {
                iMembershipAttrName = "nsRoleDN";
                iMembershipAttrScope = LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP;
            }
        } else {
            iDefaultMembershipAttr = false;
            Map<String, Object> mbrshipAttr = membershipPropList.get(0);
            String name = (String) mbrshipAttr.get(LdapConstants.CONFIG_PROP_NAME);
            if (name.trim().length() == 0) {
                iMembershipAttrName = null;
            } else {
                iMembershipAttrName = name;

                String scope = (String) mbrshipAttr.get(LdapConstants.CONFIG_PROP_SCOPE);
                if (scope == null) {
                    iMembershipAttrScope = LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP;
                } else {
                    iMembershipAttrScope = LdapHelper.getMembershipScope(scope);
                }
            }
        }
    }

    private void setMemberAttributes(Map<String, Object> groupConfig) {
        List<Map<String, Object>> memberPropList = Nester.nest(LdapConstants.CONFIG_DO_MEMBER_ATTRIBUTES, groupConfig);

        int size = memberPropList.size();

        List<String> grpTypes = getGroupTypes();
        List<String> objectClasses = new ArrayList<String>();
        for (int i = 0; i < grpTypes.size(); i++) {
            LdapEntity ldapEntity = getLdapEntity(grpTypes.get(i));
            List<String> objClses = ldapEntity.getObjectClasses();
            for (int j = 0; j < objClses.size(); j++) {
                String objCls = objClses.get(j);
                objectClasses.add(objCls);
            }
        }

        iDummyMbrMap = new HashMap<String, String>();
        if (size > 0) {
            iMbrAttrMap = new HashMap<String, String>(size);

            List<String> attrScopes = new ArrayList<String>(size);
            List<String> attrNames = new ArrayList<String>(size);

            for (Map<String, Object> mbrAttrDO : memberPropList) {
                String name = (String) mbrAttrDO.get(LdapConstants.CONFIG_PROP_NAME);
                if (name != null && name.trim().length() > 0) {
                    String objCls = (String) mbrAttrDO.get(LdapConstants.CONFIG_DO_OBJECTCLASS);
                    String scope = (String) mbrAttrDO.get(LdapConstants.CONFIG_PROP_SCOPE);
                    String dummy = (String) mbrAttrDO.get(LdapConstants.CONFIG_PROP_DUMMY_MEMBER);
                    // Default scope is direct scope.
                    if (scope == null) {
                        scope = LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP_STRING;
                    }
                    if (objCls == null) {
                        // If object class is not specified, the member attr applies to all group object classes
                        for (int j = 0; j < objectClasses.size(); j++) {
                            iMbrAttrMap.put(objectClasses.get(j), name);
                        }
                    } else {
                        iMbrAttrMap.put(objCls.toLowerCase(), name);
                    }
                    if (dummy != null) {
                        if (dummy.trim().length() == 0) {
                            iDummyMbrMap.remove(name);
                        } else {
                            iDummyMbrMap.put(name, dummy);
                        }
                    } else {
                        // IDS and DOMINO's group needs member or uniqueMember mandatory
                        if (iLdapType.startsWith(LdapConstants.IDS_LDAP_SERVER) || iLdapType.startsWith(LdapConstants.DOMINO_LDAP_SERVER)) {
                            iDummyMbrMap.put(name, LdapConstants.LDAP_DUMMY_MEMBER_DEFAULT);
                        }
                    }
                    if (!attrNames.contains(name)) {
                        attrNames.add(name);
                        attrScopes.add(scope);
                    }
                    if (objCls != null && !objectClasses.contains(objCls.toLowerCase())) {
                        if (getGroupTypes() != null && getGroupTypes().size() > 0)
                            getLdapEntity(getGroupTypes().get(0)).addObjectClass(objCls);
                    }
                }
            }
            iMbrAttrs = attrNames.toArray(new String[0]);
            iMbrAttrScope = new short[iMbrAttrs.length];

            iMbrAttrsAllScope = true;
            iMbrAttrsNestedScope = true;
            for (int i = 0; i < attrScopes.size(); i++) {
                iMbrAttrScope[i] = LdapHelper.getMembershipScope(attrScopes.get(i));
                if (iMbrAttrScope[i] == LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP) {
                    iMbrAttrsAllScope = false;
                    iMbrAttrsNestedScope = false;
                } else if (iMbrAttrScope[i] == LdapConstants.LDAP_NESTED_GROUP_MEMBERSHIP) {
                    iMbrAttrsAllScope = false;
                }
            }
        } else {
            // If groupMemberAttributeMap is not set, default it to "member" and "direct"
            // Get group member attribute
            iMbrAttrMap = new HashMap<String, String>(objectClasses.size());
            iMbrAttrScope = new short[objectClasses.size()];
            for (int i = 0; i < objectClasses.size(); i++) {
                iMbrAttrMap.put(objectClasses.get(i), LdapConstants.LDAP_ATTR_MEMBER_DEFAULT);
                iMbrAttrScope[i] = LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP;
            }
            iMbrAttrs = new String[1];
            iMbrAttrs[0] = LdapConstants.LDAP_ATTR_MEMBER_DEFAULT;

            // IDS and DOMINO's group needs member or uniqueMember mandatory
            if (iLdapType.startsWith(LdapConstants.IDS_LDAP_SERVER) || iLdapType.startsWith(LdapConstants.DOMINO_LDAP_SERVER)) {
                iDummyMbrMap.put(LdapConstants.LDAP_ATTR_MEMBER_DEFAULT, LdapConstants.LDAP_DUMMY_MEMBER_DEFAULT);
            }
            iUseDefaultMbrAttr = true;
        }
    }

    private void setDynaMemberAttributes(Map<String, Object> groupConfig) {
        List<Map<String, Object>> dynaMbrAttrList = Nester.nest(LdapConstants.CONFIG_DO_DYNAMIC_MEMBER_ATTRIBUTES, groupConfig);
        // String groupConfiguration = (String) configProps.get("groupProperties");

        LdapEntity ldapEntry = getLdapEntity(SchemaConstants.DO_GROUP);
        iDynaMbrAttrMap = new HashMap<String, String>();
        //List attrScopes = new ArrayList(size);
        List<String> attrNames = new ArrayList<String>();

        List<String> grpTypes = getGroupTypes();
        List<String> objectClasses = new ArrayList<String>();
        for (int i = 0; i < grpTypes.size(); i++) {
            LdapEntity ldapEntity = getLdapEntity(grpTypes.get(i));
            List<String> objClses = ldapEntity.getObjectClasses();
            for (int j = 0; j < objClses.size(); j++) {
                String objCls = objClses.get(j);
                objectClasses.add(objCls);
            }
        }

        if (!dynaMbrAttrList.isEmpty()) {

            Map<String, Object> mbrshipAttrDO = dynaMbrAttrList.get(0);
            String name = (String) mbrshipAttrDO.get(LdapConstants.CONFIG_PROP_NAME);
            if (name != null && name.trim().length() > 0) {
                String objCls = (String) mbrshipAttrDO.get(LdapConstants.CONFIG_DO_OBJECTCLASS);
                if (objCls == null) {
                    // If object class is not specified, the member attr applies to all group object classes
                    for (int j = 0; j < objectClasses.size(); j++) {
                        iDynaMbrAttrMap.put(objectClasses.get(j), name);
                        iDynaMbrObjectClass.add(objectClasses.get(j));
                    }
                } else {
                    iDynaMbrAttrMap.put(objCls.toLowerCase(), name);
                    iDynaMbrObjectClass.add(objCls);
                }
                if (!attrNames.contains(name)) {
                    attrNames.add(name);
                }
                if (objCls != null && !ldapEntry.getObjectClasses().contains(objCls.toLowerCase())) {
                    ldapEntry.addObjectClass(objCls);
                }
            }
            iDynaMbrAttrs = attrNames.toArray(new String[0]);
            setDynamicGroupFilter();
        }
    }

    private void setDynamicGroupFilter() {
        LdapEntity grpEntity = getLdapEntity(SchemaConstants.DO_GROUP);
        String grpSearchFilter = grpEntity.getSearchFilter();
        StringBuffer filterBuffer = new StringBuffer("(&");
        if ((iDynaMbrObjectClass != null) && (iDynaMbrObjectClass.size() > 0)) {
            if (iDynaMbrObjectClass.size() == 1) {
                filterBuffer.append("(|(objectclass=").append(iDynaMbrObjectClass.get(0)).append(")");
                filterBuffer.append(grpSearchFilter);
                filterBuffer.append(")");
            } else {
                filterBuffer.append("(|");
                for (int i = 0; i < iDynaMbrObjectClass.size(); i++) {
                    filterBuffer.append("(objectclass=").append(iDynaMbrObjectClass.get(i)).append(")");
                }
                filterBuffer.append(grpSearchFilter);
                filterBuffer.append(")");
            }
        } else {
            filterBuffer.append(grpSearchFilter);
        }
        if (iDynaMbrAttrs.length == 1) {
            filterBuffer.append("(").append(iDynaMbrAttrs[0]).append("=*))");
        } else {
            filterBuffer.append("(|");
            for (int i = 0; i < iDynaMbrAttrs.length; i++) {
                filterBuffer.append("(").append(iDynaMbrAttrs[i]).append("=*)");
            }
            filterBuffer.append("))");
        }
        iDynaGrpFilter = filterBuffer.toString();

    }

    private void setLDAPEntities(List<Map<String, Object>> entityList, List<HashMap<String, String>> baseEntries) throws WIMException {
        if (!entityList.isEmpty()) {
            int size = entityList.size();
            iLdapEntityTypeList = new ArrayList<String>(size);
            iLdapEntities = new ArrayList<LdapEntity>(size);

            iPersonAccountTypes = new ArrayList<String>(size);
            iPersonTypes = new ArrayList<String>(size);
            iGroupTypes = new ArrayList<String>(size);
            List<String> grpSearchBases = new ArrayList<String>();

            for (Map<String, Object> entityProps : entityList) {

                String entityType = (String) entityProps.get(LdapConstants.CONFIG_PROP_NAME);
                String entityTypeSearchFilter = (String) entityProps.get(LdapConstants.CONFIG_PROP_SEARCHFILTER);
                String[] objectClasses = (String[]) entityProps.get(LdapConstants.CONFIG_DO_OBJECTCLASS);

                if (getLdapEntity(entityType) != null) {
                    throw new InitializationException(WIMMessageKey.DUPLICATE_ENTITY_TYPE, Tr.formatMessage(
                                                                                                            tc,
                                                                                                            WIMMessageKey.DUPLICATE_ENTITY_TYPE,
                                                                                                            WIMMessageHelper.generateMsgParms(entityType)));
                }

                List<Map<String, Object>> rdnAttributes = Nester.nest(LdapConstants.CONFIG_DO_RDN_PROPERTY, entityProps);

                if (!rdnAttributes.isEmpty()) {
                    isVMMRdnPropertiesDefined = true;
                }

                LdapEntity ldapEntity = new LdapEntity(entityType, entityTypeSearchFilter, objectClasses, rdnAttributes);
                if (rdnAttributes.isEmpty()) {
                    setDefaultRDNs(ldapEntity);
                }

                if (entityTypeSearchFilter == null || entityTypeSearchFilter.trim().length() == 0) {
                    if (SchemaConstants.DO_PERSON_ACCOUNT.equals(entityType) || Entity.getSubEntityTypes(SchemaConstants.DO_PERSON_ACCOUNT).contains(entityType)) {
                        if (iLdapType.startsWith(LdapConstants.AD_LDAP_SERVER)) {
                            ldapEntity.setSearchFilter("(ObjectCategory=Person)");
                        } else {
                            ldapEntity.setSearchFilter(null);
                        }
                        iPersonAccountTypes.add(SchemaConstants.DO_PERSON_ACCOUNT);
                    }
                    // New:: Had to add this condition as "Entity.getSubEntityTypes(SchemaConstants.DO_GROUP)" is returning null at runtime
                    else if (SchemaConstants.DO_GROUP.equals(entityType) ||
                             (Entity.getSubEntityTypes(SchemaConstants.DO_GROUP) != null && Entity.getSubEntityTypes(SchemaConstants.DO_GROUP).contains(entityType))) {
                        if (iLdapType.startsWith(LdapConstants.AD_LDAP_SERVER)) {
                            ldapEntity.setSearchFilter("(ObjectCategory=Group)");
                        } else {
                            ldapEntity.setSearchFilter(null);
                        }
                    } else if (SchemaConstants.DO_ORGCONTAINER.equals(entityType) || Entity.getSubEntityTypes(SchemaConstants.DO_ORGCONTAINER).contains(entityType)) {
                        ldapEntity.setSearchFilter(null);
                    }
                } else {
                    ldapEntity.setSearchFilter(entityTypeSearchFilter);
                    // If entity is of type "PersonAccount", add it to the list of PersonAccount Entity types.
                    if (SchemaConstants.DO_PERSON_ACCOUNT.equals(entityType) || Entity.getSubEntityTypes(SchemaConstants.DO_PERSON_ACCOUNT).contains(entityType))
                        iPersonAccountTypes.add(SchemaConstants.DO_PERSON_ACCOUNT);
                }

                if (objectClasses != null) {
                    List<String> objclsList = new ArrayList<String>();
                    for (int j = 0; j < objectClasses.length; j++) {
                        objclsList.add(objectClasses[j]);
                    }
                    ldapEntity.setObjectClasses(objclsList);
                    ldapEntity.setObjectClassesForCreate(objclsList);
                } else {
                    if (SchemaConstants.DO_PERSON_ACCOUNT.equals(entityType) || Entity.getSubEntityTypes(SchemaConstants.DO_PERSON_ACCOUNT).contains(entityType)) {
                        List<String> objclsList = new ArrayList<String>();
                        if (iLdapType.startsWith(LdapConstants.AD_LDAP_SERVER)) {
                            objclsList.add("user");
                        } else {
                            objclsList.add("inetOrgPerson");
                        }
                        ldapEntity.setObjectClasses(objclsList);
                        ldapEntity.setObjectClassesForCreate(objclsList);
                    } else if (SchemaConstants.DO_GROUP.equals(entityType) || Entity.getSubEntityTypes(SchemaConstants.DO_GROUP).contains(entityType)) {
                        List<String> objclsList = new ArrayList<String>();
                        if (iLdapType.startsWith(LdapConstants.AD_LDAP_SERVER)) {
                            objclsList.add("group");
                        } else {
                            objclsList.add("groupOfNames");
                        }
                        ldapEntity.setObjectClasses(objclsList);
                        ldapEntity.setObjectClassesForCreate(objclsList);
                    } else if (SchemaConstants.DO_ORGCONTAINER.equals(entityType) || Entity.getSubEntityTypes(SchemaConstants.DO_ORGCONTAINER).contains(entityType)) {
                        List<String> objclsList = new ArrayList<String>();
                        objclsList.add("organization");
                        objclsList.add("organizationalUnit");
                        ldapEntity.setObjectClasses(objclsList);
                        ldapEntity.setObjectClassesForCreate(objclsList);
                    }
                }

                String[] searchBases = (String[]) entityProps.get(LdapConstants.CONFIG_PROP_SEARCHBASES);
                if (searchBases != null) {
                    searchBases = validateSearchBases(searchBases, baseEntries);
                    ldapEntity.setSearchBases(searchBases);
                }

                if (ldapEntity.getSearchBases() == null) {
                    ldapEntity.setSearchBases(getTopLdapNodes());
                } else {
                    // SearchBases get from config is WIM Node, need switch to LDAP node
                    String[] bases = ldapEntity.getSearchBases();
                    String[] ldapBases = new String[bases.length];
                    for (int j = 0; j < bases.length; j++) {
                        ldapBases[j] = switchToLdapNode(bases[j]);
                    }
                    ldapEntity.setSearchBases(ldapBases);
                }

                iLdapEntityTypeList.add(entityType);
                iLdapEntities.add(ldapEntity);
                if (Entity.getSubEntityTypes(SchemaConstants.DO_PERSON_ACCOUNT).contains(entityType)) {
                    iPersonAccountTypes.add(entityType);
                } else if (Entity.getSubEntityTypes(SchemaConstants.DO_PERSON).contains(entityType)) {
                    iPersonTypes.add(entityType);
                }
                // New:: Had to add this condition as "Entity.getSubEntityTypes(SchemaConstants.DO_GROUP)" is returning null at runtime
                else if (SchemaConstants.DO_GROUP.equalsIgnoreCase(entityType) ||
                         (Entity.getSubEntityTypes(SchemaConstants.DO_GROUP) != null && Entity.getSubEntityTypes(SchemaConstants.DO_GROUP).contains(entityType))) {
                    iGroupTypes.add(entityType);
                    grpSearchBases.addAll(ldapEntity.getSearchBaseList());
                }
            }
            iGroupSearchBases = grpSearchBases.toArray(new String[0]);
            iGroupSearchBases = NodeHelper.getTopNodes(iGroupSearchBases);
            // It is called to set the any leftover default LDAP entity type that is not defined in the configuration
            setDefaultLDAPEntries();
        } else {
            setDefaultLDAPEntries();
        }
    }

    /**
     * @param searchBases
     * @param baseEntries
     * @return
     */
    private String[] validateSearchBases(String[] searchBases, List<HashMap<String, String>> baseEntries) {

        final String METHODNAME = "validateSearchBases(String[], List<HashMap<String, String>>)";
        List<String> validSearchBase = new ArrayList<String>();

        boolean isValid = false;
        for (int j = 0; j < searchBases.length; j++) {
            for (int i = 0; i < baseEntries.size(); i++) {
                /*
                 * HashMap<String, String> baseEntrymap = new HashMap<String, String>();
                 * baseEntrymap = baseEntries.get(i);
                 */
                Set<String> keys = baseEntries.get(i).keySet();
                Iterator<String> itr = keys.iterator();

                while (itr.hasNext()) {
                    String key = itr.next();

                    if (searchBases[j].trim().toLowerCase().endsWith(key.toLowerCase())) {
                        isValid = true;
                        validSearchBase.add(searchBases[j]);
                        break;
                    }
                    isValid = false;
                }
                if (isValid)
                    break;
            }
            if (!isValid && tc.isDebugEnabled())
                Tr.debug(tc, METHODNAME + " Invalid search bases are" + searchBases[j]);

        }

        return validSearchBase.toArray(new String[0]);
    }

    /**
     * @param ldapEntity
     */
    private void setDefaultRDNs(LdapEntity ldapEntity) {
        String name = ldapEntity.getName();
        if (name.contains(SchemaConstants.DO_PERSON_ACCOUNT)) {
            String[][] rdnAttrs = new String[1][1];
            String[][] rdnObjCls = new String[1][1];
            if (iLdapType.startsWith(LdapConstants.AD_LDAP_SERVER)) {
                rdnAttrs[0][0] = "cn";
                rdnObjCls[0][0] = "user";
            } else {
                rdnAttrs[0][0] = "uid";
                rdnObjCls[0][0] = "inetOrgPerson";
            }

            ldapEntity.setRDNAttributes(rdnAttrs, rdnObjCls);
        } else if (name.contains(SchemaConstants.DO_GROUP)) {
            String[][] rdnAttrs = new String[1][1];
            rdnAttrs[0][0] = "cn";
            String[][] rdnObjCls = new String[1][1];
            if (iLdapType.startsWith(LdapConstants.AD_LDAP_SERVER)) {
                rdnObjCls[0][0] = "group";
            }
            if (iLdapType.startsWith(LdapConstants.SUN_LDAP_SERVER)) {
                rdnObjCls[0][0] = "ldapsubentry";
            } else {
                rdnObjCls[0][0] = "groupOfNames";
            }

            ldapEntity.setRDNAttributes(rdnAttrs, rdnObjCls);
        } else if (name.contains(SchemaConstants.DO_ORGCONTAINER)) {
            String[][] rdnAttrs = { { "o" }, { "ou" } };
            String[][] rdnObjCls = { { "organization" }, { "organizationalUnit" } };
            ldapEntity.setRDNAttributes(rdnAttrs, rdnObjCls);
        }
    }

    private void setDefaultLDAPEntries() {
        int size = 3;
        if (iLdapEntityTypeList == null)
            iLdapEntityTypeList = new ArrayList<String>(size);
        if (iLdapEntities == null)
            iLdapEntities = new ArrayList<LdapEntity>(size);

        if (iPersonAccountTypes == null)
            iPersonAccountTypes = new ArrayList<String>(size);
        if (iPersonTypes == null)
            iPersonTypes = new ArrayList<String>(size);
        if (iGroupTypes == null) {
            iGroupTypes = new ArrayList<String>(size);
        }
        // PersonAccount
        if (!iLdapEntityTypeList.contains(SchemaConstants.DO_PERSON_ACCOUNT)) {
            LdapEntity ldapEntity = new LdapEntity(SchemaConstants.DO_PERSON_ACCOUNT);
            List<String> objclsList = new ArrayList<String>();
            String[][] rdnAttrs = new String[1][1];
            String[][] rdnObjCls = new String[1][1];
            if (iLdapType.startsWith(LdapConstants.AD_LDAP_SERVER)) {
                objclsList.add("user");
                rdnAttrs[0][0] = "cn";
                rdnObjCls[0][0] = "user";
            } else {
                objclsList.add("inetOrgPerson");
                rdnAttrs[0][0] = "uid";
                rdnObjCls[0][0] = "inetOrgPerson";
            }

            ldapEntity.setRDNAttributes(rdnAttrs, rdnObjCls);

            ldapEntity.setObjectClasses(objclsList);
            ldapEntity.setObjectClassesForCreate(objclsList);
            if (iLdapType.startsWith(LdapConstants.AD_LDAP_SERVER)) {
                ldapEntity.setSearchFilter("(ObjectCategory=Person)");
            } else {
                ldapEntity.setSearchFilter(null);
            }
            ldapEntity.setSearchBases(getTopLdapNodes());
            iLdapEntityTypeList.add(SchemaConstants.DO_PERSON_ACCOUNT);
            iLdapEntities.add(ldapEntity);
            iPersonAccountTypes.add(SchemaConstants.DO_PERSON_ACCOUNT);
        }

        // Group
        if (!iLdapEntityTypeList.contains(SchemaConstants.DO_GROUP)) {
            LdapEntity ldapEntity = new LdapEntity(SchemaConstants.DO_GROUP);
            List<String> objclsList = new ArrayList<String>();
            String[][] rdnAttrs = new String[1][1];
            rdnAttrs[0][0] = "cn";
            String[][] rdnObjCls = new String[1][1];

            if (iLdapType.startsWith(LdapConstants.AD_LDAP_SERVER)) {
                objclsList.add("group");
                rdnObjCls[0][0] = "group";
            }
            if (iLdapType.startsWith(LdapConstants.SUN_LDAP_SERVER)) {
                objclsList.add("ldapsubentry");
                rdnObjCls[0][0] = "ldapsubentry";
            } else {
                objclsList.add("groupOfNames");
                rdnObjCls[0][0] = "groupOfNames";
            }

            ldapEntity.setRDNAttributes(rdnAttrs, rdnObjCls);

            ldapEntity.setObjectClasses(objclsList);
            ldapEntity.setObjectClassesForCreate(objclsList);
            if (iLdapType.startsWith(LdapConstants.AD_LDAP_SERVER)) {
                ldapEntity.setSearchFilter("(ObjectCategory=Group)");
            } else {
                ldapEntity.setSearchFilter(null);
            }
            iLdapEntityTypeList.add(SchemaConstants.DO_GROUP);
            iLdapEntities.add(ldapEntity);
            iGroupTypes.add(SchemaConstants.DO_GROUP);
            ldapEntity.setSearchBases(getTopLdapNodes());
            List<String> grpSearchBases = new ArrayList<String>();
            grpSearchBases.addAll(ldapEntity.getSearchBaseList());
            iGroupSearchBases = grpSearchBases.toArray(new String[0]);
            iGroupSearchBases = NodeHelper.getTopNodes(iGroupSearchBases);
        }
        // OrgContainer
        if (!iLdapEntityTypeList.contains(SchemaConstants.DO_ORGCONTAINER)) {
            LdapEntity ldapEntity = new LdapEntity(SchemaConstants.DO_ORGCONTAINER);
            String[][] rdnAttrs = { { "o" }, { "ou" } };
            String[][] rdnObjCls = { { "organization" }, { "organizationalUnit" } };
            ldapEntity.setRDNAttributes(rdnAttrs, rdnObjCls);

            List<String> objclsList = new ArrayList<String>();
            objclsList.add("organization");
            objclsList.add("organizationalUnit");
            ldapEntity.setObjectClasses(objclsList);
            ldapEntity.setObjectClassesForCreate(objclsList);
            ldapEntity.setSearchFilter(null);

            iLdapEntityTypeList.add(SchemaConstants.DO_ORGCONTAINER);
            iLdapEntities.add(ldapEntity);

            ldapEntity.setSearchBases(getTopLdapNodes());
        }
    }

    /**
     * Return the configured membership attribute
     *
     * @return
     */
    @Trivial
    public String getMembershipAttribute() {
        return iMembershipAttrName;
    }

    /**
     * Return the configured scope of the membership attribute
     *
     * @return
     */
    @Trivial
    public short getMembershipAttributeScope() {
        return iMembershipAttrScope;
    }

    /**
     * @return Returns the iUseDefaultMbrAttr.
     */
    @Trivial
    public boolean isDefaultMbrAttr() {
        return iUseDefaultMbrAttr;
    }

    @Trivial
    public boolean supportDynamicGroup() {
        return (iDynaMbrAttrs != null ? true : false);
    }

    /**
     * Return the sub-list of properties, for the given entity, supported by this repository, from a given list of properties.
     *
     * @param inEntityTypes : List of entity types
     * @param propNames : List of property names read from data object
     * @return list of properties supported by repository for given entity type
     *         If the list propNames contain VALUE_ALL_PROPERTIES i.e '*', then return the list of properties without any modification
     *         Code will handle '*' later on
     * @throws EntityTypeNotSupportedException
     */
    public List<String> getSupportedProperties(String inEntityTypes, List<String> propNames) throws EntityTypeNotSupportedException {
        List<String> prop = null;
        Set<String> s = null;
        if (propNames != null && propNames.size() > 0) {
            if (inEntityTypes == null) {
                return propNames;
            }

            prop = new ArrayList<String>();
            if (inEntityTypes.equals(SchemaConstants.DO_ENTITY)) {
                s = new HashSet<String>();
                List<LdapEntity> ldapEntities = getAllLdapEntities(inEntityTypes);
                List<String> tmpProp = null;
                for (int i = 0; i < ldapEntities.size(); i++) {
                    tmpProp = getSupportedProperties(ldapEntities.get(i), propNames);
                    s.addAll(tmpProp);
                }
                prop.addAll(s);
            } else {
                LdapEntity ldapEntity = getLdapEntity(inEntityTypes);
                if (ldapEntity == null) {
                    throw new EntityTypeNotSupportedException(WIMMessageKey.ENTITY_TYPE_NOT_SUPPORTED, Tr.formatMessage(tc, WIMMessageKey.ENTITY_TYPE_NOT_SUPPORTED,
                                                                                                                        WIMMessageHelper.generateMsgParms(inEntityTypes)));
                }
                prop = getSupportedProperties(ldapEntity, propNames);
            }
        }

        return prop;
    }

    /**
     * Return the list of properties supported for a given LDAP entity.
     * This is an overloaded method to support getSupportedProperties(String, List)
     *
     * @param ldapEntity : A given LDAP entity
     * @param propNames : List of property names read from data object
     * @return list of properties supported by repository for given LDAP entity
     *         If the list propNames contain VALUE_ALL_PROPERTIES i.e '*', then return the list of properties without any modification
     *         Code will handle '*' later on
     */
    public List<String> getSupportedProperties(LdapEntity ldapEntity, List<String> propNames) {

        List<String> prop = new ArrayList<String>();

        for (String propName : propNames) {
            if (propName.equals(SchemaConstants.VALUE_ALL_PROPERTIES)) {
                prop.add(propName);
                continue;
            }
            // call the getAttribute method to see if its supported by LDAP
            String attrName = ldapEntity.getAttribute(propName);
            if (attrName == null) {
                // check the property to attribute map to see if the property defined
                // in data object is mapped to a different ldap attribute
                attrName = iPropToAttrMap.get(propName);
            }
            if (attrName != null) {
                prop.add(propName);
            }
        }

        return prop;
    }

    /**
     * The method returns the list of VMM entity types based on the qualifiedType read from the data graph.
     * The qualifiedType is first compared with the defined VMM entity types. If a match is not found, it then
     * determines if qualifiedType is a parent entity of defined entity types.
     *
     * @param qualifiedType
     * @return a List of entity types
     */
    public List<LdapEntity> getAllLdapEntities(String qualifiedType) {
        List<LdapEntity> entityTypes = new ArrayList<LdapEntity>();

        if (qualifiedType != null) {
            for (int i = 0; i < iLdapEntityTypeList.size(); i++) {
                String entityType = iLdapEntityTypeList.get(i);
                if (entityType != null && entityType.equals(qualifiedType)) {
                    entityTypes.add(iLdapEntities.get(i));
                }
            }
            if (entityTypes.size() != 0) {
                return entityTypes;
            } else {
                for (int j = 0; j < iLdapEntityTypeList.size(); j++) {
                    String entityType = iLdapEntityTypeList.get(j);
                    // New:: Had to add this condition as "Entity.getSubEntityTypes(qualifiedType)" is returning null at runtime
                    if (qualifiedType.equalsIgnoreCase(entityType) ||
                        (Entity.getSubEntityTypes(qualifiedType) != null && Entity.getSubEntityTypes(qualifiedType).contains(entityType))) {
                        entityTypes.add(iLdapEntities.get(j));
                    }
                }
                if (entityTypes.size() != 0)
                    return entityTypes;
            }
        }
        return entityTypes;
    }

    @Trivial
    public boolean isPerson(String qualifiedEntityType) {
        return iPersonTypes.contains(qualifiedEntityType);
    }

    @Trivial
    public boolean isPersonAccount(String qualifiedEntityType) {
        return iPersonAccountTypes.contains(qualifiedEntityType);
    }

    @Trivial
    public boolean isGroup(String qualifiedEntityType) {
        return iGroupTypes.contains(qualifiedEntityType);
    }

    public LdapEntity getLdapEntity(String qualifiedType) {
        if (qualifiedType != null) {
            for (int i = 0; i < iLdapEntityTypeList.size(); i++) {
                String entityType = iLdapEntityTypeList.get(i);
                if (entityType != null && entityType.equals(qualifiedType)) {
                    return iLdapEntities.get(i);
                } else if (Entity.getSubEntityTypes(qualifiedType) != null && Entity.getSubEntityTypes(qualifiedType).contains(entityType)) {
                    return iLdapEntities.get(i);
                }
                // New:: Had to add this condition as "Entity.getSubEntityTypes('LoginAccount')" is returning null at runtime
                else if (qualifiedType.equalsIgnoreCase(SchemaConstants.DO_LOGIN_ACCOUNT) &&
                         entityType.equalsIgnoreCase(SchemaConstants.DO_PERSON_ACCOUNT)) {
                    return iLdapEntities.get(i);
                }
            }
        }
        return null;
    }

    public Set<String> getPropertyName(LdapEntity ldapEntity, String attrName) {
        Set<String> propNames = ldapEntity.getProperty(attrName);
        if (propNames == null) {
            propNames = iAttrToPropMap.get(attrName.toLowerCase());
        }
        if (propNames == null || propNames.size() == 0) {
            int pos = attrName.indexOf(";");
            if (pos > 0) {
                String stAttrName = attrName.substring(0, pos);
                propNames = ldapEntity.getProperty(stAttrName);
                if (propNames == null) {
                    propNames = iAttrToPropMap.get(stAttrName.toLowerCase());
                }
            }
            if (propNames == null || propNames.size() == 0) {
                propNames = new HashSet<String>();
                propNames.add(attrName);
            }
        }
        return propNames;
    }

    @Trivial
    public LdapAttribute getLdapAttribute(String attrName) {
        return iAttrNameToAttrMap.get(attrName.toLowerCase());
    }

    @Trivial
    public String getTimestampFormat() {
        return timestampFormat;
    }

    @Trivial
    public String getLdapType() {
        return iLdapType;
    }

    @Trivial
    private char[] getSpecialCharactors() {
        return iSpecialChars;
    }

    @Trivial
    public String escapeSpecialCharacters(String attributeValue) {
        if (attributeValue == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();

        char str[] = attributeValue.toCharArray();
        int size = attributeValue.length();

        int i = 0;
        char[] specialChars = getSpecialCharactors();
        while (i < size) {

            for (int j = 0; j < specialChars.length; j++) {
                if ((str[i] == specialChars[j])) {
                    if ((i > 0) || (i == 0)) {
                        result.append("\\");
                    }
                }
            }
            result.append(str[i]);
            i++;
        }

        if (useEncodingInSearchExpression != null) {
            result = new StringBuilder(LdapHelper.encodeAttribute(result.toString(), useEncodingInSearchExpression));
        }

        int z = 0;
        for (int y = 0; y < result.length();) {
            z = result.indexOf("\\*", z);
            if (z < 0) {
                y = result.length();
            } else {
                result.deleteCharAt(z);
                y = z;
            }
        }

        return result.toString();
    }

    /**
     * Gets the LDAP filter expression for search groups a member belongs to.
     *
     * @param groupMemberDN The Distinguished Name of the member to find groups.
     *
     * @return The LDAP filter expression.
     */
    public String getGroupMemberFilter(String groupMemberDN) {
        groupMemberDN = escapeSpecialCharacters(groupMemberDN);
        Object[] args = {
                          groupMemberDN
        };
        return new MessageFormat(iGrpMbrFilter).format(args);
    }

    @Trivial
    public String[] getGroupSearchBases() {
        return iGroupSearchBases.clone();
    }

    @Trivial
    public String getLdapNode(String node) {
        node = node.toLowerCase();
        for (int i = 0; i < iNodesForCompare.length; i++) {
            if (iNodesForCompare[i].equals(node)) {
                return iLdapNodes[i];
            }
        }
        return null;
    }

    /**
     * Returns the unique name the WIM node switched to LDAP node.
     * For example, if the node mapping is (WIM node to LDAP node):
     * cn=users,dc=yourco,dc=com <--> o=users,o=yourco
     * For example, a given unique name: uid=amber,cn=users,dc=yourco,dc=com will be switched to
     * uid=amber,o=users,o=yourco.
     *
     * @param uniqueName the unique name.
     *
     * @return unique name with LDAP node.
     */
    public String switchToLdapNode(String uniqueName) {
        if (uniqueName == null || !iNeedSwitchNode) {
            return uniqueName;
        }
        StringBuffer DNBuf = new StringBuffer(uniqueName);
        String uniqueNameForCompare = uniqueName.toLowerCase();
        // The LDAP node this DN is under.
        String ldapNode = null;
        // Replace node in the DN.
        for (int i = 0, n = iNodesForCompare.length; i < n; i++) {
            int pos = uniqueNameForCompare.lastIndexOf(iNodesForCompare[i]);
            if (pos > -1) {
                // If pos is 0 and the node is not empty node, means the DN is one of the iNodes, just simply return.
                if (pos == 0) {
                    if (iNodesForCompare[i].length() > 0 && iNodesForCompare[i].length() == uniqueName.length()) {
                        return iLdapNodes[i];
                    } else {
                        //Everyting is under empty node.
                        if (iLdapNodes[i].length() > 0) {
                            ldapNode = "," + iLdapNodes[i];
                        } else {
                            ldapNode = iLdapNodes[i];
                        }
                    }
                } else {
                    if (uniqueName.length() - pos == iNodesForCompare[i].length()) {
                        ldapNode = iLdapNodes[i];
                        // if nameInRepository is root("") node, then strip off the "," too.
                        if (ldapNode != null && ldapNode.length() == 0 && (DNBuf.charAt(pos - 1) == ',')) {
                            DNBuf = new StringBuffer(DNBuf.substring(0, pos - 1));
                        } else {
                            DNBuf = new StringBuffer(DNBuf.substring(0, pos));
                        }
                        break;
                    }
                }
            }
        }

        if (ldapNode != null) {
            DNBuf.append(ldapNode);
        }
        return DNBuf.toString();
    }

    public boolean needTranslateRDN() {
        return iNeedTranslateRDN;
    }

    public boolean needTranslateRDN(String entityType) {
        if (entityType != null) {
            LdapEntity ldapEntity = getLdapEntity(entityType);
            if (ldapEntity != null) {
                return ldapEntity.needTranslateRDN();
            }
        }
        return iNeedTranslateRDN;
    }

    @Trivial
    public List<String> getGroupTypes() {
        return iGroupTypes;
    }

    public String getDynamicMemberAttribute(Attribute grpObjCls) throws WIMSystemException {
        if (iDynaMbrAttrs == null) {
            return null;
        } else {
            if (grpObjCls != null) {
                try {
                    NamingEnumeration<?> nenu = grpObjCls.getAll();
                    while (nenu.hasMoreElements()) {
                        String objectClass = (String) nenu.nextElement();
                        if (objectClass != null) {
                            String mbrAttr = iDynaMbrAttrMap.get(objectClass.toLowerCase());
                            if (mbrAttr != null) {
                                return mbrAttr;
                            }
                        }
                    }
                } catch (NamingException e) {
                    throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(
                                                                                                  tc,
                                                                                                  WIMMessageKey.NAMING_EXCEPTION,
                                                                                                  WIMMessageHelper.generateMsgParms(e.toString(true))));
                }
            }
            return null;
        }
    }

    @Trivial
    public boolean isMemberAttributesNestedScope() {
        return iMbrAttrsNestedScope;
    }

    @Trivial
    public boolean isMemberAttributesAllScope() {
        return iMbrAttrsAllScope;
    }

    @SuppressWarnings("unchecked")
    @Trivial
    public List<LdapEntity> getLdapEntities() {
        return (List<LdapEntity>) ((ArrayList<LdapEntity>) iLdapEntities).clone();
    }

    /**
     * Returns the top level repository iNodes of this repository in lower case form.
     * Top level means these nodes are not contained by other iNodes.
     * For example, if there are the following iNodes defined:
     * <UL>
     * <LI>dc=yourco,dc=com
     * <LI>cn=users,dc=yourco,dc=com
     * <LI>cn=groups,dc=yourco,dc=com
     * </UL>
     * "dc=yourco,dc=com" is the top level node.
     * The iNodes in the array are in lower case form.
     *
     * @return A string array of all top level repository iNodes in lower case form.
     */
    @Trivial
    public String[] getTopLdapNodes() {
        return iTopLdapNodes.clone();
    }

    @Trivial
    public boolean containGroup(List<String> qualifiedEntityTypes) {
        if (qualifiedEntityTypes != null) {
            for (int i = 0; i < qualifiedEntityTypes.size(); i++) {
                String thisType = qualifiedEntityTypes.get(i);
                if (isGroup(thisType)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Trivial
    public String[] getMemberAttribute(Attribute grpObjCls) throws WIMSystemException {
        if (iMbrAttrs.length == 1) {
            return iMbrAttrs.clone();
        } else {
            if (grpObjCls != null) {
                try {
                    NamingEnumeration<?> nenu = grpObjCls.getAll();
                    ArrayList<String> attributes = new ArrayList<String>();
                    while (nenu.hasMoreElements()) {
                        String objectClass = (String) nenu.nextElement();
                        if (objectClass != null) {
                            String mbrAttr = iMbrAttrMap.get(objectClass.toLowerCase());
                            if (mbrAttr != null) {
                                attributes.add(mbrAttr);
                            }
                        }
                    }
                    return attributes.toArray(new String[0]);
                } catch (NamingException e) {
                    throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(
                                                                                                  tc,
                                                                                                  WIMMessageKey.NAMING_EXCEPTION,
                                                                                                  WIMMessageHelper.generateMsgParms(e.toString(true))));
                }

            }
            return iMbrAttrs.clone();
        }

    }

    @Trivial
    public String[] getMemberAttributes() {
        if (iMbrAttrs != null)
            return iMbrAttrs.clone();
        else
            return new String[0];
    }

    public Attribute[] getGroupMemberAttrs(Attributes attrs, Attribute objClsAttr) throws WIMException {
        ArrayList<Attribute> memberAttrs = new ArrayList<Attribute>();
        String[] mbrAttrName = null;
        if (objClsAttr != null) {
            mbrAttrName = getMemberAttribute(objClsAttr);
        } else {
            mbrAttrName = getMemberAttributes();
        }

        if (mbrAttrName != null) {
            for (int i = 0; i < mbrAttrName.length; i++) {
                Attribute attr = attrs.remove(mbrAttrName[i]);
                if (attr != null)
                    memberAttrs.add(attr);
            }
        }

        if (memberAttrs.size() > 0)
            return memberAttrs.toArray(new Attribute[0]);
        else
            return null;
    }

    @Trivial
    public boolean isIncludeGroupInSearchEntityTypes() {
        return includeGroupInSearchEntityTypes;
    }

    @Trivial
    public boolean isDummyMember(String dn) {
        return iDummyMbrMap.containsValue(dn);
    }

    @Trivial
    public boolean isActiveDirectory() {
        if (iLdapType.startsWith(LdapConstants.AD_LDAP_SERVER))
            return true;
        else
            return false;
    }

    @Trivial
    public String getUseEncodingInSearchExpression() {
        return useEncodingInSearchExpression;
    }

    @SuppressWarnings("unchecked")
    public List<String> getLdapSubEntityTypes(String entityType) {
        List<String> subTypes = new ArrayList<String>();
        // New:: Had to add this condition as "Entity.getSubEntityTypes('LoginAccount')" is returning null at runtime
        if (Entity.getSubEntityTypes(entityType) != null)
            subTypes.addAll(Entity.getSubEntityTypes(entityType));
        else if (entityType.equalsIgnoreCase(SchemaConstants.DO_LOGIN_ACCOUNT))
            subTypes.add(SchemaConstants.DO_PERSON_ACCOUNT);

        List<String> typesToRemove = new ArrayList<String>(subTypes.size());
        for (int i = 0; i < subTypes.size(); i++) {
            String subType = subTypes.get(i);
            if (getLdapEntity(subType) == null) {
                typesToRemove.add(subType);
            }
        }
        subTypes.removeAll(typesToRemove);
        return subTypes;
    }

    public String getEntityTypesFilter(Set<String> entityTypes) {
        StringBuffer filter = new StringBuffer();

        if (isRacf()) {
            // RACF (SDBM) only supports wildcard objectclass filter.
            filter.append("(objectclass=*)");
        } else {
            if (entityTypes.size() > 1) {
                filter.append("(|");
            }
            for (Iterator<String> iter = entityTypes.iterator(); iter.hasNext();) {
                LdapEntity ldapEntity = getLdapEntity(iter.next());
                if (ldapEntity != null) {
                    String str = ldapEntity.getSearchFilter();
                    if (filter.indexOf(str) == -1)
                        filter.append(ldapEntity.getSearchFilter());
                }
            }
            if (entityTypes.size() > 1) {
                filter.append(")");
            }
        }
        return filter.toString();
    }

    @Trivial
    public List<String> getLoginAttributes() {
        return iLoginAttrs;
    }

    @Trivial
    public String getCertificateMapMode() {
        return iCertMapMode;
    }

    // PM76997
    private String removeSpacesInDN(String DN) {
        String attrs[] = DN.split(",");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < attrs.length; i++) {
            if (attrs[i].indexOf("=") >= 0)
                result.append(attrs[i].trim());
            else
                result.append(attrs[i]);
            if (i != (attrs.length - 1))
                result.append(",");
        }
        return result.toString();
    }

    public String getCertificateLDAPFilter(X509Certificate cert) throws CertificateMapperException {
        if (iCertFilterEles == null) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.INVALID_CERTIFICATE_FILTER, "null");
            throw new CertificateMapperException(WIMMessageKey.INVALID_CERTIFICATE_FILTER, msg);
        }

        StringBuffer filter = new StringBuffer();
        for (int idx = 0; idx < iCertFilterEles.length; idx++) {
            String str = iCertFilterEles[idx];
            if (str.charAt(0) != '$') {
                filter.append(str);
                // TBD - Will what is appended to a string buffer yield the correct
                //       values to search for if the values are binary?
            } else if (str.equals("${UniqueKey}") || str.equals("$[UniqueKey]")) {
                filter.append(LdapHelper.getUniqueKey(cert));
            } else if (str.equals("${PublicKey}") || str.equals("$[PublicKey]")) {
                filter.append(new String(cert.getPublicKey().getEncoded()));
            } else if (str.equals("${BasicConstraints}") || str.equals("$[BasicConstraints]")) {
                // TBD - filter.append (cert.getBasicConstraints());
            } else if (str.startsWith("${Issuer") || str.startsWith("$[Issuer")) {
                // PM76997: Modify to use X500Principal.toString()
                filter.append(LdapHelper.getDNSubField(str.substring(8, str.length() - 1), removeSpacesInDN(cert.getIssuerX500Principal().toString())));
            } else if (str.equals("${IssuerUniqueID}") || str.equals("$[IssuerUniqueID]")) {
                // TBD - filter.append (cert.getIssuerUniqueID());
            } else if (str.equals("${KeyUsage}") || str.equals("$[KeyUsage]")) {
                // TBD - filter.append (cert.getKeyUsage());
            } else if (str.equals("${NotAfter}") || str.equals("$[NotAfter]")) {
                filter.append(cert.getNotAfter().toString());
            } else if (str.equals("${NotBefore}") || str.equals("$[NotBefore]")) {
                filter.append(cert.getNotBefore().toString());
            } else if (str.equals("${SerialNumber}") || str.equals("$[SerialNumber]")) {
                filter.append(cert.getSerialNumber());
            } else if (str.equals("${SigAlgName}") || str.equals("$[SigAlgName]")) {
                filter.append(cert.getSigAlgName());
            } else if (str.equals("${SigAlgOID}") || str.equals("$[SigAlgOID]")) {
                filter.append(cert.getSigAlgOID());
            } else if (str.equals("${SigAlgParams}") || str.equals("$[SigAlgParams]")) {
                filter.append(new String(cert.getSigAlgParams()));
            } else if (str.equals("${Signature}") || str.equals("$[Signature]")) {
                // TBD - filter.append (cert.getSignature());
            } else if (str.startsWith("${Subject") || str.startsWith("$[Subject")) {
                // PM76997: Modify to use X500Principal.toString()
                filter.append(LdapHelper.getDNSubField(str.substring(9, str.length() - 1),
                                                       removeSpacesInDN(cert.getSubjectX500Principal().toString())));
            } else if (str.equals("${SubjectUniqueID}") || str.equals("$[SubjectUniqueID]")) {
                // TBD - filter.append (cert.getSubjectUniqueID());
            } else if (str.equals("${TBSCertificate}") || str.equals("$[TBSCertificate]")) {
                // filter.append (cert.getTBSCertificate());
                String msg = Tr.formatMessage(tc, WIMMessageKey.TBS_CERTIFICATE_UNSUPPORTED, (Object) null);
                throw new CertificateMapperException(WIMMessageKey.TBS_CERTIFICATE_UNSUPPORTED, msg);
            } else if (str.equals("${Version}") || str.equals("$[Version]")) {
                filter.append(cert.getVersion());
            } else {
                String msg = Tr.formatMessage(tc, WIMMessageKey.UNKNOWN_CERTIFICATE_ATTRIBUTE, WIMMessageHelper.generateMsgParms(str));
                throw new CertificateMapperException(WIMMessageKey.UNKNOWN_CERTIFICATE_ATTRIBUTE, msg);
            }
        }
        return filter.toString();
    }

    @Trivial
    public boolean isSetUsePrincipalNameForLogin() {
        return usePrincipalNameForLogin;
    }

    @Trivial
    public Set<String> getAllSuppotedAttributes() {
        return iAttrs;
    }

    public String[] getAttributeNames(List<String> inEntityTypes, List<String> propNames,
                                      boolean getMbrshipAttr, boolean getMbrAttr) {
        Set<String> attrNames = new HashSet<String>();
        List<LdapEntity> ldapEntities = null;
        if (inEntityTypes != null && inEntityTypes.size() > 0) {
            ldapEntities = new ArrayList<LdapEntity>(inEntityTypes.size());
            for (String entityType : inEntityTypes) {
                List<LdapEntity> tempLdapEntities = getAllLdapEntities(entityType);
                if (tempLdapEntities != null) {
                    ldapEntities.addAll(tempLdapEntities);
                }
            }
        }
        // if propNames is null or empty, means no props need to be returned
        if (propNames != null) {
            for (int i = 0; i < propNames.size(); i++) {
                String propName = propNames.get(i);
                // if prop name is '*', which means retrieve all properties.
                if (SchemaConstants.VALUE_ALL_PROPERTIES.equals(propName)) {
                    if (ldapEntities != null && ldapEntities.size() > 0) {
                        for (int j = 0; j < ldapEntities.size(); j++) {
                            LdapEntity ldapEntity = ldapEntities.get(j);
                            // If entity type is specified, retrieve supported attributes of that entity types
                            attrNames.addAll(ldapEntity.getAttributes());
                        }
                    } else {
                        // If entity type is unknown, retrieve all supported attributes
                        attrNames.addAll(getAllSuppotedAttributes());
                    }
                    attrNames.removeAll(entityTypeProps);
                } else {
                    if (ldapEntities != null && ldapEntities.size() > 0) {
                        for (int j = 0; j < ldapEntities.size(); j++) {
                            LdapEntity ldapEntity = ldapEntities.get(j);
                            // If entity type is specified, should only one attribute.
                            attrNames.add(getAttributeName(ldapEntity, propName));
                        }
                    } else {
                        // If entity type is unknown, one prop can map to multiple attributes.
                        attrNames.addAll(getAttributeNames(propName));
                    }
                }
            }
        }
        if (ldapEntities != null && ldapEntities.size() > 0) {
            for (int i = 0; i < ldapEntities.size(); i++) {
                LdapEntity ldapEntity = ldapEntities.get(i);
                if (!LdapConstants.LDAP_DN.equalsIgnoreCase(ldapEntity.getExtId())) {
                    attrNames.add(ldapEntity.getExtId());
                }
                if (needTranslateRDN() && needTranslateRDN(ldapEntity.getName())) {
                    attrNames.addAll(ldapEntity.getRDNAttributesList());
                }
            }
        } else {
            if (needTranslateRDN()) {
                List<LdapEntity> allEntities = getLdapEntities();
                for (int i = 0; i < allEntities.size(); i++) {
                    attrNames.addAll(allEntities.get(i).getRDNAttributesList());
                }
            }
            attrNames.addAll(getExtIds());
        }
        attrNames.add(LdapConstants.LDAP_ATTR_OBJECTCLASS);

        if (getMbrshipAttr && getMembershipAttribute() != null) {
            attrNames.add(getMembershipAttribute());
        }
        if (getMbrAttr) {
            String[] mbrAttrs = getMemberAttributes();
            for (int i = 0; i < mbrAttrs.length; i++) {
                if (mbrAttrs[i] != null) {
                    attrNames.add(mbrAttrs[i]);
                }
            }
            if (supportDynamicGroup()) {
                String[] dynaMbrAttrNames = getDynamicMemberAttributes();
                for (int i = 0; i < dynaMbrAttrNames.length; i++) {
                    if (dynaMbrAttrNames[i] != null) {
                        attrNames.add(dynaMbrAttrNames[i]);
                    }

                }
            }
        }
        return attrNames.toArray(new String[0]);
    }

    private Set<String> getExtIds() {
        return iExtIds;
    }

    public String getAttributeName(LdapEntity ldapEntity, String propName) {

        /*
         * Check for extensible matching. For now we won't try to map the attribute
         * in the extensible matching filter, but in the future it may be necessary.
         */
        Matcher matcher = PATTERN_EXTENSIBLE_MATCH_FILTER.matcher(propName);
        if (matcher.matches()) {
            String attrName = matcher.find(0) ? matcher.group(1) : null;
            String oid = matcher.find(1) ? matcher.group(2) : null;
            String value = matcher.find(2) ? matcher.group(3) : null;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Attribute name: " + attrName + ", OID: " + oid + ", Value:" + value);
            }
            return propName;
        } else {

            String attrName = ldapEntity.getAttribute(propName);
            if (attrName == null) {
                attrName = iPropToAttrMap.get(propName);
            }
            if (attrName == null) {
                int pos = propName.indexOf(":");
                if (pos > -1) {
                    return propName.substring(pos + 1);
                } else {
                    return propName;
                }
            } else {
                return attrName;
            }
        }
    }

    public Set<String> getAttributeNames(String propName) {
        Set<String> attrs = new HashSet<String>();
        for (int i = 0; i < iLdapEntities.size(); i++) {
            String attr = iLdapEntities.get(i).getAttribute(propName);
            if (attr != null) {
                attrs.add(attr);
            }
        }
        String attr = iPropToAttrMap.get(propName);
        if (attr != null) {
            attrs.add(attr);
        }
        if (attrs.size() == 0) {
            int pos = propName.indexOf(":");
            if (pos > -1) {
                attrs.add(propName.substring(pos + 1));
            } else {
                attrs.add(propName);
            }
        }
        return attrs;
    }

    @Trivial
    public boolean isAnyExtIdDN() {
        return isAnyExtIdDN;
    }

    public String getEntityType(Attributes attrs, String uniqueName, String dn,
                                String extId, List<String> inEntityTypes) throws WIMSystemException {
        String entityType = SchemaConstants.DO_ENTITY;

        // Handle RACF(SDBM) types separately
        if (isRacf()) {
            if (dn.toLowerCase().contains("profiletype=user")) {
                return SchemaConstants.DO_PERSON_ACCOUNT;
            } else if (dn.toLowerCase().contains("profiletype=group")) {
                return SchemaConstants.DO_GROUP;
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "RACF entity " + dn + " is not recognized as either a PersonAccount or Group.");
                }
            }
        }

        Attribute objClsAttr = attrs.get(LdapConstants.LDAP_ATTR_OBJECTCLASS);

        if (objClsAttr != null) {
            int size = iLdapEntities.size();
            List<String> possibleTypes = new ArrayList<String>(size);
            // Get list of possible entity types according to the object class.
            for (int i = 0; i < size; i++) {
                LdapEntity ldapEntity = iLdapEntities.get(size - (i + 1));
                List<String> objClsList = ldapEntity.getObjectClasses();
                for (String objCls : objClsList) {
                    if (LdapHelper.containIgnorecaseValue(objClsAttr, objCls)) {
                        String type = ldapEntity.getName();
                        if (inEntityTypes != null && inEntityTypes.size() > 0) {
                            for (String inputType : inEntityTypes) {
                                if (inputType.equalsIgnoreCase(type) ||
                                    (Entity.getSubEntityTypes(inputType) != null && Entity.getSubEntityTypes(inputType).contains(type))) {
                                    possibleTypes.add(type);
                                }
                                // New:: Had to add this condition as "Entity.getSubEntityTypes('LoginAccount')" is returning null at runtime
                                else if (inputType.equalsIgnoreCase(SchemaConstants.DO_LOGIN_ACCOUNT) &&
                                         type.equalsIgnoreCase(SchemaConstants.DO_PERSON_ACCOUNT)) {
                                    possibleTypes.add(type);
                                }
                            }
                        } else {
                            possibleTypes.add(ldapEntity.getName());
                        }
                        break;
                    }
                }
            }
            int possibleSize = possibleTypes.size();
            if (possibleSize == 1) {
                entityType = possibleTypes.get(0);
            } else if (possibleSize > 1) {
                entityType = possibleTypes.get(0);
            }
        } else {
            entityType = inEntityTypes.get(0);
        }

        return entityType;
    }

    @FFDCIgnore(ClassCastException.class)
    public String getExtIdFromAttributes(String dn, String entityType, Attributes attrs) throws WIMSystemException {
        String extIdName = getExtId(entityType);
        if (LdapConstants.LDAP_DN.equalsIgnoreCase(extIdName)) {
            return LdapHelper.toUpperCase(dn);
        }
        Attribute extIdAttr = null;
        extIdAttr = attrs.get(extIdName);

        // check if the externalId attribute is null
        if (extIdAttr == null) {
            /*
             * throw new WIMSystemException(WIMMessageKey.EXT_ID_VALUE_IS_NULL,
             * Tr.formatMessage(
             * tc,
             * WIMMessageKey.EXT_ID_VALUE_IS_NULL,
             * WIMMessageHelper.generateMsgParms(extIdName, dn)
             * ));
             */
            return null;
        }

        if (extIdAttr.size() > 1) {
            throw new WIMSystemException(WIMMessageKey.EXT_ID_HAS_MULTIPLE_VALUES, Tr.formatMessage(
                                                                                                    tc,
                                                                                                    WIMMessageKey.EXT_ID_HAS_MULTIPLE_VALUES,
                                                                                                    WIMMessageHelper.generateMsgParms(extIdName)));
        }
        LdapAttribute ldapAttr = getAttribute(extIdName);
        try {
            Object val = extIdAttr.get();
            if (ldapAttr != null && LdapConstants.LDAP_ATTR_SYNTAX_OCTETSTRING.equalsIgnoreCase(ldapAttr.getSyntax())) {
                // TODO:: To be fixed later
                try {
                    byte[] bytes = (byte[]) val;
                    return LdapHelper.getOctetString(bytes);
                } catch (ClassCastException e) {
                    e.getMessage();
                    return val.toString();
                }
            } else {
                if (val == null)
                    return null;
                else
                    return val.toString();
            }
        } catch (NamingException e) {
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(
                                                                                          tc,
                                                                                          WIMMessageKey.NAMING_EXCEPTION,
                                                                                          WIMMessageHelper.generateMsgParms(e.toString(true))));
        }
    }

    public String getExtId(String entityType) {
        LdapEntity ldapEntity = getLdapEntity(entityType);
        if (ldapEntity != null) {
            return ldapEntity.getExtId();
        } else {
            if (iExtIds.size() == 1) {
                return iExtIds.iterator().next();
            } else {
                return LdapConstants.LDAP_DN;
            }
        }
    }

    public LdapAttribute getAttribute(String attrName) {
        return iAttrNameToAttrMap.get(attrName.toLowerCase());
    }

    public String switchToNode(String DN) {
        if (DN == null || !iNeedSwitchNode) {
            return DN;
        }
        String DNForComparison = DN.toLowerCase();
        StringBuffer uNameBuf = new StringBuffer(DN);
        boolean isUnderNode = false;
        // The LDAP node this DN is under.
        String node = null;
        for (int i = 0, n = iLdapNodesForCompare.length; i < n; i++) {
            int pos = DNForComparison.indexOf(iLdapNodesForCompare[i]);
            if (pos > -1) {
                // If pos is 0 and the node is not empty node, means the DN is one of the nodes, just simply return.
                if (pos == 0) {
                    if (iLdapNodesForCompare[i].length() > 0) {
                        return iNodes[i];
                    } else {
                        //Everything is under empty node.
                        node = iNodes[i];
                        if (node != null && node.length() != 0) {
                            uNameBuf.append(","); // if node name is not "", append "," before appending the nodename
                        }
                        isUnderNode = true;
                        break;
                    }
                } else {
                    uNameBuf = new StringBuffer(uNameBuf.substring(0, pos));
                    node = iNodes[i];
                    isUnderNode = true;
                    break;
                }
            }
        }

        if (!isUnderNode) {
            return DN;
        }

        if (node != null && node.length() != 0) {
            uNameBuf.append(node);
        } else if (node != null) {
            uNameBuf = new StringBuffer(uNameBuf.substring(0, uNameBuf.length() - 1));
        }
        return uNameBuf.toString();

    }

    @Trivial
    public String[] getDynamicMemberAttributes() {
        return iDynaMbrAttrs.clone();
    }

    @Trivial
    public String getDynamicGroupFilter() {
        return iDynaGrpFilter;
    }

    public Set<String> getAttributeNames(Set<String> entityTypes, String propName) {
        Set<String> attrs = new HashSet<String>();
        if (entityTypes == null) {
            attrs = getAttributeNames(propName);
        } else {
            for (String entityType : entityTypes) {
                LdapEntity ldapEntity = getLdapEntity(entityType);
                if (ldapEntity != null) {
                    String attr = getAttributeName(ldapEntity, propName);
                    if (attr != null) {
                        attrs.add(attr);
                    }
                }
            }
        }
        return attrs;
    }

    public Object getLdapValue(Object value, String dataType, String ldapAttrName) throws WIMSystemException {
        Object ldapValue;
        LdapAttribute ldapAttr = getLdapAttribute(ldapAttrName);
        if (SchemaConstants.DATA_TYPE_STRING.equals(dataType)) {
            ldapValue = LdapHelper.getStringLdapValue(value, ldapAttr, getLdapType());
        } else if (SchemaConstants.DATA_TYPE_DATE_TIME.equals(dataType) || SchemaConstants.DATA_TYPE_DATE.equals(dataType)) {
            ldapValue = LdapHelper.getDateLdapValue(value, ldapAttr, getLdapType());
        } else if (SchemaConstants.DATA_TYPE_INT.equals(dataType)) {
            ldapValue = LdapHelper.getIntLdapValue(value, ldapAttr, getLdapType());
        } else if (SchemaConstants.DATA_TYPE_LANG_TYPE.equals(dataType)) {
            if (value instanceof LangType) {
                ldapValue = ((LangType) value).getValue();
            } else {
                ldapValue = value.toString();
            }
        } else {
            ldapValue = value;
        }
        return ldapValue;
    }

    public short getOperator(String operator) {
        if (operator == null) {
            return -1;
        } else if (operator.equals("=")) {
            return LdapConstants.LDAP_OPERATOR_EQ;
        } else if (operator.equals("!=")) {
            return LdapConstants.LDAP_OPERATOR_NE;
        } else if (operator.equals(">")) {
            return LdapConstants.LDAP_OPERATOR_GT;
        } else if (operator.equals("<")) {
            return LdapConstants.LDAP_OPERATOR_LT;
        } else if (operator.equals(">=")) {
            return LdapConstants.LDAP_OPERATOR_GE;
        } else if (operator.equals("<=")) {
            return LdapConstants.LDAP_OPERATOR_LE;
        } else {
            return -1;
        }
    }

    public String getSyntax(String ldapAttrName) {
        LdapAttribute attr = iAttrNameToAttrMap.get(ldapAttrName.toLowerCase());
        if (attr != null) {
            return attr.getSyntax();
        } else {
            return LdapConstants.LDAP_ATTR_SYNTAX_STRING;
        }
    }

    private void setCertificateMapMode(String certMapMode) {
        if (LdapConstants.CONFIG_VALUE_FILTER_DESCRIPTOR_MODE.equalsIgnoreCase(certMapMode)) {
            iCertMapMode = LdapConstants.CONFIG_VALUE_FILTER_DESCRIPTOR_MODE;
        } else if (LdapConstants.CONFIG_VALUE_CUSTOM_MODE.equalsIgnoreCase(certMapMode)) {
            iCertMapMode = LdapConstants.CONFIG_VALUE_CUSTOM_MODE;
        } else if (LdapConstants.CONFIG_VALUE_CERT_NOT_SUPPORTED_MODE.equalsIgnoreCase(certMapMode)) {
            iCertMapMode = LdapConstants.CONFIG_VALUE_CERT_NOT_SUPPORTED_MODE;
        } else {
            iCertMapMode = LdapConstants.CONFIG_VALUE_EXTACT_DN_MODE;
        }
    }

    private void setCertificateFilter(String certFilter) throws CertificateMapperException {
        iCertFilterEles = LdapHelper.parseFilterDescriptor(certFilter);
    }

    private void setNodes(List<HashMap<String, String>> entryList) throws InvalidInitPropertyException {
        int size = entryList.size();
        Map<String, String> nodeMap = new HashMap<String, String>(size);
        Map<String, String> reposNodeMap = new HashMap<String, String>(size);
        String[] originReposNodes = new String[size];
        for (int i = 0; i < entryList.size(); i++) {
            HashMap<String, String> entry = entryList.get(i);
            String nodeName = UniqueNameHelper.getValidUniqueName(entry.keySet().iterator().next());
            if (nodeName == null) {
                throw new InvalidInitPropertyException(WIMMessageKey.INVALID_UNIQUE_NAME_SYNTAX, Tr.formatMessage(
                                                                                                                  tc,
                                                                                                                  WIMMessageKey.INVALID_UNIQUE_NAME_SYNTAX,
                                                                                                                  (Object) null));
            }
            String reposNodeName = entry.get(nodeName);
            if (reposNodeName == null) {
                reposNodeName = nodeName;
            } else {
                reposNodeName = LdapHelper.getValidDN(reposNodeName);
                if (reposNodeName == null) {
                    throw new InvalidInitPropertyException(WIMMessageKey.INVALID_DN_SYNTAX, Tr.formatMessage(
                                                                                                             tc,
                                                                                                             WIMMessageKey.INVALID_DN_SYNTAX,
                                                                                                             (Object) null));
                }
            }
            // Determine if need to switch node.
            if (!iNeedSwitchNode && !nodeName.equalsIgnoreCase(reposNodeName)) {
                iNeedSwitchNode = true;
            }

            originReposNodes[i] = reposNodeName;
            String lowNodeName = nodeName.toLowerCase();
            nodeMap.put(lowNodeName, nodeName);
            reposNodeMap.put(lowNodeName, reposNodeName);
        }
        int nodeSize = nodeMap.size();
        // Get the WIM iNodes with duplicate entries removed.
        String[] keyNodes = nodeMap.keySet().toArray(new String[0]);
        // Sort pluginNodes according the length
        Arrays.sort(keyNodes, new com.ibm.ws.security.wim.util.StringLengthComparator());

        iNodes = new String[nodeSize];
        iNodesForCompare = new String[nodeSize];

        iLdapNodes = new String[nodeSize];
        iLdapNodesForCompare = new String[nodeSize];

        for (int i = 0; i < nodeSize; i++) {
            String nodeForCompare = keyNodes[i];
            String node = nodeMap.get(nodeForCompare);
            String reposNode = reposNodeMap.get(nodeForCompare);
            String reposNodeForCompare = reposNode.toLowerCase();

            iNodes[i] = node;
            iNodesForCompare[i] = nodeForCompare;

            iLdapNodes[i] = reposNode;
            iLdapNodesForCompare[i] = reposNodeForCompare;
        }
        // Find out top level iNodes:
        iTopLdapNodes = NodeHelper.getTopNodes(originReposNodes);
    }

/*
 * private List<String> getSupportedEntityTypes() {
 * ArrayList<String> supportedEntityTypes = new ArrayList<String>();
 *
 * if (iLdapEntities == null || iLdapEntities.size() == 0)
 * return supportedEntityTypes;
 *
 * for (int i = 0; i < iLdapEntities.size(); i++) {
 * supportedEntityTypes.add(iLdapEntities.get(i).getName());
 * }
 *
 * return supportedEntityTypes;
 * }
 */

    /**
     * Returns the names of properties that are used for RDN for the given entity type.
     *
     * Entity types under WIM package should not have any name space prefix. For example, "Person".
     *
     * @param qualifiedEntityType The prefixed entity type.
     *
     * @return A list of RDN property names of the given entity type. If the entity type is not supported, null will be returned.
     */
/*
 * public List<String> getRDNProperties(String qualifiedEntityType, Map<String, Object> configProps, Set<String> pids, ConfigurationAdmin configAdmin) {
 * // TODO:: Extract Ldap Entity RDN
 * return null;
 * }
 */

    private void setGroupSearchScope(Map<String, Object> configProps) {
        if (configProps.get("recursiveSearch") != null &&
            configProps.get("recursiveSearch") instanceof Boolean)
            iRecursiveSearch = (Boolean) configProps.get("recursiveSearch");
    }

    @Trivial
    public boolean isRecursiveSearch() {
        return iRecursiveSearch;
    }

    @Trivial
    public boolean isLdapOperationalAttributeSet() {
        return iLdapOperationalAttr;
    }

    @Trivial
    public boolean updateGroupMembership() {
        if (LdapConstants.DOMINO_LDAP_SERVER.equalsIgnoreCase(iLdapType) || LdapConstants.SUN_LDAP_SERVER.equalsIgnoreCase(iLdapType))
            return true;
        else
            return false;
    }

    @Trivial
    public String getDummyMember(String mbrAttr) {
        if (iDummyMbrMap.containsKey(mbrAttr))
            return iDummyMbrMap.get(mbrAttr);
        else {
            return defaultDummyMember();
        }
    }

    /**
     * @return
     */
    private String defaultDummyMember() {
        if (LdapConstants.IDS_LDAP_SERVER.equalsIgnoreCase(iLdapType) || LdapConstants.DOMINO_LDAP_SERVER.equalsIgnoreCase(iLdapType)) {
            return LdapConstants.LDAP_DUMMY_MEMBER_DEFAULT;
        } else
            return null;
    }

    @Trivial
    public Set<String> getAttributesWithDefaultValue() {
        return iDefaultValueAttrs;
    }

    @Trivial
    public Set<String> getAttributesWithDefaultAttribute() {
        return iDefaultAttrAttrs;
    }

    /**
     * @return the iGroupMemberIdMap
     */
    public String getGroupMemberIdMap() {
        return iGroupMemberIdMap;
    }

    /**
     * @return
     */
    public boolean isRacf() {
        return isRacf;
    }

    /**
     * @return
     */
    public List<String> getLoginProperties() {
        return iLoginProps;
    }

    public Map<String, LdapAttribute> getAttributes() {
        return iAttrNameToAttrMap;
    }
}
