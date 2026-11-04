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

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

@Entity
public class EmployeeDirectSkills {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EMP_DIRECT_SKILLS",
                     joinColumns = @JoinColumn(name = "EMPLOYEE_ID"))
    @OrderColumn(name = "SKILL_ORDER")
    private List<Skill> skills = new ArrayList<>();

    public EmployeeDirectSkills() {}

    public EmployeeDirectSkills(String name, List<Skill> skills) {
        this.name   = name;
        this.skills = new ArrayList<>(skills);
    }

    public Long getId()                      { return id; }
    public void setId(Long id)               { this.id = id; }
    public String getName()                  { return name; }
    public void setName(String name)         { this.name = name; }
    public List<Skill> getSkills()           { return skills; }
    public void setSkills(List<Skill> s)     { this.skills = s; }
}
