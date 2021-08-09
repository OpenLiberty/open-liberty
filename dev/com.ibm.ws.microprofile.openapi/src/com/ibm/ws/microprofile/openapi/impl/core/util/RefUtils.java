/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.openapi.impl.core.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class RefUtils {

    public static String constructRef(String simpleRef) {
        return "#/components/schemas/" + simpleRef;
    }

    public static String constructRef(String simpleRef, String prefix) {
        return prefix + simpleRef;
    }

    public static Pair extractSimpleName(String ref) {
        int idx = ref.lastIndexOf("/");
        if (idx > 0) {
            String simple = ref.substring(idx + 1);
            if (!StringUtils.isBlank(simple)) {
                return new ImmutablePair<>(simple, ref.substring(0, idx + 1));
            }
        }
        return new ImmutablePair<>(ref, null);

    }
}
