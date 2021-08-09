/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.utils;

public class ValidationMessageConstants {
    
    public static final String CALLBACK_URL_TEMPLATE_EMPTY              = "callbackURLTemplateEmpty"; //$NON-NLS-1$
    public static final String CALLBACK_INVALID_SUBSTITUTION_VARIABLES  = "callbackInvalidSubstitutionVariables"; //$NON-NLS-1$
    public static final String CALLBACK_MUST_BE_RUNTIME_EXPRESSION      = "callbackMustBeRuntimeExpression"; //$NON-NLS-1$
    public static final String CALLBACK_INVALID_URL                     = "callbackInvalidURL"; //$NON-NLS-1$
    public static final String CALLBACK_INVALID_PATH_ITEM               = "callbackInvalidPathItem"; //$NON-NLS-1$

    public static final String CONTACT_INVALID_URL                      = "contactInvalidURL"; //$NON-NLS-1$
    public static final String CONTACT_INVALID_EMAIL                    = "contactInvalidEmail"; //$NON-NLS-1$

    public static final String EXAMPLE_ONLY_VALUE_OR_EXTERNAL_VALUE     = "exampleOnlyValueOrExternalValue"; //$NON-NLS-1$
    public static final String EXTERNAL_DOCUMENTATION_INVALID_URL       = "externalDocumentationInvalidURL"; //$NON-NLS-1$
    
    public static final String INVALID_EXTENSION_NAME                   = "invalidExtensionName"; //$NON-NLS-1$
    public static final String INVALID_URI                              = "invalidUri"; //$NON-NLS-1$
    
    public static final String HEADER_EXAMPLE_OR_EXAMPLES               = "headerExampleOrExamples"; //$NON-NLS-1$
    public static final String HEADER_SCHEMA_OR_CONTENT                 = "headerSchemaOrContent"; //$NON-NLS-1$
    public static final String HEADER_SCHEMA_AND_CONTENT                = "headerSchemaAndContent"; //$NON-NLS-1$
    public static final String HEADER_CONTENT_MAP                       = "headerContentMap"; //$NON-NLS-1$

    public static final String INFO_TERMS_OF_SERVICE                    = "infoTermsOfServiceInvalidURL"; //$NON-NLS-1$

    public static final String KEY_NOT_A_REGEX                          = "keyNotARegex";  //$NON-NLS-1$

    public static final String LICENSE_INVALID_URL                      = "licenseInvalidURL"; //$NON-NLS-1$
    public static final String LINK_MUST_SPECIFY_OPERATION_REF_OR_ID    = "linkMustSpecifyOperationRefOrId"; //$NON-NLS-1$
    public static final String LINK_OPERATION_ID_INVALID                = "linkOperationIdInvalid"; //$NON-NLS-1$
    public static final String LINK_OPERATION_REF_AND_ID                = "linkOperationRefAndId"; //$NON-NLS-1$
    public static final String LINK_OPERATION_REF_INVALID_OR_MISSING    = "linkOperationRefInvalidOrMissing"; //$NON-NLS-1$
    
    public static final String MEDIA_TYPE_ENCODING_PROPERTY             = "mediaTypeEncodingProperty"; //$NON-NLS-1$
    public static final String MEDIA_TYPE_EXAMPLE_OR_EXAMPLES           = "mediaTypeExampleOrExamples"; //$NON-NLS-1$
    public static final String MEDIA_TYPE_EMPTY_SCHEMA                  = "mediaTypeEmptySchema"; //$NON-NLS-1$
    
    public static final String NON_APPLICABLE_FIELD                     = "nonApplicableField"; //$NON-NLS-1$
    public static final String NON_APPLICABLE_FIELD_WITH_VALUE          = "nonApplicableFieldWithValue"; //$NON-NLS-1$
    public static final String NULL_VALUE_IN_MAP                        = "nullValueInMap";  //$NON-NLS-1$
    public static final String NULL_OR_EMPTY_KEY_IN_MAP                 = "nullOrEmptyKeyInMap";  //$NON-NLS-1$

    public static final String OAUTH_FLOW_INVALID_URL                   = "oAuthFlowInvalidURL"; //$NON-NLS-1$

    public static final String OPENAPI_VERSION_INVALID                  = "openAPIVersionInvalid"; //$NON-NLS-1$
    public static final String OPENAPI_TAG_IS_NOT_UNIQUE                = "openAPITagIsNotUnique"; //$NON-NLS-1$   
    
    public static final String OPERATION_IDS_MUST_BE_UNIQUE             = "operationIdsMustBeUnique"; //$NON-NLS-1$
    
    public static final String PARAMETER_IN_FIELD_INVALID               = "parameterInFieldInvalid"; //$NON-NLS-1$
    public static final String PARAMETER_EXAMPLE_OR_EXAMPLES            = "parameterExampleOrExamples"; //$NON-NLS-1$
    public static final String PARAMETER_SCHEMA_OR_CONTENT              = "parameterSchemaOrContent"; //$NON-NLS-1$
    public static final String PARAMETER_SCHEMA_AND_CONTENT             = "parameterSchemaAndContent"; //$NON-NLS-1$
    public static final String PARAMETER_CONTENT_MAP_MUST_NOT_BE_EMPTY  = "parameterContentMapMustNotBeEmpty"; //$NON-NLS-1$

