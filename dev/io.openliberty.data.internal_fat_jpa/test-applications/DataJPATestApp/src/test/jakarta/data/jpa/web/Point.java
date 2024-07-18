/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import jakarta.persistence.Embeddable;

/**
 * Java record that is used as an Embeddable by the DirectedLineSegment entity.
 */
@Embeddable
public record Point(int x, int y) {
    // TODO remove once #29117 is fixed
    public Point() {
        this(0, 0);
    }
}
