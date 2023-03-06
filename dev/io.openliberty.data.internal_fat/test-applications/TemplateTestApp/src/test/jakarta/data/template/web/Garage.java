/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package test.jakarta.data.template.web;

/**
 * An embeddable without annotations at depth 1, with a field that is another embeddable type.
 */
public class Garage {
    public static enum Type {
        Attached, Detached, TuckUnder
    };

    public int area;

    public GarageDoor door;

    public Type type;
}
