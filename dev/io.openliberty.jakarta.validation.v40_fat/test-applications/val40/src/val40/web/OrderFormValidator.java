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

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Custom class-level constraint validator for {@link ValidOrder}.
 *
 * <p>This validator deliberately calls the deprecated {@code addNode(String)} method
 * from the Jakarta Validation 4.0 API. In Validation 4.0, {@code addNode} is annotated
 * with {@code @Deprecated(since = "1.1", forRemoval = true)}, so any IDE that resolves
 * the 4.0 API jar will render the call with a <s>strikethrough</s> and emit a
 * {@code -Xlint:removal} compiler warning at build time.
 *
 * <p>The modern replacements are:
 * <ul>
 *   <li>{@link ConstraintValidatorContext.ConstraintViolationBuilder#addPropertyNode(String)}
 *       – for a named property on a bean (preferred)</li>
 *   <li>{@link ConstraintValidatorContext.ConstraintViolationBuilder#addBeanNode()}
 *       – for a class-level (bean) node</li>
 *   <li>{@link ConstraintValidatorContext.ConstraintViolationBuilder#addParameterNode(int)}
 *       – for a cross-parameter method argument</li>
 * </ul>
 */
public class OrderFormValidator implements ConstraintValidator<ValidOrder, OrderForm> {

    @Override
    public boolean isValid(OrderForm order, ConstraintValidatorContext context) {
        if (order == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        // -----------------------------------------------------------------------
        // DEPRECATED USAGE – addNode(String)
        //
        // In Jakarta Validation 4.0 this method is annotated:
        //   @Deprecated(since = "1.1", forRemoval = true)
        //
        // Your IDE will show this call with a strikethrough and the compiler will
        // emit a -Xlint:removal warning, e.g.:
        //
        //   warning: [removal] addNode(String) in ConstraintViolationBuilder has been
        //   deprecated and marked for removal
        //
        // The call still works at runtime (deprecated ≠ removed), but you should
        // migrate to addPropertyNode("username") shown below.
        // -----------------------------------------------------------------------
        if (order.getUsername() == null || order.getUsername().isBlank()) {
            context.buildConstraintViolationWithTemplate("username must not be blank")
                   .addNode("username")          // <-- deprecated in Validation 4.0
                   .addConstraintViolation();
            valid = false;
        }

        // -----------------------------------------------------------------------
        // MODERN REPLACEMENT – addPropertyNode(String)   [since Validation 1.1]
        //
        // Use this instead of addNode. No deprecation warning, no strikethrough.
        // -----------------------------------------------------------------------
        if (order.getQuantity() <= 0) {
            context.buildConstraintViolationWithTemplate("quantity must be greater than zero")
                   .addPropertyNode("quantity")  // <-- correct modern API
                   .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
