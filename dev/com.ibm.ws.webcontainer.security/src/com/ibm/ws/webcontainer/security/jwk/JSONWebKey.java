/*******************************************************************************
 * Copyright (c) 2016, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.jwk;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * TODO: This should replace com.ibm.ws.webcontainer.security.openidconnect.JSONWebKey
 */
public interface JSONWebKey {

    public abstract String getKeyID();

    public abstract String getKeyX5t();

    public abstract String getKeyX5tS256();

    public abstract String getAlgorithm();

    public abstract String getKeyUse();

    public abstract String getKeyType();

    public abstract PublicKey getPublicKey();

    public abstract PrivateKey getPrivateKey();

    public abstract byte[] getSharedKey();

    public abstract long getCreated();

}