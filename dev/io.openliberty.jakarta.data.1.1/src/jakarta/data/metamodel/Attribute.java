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

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface Attribute<T> {

    default Class<?> attributeType() {
        throw new UnsupportedOperationException();
    }

    default Class<T> declaringType() {
        throw new UnsupportedOperationException();
    }

    String name();

}
