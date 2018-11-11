/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.config;

import java.util.Properties;

public interface SimpleConfig {
    void reset();

    ValidationMode getDefaultMode();

    ValidationMode getValidationMode(String specifier);

    boolean setValidationMode(ValidationMode mode, String specifier);

    boolean setPermission(PermissionMode perm, String s);

    void load(Properties props);
}