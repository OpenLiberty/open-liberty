/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.v21;

import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.ws.jpa.management.JPAEMFactory;

@SuppressWarnings("serial")
public class JPAEMFactoryV21 extends JPAEMFactory {
    public JPAEMFactoryV21(JPAPuId puId, J2EEName j2eeName, EntityManagerFactory emf) {
        super(puId, j2eeName, emf);
    }

    private Object writeReplace() {
        // jpa-2.1 might not be enabled when this is deserialized, so serialize
        // the base wrapper.  During deserialization, readResolve will rewrap
        // with this class if needed.
        return new JPAEMFactory(this);
    }

    @Override
    public <T> void addNamedEntityGraph(String arg0, EntityGraph<T> arg1) {
        ivFactory.addNamedEntityGraph(arg0, arg1);
    }

    @Override
    public void addNamedQuery(String arg0, Query arg1) {
        ivFactory.addNamedQuery(arg0, arg1);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType arg0) {
        return ivFactory.createEntityManager(arg0);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType arg0, @SuppressWarnings("rawtypes") Map arg1) {
        return ivFactory.createEntityManager(arg0, arg1);
    }
}
