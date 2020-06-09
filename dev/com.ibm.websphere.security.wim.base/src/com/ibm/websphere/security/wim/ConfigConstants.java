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
package com.ibm.websphere.security.wim;

/**
 * The interface containing all configuration related constants.
 */
public interface ConfigConstants {

    /**
     * The constant for context pool configuration tag.
     */
    static final String CONFIG_CONTEXT_POOL = "contextPool";

    /**
     * The constant for cache configuration tag.
     */
    //static final String CACHE_CONFIG = "cacheConfig";
    static final String CACHE_CONFIG = "ldapCache";

    /**
     * The constant for attributes cache configuration tag.
     */
    static final String ATTRIBUTES_CACHE_CONFIG = "attributesCache";

    /**
     * The constant for search results cache configuration tag.
     */
    static final String SEARCH_CACHE_CONFIG = "searchResultsCache";

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

    /**
     * Constant for Ignore case in Ldap config
     */
    static final Object CONFIG_IGNORE_CASE = "ignoreCase";

    /**
     * Constant for Reuse Connection in Ldap Config
     */
    static final String CONFIG_REUSE_CONNECTION = "reuseConnection";

    /**
     * Constant for Timestamp Format in Ldap Config
     */
    static final String TIMESTAMP_FORMAT = "timestampFormat";

    String BASE_SUBDIR = "wim";

    /**
     * The sub directory for schema file
     */
    String SCHEMA_SUBDIR = "schema";

    String SCHEMAFILE_SUBDIR = "etc";

    String WIM_CONFIG_NS_URI = "http://www.ibm.com/websphere/wim/config";
    String WIM_POLICY_CONFIG_NS_URI = "http://www.ibm.com/websphere/wim/policyConfig";
    /**
     * The name of the data object in vmm configuration data graph which indicates vmm to use dynamic model.
     */
    String CONFIG_DO_DYNAMIC_MODEL = "dynamicModel";
    /**
     * The name of the property in vmm configuration data graph which represents the XSD file name.
     */
    String CONFIG_PROP_XSD_FILE_NAME = "xsdFileName";
    /**
     * The name of the property in vmm configuration data graph which indicates model to use global schema.
     */
    String CONFIG_PROP_USE_GLOBAL_SCHEMA = "useGlobalSchema";

