/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.iterable;

import java.util.ArrayList;
import java.util.Arrays;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * A CDI RequestScoped bean. Allows us to test the TestIterable.
 */
@Named
@RequestScoped
public class IterableTestBean {
    private final Integer[] numberArray = new Integer[] { 5, 6 };

    // Create a TestIterable passing in the values of the numberArray we want to iterate over.
    private TestIterable<Integer> testIterable = new TestIterable<Integer>(new ArrayList<Integer>(Arrays.asList(numberArray)));

    public void setTestIterable(TestIterable<Integer> testIterable) {
        this.testIterable = testIterable;
    }

    public TestIterable<Integer> getTestIterable() {
        return this.testIterable;
    }

}
