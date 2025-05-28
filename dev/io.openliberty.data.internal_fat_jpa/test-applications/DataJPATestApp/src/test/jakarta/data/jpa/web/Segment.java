/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Entity with embeddables that are Java records.
 */
@Entity
public class Segment {

    @GeneratedValue
    @Id
    public Long id;

    @Embedded
    @Column(nullable = false)
    @AttributeOverrides( // required by EclipseLink to have 2 of same embedded type
    { @AttributeOverride(name = "x", column = @Column(name = "POINTAX")),
      @AttributeOverride(name = "y", column = @Column(name = "POINTAY"))
    })
    public Point pointA;

    @Embedded
    @Column(nullable = false)
    @AttributeOverrides( // required by EclipseLink to have 2 of same embedded type
    { @AttributeOverride(name = "x", column = @Column(name = "POINTBX")),
      @AttributeOverride(name = "y", column = @Column(name = "POINTBY"))
    })
    public Point pointB;

    @Override
    public String toString() {
        return "Segment#" + id + " (" +
               pointA.x() + ", " + pointA.y() + ") -> (" +
               pointB.x() + ", " + pointB.y() + ")";
    }
}