    /**
     * The name of the data object in vmm configuration data graph which indicates vmm to use static model.
     */
    String CONFIG_DO_STATIC_MODEL = "staticModel";
    /**
     * The name of the property in vmm configuration data graph which represents the full qualified interface name for the package.
     */
    String CONFIG_PROP_PACKAGE_NAME = "packageName";
    /**
     * The name of the data object in vmm configuration data graph which contains all other config information.
     */
    String CONFIG_DO_CONFIG_PROVIDER = "configurationProvider";
    /**
     * The name of the data object in vmm configuration data graph which contains all other Policy config information.
     */
    String CONFIG_DO_POLICY = "policyConfiguration";
    /**
     * The name of the data object in vmm configuration data graph which represents the supported entity types.
     */
    String CONFIG_DO_SUPPORTED_ENTITY_TYPES = "supportedEntityTypes";
    /**
     * The name of the property in vmm configuration data graph which represents a name.
     */
    String CONFIG_PROP_NAME = "name";
    /**
     * The name of the data object in vmm configuration data graph which represents the RDN properties of the supported entity types.
     */
    String CONFIG_DO_RDN_PROPERTY = "rdnProperty";
    /**
     * The name of the data object in vmm configuration data graph which contains the entity types not supported in create operation.
     */
    String CONFIG_DO_ENTITY_TYPES_NOT_ALLOW_CREATE = "EntityTypesNotAllowCreate";
    /**
     * The name of the data object in vmm configuration data graph which contains the entity types not supported in update operation.
     */
    String CONFIG_DO_ENTITY_TYPES_NOT_ALLOW_UPDATE = "EntityTypesNotAllowUpdate";
    /**
     * The name of the data object in vmm configuration data graph which contains the entity types not supported in read operation.
     */
    String CONFIG_DO_ENTITY_TYPES_NOT_ALLOW_READ = "EntityTypesNotAllowRead";
    /**
     * The name of the data object in vmm configuration data graph which contains the entity types not supported in delete operation.
     */
    String CONFIG_DO_ENTITY_TYPES_NOT_ALLOW_DELETE = "EntityTypesNotAllowDelete";
    /**
     * The name of the data object in vmm configuration data graph which represents nodes of repositories.
     */
    /**
     * The name of the data object in vmm configuration data graph which represents base entries of repositories.
     */
    String CONFIG_DO_BASE_ENTRIES = "baseEntries";
    /**
     * The property in vmm configuration data graph which defines the node name in repository, for example, LDAP node.
     */
    String CONFIG_PROP_NAME_IN_REPOSITORY = "nameInRepository";
    /**
     * The name of the data object in vmm configuration data graph which is used to define the repositories for groups.
     */
    String CONFIG_DO_REPOSITORIES_FOR_GROUPS = "repositoriesForGroups";
    /**
     * The name of the data object in vmm configuration data graph which represents repositories
     */
    String CONFIG_DO_REPOSITORIES = "repositories";
    /**
     * The type name of the LDAP repository type.
     */
    String CONFIG_DO_LDAP_REPOSITORY_TYPE = "LdapRepositoryType";
    /**
     * The type name of the Database repository type.
     */
    String CONFIG_DO_DATABASE_REPOSITORY_TYPE = "DatabaseRepositoryType";
    /**
     * The type name of the File repository type.
     */
    String CONFIG_DO_FILE_REPOSITORY_TYPE = "FileRepositoryType";
    /**
     * The type name of the custom properties type.
     */
    String CONFIG_DO_CUSTOM_PROPERTIES = "CustomProperties";
    /**
     * The type name of the RealmType
     */
    String CONFIG_REALM_TYPE = "RealmType";
    /**
     * The type name of the Realm Configuration Type
     */
    String CONFIG_REALM_CONFIG_TYPE = "RealmConfigurationType";
    /**
     * The type name of the profile repository type.
     */
    String CONFIG_PROFILE_REPOSITORY_TYPE = "ProfileRepositoryType";
    /**
     * The type name of the property extension repository type.
     */
    String CONFIG_PROPERTY_EXTENSION_REPOSITORY_TYPE = "PropertyExtensionRepositoryType";
    /**
     * The UUID property.
     */
    String CONFIG_PROP_ID = "id";
    /**
     * The default parent property.
     */
    String CONFIG_PROP_DEFAULT_PARENT = "defaultParent";
    /**
     * The property name which is used for specifying adapter class.
     */
    String CONFIG_PROP_REPOS_ADAPTER_CLASS_NAME = "adapterClassName";
    //String CONFIG_DO_LDAP_ENTITY_TYPE_LIST = "ldapEntityTypeList";

    //String CONFIG_DO_LDAP_ENTITY_TYPE_INFO = "ldapEntityTypeInfo";
    //String CONFIG_DO_OBJECT_CLASS = "objectClass";
    //String CONFIG_DO_CREATE_ENTITY_TYPE_LIST = "createEntityTypeList";
    //String CONFIG_DO_READ_ENTITY_TYPE_LIST = "readEntityTypeList";
    //String CONFIG_DO_UPDATE_ENTITY_TYPE = "updateEntityTypeList";
    //String CONFIG_DO_DELETE_ENTITY_TYPE = "deleteEntityTypeList";
    //String CONFIG_PROP_RDN_PROPERTY = "rdnProperty";

    String CONFIG_PROP_ENTITY_TYPE_NAME = "entityTypeName";
    String CONFIG_PROP_MAX_SEARCH_RESULTS = "maxSearchResults";
    String CONFIG_PROP_MAX_PAGING_RESULTS = "maxPagingResults";
    String CONFIG_PROP_MAX_TOTAL_PAGING_RESULTS = "maxTotalPagingResults";
    String CONFIG_PROP_SEARCH_TIME_OUT = "searchTimeout";
    String CONFIG_PROP_PAGED_CACHE_TIME_OUT = "pagedCacheTimeOut";
    String CONFIG_PROP_PAGING_ENTITY_OBJECT = "pagingEntityObject";
    String CONFIG_PROP_PAGING_CACHES_DISK_OFF_LOAD = "pagingCachesDiskOffLoad";

