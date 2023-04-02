/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.microprofile.config.converters;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public class IdentityConverter extends BuiltInConverter {

    @Trivial
    public IdentityConverter() {
        super(String.class);
    }

    /** {@inheritDoc} */
    @Override
    public String convert(String value) {
        //identity function
        return value;
    }
}
