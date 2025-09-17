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
 * The extends here for unannotated entities indicates a MappedSuperclass rather
 * than Inheritance. The former is more straightforward because it does not require
 * an inheritance strategy to be chosen or involve discriminators. If a user needs
 * these more advanced capabilities, they can use annotated entities and specify
 * the annotations for them.
 */
public class Apartment extends Residence {

    // All upper case is a bad practice, but the spec allows it
    public long APTID;

    public int quartersWidth;

    public Bedroom quarters;

}
