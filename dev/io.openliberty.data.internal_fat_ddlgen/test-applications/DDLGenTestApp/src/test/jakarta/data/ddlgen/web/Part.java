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

/**
 * A record entity with a composite id.
 */
public record Part(Identifier id, String name, float price) {
    /**
     * Composite id for the Part entity.
     * TODO switch to a record once #29117 is fixed
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
