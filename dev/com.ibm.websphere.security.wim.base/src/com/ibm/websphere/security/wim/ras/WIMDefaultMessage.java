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
package com.ibm.websphere.security.wim.ras;

/**
 *
 * This class defines the default messages against the corresponding message keys
 * 
 * @see WIMMessageKey
 *
 */
public interface WIMDefaultMessage {

    public static final String ENTITY_IDENTIFIER_NOT_SPECIFIED = "CWIML1009E: The identifier of the entity was not found.";
    public static final String EXTERNAL_NAME_CONTROL_NOT_FOUND = "CWIML1024E: The ''{0}'' external name is specified, but the external name control is not specified.";
    public static final String INVALID_IDENTIFIER = "CWIML1010E: The uniqueid = ''{0}'', uniqueName = ''{1}'' identifier is not valid.";
    public static final String ENTITY_NOT_IN_REALM_SCOPE = "CWIML0515E: The ''{0}'' entity is not in the scope of the ''{1}'' realm.";
    public static final String MISSING_SORT_KEY = "CWIML1001E: The sort key is missing from the SortControl data object.";
    public static final String MISSING_SEARCH_CONTROL = "CWIML1017E: The SearchControl data object is missing from the input data object of the search operation.";
    public static final String MISSING_COOKIE = "CWIML1002E: The cookie used to get the next page of the search results is missing from the PageControl data object.";
    public static final String INVALID_COOKIE = "CWIML1041E: The cookie specified in the PageControl data object is invalid.";
    public static final String INCORRECT_COUNT_LIMIT = "CWIML1022E: The ''{0}'' count limit specified in the SearchControl data object is invalid.";
    public static final String INCORRECT_SEARCH_LIMIT = "CWIML1031E: The ''{0}'' search limit specified in the SearchControl data object is invalid.";
    public static final String CANNOT_SPECIFY_COUNT_LIMIT = "CWIML1019E: Cannot specify the countLimit parameter in a SearchControl DataObject when the PageControl DataObject is also specified in a search call.";
    public static final String MISSING_SEARCH_EXPRESSION = "CWIML1003E: The expression property is missing from the SearchControl data object.";
    public static final String SEARCH_EXPRESSION_ERROR = "CWIML1004E: The ''{0}'' error exists in the search expression specified in the SearchControl data object.";
    public static final String INVALID_SEARCH_EXPRESSION = "CWIML1029E: The search expression ''{0}'' is not valid.";
    public static final String EXCEED_MAX_TOTAL_SEARCH_LIMIT = "CWIML1018E: ''{0}'' search results exceeds the ''{1}'' maximum search limit.";
    public static final String INVALID_CHANGETYPE = "CWIML4552E: The changeType, ''{0}'', is not valid for a search for changed entities.";
    public static final String MISSING_ENTITY_DATA_OBJECT = "CWIML1030E: Entity DataObject is missing for operation ''{0}''.";
    public static final String ACTION_MULTIPLE_ENTITIES_SPECIFIED = "CWIML1016E: The ''{0}'' action does not support multiple entities. Specify only one entity for this operation.";
    public static final String MISSING_OR_EMPTY_PRINCIPAL_NAME = "CWIML4536E: The principal name is missing or empty.";
    public static final String MULTIPLE_PRINCIPALS_FOUND = "CWIML4538E: Multiple principals were found for the ''{0}'' principal name.";
    public static final String AUTHENTICATE_NOT_SUPPORTED = "CWIML4530E: The authentication is not supported by the ''{0}'' repository. Root cause: ''{1}''.";
    public static final String CERTIFICATE_MAP_FAILED = "CWIML3011E: Mapping of the certificate failed.";
    public static final String PRINCIPAL_NOT_FOUND = "CWIML4537E: No principal is found from the ''{0}'' principal name.";
    public static final String NON_EXISTING_SEARCH_BASE = "CWIML1042E: The search base ''{0}'' does not exist in the current realm.";
    public static final String ENTITY_NOT_FOUND = "CWIML4001E: The ''{0}'' entity was not found.";
    public static final String INVALID_LEVEL_IN_CONTROL = "CWIML4514E: An incorrect value for the ''{0}'' property level, is specified in ''{1}''.";
    public static final String GENERIC = "CWIML1999E: An exception occurred during processing: {0}";
    public static final String INVALID_UNIQUE_NAME_SYNTAX = "CWIML1011E: The {0} unique name is not valid.";
    public static final String INVALID_PROPERTY_VALUE_FORMAT = "CWIML1015E: The format of the value of the {0} property is not valid.";

