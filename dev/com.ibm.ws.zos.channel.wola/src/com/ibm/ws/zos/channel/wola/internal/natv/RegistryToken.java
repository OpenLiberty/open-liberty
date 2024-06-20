/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.zos.channel.wola.internal.natv;

/**
 * A wrapper around a native registry token (see util_registry.mc).
 */
public class RegistryToken extends ByteBufferBacked<RegistryToken> {

    private static final int RegistryTokenSize = 64;

    public RegistryToken() {
        super(RegistryTokenSize);
    }

}
