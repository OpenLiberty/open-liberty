/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec40;

import com.ibm.ws.security.javaeesec.JavaEESecConstants;

import jakarta.security.enterprise.identitystore.IdentityStore.ValidationType;

/**
 * Constants for Java EE Security - for 4.0, they all relate to the InMemoryIdentityStore.
 */
public class JakartaSec40Constants extends JavaEESecConstants {

    // key of the InMemoryIdentityStoreDefinition attribute modelling the Credentials.
    public static final String VALUE = "value";

    // InMemoryIdentityStoreDefinition spec defaults
    public static final int SPEC_DEFAULT_PRIORITY = 90;
    public static final String SPEC_DEFAULT_PRIORITYEXPRESSION = "";
    public static final ValidationType[] SPEC_DEFAULT_USEFOR = { ValidationType.VALIDATE, ValidationType.PROVIDE_GROUPS };
    public static final String SPEC_DEFAULT_USEFOREXPRESSION = "";
    public static final String[] SPEC_DEFAULT_GROUPS = {};
}
