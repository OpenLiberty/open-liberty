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
package com.informix.jdbcx;

import java.util.HashMap;
import java.util.Map;

public class IfxConstants {
    public enum TestType {
        STARTUP,
        RUNTIME,
        DUPLICATE_RESTART,
        DUPLICATE_RUNTIME,
        HALT,
        CONNECT,
        LEASE;

        private static final Map<Integer, TestType> _map = new HashMap<Integer, TestType>();
        static {
            for (TestType testType : TestType.values()) {
                _map.put(testType.ordinal(), testType);
            }
        }

        public static TestType from(int ordinal) {
            return _map.get(ordinal);
        }
    };
}