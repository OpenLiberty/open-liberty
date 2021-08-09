/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.converters;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.converters.BuiltInConverter;

public class CharacterConverter extends BuiltInConverter {

    @Trivial
    public CharacterConverter() {
        super(Character.class);
    }

    /** {@inheritDoc} */
    @Override
    public Character convert(String v) {
        if (v == null) {
            return null;
        }
        if (v.length() != 1) {
            throw new IllegalArgumentException("Not a valid character: " + v);
        } else {
            return v.charAt(0);
        }
    }
}
