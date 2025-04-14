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
package test.jakarta.data.errpaths.web;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

/**
 * An invalid entity - because it has Jakarta Persistence annotations
 * but lacks an Entity annotation
 */
// intentionally lacks @Entity to test error path
public class Invitation {

    @Id
    public long id;

    @ElementCollection(fetch = FetchType.EAGER)
    public Set<String> invitees = new HashSet<>();

    @Column(nullable = false)
    public String place;

    @Column(nullable = false)
    public LocalDateTime time;

    @Override
    public String toString() {
        return "Invitation#" + id + " @" + time + " in " + place + " for " + invitees;
    }
}
