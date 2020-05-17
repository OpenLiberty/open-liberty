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

    String CONFIG_PROP_SUPPORT_CHANGE_LOG = "supportChangeLog";

    String CONFIG_SUPPORT_CHANGE_LOG_NATIVE = "native";
    String CONFIG_VALUE_FILTER_DESCRIPTOR_MODE = "CERTIFICATE_FILTER";
    String CONFIG_PROP_NAME = "name";
    String CONFIG_PROP_SYNTAX = "syntax";
    String CONFIG_PROP_PROPERTY_NAME = "propertyName";
    String CONFIG_PROP_ENTITY_TYPES = "entityTypes";
    String CONFIG_PROP_ENTITY_TYPE = "entityType";
    String CONFIG_PROP_DEFAULT_VALUE = "defaultValue";
    String CONFIG_PROP_DEFAULT_ATTRIBUTE = "substituteWithValueOf";

    /**
     * Constant for Ignore case in Ldap config
     */
    static final Object CONFIG_IGNORE_CASE = "ignoreCase";
    /**
     * The constant for attributes cache configuration tag.
     */
    static final String ATTRIBUTES_CACHE_CONFIG = "attributesCache";
    /**
     * The constant for cache configuration tag.
     */
    static final String CACHE_CONFIG = "ldapCache";
    /**
     * The constant for context pool configuration tag.
     */
    static final String CONFIG_CONTEXT_POOL = "contextPool";
    String CONFIG_PROP_READ_TIMEOUT = "timeout";
    /**
     * Define whether or not write operations are allowed on secondary servers.
     * default value is false.
     */
    String CONFIG_PROP_ALLOW_WRITE_TO_SECONDARY_SERVERS = "allowWriteToSecondaryServers";
    String CONFIG_PROP_ATTRIBUTE_RANGE_STEP = "attributeRangeStep";
    String CONFIG_PROP_ATTRIBUTE_SIZE_LIMIT = "sizeLimit";
    String CONFIG_PROP_BIND_DN = "bindDN";
    String CONFIG_PROP_BIND_PASSWORD = "bindPassword";
    String CONFIG_PROP_CACHE_SIZE = "size";
    String CONFIG_PROP_CACHE_TIME_OUT = "timeout";
    /**
     * Define how long will LDAP adapter aborts the connection attempt if the connection cannot be established.
     * Unit is second. By default, this timeout period is the network (TCP) timeout value, which is in the order of a few minutes.
     */
    String CONFIG_PROP_CONNECT_TIMEOUT = "connectTimeout";
    String CONFIG_PROP_ENABLED = "enabled";
    String CONFIG_PROP_HOST = "host";
    String CONFIG_PROP_INIT_POOL_SIZE = "initialSize";
    String CONFIG_PROP_MAX_POOL_SIZE = "maxSize";
    String CONFIG_PROP_POOL_TIME_OUT = "timeout";
    String CONFIG_PROP_POOL_WAIT_TIME = "waitTime";
    String CONFIG_PROP_PORT = "port";
    String CONFIG_PROP_PREF_POOL_SIZE = "preferredSize";
    /**
     * Define the polling interval for testing primary server availability so that can return back to primary server.
     * The polling is only enabled if the returnToPrimaryServer is set true.
     * Unit is minute. Default value is 15 minutes.
     * value less than 0 means there is no polling.
     */
    String CONFIG_PROP_PRIMARY_SERVER_QUERY_TIME_INTERVAL = "primaryServerQueryTimeInterval";
    String CONFIG_PROP_REFERAL = "referal";
    String CONFIG_PROP_REFERRAL = "referral";
    /**
     * Define whether or not automatically return back to the primary server if it is available again after failing over to secondary server.
     * default value is false.
     */
    String CONFIG_PROP_RETURN_TO_PRIMARY_SERVER = "returnToPrimaryServer";
    String CONFIG_PROP_SEARCH_COUNT_LIMIT = "searchCountLimit";
    String CONFIG_PROP_SEARCH_PAGE_SIZE = "searchPageSize";
    String CONFIG_PROP_SEARCH_RESULTS_SIZE_LIMIT = "resultsSizeLimit";
    String CONFIG_PROP_SEARCH_TIME_OUT = "searchTimeout";
    String CONFIG_PROP_SERVER_TTL_ATTRIBUTE = "serverTTLAttribute";
    String CONFIG_PROP_SSL_ENABLED = "sslEnabled";
    /**
     * Constant for Reuse Connection in Ldap Config
     */
    static final String CONFIG_REUSE_CONNECTION = "reuseConnection";
    /**
     * The constant for search results cache configuration tag.
     */
    static final String SEARCH_CACHE_CONFIG = "searchResultsCache";
    String CONFIG_PROP_LDAP_SERVER_TYPE = "ldapType";
    String CONFIG_LDAP_IDS52 = "IDS52";
    String CONFIG_PROP_CERTIFICATE_MAP_MODE = "certificateMapMode";
    String CONFIG_VALUE_CUSTOM_MODE = "CUSTOM";
    String CONFIG_VALUE_CERT_NOT_SUPPORTED_MODE = "NOT_SUPPORTED";
    String CONFIG_VALUE_EXTACT_DN_MODE = "EXACT_DN";
    String[] CONFIG_PROP_CERTIFICATE_MAP_MODE_VALUES = {
                                                         CONFIG_VALUE_EXTACT_DN_MODE, CONFIG_VALUE_FILTER_DESCRIPTOR_MODE, CONFIG_VALUE_CUSTOM_MODE,
                                                         CONFIG_VALUE_CERT_NOT_SUPPORTED_MODE
    };
    String CONFIG_PROP_CERTIFICATE_FILTER = "certificateFilter";
    String CONFIG_PROP_CERTIFICATE_MAPPER_ID = "certificateMapperId";
    /**
     * Constant for Timestamp Format in Ldap Config
     */
    static final String TIMESTAMP_FORMAT = "timestampFormat";
    String CONFIG_PROP_LOGIN_PROPERTIES = "loginProperty";
    String CONFIG_PROP_TRANSLATE_RDN = "translateRDN";
    /**
     * Custom property if set indicates to VMM that it needs encode certain characters while creating search expression.
     */
    String CONFIG_CUSTOM_PROP_USE_ENCODING_IN_SEARCH_EXPRESSION = "useEncodingInSearchExpression";
    /**
     * Constant for AD filters
     */
    static final String CONFIG_ACTIVE_DIRECTORY_FILTERS = "activedFilters";
    /**
     * Constant for Custom filters
     */
    static final String CONFIG_CUSTOM_FILTERS = "customFilters";
    /**
     * Constant for Domino filters
     */
    static final String CONFIG_DOMINO_FILTERS = "domino50Filters";
    /**
     * Constant for Novell filters
     */
    static final String CONFIG_NOVELL_DIRECTORY_FILTERS = "edirectoryFilters";
    /**
     * Constant for TDS filters
     */
    static final String CONFIG_TDS_FILTERS = "idsFilters";
    /**
     * Constant for Sun filters
     */
    static final String CONFIG_SUN_DIRECTORY_FILTERS = "iplanetFilters";
    /**
     * Constant for Netscape filters
     */
    static final String CONFIG_NETSCAPE_DIRECTORY_FILTERS = "netscapeFilters";
    /**
     * Constant for Secureway filters
     */
    static final String CONFIG_SECUREWAY_DIRECTORY_FILTERS = "securewayFilters";
    /**
     * Constant for userFilter
     */
    static final String CONFIG_USER_FILTER = "userFilter";
    /**
     * Constant for groupFilter
     */
    static final String CONFIG_GROUP_FILTER = "groupFilter";
    /**
     * Constant for userIdMap
     */
    static final String CONFIG_USER_ID_FILTER = "userIdMap";
    /**
     * Constant for groupIdMap
     */
    static final String CONFIG_GROUP_ID_FILTER = "groupIdMap";
    /**
     * Constant for groupMemberIdMap
     */
    static final String CONFIG_GROUP_MEMBER_ID_FILTER = "groupMemberIdMap";

    String CONFIG_DO_ATTRIBUTE_CONFIGUARTION = "attributeConfiguration";
    String CONFIG_DO_ATTRIBUTES = "attribute";
    String CONFIG_DO_PROPERTIES_NOT_SUPPORTED = "propertyNotSupported";
    String CONFIG_DO_EXTERNAL_ID_ATTRIBUTE = "externalIdAttribute";
    String CONFIG_PROP_AUTO_GENERATE = "autoGenerate";
    String CONFIG_LDAP_IDS4 = "IDS4";
    String CONFIG_DO_MEMBERSHIP_ATTRIBUTES = "membershipAttribute";
    String CONFIG_PROP_SCOPE = "scope";
    String CONFIG_DO_MEMBER_ATTRIBUTES = "memberAttribute";
    String CONFIG_PROP_DUMMY_MEMBER = "dummyMember";
    String CONFIG_DO_DYNAMIC_MEMBER_ATTRIBUTES = "dynamicMemberAttribute";
    String CONFIG_PROP_SEARCHFILTER = "searchFilter";
    /**
     * The name of the data object in vmm configuration data graph which represents definitive object classes.
     */
    String CONFIG_DO_OBJECTCLASS = "objectClass";
    /**
     * The name of the data object in vmm configuration data graph which represents the RDN properties of the supported entity types.
     */
    String CONFIG_DO_RDN_PROPERTY = "rdnProperty";
    String CONFIG_PROP_SEARCHBASES = "searchBase";

    // LDAP Adapter related configuration
    String CONFIG_PROP_SSL_CONFIGURATION = "sslConfiguration";

}
