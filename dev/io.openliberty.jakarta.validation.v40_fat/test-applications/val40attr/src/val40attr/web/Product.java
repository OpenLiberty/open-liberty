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

/**
 * Simple product bean used as the validation target in the
 * {@code ConstraintDescriptor#getAttribute} demo.
 *
 * <p>The {@code stock} field carries a {@link MinStock} annotation with
 * an explicit {@code min = 5}. That attribute value is what the FAT tests
 * retrieve via the new 4.0 {@code getAttribute(String, Class)} API.
 */
public class Product {

    private final String name;

    @MinStock(min = 5)
    private final int stock;

    public Product(String name, int stock) {
        this.name = name;
        this.stock = stock;
    }

    public String getName() {
        return name;
    }

    public int getStock() {
        return stock;
    }
}
