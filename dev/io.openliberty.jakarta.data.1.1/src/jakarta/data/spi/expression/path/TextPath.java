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
package jakarta.data.spi.expression.path;

import jakarta.data.expression.NavigableExpression;
import jakarta.data.expression.TextExpression;
import jakarta.data.metamodel.TextAttribute;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface TextPath<T, U> extends //
                Path<T, U>, //
                TextExpression<T> {

    static <T, U> TextPath<T, U> of(NavigableExpression<T, U> expression,
                                    TextAttribute<U> attribute) {

        return new TextPathRecord<>(expression, attribute);
    }
}
