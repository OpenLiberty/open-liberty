/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap;

/**
 * Constants used by LDAP repository.
 */
public interface LdapConstants {
    String LDAP_DEFAULT_ADPTER_CLASS = "com.ibm.ws.wim.adapter.ldap.LdapAdapter";

    /**
     * The name of LDAP Distinguished Name.
     * Used in wmmLDAPAttributes.xml file to map to extId.
     */
    String LDAP_DN = "distinguishedName";

    /**
     * The equal sign used in LDAP Distinguished Name.
     */
    String LDAP_DN_EQUAL = "=";

    /**
     * The separator used in LDAP Distinguished Name.
     */
    String LDAP_DN_SEPARATOR = ",";

    /**
     * Initial context factory class provided by SUN JNDI LDAP Provider
     */
    String LDAP_SUN_SPI = "com.sun.jndi.ldap.LdapCtxFactory";

    /**
     * The Distinguished Name LDAP data type, used in WIM external id attribute map.
     */
    String LDAP_DISTINGUISHED_NAME = "DistinguishedName";

    /**
     * The name of the LDAP attribute which stores object class.
     */
    String LDAP_ATTR_OBJECTCLASS = "objectClass";

    /**
     * A String array contains only attribute objectClass.
     * Used in LDAP search to return objectClass attribute.
     */
    String[] LDAP_ATTR_OBJECTCLASS_ARRAY = { LDAP_ATTR_OBJECTCLASS };

    /**
     * The estimated size of a object class filter, used to give <code>StringBuffer</code> a initial size.
     */
    int LDAP_OBJCLS_FILTER_ESTIMATED_SIZE = 20;

    String LDAP_ATTR_SYNTAX_STRING = "string";

    /**
     * The Octect String LDAP syntax
     */
    String LDAP_ATTR_SYNTAX_OCTETSTRING = "octetString";

    /**
     * Constant for Unicode password Syntax
     */
    String LDAP_ATTR_SYNTAX_UNICODEPWD = "unicodePwd";

    /**
     * The constant for LDAP property type GUID
     */
    public static final String LDAP_ATTR_SYNTAX_GUID = "GUID";

    /**
     * The constant for RACF(SDBM) membership attribute
     */
    static final String LDAP_ATTR_RACF_CONNECT_GROUP_NAME = "racfconnectgroupname";

    /**
     * The constant for RACF(SDBM) member attribute
     */
    static final String RACF_GROUP_USER_ID = "racfgroupuserids";

    /**
     * The constant for RACFID attribute name
     */
    static final String LDAP_ATTR_RACF_ID = "racfid";

    /**
     * The name of the LDAP attribute in IDS5 or above which is automatically generated to unique identify entries.
     */
    String LDAP_ATTR_IBMENTRYUUID = "ibm-entryuuid";

    /**
     * The name of the LDAP attribute in Active Directory which is automatically generated to unique identify entries.
     */
    String LDAP_ATTR_OBJECTGUID = "objectguid";

    /**
     * The name of the LDAP attribute in Novell eDirectory which is automatically generated to unique identify entries.
     */
    String LDAP_ATTR_GUID = "guid";

    /**
     * The name of the LDAP attribute in Sun ONE directory server which is automatically generated to unique identify entries.
     */
    String LDAP_ATTR_NSUNIQUEID = "nsuniqueid";

    /**
     * The name of the LDAP attribute in Domino directory server which is automatically generated to unique identify entries.
     */
    String LDAP_ATTR_DOMINOUNID = "dominounid";

    /**
     * The name of LDAP Attribute Definition.
     */
    String LDAP_ATTRIBUTE_DEFINITION = "AttributeDefinition";

    /**
     * The default name of group member attribute.
     */
    String LDAP_ATTR_MEMBER_DEFAULT = "member";

    /**
     * The membership attribute for Active Directory.
     */
    String LDAP_ATTR_MEMBER_OF = "memberof";

    /**
     * The membership attribute for IBM Directory Server.
     */
    String LDAP_ATTR_IBM_ALL_GROUP = "ibm-allGroups";

    /**
     * The name of the LDAP attribute in Active Directory which is used to control the user account type.
     */
    String LDAP_ATTR_USER_ACCOUNT_CONTROL = "userAccountControl";

