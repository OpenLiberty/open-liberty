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
package com.ibm.ws.testtooling.testinfo;

import java.util.HashMap;
import java.util.Map;

public class JPAPersistenceContext implements java.io.Serializable {
    private static final long serialVersionUID = 2188076802500305825L;

    public enum PersistenceContextType {
        JSE,
        APPLICATION_MANAGED_RL,
        APPLICATION_MANAGED_JTA,
        CONTAINER_MANAGED_TS,
        CONTAINER_MANAGED_ES;
    };

    public enum PersistenceInjectionType {
        JSE,
        JNDI,
        FIELD,
        METHOD
    };

    public enum TransactionManagement {
        CONTAINER,
        USER
    }

    public enum TransactionSynchronization {
        SYNCHRONIZED, UNSYNCHRONIZED, UNKNOWN
    }

    private String name; // Context Identifier
    private PersistenceContextType pcType; // PC Type
    private PersistenceInjectionType injectionType; // Injection Type
    private TransactionManagement txMgmt = TransactionManagement.USER; // Transaction Management, default is user
    private String resource; // Resource (PU name, JNDI name, field name, getter-method name)
    @SuppressWarnings("rawtypes")
    private HashMap emfMap = new HashMap(); // EMF Properties, for JSE only
    @SuppressWarnings("rawtypes")
    private HashMap emMap = new HashMap(); // EM Properties, for JSE and app-managed only
    private TransactionSynchronization txSync = TransactionSynchronization.UNKNOWN; // Transaction Synchronization

    public JPAPersistenceContext() {

    }

    public JPAPersistenceContext(String name, PersistenceContextType pcType, PersistenceInjectionType injectionType,
                                 String resource) {
        this.name = name;
        this.pcType = pcType;
        this.injectionType = injectionType;
        this.resource = resource;
    }

    public JPAPersistenceContext(String name, PersistenceContextType pcType, PersistenceInjectionType injectionType,
                                 String resource, TransactionManagement txMgmt) {
        this.name = name;
        this.pcType = pcType;
        this.injectionType = injectionType;
        this.resource = resource;
        this.txMgmt = txMgmt;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public JPAPersistenceContext(String name, PersistenceContextType pcType, PersistenceInjectionType injectionType,
                                 String resource, Map emfMap, Map emMap) {
        this.name = name;
        this.pcType = pcType;
        this.injectionType = injectionType;
        this.resource = resource;

        this.emfMap.putAll(emfMap);
        this.emMap.putAll(emMap);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public JPAPersistenceContext(String name, PersistenceContextType pcType, PersistenceInjectionType injectionType,
                                 String resource, Map emfMap, Map emMap, TransactionManagement txMgmt) {
        this.name = name;
        this.pcType = pcType;
        this.injectionType = injectionType;
        this.resource = resource;
        this.txMgmt = txMgmt;

        this.emfMap.putAll(emfMap);
        this.emMap.putAll(emMap);
    }

    public JPAPersistenceContext(String name, PersistenceContextType pcType, PersistenceInjectionType injectionType,
                                 String resource, TransactionSynchronization txSync) {
        this.name = name;
        this.pcType = pcType;
        this.injectionType = injectionType;
        this.resource = resource;
        this.txSync = txSync;
    }

    public JPAPersistenceContext(String name, PersistenceContextType pcType, PersistenceInjectionType injectionType,
                                 String resource, TransactionManagement txMgmt, TransactionSynchronization txSync) {
        this.name = name;
        this.pcType = pcType;
        this.injectionType = injectionType;
        this.resource = resource;
        this.txMgmt = txMgmt;
        this.txSync = txSync;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public JPAPersistenceContext(String name, PersistenceContextType pcType, PersistenceInjectionType injectionType,
                                 String resource, Map emfMap, Map emMap, TransactionSynchronization txSync) {
        this.name = name;
        this.pcType = pcType;
        this.injectionType = injectionType;
        this.resource = resource;
        this.txSync = txSync;

        this.emfMap.putAll(emfMap);
        this.emMap.putAll(emMap);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public JPAPersistenceContext(String name, PersistenceContextType pcType, PersistenceInjectionType injectionType,
                                 String resource, Map emfMap, Map emMap, TransactionManagement txMgmt, TransactionSynchronization txSync) {
        this.name = name;
        this.pcType = pcType;
        this.injectionType = injectionType;
        this.resource = resource;
        this.txMgmt = txMgmt;
        this.txSync = txSync;

        this.emfMap.putAll(emfMap);
        this.emMap.putAll(emMap);
    }

    @SuppressWarnings("unchecked")
    public JPAPersistenceContext(JPAPersistenceContext original) {
        this.name = original.getName();
        this.pcType = original.getPcType();
        this.injectionType = original.getInjectionType();
        this.resource = original.getResource();
        this.txMgmt = original.getTxMgmt();
        this.txSync = original.getTxSynchronizationType();

        this.emfMap.putAll(original.getEmfMap());
        this.emMap.putAll(original.getEmMap());
    }

    @SuppressWarnings("unchecked")
    public JPAPersistenceContext(JPAPersistenceContext original, String nameOverride) {
        this.name = nameOverride;
        this.pcType = original.getPcType();
        this.injectionType = original.getInjectionType();
        this.resource = original.getResource();
        this.txMgmt = original.getTxMgmt();
        this.txSync = original.getTxSynchronizationType();

        this.emfMap.putAll(original.getEmfMap());
        this.emMap.putAll(original.getEmMap());
    }

    public final String getName() {
        return name;
    }

    public final PersistenceContextType getPcType() {
        return pcType;
    }

    public final PersistenceInjectionType getInjectionType() {
        return injectionType;
    }

    public final String getResource() {
        return resource;
    }

    @SuppressWarnings("rawtypes")
    public final Map getEmfMap() {
        return emfMap;
    }

    @SuppressWarnings("rawtypes")
    public final Map getEmMap() {
        return emMap;
    }

    public final TransactionManagement getTxMgmt() {
        return txMgmt;
    }

    public final TransactionSynchronization getTxSynchronizationType() {
        return txSync;
    }

    @Override
    public String toString() {
        return "JPAPersistenceContext [name=" + name + ", pcType=" + pcType
               + ", injectionType=" + injectionType + ", txMgmt=" + txMgmt
               + ", resource=" + resource + ", emfMap=" + emfMap + ", emMap="
               + emMap + ", txSync=" + txSync + "]";
    }

}