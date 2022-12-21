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

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 *
 */
@Entity
public class Business {

    public String name;

    @GeneratedValue
    @Id
    public int id;

    public Business() {
    }

    Business(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Business id=" + id + "; name=" + name;
    }
}