    /**
     * The name of the LDAP attribute in Active Directory which is used to store password.
     */
    String LDAP_ATTR_UNICODEPWD = "unicodePwd";

    String LDAP_ATTR_SAM_ACCOUNT_NAME = "samAccountName";

    String LDAP_ATTR_GROUP_TYPE = "groupType";

    String LDAP_ATTR_USER_PASSWORD = "userPassword";

    String LDAP_ATTR_ENTRY_OWNER = "entryowner";

    String LDAP_ATTR_SELFACCESS_ID = "access-id:cn=this";

    /**
     * The default value of searchPageSize.
     * 0 means no paged search is used.
     */
    int LDAP_SEARCH_PAGE_SIZE_DEFAULT = 0;

    /**
     * Used in groupMembershipAttributeMap and groupMemberAttributeMap to indicate
     * the specified groupMembership attribute only includes direct groups and the member attribute
     * only includes direct member.
     * For example, the 'memberOf' attribute.
     */
    String LDAP_DIRECT_GROUP_MEMBERSHIP_STRING = "direct";

    short LDAP_DIRECT_GROUP_MEMBERSHIP = 0;

    /**
     * Used in groupMembershipAttributeMap and groupMemberAttributeMap to indicate
     * the specified groupMembership attribute includes direct groups and nested groups and the member
     * attribute includes direct member and nested member.
     */
    String LDAP_NESTED_GROUP_MEMBERSHIP_STRING = "nested";

    short LDAP_NESTED_GROUP_MEMBERSHIP = 1;

    /**
     * Used in groupMembershipAttributeMap and groupMemberAttributeMap to indicate the specified
     * groupMembership attribute includes direct group, nested groups and dynamic groups and the member attribute
     * includes direct member, nested member and dynamic members.
     * For example, the 'ibm-AllGroups' attribute.
     */
    String LDAP_ALL_GROUP_MEMBERSHIP_STRING = "all";

    short LDAP_ALL_GROUP_MEMBERSHIP = 2;

    String LDAP_DUMMY_MEMBER_DEFAULT = "uid=dummy";

    String LDAP_ACCOUNT_UNIQUE_ID_PREFIX = "account";

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
    short LDAP_OPERATOR_EQ = 0;

    short LDAP_OPERATOR_NE = 1;

    short LDAP_OPERATOR_GT = 2;

    short LDAP_OPERATOR_LT = 3;

    short LDAP_OPERATOR_GE = 4;

    short LDAP_OPERATOR_LE = 5;

    String LDAP_ENV_PROP_FACTORY_SOCKET = "java.naming.ldap.factory.socket";

    String LDAP_ENV_PROP_DEREF_ALIASES = "java.naming.ldap.derefAliases";

    String LDAP_ENV_PROP_CONNECT_POOL = "com.sun.jndi.ldap.connect.pool";

    String LDAP_ENV_PROP_CONNECT_TIMEOUT = "com.sun.jndi.ldap.connect.timeout";

    String LDAP_ENV_PROP_READ_TIMEOUT = "com.sun.jndi.ldap.read.timeout";

    String LDAP_ENV_PROP_ATTRIBUTES_BINARY = "java.naming.ldap.attributes.binary";

    String LDAP_URL_PREFIX = "ldap://";

    String LDAP_URL_SSL_PREFIX = "ldaps://";

    String IDS_LDAP_SERVER = "IBM TIVOLI DIRECTORY SERVER";

    String NOVELL_LDAP_SERVER = "NOVELL EDIRECTORY";

    String AD_LDAP_SERVER = "MICROSOFT ACTIVE DIRECTORY";

    String SUN_LDAP_SERVER = "SUN JAVA SYSTEM DIRECTORY SERVER";

    String DOMINO_LDAP_SERVER = "IBM LOTUS DOMINO";

    String NETSCAPE_LDAP_SERVER = "NETSCAPE DIRECTORY SERVER";

    String SECUREWAY_LDAP_SERVER = "IBM SECUREWAY DIRECTORY SERVER";

    String CUSTOM_LDAP_SERVER = "CUSTOM";
}
