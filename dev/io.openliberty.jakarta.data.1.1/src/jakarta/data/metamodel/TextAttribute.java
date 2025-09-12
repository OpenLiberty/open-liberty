/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
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

import jakarta.data.Sort;
import jakarta.data.expression.TextExpression;
import jakarta.data.messages.Messages;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface TextAttribute<T> extends ComparableAttribute<T, String>, TextExpression<T> {

    default Sort<T> ascIgnoreCase() {
        return Sort.ascIgnoreCase(name());
    }

    @Override
    default Class<String> attributeType() {
        return String.class;
    }

    default Sort<T> descIgnoreCase() {
        return Sort.descIgnoreCase(name());
    }

    static <T> TextAttribute<T> of(Class<T> entityClass,
                                   String name) {
        Messages.requireNonNull(entityClass, "entityClass");
        Messages.requireNonNull(name, "name");

        return new TextAttributeRecord<>(entityClass, name);
    }

}