    //String NODE = "node";
    String CONFIG_PROP_ADMIN_ID = "adminId";
    String CONFIG_PROP_ADMIN_PASSWORD = "adminPassword";
    String CONFIG_PROP_IS_EXTID_UNIQUE = "isExtIdUnique";
    String CONFIG_PROP_GENERATE_EXTID = "generateExtId";
    String CONFIG_PROP_SUPPORT_SORTING = "supportSorting";
    String CONFIG_PROP_SUPPORT_PAGING = "supportPaging";
    String CONFIG_PROP_SUPPORT_CHANGE_LOG = "supportChangeLog";
    String CONFIG_PROP_SUPPORT_TRANSACTIONS = "supportTransactions";
    String CONFIG_PROP_SUPPORT_EXTERNAL_NAME = "supportExternalName";
    String CONFIG_PROP_SUPPORT_ASYNC_MODE = "supportAsyncMode";
    String CONFIG_PROP_ACCESS_ENABLED = "isAccessEnabled";

    String CONFIG_DO_ENTRY_MAPPING_REPOSITORY = "entryMappingRepository";
    String CONFIG_DO_PROPERTY_EXTENSION_REPOSITORY = "propertyExtensionRepository";

    String CONFIG_PROP_DATASOURCE_NAME = "dataSourceName";
    String CONFIG_PROP_DATABASE_TYPE = "databaseType";
    String CONFIG_PROP_ENTITY_RETRIEVAL_LIMIT = "entityRetrievalLimit";
    String CONFIG_PROP_SALT_LENGTH = "saltLength";
    String CONFIG_PROP_ENCRYPTION_KEY = "encryptionKey";
    String CONFIG_PROP_LOGIN_PROPERTIES = "loginProperty";
    String CONFIG_DO_USER_REGISTRY = "UserRegistry";
    String CONFIG_PROP_REPOS_NAME = "repositoryName";
    String CONFIG_PROP_DB_ADMIN_ID = "dbAdminId";
    String CONFIG_PROP_DB_ADMIN_PASSWORD = "dbAdminPassword";
    String CONFIG_PROP_DB_URL = "dbURL";
    String CONFIG_PROP_DB_SCHEMA = "dbSchema";
    String CONFIG_PROP_JDBC_DRIVER_CLASS = "JDBCDriverClass";

    String CONFIG_PROP_BASE_DIRECTORY = "baseDirectory";
    String CONFIG_PROP_FILE_NAME = "fileName";
    String CONFIG_PROP_MESSAGEDIGEST_ALGORITHM = "messageDigestAlgorithm";
    String CONFIG_PROP_CASE_SENSITIVE = "caseSensitive";

    // realmInfo
    String CONFIG_DO_REALM_CONFIG = "realmConfiguration";
    String CONFIG_DO_REALMS = "realms";

    String CONFIG_PROP_REALM_NAME = "name";
    String CONFIG_PROP_REALM_NODE = "node";
    String CONFIG_PROP_SECURITY_USE = "securityUse";
    String CONFIG_PROP_ALLOW_OPERATION_IF_REPOS_DOWN = "allowOperationIfReposDown";
    /**
     * The constant string for the error repository id key
     */
    String VALUE_CONTEXT_FAILURE_REPOSITORY_IDS_KEY = "failureRepositoryIDs";

    String CONFIG_PROP_DELIMITER = "delimiter";
    String CONFIG_DO_PARTICIPATING_BASE_ENTRIES = "participatingBaseEntries";
    String CONFIG_DO_DEFAULT_PARENTS = "defaultParents";
    String CONFIG_PROP_PARENTS_UNIQUE_NAME = "parentUniqueName";
    String CONFIG_DO_UNIQUE_USER_ID_MAPPING = "uniqueUserIdMapping";
    String CONFIG_DO_USER_SECURITY_NAME_MAPPING = "userSecurityNameMapping";
    String CONFIG_DO_USER_DISPLAY_NAME_MAPPING = "userDisplayNameMapping";
    String CONFIG_DO_UNIQUE_GROUP_ID_MAPPING = "uniqueGroupIdMapping";
    String CONFIG_DO_GROUP_SECURITY_NAME_MAPPING = "groupSecurityNameMapping";
    String CONFIG_DO_GROUP_DISPLAY_NAME_MAPPING = "groupDisplayNameMapping";
    String CONFIG_URATTR_UNIQUE_USER_ID = "uniqueUserId";
    String CONFIG_URATTR_USER_SECURITY_NAME = "userSecurityName";
    String CONFIG_URATTR_USER_DISPLAY_NAME = "userDisplayName";
    String CONFIG_URATTR_UNIQUE_GROUP_ID = "uniqueGroupId";
    String CONFIG_URATTR_GROUP_SECURITY_NAME = "groupSecurityName";
    String CONFIG_URATTR_GROUP_DISPLAY_NAME = "groupDisplayName";

