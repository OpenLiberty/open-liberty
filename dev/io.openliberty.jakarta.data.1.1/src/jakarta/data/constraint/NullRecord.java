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
package jakarta.data.constraint;

/**
 * Method signatures are copied from Jakarta Data.
 */
record NullRecord<V>() implements Null<V> {

    static final NullRecord<?> INSTANCE = new NullRecord<>();

    @Override
    public NotNull<V> negate() {
        return NotNull.instance();
    }

    @Override
    public String toString() {
        return "IS NULL";
    }
}
