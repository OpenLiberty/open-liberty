/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.persistence.models;

import java.util.Iterator;

/**
 * A record that has an inner generic class
 */
public class MyMap<X, Y> {

    public class MyIterator<Z extends X> implements Iterator<Z> {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Z next() {
            return null;
        }

    }

}
