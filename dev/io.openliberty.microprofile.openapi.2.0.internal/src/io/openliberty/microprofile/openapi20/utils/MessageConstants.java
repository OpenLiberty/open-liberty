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

    public static final String CSS_NOT_PROCESSED                    = "CSS_NOT_PROCESSED"; //$NON-NLS-1$
    public static final String CSS_SECTION_NOT_FOUND                = "CSS_SECTION_NOT_FOUND"; //$NON-NLS-1$
    public static final String CUSTOM_CSS_NOT_PROCESSED             = "CUSTOM_CSS_NOT_PROCESSED"; //$NON-NLS-1$
    public static final String INVALID_CSS_BACKGROUND_IMAGE         = "INVALID_CSS_BACKGROUND_IMAGE"; //$NON-NLS-1$
    public static final String OPENAPI_APPLICATION_PROCESSED        = "OPENAPI_APPLICATION_PROCESSED"; //$NON-NLS-1$
    public static final String OPENAPI_APPLICATION_PROCESSING_ERROR = "OPENAPI_APPLICATION_PROCESSING_ERROR"; //$NON-NLS-1$
    public static final String OPENAPI_DOCUMENT_VALIDATION_ERROR    = "OPENAPI_DOCUMENT_VALIDATION_ERROR"; //$NON-NLS-1$
    public static final String OPENAPI_DOCUMENT_VALIDATION_WARNING  = "OPENAPI_DOCUMENT_VALIDATION_WARNING"; //$NON-NLS-1$
    public static final String OPENAPI_FILE_PARSE_ERROR             = "OPENAPI_FILE_PARSE_ERROR"; //$NON-NLS-1$
    public static final String OPENAPI_FILTER_LOAD_ERROR            = "OPENAPI_FILTER_LOAD_ERROR"; //$NON-NLS-1$
    public static final String OPENAPI_MODEL_READER_LOAD_ERROR      = "OPENAPI_MODEL_READER_LOAD_ERROR"; //$NON-NLS-1$
    public static final String UNSUPPORTED_CSS_VALUE                = "UNSUPPORTED_CSS_VALUE"; //$NON-NLS-1$
    
    private MessageConstants() {
        // This class is not meant to be instantiated.
    }
}
