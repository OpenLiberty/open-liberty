/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.test.dependentscopedproducer.servlets;

import static org.junit.Assert.fail;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.test.dependentscopedproducer.DependentSterotype;
import com.ibm.ws.cdi.test.dependentscopedproducer.NullBean;
import com.ibm.ws.cdi.test.dependentscopedproducer.NullBeanThree;
import com.ibm.ws.cdi.test.dependentscopedproducer.NullBeanTwo;
import com.ibm.ws.cdi.test.dependentscopedproducer.producers.NullBeanProducer;

import componenttest.app.FATServlet;

@WebServlet("/NullProducer")
public class NullProducerServlet extends FATServlet {

    @Inject
    NullBean nullBean;
    @Inject
    @DependentSterotype
    NullBeanTwo nullBeanTwo;
    @Inject
    NullBeanThree nullBeanThree;

    @Test
    public void testDependentScopedProducerHandlesNullCorrectly() throws IOException {
        StringBuilder builder = new StringBuilder();

        boolean passed = true;

        if (nullBean == null) {
            builder.append("nullBean was null ");
            if (NullBeanProducer.isNullOne()) {
                builder.append("and it should be null");
            } else {
                builder.append("but it shouldn't be null");
                passed = false;
            }
        } else {
            builder.append("nullBean was not null null");
            if (!NullBeanProducer.isNullOne()) {
                builder.append("and it shouldn't be null");
            } else {
                builder.append("but it should be null");
                passed = false;
            }
        }

        builder.append(System.lineSeparator());

        if (nullBeanTwo == null) {
            builder.append("nullBeanTwo was null null");
            if (NullBeanProducer.isNullTwo()) {
                builder.append("and it should be null");
            } else {
                builder.append("but it shouldn't be null");
                passed = false;
            }
        } else {
            builder.append("nullBeanTwo was not null ");
            if (!NullBeanProducer.isNullTwo()) {
                builder.append("and it shouldn't be null");
            } else {
                builder.append("but it should be null");
                passed = false;
            }
        }

        builder.append(System.lineSeparator());

        if (nullBeanThree == null) {
            builder.append("nullBeanThree was null ");
            if (NullBeanProducer.isNullThree()) {
                builder.append("and it should be null");
            } else {
                builder.append("but it shouldn't be null");
                passed = false;
            }
        } else {
            builder.append("nullBeanThree was null ");
            if (!NullBeanProducer.isNullThree()) {
                builder.append("and it shouldn't be null");
            } else {
                builder.append("but it should be null");
                passed = false;
            }
        }

        builder.append(System.lineSeparator());

        if (passed) {
            builder.append("Test Passed! ");
        } else {
            builder.append("Test Failed! ");
            fail(builder.toString());
        }
    }

}
