/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package val31.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 *
 */
public record Person(@NotNull String name) {
    public String checkNameSize(@Size(max = 10) String x) {

        return x + "String value";
    }

    @Size(min = 6)
    public String getName() {
        return this.name;
    }
}
