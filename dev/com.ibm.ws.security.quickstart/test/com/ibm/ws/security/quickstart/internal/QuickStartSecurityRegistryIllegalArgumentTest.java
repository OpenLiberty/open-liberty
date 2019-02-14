/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.quickstart.internal;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.ws.security.registry.UserRegistryIllegalArgumentTemplate;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * @see UserRegistryIllegalArgumentTemplate
 */
public class QuickStartSecurityRegistryIllegalArgumentTest extends UserRegistryIllegalArgumentTemplate {

    public QuickStartSecurityRegistryIllegalArgumentTest() {
         super(new QuickStartSecurityRegistry("user", Password.create(new SerializableProtectedString("pwd".toCharArray()))));
    }
}
