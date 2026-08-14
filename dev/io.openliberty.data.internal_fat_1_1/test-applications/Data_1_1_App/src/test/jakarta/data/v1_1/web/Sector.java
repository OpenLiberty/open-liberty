/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jakarta.data.v1_1.web;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Entity to use in repository that is annotated with jakarta.annotation.security
 * annotations.
 */
@Entity
public class Sector {

    @Column(nullable = false)
    float centralAngle; // in degrees, from initial side to terminal side

    @Column(nullable = false)
    float initialSideAngle; // from 0 degrees to initial side

    @Column(nullable = false)
    @Id
    String label;

    @Column(nullable = false)
    float radius; // length of initial side and terminal side of the sector

    @Column(nullable = false)
    int x; // of point at center of circle to which the sector belongs

    @Column(nullable = false)
    int y; // of point at center of circle to which the sector belongs

    public static Sector of(String label,
                            int x,
                            int y,
                            float radius,
                            float centralAngle,
                            float initialSideAngle) {
        Sector s = new Sector();
        s.centralAngle = centralAngle;
        s.initialSideAngle = initialSideAngle;
        s.label = label;
        s.radius = radius;
        s.x = x;
        s.y = y;
        return s;
    }

    /**
     * Textual description. For example:
     *
     * Sector#1 (3,8) radius 10 @120˚ with central angle 60˚
     */
    @Override
    public String toString() {
        return new StringBuilder("Sector#")
                        .append(label)
                        .append('(')
                        .append(x)
                        .append(',')
                        .append(y)
                        .append(") radius ")
                        .append(radius)
                        .append(" @")
                        .append(initialSideAngle)
                        .append("˚ with central angle ")
                        .append(centralAngle)
                        .append('˚')
                        .toString();
    }
}
