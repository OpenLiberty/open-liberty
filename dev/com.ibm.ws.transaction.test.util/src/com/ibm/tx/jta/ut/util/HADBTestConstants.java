/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.tx.jta.ut.util;

import static java.util.Collections.unmodifiableMap;

import java.util.HashMap;
import java.util.Map;

public class HADBTestConstants {
    public enum HADBTestType {
        STARTUP,
        RUNTIME,
        DUPLICATE_RESTART,
        DUPLICATE_RUNTIME,
        HALT,
        CONNECT,
        LEASE,
        NONE;

        private static final Map<Integer, HADBTestType> _map;

        static {
            Map<Integer, HADBTestType> types = new HashMap<>();
            for (HADBTestType type: HADBTestType.values()) types.put(type.ordinal(), type);
            _map = unmodifiableMap(types);
        }

        public static HADBTestType from(int ordinal) {
            return _map.get(ordinal);
        }
    }
}
