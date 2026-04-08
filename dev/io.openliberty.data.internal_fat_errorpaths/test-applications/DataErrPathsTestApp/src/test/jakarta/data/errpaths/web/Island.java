/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jakarta.data.errpaths.web;

import jakarta.validation.constraints.NotBlank;

/**
 * An invalid record entity - because it has a Jakarta Validation annotation
 * on a method.
 */
public record Island(long id, String name) {

    /**
     * Invalid attempt to add a Jakarta Validation annotation to a record component
     */
    @NotBlank
    public String name() {
        return name;
    }

}
