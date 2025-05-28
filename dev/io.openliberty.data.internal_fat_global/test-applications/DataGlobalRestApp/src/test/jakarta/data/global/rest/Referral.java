/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.global.rest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * A simple entity for a repository that relies on a DataSource with a
 * java:global/env resource reference that is defined in another application.
 */
@Entity
public class Referral {
    @Id
    public String email;

    @Column(nullable = false)
    public String name;

    @Column
    public Long phone;

    public static Referral of(String email, String name, Long phone) {
        Referral r = new Referral();
        r.email = email;
        r.name = name;
        r.phone = phone;
        return r;
    }

    @Override
    public String toString() {
        return "Referral:" + email + " " + name + " #" + phone;
    }
}