    String CONFIG_UR_ATTR_NAME = "URAttrName";

    /**
     * @deprecated This field has been replaced by <code> CONFIG_PROP_PROPERTY_FOR_INPUT </code>
     *             The constant for the virtual member manager property that maps to the user registry attribute for input.
     */
    @Deprecated
    String CONIG_PROP_PROPERTY_FOR_INPUT = "inputProperty";

    /**
     * @deprecated This field has been replaced by <code> CONFIG_PROP_PROPERTY_FOR_OUTPUT </code>
     *             The constant for the virtual member manager property that maps to the user registry attribute for output.
     */
    @Deprecated
    String CONIG_PROP_PROPERTY_FOR_OUTPUT = "outputProperty";

    String CONFIG_PROP_PROPERTY_FOR_INPUT = "inputProperty";
    String CONFIG_PROP_PROPERTY_FOR_OUTPUT = "outputProperty";

    String CONFIG_PROP_DEFAULT_REALM = "defaultRealm";

    String CONFIG_VALUE_SECURITY_USE_ACTIVE = "active";
    String CONFIG_VALUE_SECURITY_USE_INACTIVE = "inactive";
    String CONFIG_VALUE_SECURITY_USE_NOT_SELECTABLE = "notSelectable";

    String[] CONFIG_REALM_SECURITY_USE_VALUES = {
                                                  CONFIG_VALUE_SECURITY_USE_ACTIVE,
                                                  CONFIG_VALUE_SECURITY_USE_INACTIVE,
                                                  CONFIG_VALUE_SECURITY_USE_NOT_SELECTABLE
    };

    // LDAP Adapter related configuration
    String CONFIG_DO_LDAP_SERVER_CONFIGURATION = "ldapServerConfiguration";
    String CONFIG_PROP_LDAP_SERVER_TYPE = "ldapType";
    String CONFIG_DO_LDAP_SERVERS = "ldapServers";

    String CONFIG_PROP_SEARCH_TIME_LIMIT = "searchTimeLimit";
    String CONFIG_PROP_SEARCH_COUNT_LIMIT = "searchCountLimit";
    String CONFIG_PROP_SEARCH_PAGE_SIZE = "searchPageSize";
    String CONFIG_PROP_ATTRIBUTE_RANGE_STEP = "attributeRangeStep";
    String CONFIG_PROP_SSL_KEY_STORE = "sslKeyStore";
    String CONFIG_PROP_SSL_KEY_STORE_TYPE = "sslKeyStoreType";
    String CONFIG_PROP_SSL_KEY_STORE_PASSOWRD = "sslKeyStorePassword";
    String CONFIG_PROP_SSL_TRUST_STORE = "sslTrustStore";
    String CONFIG_PROP_SSL_TRUST_STORE_TYPE = "sslTrustStoreType";
    String CONFIG_PROP_SSL_TRUST_STORE_PASSWORD = "sslTrustStorePassword";
    String CONFIG_PROP_SSL_DEBUG = "sslDebug";

    String CONFIG_DO_CONNECTIONS = "connections";
    String CONFIG_PROP_HOST = "host";
    String CONFIG_PROP_PORT = "port";
    String CONFIG_PROP_BIND_DN = "bindDN";
    String CONFIG_PROP_BIND_PASSWORD = "bindPassword";
    String CONFIG_PROP_SSL_ENABLED = "sslEnabled";
    String CONFIG_PROP_AUTHENTICATION = "authentication";
    String CONFIG_PROP_REFERAL = "referal";
    String CONFIG_PROP_REFERRAL = "referral";
    String CONFIG_PROP_DEREFALIASES = "derefAlias";
    String CONFIG_PROP_SSL_CONFIGURATION = "sslConfiguration";
    String CONFIG_PROP_CONNECTION_POOL = "connectionPool";

