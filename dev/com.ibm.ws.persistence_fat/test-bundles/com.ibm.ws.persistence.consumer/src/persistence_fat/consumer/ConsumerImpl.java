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

package persistence_fat.consumer;

import java.io.Serializable;
import java.io.Writer;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUtil;

import org.eclipse.persistence.config.PersistenceUnitProperties;

import com.ibm.wsspi.persistence.PersistenceServiceUnit;
import com.ibm.wsspi.persistence.PersistenceServiceUnitConfig;

import persistence_fat.consumer.internal.model.Car;
import persistence_fat.consumer.internal.model.Person;
import persistence_fat.persistence.test.Consumer;

public class ConsumerImpl implements Consumer {
    ConsumerService _service;

    PersistenceServiceUnit _activePu;
    PersistenceServiceUnit _rootPu;
    PersistenceServiceUnit _tempPu;

    ConsumerImpl(PersistenceServiceUnit pu, ConsumerService service) {
        _activePu = pu;
        _rootPu = pu;
        _service = service;
    }

    @Override
    public void createTables() {
        _activePu.createTables();
    }

    @Override
    public long persistPerson(boolean newTran, int numCars) {
        return persistPerson(newTran, numCars, null);
    }

    @Override
    public long persistPerson(boolean newTran, int numCars, String data) {
        return persistPerson(newTran, numCars, data, null);
    }

    @Override
    public long persistPerson(boolean newTran, int numCars, String data, Serializable serializable) {
        if (newTran) {
            // TODO
        }
        EntityManager em = _activePu.createEntityManager();
        try {
            Person p = new Person("first", "last", data);
            for (int i = 0; i < numCars; i++) {
                Car c = new Car();
                p.addCar(c);
            }
            p.setSerializableField(serializable);
            em.persist(p);
            return p.getId();
        } finally {
            em.close();
        }
    }

    @Override
    public long getRandomPersonId() {
        throw new RuntimeException("not implemented!");
    }

    @Override
    public boolean personExists(long personId) {
        EntityManager em = _activePu.createEntityManager();
        try {
            return em.find(Person.class, personId) != null;
        } finally {
            em.close();
        }
    }

    @Override
    public int getNumCars(long personId) {
        EntityManager em = _activePu.createEntityManager();
        try {
            Person p = em.find(Person.class, personId);
            if (p == null) {
                throw new RuntimeException("null person.id=" + personId);
            }
            System.err.println("loaded person " + p.getId());
            PersistenceUtil util = Persistence.getPersistenceUtil();
            System.err.println("PersistenceUtil = " + util.getClass());
            if (util.isLoaded(p, "cars")) {
                throw new RuntimeException("Cars should not have been eagerly loaded.");
            }
            System.err.println("calling person.getCars()");
            p.getCars().get(0);
            return p.getCars().size();
        } finally {
            em.close();
        }
    }

    @Override
    public String[] getPersonName(long personId) {
        EntityManager em = _activePu.createEntityManager();
        try {
            Person p = em.find(Person.class, personId);
            return new String[] { p.getFirst(), p.getLast() };
        } finally {
            em.close();
        }
    }

    @Override
    public Integer queryPersonDataParameter(String queryStr) {
        EntityManager em = _activePu.createEntityManager();
        try {
            Integer res = em.createQuery("SELECT COUNT(p.id) FROM Person p WHERE p.data LIKE :str", Integer.class)
                            .setParameter("str", queryStr)
                            .getSingleResult();
            return res;
        } finally {
            em.close();
        }
    }

    @Override
    public Integer queryPersonDataLiteral(String queryStr) {
        EntityManager em = _activePu.createEntityManager();
        try {
            Integer res = em.createQuery("SELECT COUNT(p.id) FROM Person p WHERE p.data LIKE " + queryStr, Integer.class)
                            .setParameter("str", queryStr)
                            .getSingleResult();
            return res;
        } finally {
            em.close();
        }
    }

    @Override
    public String test() {

        return null;
    }

    @Override
    public void clearTempConfig() {
        if (_tempPu == _activePu) {
            _activePu = _rootPu;
            _tempPu.close();
        }
    }

    @Override
    public void updateUnicodeConfig(Boolean val) {
        PersistenceServiceUnitConfig conf = _service.getPersistenceServiceUnitConfig(true);
        conf.setAllowUnicode(val);

        _tempPu = _service.createPersistenceServiceUnit(conf);
        _activePu = _tempPu;
        System.out.println(ConsumerImpl.class.getName() + ".updateUnicodeConfig(" + val + ") new=" + _activePu
                           + " orig=" + _rootPu);

    }

    @Override
    public Serializable getPersonSerializableData(long personId) {
        EntityManager em = _activePu.createEntityManager();
        try {
            return em.find(Person.class, personId).getSerializable();
        } finally {
            em.close();
        }
    }

    @Override
    public void generateDDL(Writer out, DATABASE db) {
        if (db == null) {
            _activePu.generateDDL(out);
        } else {
            PersistenceServiceUnitConfig conf = _service.getPersistenceServiceUnitConfig(true);
            Map<String, Object> properties = conf.getProperties();
            properties.put(PersistenceUnitProperties.SCHEMA_DATABASE_PRODUCT_NAME, db.toString());
            _tempPu = _service.createPersistenceServiceUnit(conf);
            _tempPu.generateDDL(out);
            _tempPu.close();
        }
    }

    @Override
    public String getDatabaseTerminationToken() {
        return _activePu.getDatabaseTerminationToken();
    }
}
