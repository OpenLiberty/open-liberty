/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.hibernate.integration.web;

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
    public int id;

    public int x1, y1;
    public int x2, y2;

    public static Segment of(int x1, int y1, int x2, int y2) {
        Segment inst = new Segment();
        inst.x1 = x1;
        inst.y1 = y1;
        inst.x2 = x2;
        inst.y2 = y2;
        return inst;
    }

    @Override
    public String toString() {
        return "Segment#" + id + " (" +
               x1 + ", " + y1 + ") -> (" +
               x2 + ", " + y2 + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Segment))
            return false;

        Segment inst = (Segment) o;

        return this.id == inst.id &&
               this.x1 == inst.x1 &&
               this.y1 == inst.y1 &&
               this.x2 == inst.x2 &&
               this.y2 == inst.y2;
    }
}
