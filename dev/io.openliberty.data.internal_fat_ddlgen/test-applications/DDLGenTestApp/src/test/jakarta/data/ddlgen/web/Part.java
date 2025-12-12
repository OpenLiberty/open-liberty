/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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
package test.jakarta.data.ddlgen.web;

/**
 * A record entity with a composite id.
 */
public record Part(Identifier id, String name, float price, int version) {

    public static final Part of(String partNum,
                                String vendor,
                                String name,
                                float price) {
        return new Part(new Identifier(partNum, vendor), name, price, 0);
    }

    /**
     * Composite id for the Part entity.
     */
    public static record Identifier(String partNum, String vendor) {

        @Override
        public String toString() {
            return vendor + ":" + partNum;
        }

    }
}
