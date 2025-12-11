/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.persistence.orm;

/**
 * A mapped superclass
 * which lacks an id field
 * but has an embeddable field
 */
public class SuperBeta extends SuperAlpha {

    public Script script;

    public static class Script {
        public char uppercase;
        public char lowercase;
    }
}
