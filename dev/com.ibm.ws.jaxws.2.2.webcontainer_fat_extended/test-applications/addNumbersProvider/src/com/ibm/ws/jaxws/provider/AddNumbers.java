/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws.provider;

@javax.jws.WebService(serviceName = "AddNumbers")
public class AddNumbers {

    public String addNumbers(int arg0, int arg1) throws AddNumbersException, EqualNumbersException, LargeNumbersException {
        if (arg0 + arg1 < 0) {
            throw new AddNumbersException("Sum is less than 0.");
        } else if (arg0 == arg1) {
            throw new EqualNumbersException("Expected all unequal numbers.", "Numbers can't be equal");
        } else if (arg0 >= 10000 || arg1 >= 1000) {
            throw new LargeNumbersException("Expected all numbers less than 10000");
        }
        return "Result = " + String.valueOf(arg0 + arg1);
    }

    public String addNegatives(int arg0, int arg1) throws AddNegativesException {
        // expect 2 negative numbers
        if (arg0 > 0 || arg1 > 0) {
            throw new AddNegativesException("Expected all negative numbers.");
        }
        return "Result = " + String.valueOf(arg0 + arg1);
    }

}
