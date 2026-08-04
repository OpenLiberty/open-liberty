/*******************************************************************************
 * Copyright (c) 2025,2026 IBM Corporation and others.
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

import jakarta.annotation.Nonnull;
import jakarta.data.messages.Messages;

/**
 * Method signatures are copied from Jakarta Data.
 */
public class Restrict {

    private Restrict() {
    }

    @Nonnull
    public static <T> Restriction<T> //
                    all(@Nonnull List<? extends Restriction<? super T>> restrictions) {

        return new CompositeRestrictionRecord<>( //
                        CompositeRestriction.Type.ALL, //
                        List.copyOf(restrictions));
    }

    @Nonnull
    @SafeVarargs
    public static <T> Restriction<T> all(@Nonnull Restriction<? super T>... restrictions) {

        return new CompositeRestrictionRecord<T>( //
                        CompositeRestriction.Type.ALL, //
                        List.of(restrictions));
    }

    @Nonnull
    public static <T> Restriction<T> //
                    any(@Nonnull List<? extends Restriction<? super T>> restrictions) {

        return new CompositeRestrictionRecord<>( //
                        CompositeRestriction.Type.ANY, //
                        List.copyOf(restrictions));
    }

    @Nonnull
    @SafeVarargs
    public static <T> Restriction<T> any(@Nonnull Restriction<? super T>... restrictions) {

        return new CompositeRestrictionRecord<T>( //
                        CompositeRestriction.Type.ANY, //
                        List.of(restrictions));
    }

    @Nonnull
    public static <T> Restriction<T> not(@Nonnull Restriction<T> restriction) {

        Messages.requireNonNull(restriction, "restriction");

        return restriction.negate();
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> Restriction<T> unrestricted() {

        return (Restriction<T>) Unrestricted.INSTANCE;
    }

}
