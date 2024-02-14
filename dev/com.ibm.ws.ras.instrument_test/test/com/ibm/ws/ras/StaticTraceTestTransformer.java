/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

package com.ibm.ws.ras;

import java.io.ByteArrayInputStream;
import java.lang.instrument.IllegalClassFormatException;

import com.ibm.ws.ras.instrument.internal.main.LibertyTracePreprocessInstrumentation;

public class StaticTraceTestTransformer extends LibertyTracePreprocessInstrumentation {

    StaticTraceTestTransformer() {
        super();

    }
    
    
    /**
     * Instrument the classes.
     */
    public byte[] transform(String className, byte[] classfileBuffer) throws IllegalClassFormatException {
    	//setFfdc(true);
    	
        try {
            return transform(className, new ByteArrayInputStream(classfileBuffer));
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

}
