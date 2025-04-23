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

@Entity
public class Reminder {
    @Id
    public long id;

    @Column(nullable = false)
    public String message;

    public static Reminder of(long id, String message) {
        Reminder r = new Reminder();
        r.id = id;
        r.message = message;
        return r;
    }

    @Override
    public String toString() {
        return "Reminder#" + id + ":" + message;
    }
}
