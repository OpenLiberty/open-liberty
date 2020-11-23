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

public class MessageConstants {

    /**
     * CWWKO1652W: The server cannot read the specified CSS document {0} due to {1} : {2}.
     */
    public static final String CSS_NOT_PROCESSED                    = "CSS_NOT_PROCESSED"; //$NON-NLS-1$
    
    /**
     * CWWKO1656W: The server read the specified CSS document {0}, but was unable to find <.swagger-ui .headerbar >.
     */
    public static final String CSS_SECTION_NOT_FOUND                = "CSS_SECTION_NOT_FOUND"; //$NON-NLS-1$
    
    /**
     * CWWKO1655W: The custom CSS file {0} that was specified for the OpenAPI UI was not processed. The server will restore the default values for the OpenAPI UI. Reason={1} : {2}.
     */
    public static final String CUSTOM_CSS_NOT_PROCESSED             = "CUSTOM_CSS_NOT_PROCESSED"; //$NON-NLS-1$
    
    /**
     * CWWKO1654W: The background image that was specified at {0} either does not exist or is invalid.
     */
    public static final String INVALID_CSS_BACKGROUND_IMAGE         = "INVALID_CSS_BACKGROUND_IMAGE"; //$NON-NLS-1$
    
    /**
     * CWWKO1660I: The application {0} was processed and an OpenAPI document was produced.
     */
    public static final String OPENAPI_APPLICATION_PROCESSED        = "OPENAPI_APPLICATION_PROCESSED"; //$NON-NLS-1$
    
    /**
     * CWWKO1661E: An error occured when processing application {0} and an OpenAPI document was not produced.
     */
    public static final String OPENAPI_APPLICATION_PROCESSING_ERROR = "OPENAPI_APPLICATION_PROCESSING_ERROR"; //$NON-NLS-1$
    
    /**
     * CWWKO1662W: An unexpected error occurred while writing the OpenAPI cache for application {0}. The error is {1}
     */
    public static final String OPENAPI_CACHE_WRITE_ERROR            = "OPENAPI_CACHE_WRITE_ERROR"; //$NON-NLS-1$
    
    /**
     * CWWKO1650E: Validation of the OpenAPI document produced the following error(s):
     */
    public static final String OPENAPI_DOCUMENT_VALIDATION_ERROR    = "OPENAPI_DOCUMENT_VALIDATION_ERROR"; //$NON-NLS-1$
    
    /**
     * CWWKO1651W: Validation of the OpenAPI document produced the following warning(s):
     */
    public static final String OPENAPI_DOCUMENT_VALIDATION_WARNING  = "OPENAPI_DOCUMENT_VALIDATION_WARNING"; //$NON-NLS-1$
    
    /**
     * CWWKO1659E: Failed to parse the OpenAPI document for application: {0}.
     */
    public static final String OPENAPI_FILE_PARSE_ERROR             = "OPENAPI_FILE_PARSE_ERROR"; //$NON-NLS-1$
    
    /**
     * CWWKO1657E: Failed to load the OpenAPI filter class: {0}.
     */
    public static final String OPENAPI_FILTER_LOAD_ERROR            = "OPENAPI_FILTER_LOAD_ERROR"; //$NON-NLS-1$
    
    /**
     * CWWKO1658E: Failed to load the OpenAPI model reader class: {0}.
     */
    public static final String OPENAPI_MODEL_READER_LOAD_ERROR      = "OPENAPI_MODEL_READER_LOAD_ERROR"; //$NON-NLS-1$
    
    /**
     * CWWKO1653W: The value that was specified for the {0} property is not supported. The value must be {1}.
     */
    public static final String UNSUPPORTED_CSS_VALUE                = "UNSUPPORTED_CSS_VALUE"; //$NON-NLS-1$
    
    private MessageConstants() {
        // This class is not meant to be instantiated.
    }
}
