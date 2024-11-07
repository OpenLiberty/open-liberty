/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import java.util.Objects;

import test.jakarta.data.ddlgen.web.Part.Identifier;

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
     * TODO switch to a record once issue is resolved: https://github.com/OpenLiberty/open-liberty/issues/29460
     */
    public static class Identifier {
        public String partNum;
        public String vendor;

        public Identifier() {
        }

        public Identifier(String partNum, String vendor) {
            this.partNum = partNum;
            this.vendor = vendor;
        }

        @Override
        public boolean equals(Object other) {
            Identifier o;
            return other instanceof Identifier &&
                   Objects.equals((o = (Identifier) other).partNum, partNum) &&
                   Objects.equals(o.vendor, vendor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(partNum, vendor);
        }

        public String partNum() {
            return partNum;
        }

        @Override
        public String toString() {
            return vendor + ":" + partNum;
        }

        public String vendor() {
            return vendor;
        }
    }
}