    /**
     * Define how long will LDAP adapter aborts the connection attempt if the connection cannot be established.
     */
    String CONFIG_PROP_CONNECT_TIMEOUT = "connectTimeout";

    /**
     * Define how long will LDAP adapter aborts the read attempt if the connection cannot be established.
     */
    String CONFIG_PROP_READ_TIMEOUT = "readTimeout";

    /**
     * Define whether JNDI BER packets will be written to the System.out
     */
    String CONFIG_PROP_JNDI_OUTPUT_ENABLED = "jndiOutputEnabled";

    /**
     * Define whether or not write operations are allowed on secondary servers.
     * default value is false.
     */
    String CONFIG_PROP_ALLOW_WRITE_TO_SECONDARY_SERVERS = "allowWriteToSecondaryServers";
    /**
     * Define whether or not automatically return back to the primary server if it is available again after failing over to secondary server.
     * default value is false.
     */
    String CONFIG_PROP_RETURN_TO_PRIMARY_SERVER = "returnToPrimaryServer";
    /**
     * Define the polling interval for testing primary server availability so that can return back to primary server.
     * The polling is only enabled if the returnToPrimaryServer is set true.
     * Unit is minute. Default value is 15 minutes.
     * value less than 0 means there is no polling.
     */
    String CONFIG_PROP_PRIMARY_SERVER_QUERY_TIME_INTERVAL = "primaryServerQueryTimeInterval";

    String CONFIG_PROP_ENVIRONMENT_PROPERTIES = "environmentProperties";
    String CONFIG_PROP_VALUE = "value";

    String CONFIG_DO_CONTEXT_POOL = "contextPool";
    String CONFIG_PROP_ENABLED = "enabled";
    String CONFIG_PROP_INIT_POOL_SIZE = "initialSize";
    String CONFIG_PROP_MAX_POOL_SIZE = "maxSize";
    String CONFIG_PROP_PREF_POOL_SIZE = "preferredSize";
    String CONFIG_PROP_POOL_TIME_OUT = "timeout";
    String CONFIG_PROP_POOL_WAIT_TIME = "waitTime";

    String CONFIG_DO_CACHE_CONFIGURATION = "cacheConfiguration";
    String CONFIG_PROP_CACHES_DISK_OFF_LOAD = "cachesDiskOffLoad";
    String CONFIG_DO_ATTRIBUTES_CACHE = "attributesCache";
    String CONFIG_PROP_CACHE_SIZE = "size";
    String CONFIG_PROP_CACHE_TIME_OUT = "timeout";
    String CONFIG_PROP_ATTRIBUTE_SIZE_LIMIT = "sizeLimit";
    String CONFIG_PROP_CACHE_DIST_POLICY = "cacheDistPolicy";

    String CONFIG_DO_SEARCH_RESULTS_CACHE = "searchResultsCache";
    String CONFIG_PROP_SEARCH_RESULTS_SIZE_LIMIT = "resultsSizeLimit";

    String CONFIG_DO_GROUP_CONFIGURATION = "groupConfiguration";
    String CONFIG_PROP_UPDATE_GROUP_MEMBERSHIP = "updateGroupMembership";
    String CONFIG_DO_MEMBER_ATTRIBUTES = "memberAttribute";
    String CONFIG_PROP_OBJECT_CLASS = "objectClass";
    String CONFIG_PROP_SCOPE = "scope";
    String CONFIG_PROP_DUMMY_MEMBER = "dummyMember";
    String CONFIG_DO_DYNAMIC_MEMBER_ATTRIBUTES = "dynamicMemberAttribute";
    String CONFIG_DO_MEMBERSHIP_ATTRIBUTES = "membershipAttribute";
    String CONFIG_PROP_LDAP_TIMESTAMP_FORMAT = "ldapTimestampFormat";
    /**
     * Custom property if set indicates to VMM that user wants to use the input
     * principal name to be used for authenticating on back-end LDAP instead of the
     * user DN (which is the default behavior).
     */
    String CONFIG_CUSTOM_PROP_USE_INPUT_PRINCIPALNAME_FOR_LOGIN = "useInputPrincipalNameForLogin";

    //PM59885
    /**
     * Custom property if set indicates to VMM to getMembers for groups without spaces in DN.
     * if users/groups has spaces in DN in LDAP.
     */
    String CONFIG_CUSTOM_PROP_RETURN_DN_WITHOUT_SPACE_IN_GETMEMBERS = "returnDNWithOutSpaceInGetMembers";

