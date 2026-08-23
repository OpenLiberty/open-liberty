
/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.jpa.data.tests.models;

public class ParticipantOrm {

    public Integer id;

    public NameOrm name;

    public static ParticipantOrm of(String firstName, String lastName, int id) {

        ParticipantOrm p = new ParticipantOrm();
        p.id = id;
        p.name = new NameOrm(firstName, lastName);
        return p;
    }

    public static record NameOrm(String first, String last) {

    }
}
