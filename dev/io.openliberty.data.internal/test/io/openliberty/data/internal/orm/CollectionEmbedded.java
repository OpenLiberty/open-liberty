/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.orm;

import java.util.List;

/**
 * A simple entity with id that ends in Id
 * and an entity collection field
 */
public class CollectionEmbedded {

    public int collectionId;

    public List<Name> friends;

    public static class Name {
        public String firstName;
        public String lastName;
    }
}
