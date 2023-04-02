// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package io.openliberty.guides.event.dao;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import io.openliberty.guides.event.models.Event;

@RequestScoped
// tag::EventDao[]
public class EventDao {

    // tag::PersistenceContext[]
    @PersistenceContext(name = "jpa-unit")
    // end::PersistenceContext[]
    private EntityManager em;

    // tag::createEvent[]
    public void createEvent(Event event) {
        // tag::Persist[]
        em.persist(event);
        // end::Persist[]
    }
    // end::createEvent[]

    // tag::readEvent[]
    public Event readEvent(int eventId) {
        // tag::Find[]
        return em.find(Event.class, eventId);
        // end::Find[]
    }
    // end::readEvent[]

    // tag::updateEvent[]
    public void updateEvent(Event event) {
        // tag::Merge[]
        em.merge(event);
        // end::Merge[]
    }
    // end::updateEvent[]

    // tag::deleteEvent[]
    public void deleteEvent(Event event) {
        // tag::Remove[]
        em.remove(event);
        // end::Remove[]
    }
    // end::deleteEvent[]

    // tag::readAllEvents[]
    public List<Event> readAllEvents() {
        return em.createNamedQuery("Event.findAll", Event.class).getResultList();
    }
    // end::readAllEvents[]

    // tag::findEvent[]
    public List<Event> findEvent(String name, String location, String time) {
        return em.createNamedQuery("Event.findEvent", Event.class)
                        .setParameter("name", name)
                        .setParameter("location", location)
                        .setParameter("time", time)
                        .getResultList();
    }
    // end::findEvent[]
}
// end::EventDao[]
