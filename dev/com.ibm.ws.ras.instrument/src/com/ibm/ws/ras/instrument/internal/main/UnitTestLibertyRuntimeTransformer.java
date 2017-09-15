package com.ibm.ws.ras.instrument.internal.main;

/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.lang.instrument.Instrumentation;

/**
 * A java agent implementation that will cause aggressive instrumentation of
 * code with tracing in a unit test environment. This is required to provide
 * trace outside of the OSGi framework used by the component tests and at
 * runtime.
 */
public class UnitTestLibertyRuntimeTransformer extends LibertyRuntimeTransformer {

    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        LibertyRuntimeTransformer.setInstrumentation(inst);
        LibertyRuntimeTransformer.setInjectAtTransform(true);

        // Skip debug info for class files destroyed by emma when we're requested
        if (agentArgs != null && !agentArgs.trim().isEmpty()) {
            String[] keyValue = agentArgs.split("=");
            if (keyValue.length == 2) {
                if (keyValue[0].equals("skipDebugData") && Boolean.parseBoolean(keyValue[1])) {
                    LibertyRuntimeTransformer.setSkipDebugData(true);
                }
            }
        }
    }

}
