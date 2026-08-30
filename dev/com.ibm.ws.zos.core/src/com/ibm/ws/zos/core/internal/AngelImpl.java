/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.core.internal;

import com.ibm.ws.zos.core.Angel;

/**
 * Implementation of {@code Angel} that is registered.
 */
public class AngelImpl implements Angel {

    final private int drmVersion;
    final private String angelName;

    AngelImpl(int drmVersion, String angelName) {
        this.drmVersion = drmVersion;
        this.angelName = angelName;
    }

    /**
     * @see com.ibm.ws.zos.core.Angel#getDRM_Version()
     */
    @Override
    public int getDRM_Version() {
        return drmVersion;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(";drmVersion=").append(drmVersion);
        sb.append(";angelName=").append(angelName);
        return sb.toString();
    }

    @Override
    public String getName() {
        return angelName;
    }
}
