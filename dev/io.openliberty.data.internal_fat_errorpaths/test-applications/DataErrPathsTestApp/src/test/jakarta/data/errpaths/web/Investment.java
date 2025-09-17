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
package test.jakarta.data.errpaths.web;

import jakarta.persistence.Column;

/**
 * An invalid record entity - because it has a Jakarta Persistence annotation
 * on a method.
 */
public record Investment(long id, float amount, String symbol) {

    /**
     * Invalid attempt to add a Jakarta Persistence annotation to a record component
     */
    @Column(nullable = false)
    public String symbol() {
        return symbol;
    }
}
