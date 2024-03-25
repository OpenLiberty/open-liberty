/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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

package test.jakarta.data.inmemory.web;

import java.util.Set;

import test.jakarta.data.inmemory.provider.CompositeEntity;

/**
 * Entity class for tests
 */
@CompositeEntity
public class Composite {
    public long id;
    public Set<Long> factors;
    public int numUniqueFactors;

    public Composite() {
    }

    public Composite(long id, Set<Long> factors) {
        this.id = id;
        this.factors = factors;
        this.numUniqueFactors = factors.size();
    }
}
