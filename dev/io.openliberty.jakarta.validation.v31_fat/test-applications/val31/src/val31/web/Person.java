/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package val31.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 *
 */
public record Person(@NotNull String name) {

    @Valid
    public boolean checkLength() {
        String val = this.name;
        boolean check = false;
        int x = val.length();
        if (x > 2 && x <= 12) {
            check = true;
        }
        return check;
    }
}
