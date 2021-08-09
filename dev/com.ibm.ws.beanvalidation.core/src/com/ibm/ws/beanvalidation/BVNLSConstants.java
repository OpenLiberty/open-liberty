/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.beanvalidation;

/**
 * Defines the constants necessary to NLS conversions. There is a direct relationship between this file and the nlsprops file that this file {@link #NLS_RESOURCE_FILE refers to}
 * .<br>
 * Error message key constants for the Bean Validation localizable message keys. There should be a one to one correspondence between the constant that represents
 * the message key and each message key/message found in the Bean Validation message bundle (i.e., the BVNLSMessages.properties file). Whenever a new message is
 * added to the Bean Validation message bundle, a new message key constant should be add to this file. In other words, there is a tight coupling between these
 * constants and the Bean Validation message bundle. The bean validation code should use these message key constants rather than a String that represents the message key.
 * 
 * @author westland@us.ibm.com
 * @version %I%
 */
public interface BVNLSConstants {

    /**
     * Name of the default resource bundle used by code shipped with BeanValidation
     */
    public static final String BV_RESOURCE_BUNDLE = "com.ibm.ws.beanvalidation.resources.nls.BVNLSMessages";
    public static final String BVKEY_UNABLE_TO_REGISTER_WITH_INJECTION_ENGINE = "BVKEY_UNABLE_TO_REGISTER_WITH_INJECTION_ENGINE";
    public static final String BVKEY_UNABLE_TO_CREATE_VALIDATION_FACTORY = "BVKEY_UNABLE_TO_CREATE_VALIDATION_FACTORY";
    public static final String BVKEY_CLASS_NOT_FOUND = "BVKEY_CLASS_NOT_FOUND";
    public static final String BVKEY_SYNTAX_ERROR_IN_VALIDATION_XML = "BVKEY_SYNTAX_ERROR_IN_VALIDATION_XML";
    public static final String BVKEY_NOT_A_BEAN_VALIDATION_XML = "BVKEY_NOT_A_BEAN_VALIDATION_XML";

}
