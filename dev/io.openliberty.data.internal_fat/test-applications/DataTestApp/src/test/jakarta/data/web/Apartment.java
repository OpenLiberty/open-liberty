/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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
package test.jakarta.data.web;

/**
 * Entity with embeddable and mapped superclass
 * Entity has a field with a colliding non-delimited attribute name with embeddable
 *
 * TODO extends here could indicate either a mapped superclass or inheritance
 * Today we treat this as a mapped superclass, but we could support inheritance but would need a way to distinguish between the two.
 */
public class Apartment extends Residence {

    // All upper case is a bad practice, but the spec allows it
    public long APTID;

    public int quartersWidth;

    public Bedroom quarters;

}
