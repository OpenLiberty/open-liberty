/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package persistence_fat.consumer.ejb;

import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import persistence_fat.consumer.model.Person;

@Singleton
public class PersonBean {

    @PersistenceContext(unitName = "jpa-pu")
    EntityManager em;

    public boolean personExists(long id) {
        Person p = em.find(Person.class, id);
        return p != null;
    }

    public Person findPersonById(long id) {
        return em.find(Person.class, id);
    }

}
