/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.internal;

import com.ibm.ws.security.common.structures.CacheValue;

public class JwtCacheValue extends CacheValue {

    private final long clockSkew;

    public JwtCacheValue(Object value, long clockSkew) {
        super(value);
        this.clockSkew = clockSkew;
    }

    public long getClockSkew() {
        return clockSkew;
    }

}
