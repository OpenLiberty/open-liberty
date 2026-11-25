/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.testutils;

import static org.junit.Assert.assertTrue;

import org.junit.rules.ExternalResource;

import com.ibm.ws.kernel.productinfo.ProductInfo;

/**
 * A rule which allows a test to set the beta edition system property.
 * <p>
 * The state of the property (including whether it was set at all) is captured before each test and restored after it,
 * so changes made by one test cannot leak into another.
 *
 * <pre>
 * &#064;Rule
 * public BetaPropertyRule betaProperty = new BetaPropertyRule();
 *
 * &#064;Test
 * public void test() {
 *     betaProperty.set(true);
 * }
 * </pre>
 */
public class BetaPropertyRule extends ExternalResource {

    private boolean testRunning = false;
    private String originalValue;

    @Override
    protected void before() {
        originalValue = System.getProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY);
        testRunning = true;
    }

    @Override
    protected void after() {
        testRunning = false;
        if (originalValue == null) {
            System.clearProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY);
        } else {
            System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, originalValue);
        }
    }

    /**
     * Sets the beta edition system property for the remainder of the current test.
     * <p>
     * Passing {@code true} sets the property to {@code "true"}. Passing {@code false} clears the property, which
     * {@link ProductInfo#getBetaEdition()} treats the same as {@code "false"}.
     *
     * @param beta whether the runtime should be treated as a beta edition
     * @throws AssertionError if no test is running
     */
    public void set(boolean beta) {
        assertTrue("BetaPropertyRule.set() can only be called while a test is running", testRunning);
        if (beta) {
            System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, "true");
        } else {
            System.clearProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY);
        }
    }
}
