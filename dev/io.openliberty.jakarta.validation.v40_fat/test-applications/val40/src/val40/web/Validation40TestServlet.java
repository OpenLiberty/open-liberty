/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package val40.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Sample application servlet demonstrating the Jakarta Validation 4.0
 * {@code ConstraintViolationBuilder} deprecation feature.
 *
 * <p>The key change in Validation 4.0 is that {@code addNode(String)} is now
 * annotated {@code @Deprecated(since = "1.1", forRemoval = true)}. This means:
 *
 * <ul>
 *   <li>In your IDE, any call to {@code addNode()} will appear with a
 *       <s>strikethrough</s> — even in this file if you hover over
 *       {@link val40.web.OrderFormValidator}.</li>
 *   <li>Compiling with {@code javac -Xlint:removal} (or building via Gradle/Maven
 *       with warnings enabled) will print a {@code [removal]} warning for every
 *       call site.</li>
 *   <li>The method still executes correctly at runtime — deprecated does not mean
 *       removed — which is what the tests below confirm.</li>
 * </ul>
 *
 * <p>Open {@link OrderFormValidator} to see the deprecated call side-by-side with
 * the modern {@code addPropertyNode} replacement.
 */
@SuppressWarnings("serial")
@WebServlet("/Validation40TestServlet")
public class Validation40TestServlet extends FATServlet {

    @Inject
    Validator validator;

    /**
     * A valid order (non-blank username, positive quantity) produces no violations.
     */
    @Test
    public void testValidOrder() {
        OrderForm order = new OrderForm("alice", 3);

        Set<ConstraintViolation<OrderForm>> violations = validator.validate(order);

        assertTrue("Valid order should produce no constraint violations",
                   violations.isEmpty());
    }

    /**
     * A blank username triggers the validator path that calls the deprecated
     * {@code addNode("username")} method.
     *
     * <p>Even though the call site in {@link OrderFormValidator} uses the
     * deprecated API, validation still runs and the violation is reported
     * correctly — confirming that {@code addNode} works at runtime in 4.0.
     *
     * <p>Open {@link OrderFormValidator#isValid} to see the deprecation
     * warning / strikethrough in your IDE.
     */
    @Test
    public void testDeprecatedAddNodeStillProducesViolation() {
        OrderForm order = new OrderForm("", 3);   // blank username → hits addNode path

        Set<ConstraintViolation<OrderForm>> violations = validator.validate(order);

        assertEquals("Blank username should produce exactly one violation", 1, violations.size());

        ConstraintViolation<OrderForm> v = violations.iterator().next();
        assertEquals("Violation message should match", "username must not be blank", v.getMessage());
        assertEquals("Path built by deprecated addNode should still be 'username'",
                     "username", v.getPropertyPath().toString());
    }

    /**
     * A non-positive quantity triggers the validator path that calls the modern
     * {@code addPropertyNode("quantity")} method — the correct replacement for
     * the deprecated {@code addNode}.
     *
     * <p>No deprecation warning or strikethrough for this path.
     */
    @Test
    public void testModernAddPropertyNodeProducesViolation() {
        OrderForm order = new OrderForm("bob", 0);  // zero quantity → hits addPropertyNode path

        Set<ConstraintViolation<OrderForm>> violations = validator.validate(order);

        assertEquals("Zero quantity should produce exactly one violation", 1, violations.size());

        ConstraintViolation<OrderForm> v = violations.iterator().next();
        assertEquals("Violation message should match", "quantity must be greater than zero", v.getMessage());
        assertEquals("Path built by addPropertyNode should be 'quantity'",
                     "quantity", v.getPropertyPath().toString());
    }

    /**
     * Both fields invalid: one violation comes from the deprecated {@code addNode}
     * path, the other from the modern {@code addPropertyNode} path.
     */
    @Test
    public void testBothViolationsReported() {
        OrderForm order = new OrderForm("", 0);  // both blank username and zero quantity

        Set<ConstraintViolation<OrderForm>> violations = validator.validate(order);

        assertEquals("Both invalid fields should produce two violations", 2, violations.size());
    }
}
