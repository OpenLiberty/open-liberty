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

/**
 * Simple bean used as the validation target for {@link ValidOrder}.
 */
@ValidOrder
public class OrderForm {

    private final String username;
    private final int quantity;

    public OrderForm(String username, int quantity) {
        this.username = username;
        this.quantity = quantity;
    }

    public String getUsername() {
        return username;
    }

    public int getQuantity() {
        return quantity;
    }
}
