/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

// Validating isn't necessary for booleans, but for future config values it will be
// TODO: add validation of serviceName and portName strings - want to prevent wildcards and duplicates
public class ConfigValidation {

	private static final TraceComponent tc = Tr.register(ConfigValidation.class);


        /**
         * @param value - key name
         * @return true since no need to validate booleans
         */
        public static boolean validateEnableSchemaValidation(boolean value) {
            return true;
        }

        /**
         * @param value - key name
         * @return true since no need to validate booleans
         */
        public static boolean validateIgnoreUnexpectedElements(boolean value) {
            return true;
        }
}
