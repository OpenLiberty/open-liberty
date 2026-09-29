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

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for the {@link MinStock} constraint.
 *
 * <p>Reads {@code min} from the annotation at initialisation time and rejects
 * any stock value strictly less than that threshold.
 */
public class MinStockValidator implements ConstraintValidator<MinStock, Integer> {

    private int min;

    @Override
    public void initialize(MinStock annotation) {
        this.min = annotation.min();
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null handling is @NotNull's responsibility
        }
        return value >= min;
    }
}
