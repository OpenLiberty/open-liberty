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
package jakarta.data.restrict;

import java.util.List;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface CompositeRestriction<T> extends Restriction<T> {

    enum Type {
        ALL,
        ANY;

        String asQueryLanguage() {
            return this == ALL ? "AND" : "OR";
        }
    }

    boolean isNegated();

    List<Restriction<? super T>> restrictions();

    Type type();
}
