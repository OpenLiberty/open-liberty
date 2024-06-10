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
package com.ibm.ws.kernel.feature.internal.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Test case for feature resolution.
 */
public class TestCase {
    public TestCase(List<String> inputs, List<String> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public TestCase(String[] featureInputs, String[] featureOutputs) {
        this( asList(featureInputs), asList(featureOutputs) );
    }

    private static List<String> asList(String[] stringArray) {
        List<String> stringList = new ArrayList<String>(stringArray.length);
        for ( String value : stringArray ) {
            stringList.add(value);
        }
        return stringList;
    }

    //

    private final List<String> inputs;

    public List<String> getInputs() {
        return inputs;
    }

    //

    private final List<String> outputs;

    public List<String> getOutputs(){
        return outputs;
    }
}
