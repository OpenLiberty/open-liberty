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
    public byte[] transform(byte[] classfileBuffer) throws IllegalClassFormatException {
    	//setFfdc(true);
    	
        try {
            return transform(new ByteArrayInputStream(classfileBuffer));
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

}
