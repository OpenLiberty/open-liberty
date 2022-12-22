/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 *
 */
@Embeddable
public class Street {

    public String direction;

    @Column(name = "STREETNAME")
    public String name;

    public Street() {
    }

    Street(String name, String direction) {
        this.name = name;
        this.direction = direction;
    }

    @Override
    public String toString() {
        return name + " " + direction;
    }
}
