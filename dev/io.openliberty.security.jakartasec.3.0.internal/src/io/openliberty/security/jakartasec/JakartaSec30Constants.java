/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec;

import com.ibm.ws.security.javaeesec.JavaEESecConstants;

/**
 * Constants for Java EE Security
 */
public class JakartaSec30Constants extends JavaEESecConstants {

    public static final String OIDC_ANNOTATION = "oidc_annotation";

    public static final String BASE_URL_VARIABLE = "baseURL";

    public static final String BASE_URL_DEFAULT = "${" + BASE_URL_VARIABLE + "}/Callback";

}