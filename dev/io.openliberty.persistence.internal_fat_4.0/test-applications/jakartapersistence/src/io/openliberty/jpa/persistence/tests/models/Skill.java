/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.persistence.tests.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Skill {

    @Column(name = "SKILL_NAME")
    private String name;

    @Column(name = "SKILL_LEVEL")
    private int level;

    public Skill() {}

    public Skill(String name, int level) {
        this.name  = name;
        this.level = level;
    }

    public String getName()       { return name; }
    public void setName(String n) { this.name = n; }
    public int getLevel()         { return level; }
    public void setLevel(int l)   { this.level = l; }
}
