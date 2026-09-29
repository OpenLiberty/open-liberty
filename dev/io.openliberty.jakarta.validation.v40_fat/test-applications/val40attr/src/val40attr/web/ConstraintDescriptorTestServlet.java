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
package val40attr.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.metadata.BeanDescriptor;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.metadata.PropertyDescriptor;

/**
 * FAT servlet demonstrating {@code ConstraintDescriptor#getAttribute(String, Class)}
 * introduced in Jakarta Validation 4.0.
 *
 * <h2>Background</h2>
 * <p>Every constraint annotation has <em>attributes</em> — {@code message},
 * {@code groups}, {@code payload}, and any custom ones (e.g. {@code min} on
 * {@link MinStock}). Before Validation 4.0 the only programmatic way to read a
 * specific attribute at runtime was via the raw map returned by
 * {@code ConstraintDescriptor#getAttributes()}, which required an unchecked cast:
 *
 * <pre>
 *   // Pre-4.0 — verbose and not type-safe
 *   int min = (int) descriptor.getAttributes().get("min");
 * </pre>
 *
 * <p>Validation 4.0 adds a typed convenience method directly on
 * {@code ConstraintDescriptor}:
 *
 * <pre>
 *   // Validation 4.0 — concise and type-safe, no cast required
 *   int min = descriptor.getAttribute("min", int.class);
 * </pre>
 *
 * <h2>Test setup</h2>
 * <p>{@link Product} declares {@code @MinStock(min = 5)} on its {@code stock}
 * field. The tests below navigate the validator metadata graph to reach that
 * {@code ConstraintDescriptor} and then use both the old and new APIs to read
 * back the attribute values.
 */
@SuppressWarnings("serial")
@WebServlet("/ConstraintDescriptorTestServlet")
public class ConstraintDescriptorTestServlet extends FATServlet {

    @Inject
    Validator validator;

    // -----------------------------------------------------------------------
    // Helper — resolves the single ConstraintDescriptor for Product.stock
    // -----------------------------------------------------------------------
    private ConstraintDescriptor<?> stockConstraintDescriptor() {
        BeanDescriptor beanDesc = validator.getConstraintsForClass(Product.class);
        PropertyDescriptor propDesc = beanDesc.getConstraintsForProperty("stock");
        return propDesc.getConstraintDescriptors().iterator().next();
    }

    // -----------------------------------------------------------------------
    // Baseline — confirm validation itself works before testing metadata API
    // -----------------------------------------------------------------------

    /**
     * A product with stock >= 5 should produce no violations.
     */
    @Test
    public void testValidProductProducesNoViolations() {
        Set<ConstraintViolation<Product>> violations =
            validator.validate(new Product("Widget", 10));

        assertTrue("Stock >= min should produce no violations", violations.isEmpty());
    }

    /**
     * A product with stock below the declared minimum should produce exactly one violation.
     */
    @Test
    public void testLowStockProducesViolation() {
        Set<ConstraintViolation<Product>> violations =
            validator.validate(new Product("Widget", 2));

        assertEquals("Stock below min should produce exactly one violation",
                     1, violations.size());
    }

    // -----------------------------------------------------------------------
    // ConstraintDescriptor#getAttribute — new in Validation 4.0
    // -----------------------------------------------------------------------

    /**
     * Reads the custom {@code min} attribute via the new type-safe
     * {@code getAttribute(String, Class)} API and asserts it equals the
     * declared value of {@code 5}.
     *
     * <p>This is the primary demonstration of the Validation 4.0 feature:
     * no cast, no unchecked warning.
     */
    @Test
    public void testGetAttributeReturnsTypedMinValue() {
        ConstraintDescriptor<?> descriptor = stockConstraintDescriptor();

        // Validation 4.0 — type-safe, no unchecked cast
        int min = descriptor.getAttribute("min", int.class);

        assertEquals("getAttribute(\"min\", int.class) must return the declared min",
                     5, min);
    }

    /**
     * Reads the standard {@code message} attribute — present on every constraint —
     * using the new {@code getAttribute} API.
     *
     * <p>Demonstrates that {@code getAttribute} works for {@code String} types
     * just as well as for primitives.
     */
    @Test
    public void testGetAttributeReturnsMessageTemplate() {
        ConstraintDescriptor<?> descriptor = stockConstraintDescriptor();

        String message = descriptor.getAttribute("message", String.class);

        assertTrue("message attribute should contain '{min}' interpolation token",
                   message.contains("{min}"));
    }

    /**
     * Compares the pre-4.0 approach (raw map + unchecked cast) with the new
     * 4.0 typed API to confirm they return the same value.
     *
     * <p>The old approach still compiles and runs correctly — this test
     * simply documents the before/after side-by-side.
     */
    @Test
    public void testGetAttributeMatchesGetAttributesMap() {
        ConstraintDescriptor<?> descriptor = stockConstraintDescriptor();

        // Pre-4.0 style — raw map lookup with unchecked cast
        int minViaMap = (int) descriptor.getAttributes().get("min");

        // Validation 4.0 style — typed, no cast
        int minViaTyped = descriptor.getAttribute("min", int.class);

        assertEquals("getAttribute and getAttributes().get() must return the same value",
                     minViaMap, minViaTyped);
    }

    /**
     * Verifies that passing a wrong type to {@code getAttribute} throws an
     * exception rather than silently returning a wrong value.
     *
     * <p>The spec does not prescribe a specific exception type, but any
     * runtime signal (ClassCastException, IllegalArgumentException, etc.) is
     * acceptable — the important thing is that it fails fast instead of
     * producing a corrupted result.
     */
    @Test
    public void testGetAttributeWrongTypeThrows() {
        ConstraintDescriptor<?> descriptor = stockConstraintDescriptor();

        try {
            // "min" is an int — asking for String should fail
            descriptor.getAttribute("min", String.class);
            fail("Expected an exception when requesting 'min' as String");
        } catch (Exception e) {
            // pass — any exception is acceptable per the spec
        }
    }
}
