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

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * JPA entity representing a student.  
 * Contains a roll number, name, and an array of marks.
 */
@Entity
public class Student {

    @Id
    public long rollNo;

    public String name;

    public int[] marks;

    public Student() {
    }

    public Student(long rollNo, String name, int[] marks) {
        this.rollNo = rollNo;
        this.name = name;
        this.marks = marks;
    }
}