    public static final String PATH_ITEM_INVALID_FORMAT                 = "pathItemInvalidFormat"; //$NON-NLS-1$
    public static final String PATH_ITEM_INVALID_REF                    = "pathItemInvalidRef"; //$NON-NLS-1$
    public static final String PATH_ITEM_REQUIRED_FIELD                 = "pathItemRequiredField"; //$NON-NLS-1$
    public static final String PATH_ITEM_DUPLICATE                      = "pathItemDuplicate"; //$NON-NLS-1$
    public static final String PATH_ITEM_OP_NO_PATH_PARAM_DECLARED      = "pathItemOperationNoPathParameterDeclared"; //$NON-NLS-1$
    public static final String PATH_ITEM_OP_PARAM_NOT_DECLARED_MULTIPLE = "pathItemOperationParameterNotDeclaredMultiple"; //$NON-NLS-1$
    public static final String PATH_ITEM_OP_PARAM_NOT_DECLARED_SINGLE   = "pathItemOperationParameterNotDeclaredSingle"; //$NON-NLS-1$
    public static final String PATH_ITEM_OPERATION_DUPLICATE            = "pathItemOperationDuplicate"; //$NON-NLS-1$
    public static final String PATH_ITEM_OPERATION_NULL_PARAMETER       = "pathItemOperationNullParameter"; //$NON-NLS-1$
    public static final String PATH_ITEM_OPERATION_REQUIRED_FIELD       = "pathItemOperationRequiredField"; //$NON-NLS-1$
    public static final String PATH_ITEM_PARAM_NOT_DECLARED_MULTIPLE    = "pathItemParameterNotDeclaredMultiple"; //$NON-NLS-1$
    public static final String PATH_ITEM_PARAM_NOT_DECLARED_SINGLE      = "pathItemParameterNotDeclaredSingle"; //$NON-NLS-1$

    public static final String PATHS_REQUIRES_SLASH                     = "pathsRequiresSlash"; //$NON-NLS-1$

    public static final String REFERENCE_NOT_PART_OF_MODEL              = "referenceNotPartOfModel"; //$NON-NLS-1$
    public static final String REFERENCE_NOT_VALID                      = "referenceNotValid"; //$NON-NLS-1$
    public static final String REFERENCE_NOT_VALID_FORMAT               = "referenceNotValidFormat"; //$NON-NLS-1$
    public static final String REFERENCE_NULL                           = "referenceNull"; //$NON-NLS-1$
    public static final String REFERENCE_TO_OBJECT_INVALID              = "referenceToObjectInvalid"; //$NON-NLS-1$
    
    public static final String REQUIRED_FIELD_MISSING                   = "requiredFieldMissing"; //$NON-NLS-1$

    public static final String RESPONSE_MUST_CONTAIN_ONE_CODE           = "responseMustContainOneCode"; //$NON-NLS-1$
    public static final String RESPONSE_SHOULD_CONTAIN_SUCCESS          = "responseShouldContainSuccess"; //$NON-NLS-1$

    public static final String SCHEMA_TYPE_ARRAY_NULL_ITEMS             = "schemaTypeArrayNullItems"; //$NON-NLS-1$
    public static final String SCHEMA_READ_ONLY_OR_WRITE_ONLY           = "schemaReadOnlyOrWriteOnly"; //$NON-NLS-1$
    public static final String SCHEMA_MULTIPLE_OF_LESS_THAN_ONE         = "schemaMultipleOfLessThanOne"; //$NON-NLS-1$
    public static final String SCHEMA_MULTIPLE_OF_LESS_THAN_ZERO        = "schemaPropertyLessThanZero"; //$NON-NLS-1$
    public static final String SCHEMA_TYPE_DOES_NOT_MATCH_PROPERTY      = "schemaTypeDoesNotMatchProperty"; //$NON-NLS-1$
    
    public static final String SECURITY_REQ_NOT_DECLARED                = "securityRequirementNotDeclared"; //$NON-NLS-1$
    public static final String SECURITY_REQ_SCOPE_NAMES_REQUIRED        = "securityRequirementScopeNamesRequired"; //$NON-NLS-1$
    public static final String SECURITY_REQ_FIELD_NOT_EMPTY             = "securityRequirementFieldNotEmpty"; //$NON-NLS-1$
    public static final String SECURITY_REQ_IS_EMPTY                    = "securityRequirementIsEmpty"; //$NON-NLS-1$
    
    public static final String SECURITY_SCHEME_IN_FIELD_INVALID         = "securitySchemeInFieldInvalid"; //$NON-NLS-1$
    public static final String SECURITY_SCHEMA_INVALID_URL              = "securitySchemeInvalidURL"; //$NON-NLS-1$

    public static final String SERVER_VARIABLE_NOT_DEFINED              = "serverVariableNotDefined"; //$NON-NLS-1$
    public static final String SERVER_INVALID_URL                       = "serverInvalidURL"; //$NON-NLS-1$

    public static final String VALIDATION_MESSAGE                       = "validationMessage"; //$NON-NLS-1$
    
    public static final String VARIABLE_OAUTH_FLOW_OBJECT               = "OAuth Flow Object"; //$NON-NLS-1$
    public static final String VARIABLE_SECURITY_SCHEME_OBJECT          = "Security Scheme Object"; //$NON-NLS-1$
    
    private ValidationMessageConstants() {
        // This class is not meant to be instantiated.
    }
}
