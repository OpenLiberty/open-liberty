/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.agenthelper;

public class PreMainUtil {
    public static final String FACTORY_INIT_PROPERTY = "com.ibm.serialization.validators.factory.instance";
    public static final String DEBUG_PROPERTY = "com.ibm.websphere.kernel.instrument.serialfilter.debug";
    // Since logger is not activated while processing premain, the trace data needs to be logged by using System.out.
    public static boolean isDebugEnabled() {
        String value = System.getProperty(DEBUG_PROPERTY);
        if (value != null && "true".equalsIgnoreCase(value)) {
            return true;
        }
        return false;
    }
}
