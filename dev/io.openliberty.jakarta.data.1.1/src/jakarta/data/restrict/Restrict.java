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
import java.util.Objects;

/**
 * Method signatures are copied from Jakarta Data.
 */
public class Restrict {

    private Restrict() {
    }

    public static <T> Restriction<T> //
                    all(List<? extends Restriction<? super T>> restrictions) {

        return new CompositeRestrictionRecord<T>( //
                        CompositeRestriction.Type.ALL, //
                        List.copyOf(restrictions));
    }

    @SafeVarargs
    public static <T> Restriction<T> all(Restriction<? super T>... restrictions) {

        return new CompositeRestrictionRecord<T>( //
                        CompositeRestriction.Type.ALL, //
                        List.of(restrictions));
    }

    public static <T> Restriction<T> //
                    any(List<? extends Restriction<? super T>> restrictions) {

        return new CompositeRestrictionRecord<T>( //
                        CompositeRestriction.Type.ANY, //
                        List.copyOf(restrictions));
    }

    @SafeVarargs
    public static <T> Restriction<T> any(Restriction<? super T>... restrictions) {

        return new CompositeRestrictionRecord<T>( //
                        CompositeRestriction.Type.ANY, //
                        List.of(restrictions));
    }

    public static <T> Restriction<T> not(Restriction<T> restriction) {

        Objects.requireNonNull(restriction, "restriction");

        return restriction.negate();
    }

    @SuppressWarnings("unchecked")
    public static <T> Restriction<T> unrestricted() {

        return (Restriction<T>) Unrestricted.INSTANCE;
    }

}
