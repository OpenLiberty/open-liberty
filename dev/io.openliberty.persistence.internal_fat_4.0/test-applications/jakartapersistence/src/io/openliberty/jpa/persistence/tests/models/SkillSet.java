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
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

/**
 * Embeddable that owns an @ElementCollection of {@link Skill}.
 * Embedding this inside an @Entity and then calling EntityAgent.insert/update/upsert
 * tests the "declared inside an embeddable" code path described in the JPA 4.0 spec issue.
 */
@Embeddable
public class SkillSet {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EMPLOYEE_SKILLS",
                     joinColumns = @JoinColumn(name = "EMPLOYEE_ID"))
    @OrderColumn(name = "SKILL_ORDER")
    private List<Skill> skills = new ArrayList<>();

    public SkillSet() {}

    public SkillSet(List<Skill> skills) {
        this.skills = new ArrayList<>(skills);
    }

    public List<Skill> getSkills()       { return skills; }
    public void setSkills(List<Skill> s) { this.skills = s; }
}
