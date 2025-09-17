/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.jpa.persistence.tests.models;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

@Entity
public class Ticket {

    @Id
    private int id;
    private String name;

    @Enumerated(EnumType.ORDINAL)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    public static Ticket of(int id, String name, TicketStatus status, Priority priority) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setName(name);
        ticket.setStatus(status);
        ticket.setPriority(priority);
        return ticket;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the status
     */
    public TicketStatus getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    /**
     * @return the priority
     */
    public Priority getPriority() {
        return priority;
    }

    /**
     * @param priority the priority to set
     */
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {

        return "id=" + id + " name=" + name + " status=" + status + " priority=" + priority;
    }

}