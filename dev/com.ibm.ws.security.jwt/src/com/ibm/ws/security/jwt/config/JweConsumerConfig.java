/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.config;

import java.security.GeneralSecurityException;
import java.security.Key;

import com.ibm.websphere.ras.annotation.Sensitive;

public interface JweConsumerConfig {

    String getId();

    public String getKeyManagementKeyAlias();

    @Sensitive
    public Key getJweDecryptionKey() throws GeneralSecurityException;

}
