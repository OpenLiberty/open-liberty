/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Embeddable object for the Employee entity.
 */
@Embeddable
public class Badge {

    public char accessLevel;

    @Column(name = "BADGENUM")
    public short number;

    public Badge() {
    }

    public Badge(short number, char accessLevel) {
        this.number = number;
        this.accessLevel = accessLevel;
    }

    @Override
    public String toString() {
        return "Badge#" + number + " Level " + accessLevel;
    }
}
