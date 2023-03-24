/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.jpa.olgh21204.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class StepExecutionEntityOLGH21204 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String text;

    @ManyToOne
    @JoinColumn(name = "FK_ID")
    private StepExecutionEntityOLGH21204 childEntity;

    public StepExecutionEntityOLGH21204() {
    }

    public StepExecutionEntityOLGH21204(StepExecutionEntityOLGH21204 childEntity) {
        this.childEntity = childEntity;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public StepExecutionEntityOLGH21204 getChildEntity() {
        return childEntity;
    }

    public void setChildEntity(StepExecutionEntityOLGH21204 childEntity) {
        this.childEntity = childEntity;
    }

    @Override
    public String toString() {
        return "com.ibm.ws.jpa.olgh21204.model.StepExecutionEntityOLGH21204[id=" + id + "]";
    }
}
