/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package test.jakarta.data.jpa.web;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Embeddable for StreetAddress and DeliveryLocation
 */
@Embeddable
public class Street {

    public String direction;

    @Column(name = "STREETNAME")
    public String name;

    public Street() {
    }

    public Street(String name, String direction) {
        this.name = name;
        this.direction = direction;
    }

    @Override
    public String toString() {
        return name + " " + direction;
    }
}
