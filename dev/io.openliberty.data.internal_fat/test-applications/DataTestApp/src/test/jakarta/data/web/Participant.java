/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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
package test.jakarta.data.web;

/**
 * An unannotated entity with a record attribute that should
 * be interpreted as an embeddable.
 */
public class Participant {

    private Integer pID;

    public Name name;

    public static record Name(String first, String last) {
    }

    // Exception Description: Could not load the field named [PID] on the class [class test.jakarta.data.web.Participant]. Ensure there is a corresponding field with that name defined on the class.

    public Integer getPID() {
        return pID;
    }

    public static Participant of(String firstName, String lastName, int id) {
        Participant p = new Participant();
        p.pID = id;
        p.name = new Name(firstName, lastName);
        return p;
    }

    public void setPID(Integer value) {
        pID = value;
    }
}