    /**
     * The name of the data object in vmm configuration data graph which represents the entity types supported in LDAP adapter.
     */
    String CONFIG_DO_LDAP_ENTITY_TYPES = "ldapEntityTypes";
    String CONFIG_PROP_OBJECTCLASS = "objectClass";
    String CONFIG_PROP_SEARCHBASES = "searchBase";
    String CONFIG_PROP_SEARCHFILTER = "searchFilter";
    String CONFIG_PROP_TRANSLATE_RDN = "translateRDN";
    String CONFIG_PROP_CERTIFICATE_MAP_MODE = "certificateMapMode";
    String CONFIG_VALUE_EXTACT_DN_MODE = "EXACT_DN";
    String CONFIG_VALUE_FILTER_DESCRIPTOR_MODE = "CERTIFICATE_FILTER";
    String CONFIG_VALUE_CUSTOM_MODE = "CUSTOM";
    String CONFIG_VALUE_CERT_NOT_SUPPORTED_MODE = "NOT_SUPPORTED";
    String[] CONFIG_PROP_CERTIFICATE_MAP_MODE_VALUES = {
                                                         CONFIG_VALUE_EXTACT_DN_MODE, CONFIG_VALUE_FILTER_DESCRIPTOR_MODE, CONFIG_VALUE_CUSTOM_MODE,
                                                         CONFIG_VALUE_CERT_NOT_SUPPORTED_MODE
    };
    String CONFIG_PROP_CERTIFICATE_FILTER = "certificateFilter";
    String CONFIG_PROP_CERTIFICATE_MAPPER_ID = "certificateMapperId";
    /**
     * The name of the property "readOnly" in the vmm configuration
     */
    String CONFIG_PROP_READ_ONLY = "readOnly";

    /**
     * The name of the data object in vmm configuration data graph which represents definitive object classes.
     */
    String CONFIG_DO_OBJECTCLASSES = "objectClass";

    /**
     * The name of the data object in vmm configuration data graph which represents object classes for creating.
     */
    String CONFIG_DO_OBJECTCLASSES_FOR_CREATE = "objectClassesForCreate";
    String CONFIG_DO_ATTRIBUTE_CONFIGUARTION = "attributeConfiguration";
    String CONFIG_DO_ATTRIBUTES = "attribute";
    String CONFIG_PROP_PROPERTY_NAME = "propertyName";
    String CONFIG_DO_EXTERNAL_ID_ATTRIBUTE = "externalIdAttribute";
    String CONFIG_PROP_SYNTAX = "syntax";
    String CONFIG_PROP_DEFAULT_VALUE = "defaultValue";
    String CONFIG_PROP_DEFAULT_ATTRIBUTE = "substituteWithValueOf";
    String CONFIG_PROP_AUTO_GENERATE = "autoGenerate";
    String CONFIG_PROP_ENTITY_TYPES = "entityTypes";
    String CONFIG_PROP_ENTITY_TYPE = "entityType";
    String CONFIG_PROP_ENTITY_TYPES_LIST = "entityTypesList";
    String CONFIG_DO_PROPERTIES_NOT_SUPPORTED = "propertyNotSupported";

    // Defines the LDAP server types supported by WIM
    String CONFIG_LDAP_SECUREWAY = "SECUREWAY";
    String CONFIG_LDAP_IDS = "IDS";
    String CONFIG_LDAP_IDS4 = "IDS4";
    String CONFIG_LDAP_IDS51 = "IDS51";
    String CONFIG_LDAP_IDS52 = "IDS52";
    String CONFIG_LDAP_IDS6 = "IDS6";
    String CONFIG_LDAP_ZOSDS = "ZOSDS";
    String CONFIG_LDAP_DOMINO = "DOMINO";
    String CONFIG_LDAP_DOMINO5 = "DOMINO5";
    String CONFIG_LDAP_DOMINO6 = "DOMINO6";
    String CONFIG_LDAP_DOMINO65 = "DOMINO65";
    String CONFIG_LDAP_NDS = "NDS";
    String CONFIG_LDAP_SUNONE = "SUNONE";
    String CONFIG_LDAP_AD = "AD";
    String CONFIG_LDAP_AD2000 = "AD2000";
    String CONFIG_LDAP_AD2003 = "AD2003";
    String CONFIG_LDAP_ADAM = "ADAM";
    //String CONFIG_LDAP_OPENLDAP = "OPENLDAP";
    String CONFIG_LDAP_CUSTOM = "CUSTOM";

