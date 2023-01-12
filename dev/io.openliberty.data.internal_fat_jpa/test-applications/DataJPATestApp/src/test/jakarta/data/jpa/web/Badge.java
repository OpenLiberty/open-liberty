/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import jakarta.persistence.Id;

/**
 *
 */
@Embeddable
public class Badge {

    public char accessLevel;

    @Column(name = "BADGENUM")
    @Id
    public short number;

    public Badge() {
    }

    Badge(short number, char accessLevel) {
        this.number = number;
        this.accessLevel = accessLevel;
    }

    @Override
    public String toString() {
        return "Badge#" + number + " Level " + accessLevel;
    }
}
