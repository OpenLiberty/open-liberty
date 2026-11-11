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

import java.util.List;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class EmployeeWithSkills {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @Embedded
    private SkillSet skillSet = new SkillSet();

    public EmployeeWithSkills() {}

    public EmployeeWithSkills(String name, List<Skill> skills) {
        this.name     = name;
        this.skillSet = new SkillSet(skills);
    }

    public Long getId()                       { return id; }
    public void setId(Long id)                { this.id = id; }
    public String getName()                   { return name; }
    public void setName(String name)          { this.name = name; }
    public SkillSet getSkillSet()             { return skillSet; }
    public void setSkillSet(SkillSet ss)      { this.skillSet = ss; }

    public List<Skill> getSkills() {
        return skillSet.getSkills();
    }
    public void setSkills(List<Skill> skills) {
        skillSet.setSkills(skills);
    }
}
