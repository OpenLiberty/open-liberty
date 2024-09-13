/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
 * Entity has a field with a colliding delimited attribute name with embeddable
 */
public class Apartment2 extends Residence {

    public long aptId;

    public int quarters_width;

    public Bedroom quarters;

}
