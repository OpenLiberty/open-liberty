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
package jakarta.data.metamodel;

import jakarta.data.expression.BooleanExpression;
import jakarta.data.messages.Messages;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface BooleanAttribute<T> //
                extends ComparableAttribute<T, Boolean>, BooleanExpression<T> {

    static <T> BooleanAttribute<T> of(Class<T> entityClass,
                                      String name,
                                      Class<Boolean> attributeType) {

        Messages.requireNonNull(entityClass, "entityClass");
        Messages.requireNonNull(name, "name");
        Messages.requireNonNull(attributeType, "attributeType");

        return new BooleanAttributeRecord<>(entityClass, name, attributeType);
    }

    @Override
    Class<Boolean> type();
}
