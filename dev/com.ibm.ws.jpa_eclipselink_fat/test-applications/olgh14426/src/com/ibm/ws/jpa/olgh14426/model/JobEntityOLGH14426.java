/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh14426.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class JobEntityOLGH14426 {

    @Id
    private Long id;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL)
    private List<EventEntityOLGH14426> events = new ArrayList<EventEntityOLGH14426>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<EventEntityOLGH14426> getEvents() {
        return events;
    }

    public void setEvents(List<EventEntityOLGH14426> events) {
        this.events = events;
    }
}