    /**
     * List of supported LDAP server types. If a constant is added or removed,
     * then this list should be updated too.
     */
    String[] CONFIG_LDAP_SUPPORTED_TYPES = {
                                             CONFIG_LDAP_IDS,
                                             CONFIG_LDAP_ZOSDS,
                                             CONFIG_LDAP_DOMINO,
                                             CONFIG_LDAP_NDS,
                                             CONFIG_LDAP_SUNONE,
                                             CONFIG_LDAP_AD,
                                             CONFIG_LDAP_ADAM,
                                             CONFIG_LDAP_CUSTOM
    };

    // DB Types
    String CONFIG_DB_DB2 = "db2";
    String CONFIG_DB_ORACLE = "oracle";
    String CONFIG_DB_INFORMIX = "informix";
    String CONFIG_DB_SQLSERVER = "sqlserver";
    String CONFIG_DB_DERBY = "derby";
    String CONFIG_DB_DB2ZOS = "db2zos";
    String CONFIG_DB_DB2ISERIES = "db2iseries";

    /**
     * List of supported DB types. If constant is added or removed,
     * then this list should be updated too.
     */
    String[] CONFIG_DB_SUPPORTED_TYPES = {
                                           CONFIG_DB_DB2,
                                           CONFIG_DB_ORACLE,
                                           CONFIG_DB_INFORMIX,
                                           CONFIG_DB_SQLSERVER,
                                           CONFIG_DB_DERBY,
                                           CONFIG_DB_DB2ZOS,
                                           CONFIG_DB_DB2ISERIES
    };

    // Message Digest algorithms
    String CONFIG_MDALGO_SHA1 = "SHA-1";
    String CONFIG_MDALGO_SHA256 = "SHA-256";
    String CONFIG_MDALGO_SHA384 = "SHA-384";
    String CONFIG_MDALGO_SHA512 = "SHA-512";

    /**
     * List of supported Message Digest Algorithms. If constant is added or removed,
     * then this list should be updated too.
     */
    String[] CONFIG_SUPPORTED_MDALGORITHMS = {
                                               CONFIG_MDALGO_SHA1, CONFIG_MDALGO_SHA256, CONFIG_MDALGO_SHA384, CONFIG_MDALGO_SHA512
    };

    // String constants for authentication types
    String CONFIG_AUTHENTICATION_TYPE_NONE = "none";
    String CONFIG_AUTHENTICATION_TYPE_SIMPLE = "simple";
    String CONFIG_AUTHENTICATION_TYPE_STRONG = "strong";
    String[] CONFIG_AUTHENTICATION_TYPES = {
                                             CONFIG_AUTHENTICATION_TYPE_NONE, CONFIG_AUTHENTICATION_TYPE_SIMPLE, CONFIG_AUTHENTICATION_TYPE_STRONG
    };

    // string constants for scope
    String CONFIG_SCOPE_DIRECT = "direct";
    String CONFIG_SCOPE_NESTED = "nested";
    String CONFIG_SCOPE_ALL = "all";
    String[] CONFIG_SCOPES = {
                               CONFIG_SCOPE_DIRECT, CONFIG_SCOPE_NESTED, CONFIG_SCOPE_ALL
    };

    String CONFIG_PROP_SERVER_EXTERNAL_NAME = "serverExternalName";
    String CONFIG_PROP_TOPIC_SUBSCRIBER_NAME = "topicSubscriberName";

    /**
     * DynaCache distribution policy: not shared
     */
    String CONFIG_CACHE_DIST_NONE = "none";

    /**
     * DynaCache distribution policy: shared push
     */
    String CONFIG_CACHE_DIST_PUSH = "push";

    /**
     * DynaCache distribution policy: shared push pull
     */
    String CONFIG_CACHE_DIST_PUSH_PULL = "push_pull";

    /**
     * List of Dynacache distribution policies.
     */
    String[] CONFIG_CACHE_DIST_POLICIES = {
                                            CONFIG_CACHE_DIST_NONE,
                                            CONFIG_CACHE_DIST_PUSH,
                                            CONFIG_CACHE_DIST_PUSH_PULL
    };