    public static final String INVALID_REALM_NAME = "CWIMK0006E: The realm name specified is not valid: {0}";
    public static final String MISSING_BASE_ENTRY_IN_REALM = "CWIMK0007E: There is no valid base entry defined for the realm: {0}";
    public static final String MISSING_BASE_ENTRY = "CWIMK0008E: The repository {0} must contain at least one base entry.";
    public static final String NAMING_EXCEPTION = "CWIML4520E: The ''{0}'' naming exception occurred during processing.";
    public static final String SYSTEM_EXCEPTION = "CWIML1998E: The following system exception occurred during processing: ''{0}''.";
    public static final String INVALID_PROPERTY_VALUE = "CWIML1013E: The value of the property {0} is not valid for entity {1}.";
    public static final String MISSING_MANDATORY_PROPERTY = "CWIML1028E: The value of mandatory property ''{0}'' is missing.";
    public static final String ENTITY_IS_NOT_A_GROUP = "CWIML4525E: The ''{0}'' entity type is not a Group type. This operation is only supported by a Group type.";
    public static final String INVALID_INIT_PROPERTY = "CWIML4518E: The ''{0}'' initialization property is not valid.";
    public static final String LDAP_ENTRY_NOT_FOUND = "CWIML4527E: The LDAP entry ''{0}'' was not found: ''{1}''.";
    public static final String PROPERTY_NOT_DEFINED = "CWIML0514W: The ''{0}'' property is not defined.";
    public static final String CANNOT_SEARCH_PRINCIPAL_NAME_WITH_OTHER_PROPS = "CWIML1021E: Search principalName with other properties is not supported.";
    public static final String NULL_CHECKPOINT_VALUE = "CWIML4550E: An empty or null checkpoint value was passed to an adapter that supports change tracking.";
    public static final String MISSING_OR_EMPTY_PASSWORD = "CWIML4541E: The password is missing or empty.";
    public static final String PASSWORD_CHECKED_FAILED = "CWIML4529E: The password verification for the ''{0}'' principal name failed. Root cause: ''{1}''.";
    public static final String INVALID_CERTIFICATE_FILTER = "CWIML0002E: The syntax of the ''{0}'' certificate filter is not valid. The correct syntax is: LDAP attribute=${Client certificate attribute} (for example, uid=${SubjectCN}).";
    public static final String TBS_CERTIFICATE_UNSUPPORTED = "CWIML0008E: getTBSCertificate() is unsupported in filter expression.";
    public static final String UNKNOWN_CERTIFICATE_ATTRIBUTE = "CWIML0009E: An unknown certificate attribute ''{0}'' was used in the filter specification.";
    public static final String UNKOWN_DN_FIELD = "CWIML0003E: The ''{0}'' Distinguished Name field is unknown.";
    public static final String EXT_ID_VALUE_IS_NULL = "CWIML4548E: The LDAP attribute used as an external identifier ''{0}'' has a null value for entity ''{1}''.";
    public static final String EXT_ID_HAS_MULTIPLE_VALUES = "CWIML4528E: The LDAP attribute used as an external identifier contains multiple values: ''{0}''.";
    public static final String INVALID_DN_SYNTAX = "CWIML4517E: The {0} distinguished name (DN) is not valid.";
    public static final String INIT_POOL_SIZE_TOO_BIG = "CWIML4532E: The initial {0} setting for the context pool size must be less the {1} setting for the maximum context pool size.";
    public static final String PREF_POOL_SIZE_TOO_BIG = "CWIML4533E: The preferred setting of {0} for the context pool size must be less than the setting of {1} for the maximum context pool size.";
    public static final String MISSING_INI_PROPERTY = "CWIML0004E: The initialization property {0} is missing from the configuration.";
    public static final String DUPLICATE_ENTITY_TYPE = "CWIML4531E: A duplicate {0} entity type is defined in the configuration XML file.";
}
