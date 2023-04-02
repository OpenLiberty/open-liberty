/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.security.jakartasec.tokens;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Clone claims to prevent modifications to original map.
 */
public class CloneClaimsAction implements BiConsumer<String, Object> {

    private final Map<String, Object> result;

    public CloneClaimsAction(Map<String, Object> result) {
        this.result = result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void accept(String key, Object value) {
        if (value instanceof List) {
            // Copy contents to prevent modifications.
            List<String> copy = new ArrayList<String>();
            copy.addAll((List<String>) value);
            result.put(key, copy);
        } else {
            result.put(key, value);
        }
    }

}
