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
package test.jakarta.data.global.webapp;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * A simple entity to use with a Jakarta Data repository that requires a DataSource
 * with a java:global/env resource reference that that is defined by this
 * application.
 */
@Entity
public class Letter {
    @Id
    public char id;

    public static Letter of(char letter) {
        Letter w = new Letter();
        w.id = letter;
        return w;
    }

    @Override
    public String toString() {
        return "Letter:" + id;
    }
}