    /**
     * Custom property name for Change Handler Class
     */
    String PROP_CHANGE_HANDLER_CLASS_NAME = "ChangeHandlerClassName";

    /**
     * One of the possible values for supportChangeLog repository configuration attribute is None
     * which means there is no mechanism in the repository to track changes
     */
    String CONFIG_SUPPORT_CHANGE_LOG_NONE = "none";

    /**
     * One of the possible values for supportChangeLog repository configuration attribute is "native",
     * which means virtual member manager is using the change tracking mechanism provided by the repository.
     */
    String CONFIG_SUPPORT_CHANGE_LOG_NATIVE = "native";

    //String CONFIG_SUPPORT_CHANGE_LOG_VMM = "vmm";
    String[] CONFIG_CHANGELOG_SUPPORT_TYPES = {
                                                CONFIG_SUPPORT_CHANGE_LOG_NONE,
                                                CONFIG_SUPPORT_CHANGE_LOG_NATIVE,
                    //CONFIG_SUPPORT_CHANGE_LOG_VMM
    };

    /**
     * Repository id for Property extension repository
     */
    String PROPERTY_EXTENSION_REPOSITORY_ID = "LA"; //80054.7X

//    String SEMICOLON_DELIMITER = ";";
    /**
     * The name of LDAP Distinguished Name used as external id attribute.
     */
    String DISTINGUISHED_NAME = "distinguishedName";

    //Authz related Constants
    String CONFIG_JACC_POLICY_CLASS = "jaccPolicyClass";
    String CONFIG_JACC_ROLEMAPPING_CLASS = "jaccRoleMappingClass";
    String CONFIG_JACC_POLICY_FACTORY_CLASS = "jaccPolicyConfigFactoryClass";
    String CONFIG_JACC_ROLEMAPPING_FACTORY_CLASS = "jaccRoleMappingConfigFactoryClass";
    String CONFIG_JACC_ROLEPERMISSION_POLICY_ID = "jaccRoleToPermissionPolicyId";
    String CONFIG_JACC_PRINCIPALROLE_POLICY_ID = "jaccPrincipalToRolePolicyId";
    String CONFIG_JACC_ROLEPERMISSION_FILENAME = "jaccRoleToPermissionPolicyFileName";
    String CONFIG_JACC_PRINCIPALROLE_FILENAME = "jaccPrincipalToRolePolicyFileName";
    String CONFIG_ROOT = CONFIG_DO_CONFIG_PROVIDER;
    String CONFIG_AUTHORIZATION = "authorization";

    String CONFIG_ADMIN_ROLE = "IdMgrAdmin";
    String CONFIG_READER_ROLE = "IdMgrReader";
    String CONFIG_WRITER_ROLE = "IdMgrWriter";

    String CONFIG_PROP_ROLE_NAME = "roleName";
    String CONFIG_PROP_GROUP_NAME = "groupId";
    String CONFIG_PROP_USER_NAME = "userId";

    String CONFIG_WILD_CHAR = "*";
    String ALL_AUTHENTICATED = "AllAuthenticatedUsers";

    /**
     * The name of config value "notSupported".
     */
    String CONFIG_VALUE_NOT_SUPPORTED_MODE = "notSupported";

    /**
     * The name of config value "exactDN".
     */
    String CONFIG_VALUE_EXACT_DN = "exactDN";

    /**
     * Custom property if set indicates to VMM that Groups should always be included
     * in the list of entity types while searching for Group members by member attribute
     * even if the user has a query that does not include the type "Group" as a return
     * type.
     */
    String CONFIG_CUSTOM_PROP_RETURN_NESTED_NON_GROUP_MEMBERS = "com.ibm.ws.wim.adapter.ldap.returnNestedNonGroupMembers";

    /**
     * Custom property which allows WAS to start up if even Database Repository is down
     */
    String CONFIG_PROP_ALLOW_START_IF_DB_DOWN = "allowStartupIfDBDown";
    /**
     * Custom property if set indicates to VMM that it needs encode certain characters while creating search expression.
     */
    String CONFIG_CUSTOM_PROP_USE_ENCODING_IN_SEARCH_EXPRESSION = "useEncodingInSearchExpression";
}
