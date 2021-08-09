/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.ejbdd;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejb.Entity;

public class EntityMockery extends ComponentViewableBeanMockery<EntityMockery> {
    private final int persistenceType;
    private int cmpVersion = Entity.CMP_VERSION_UNSPECIFIED;

    EntityMockery(Mockery mockery, String name, int persistenceType) {
        super(mockery, name, EnterpriseBean.KIND_ENTITY);
        this.persistenceType = persistenceType;
    }

    public EntityMockery cmpVersion(int cmpVersion) {
        this.cmpVersion = cmpVersion;
        return this;
    }

    public Entity mock() {
        final Entity entity = mockComponentViewableBean(Entity.class);
        mockery.checking(new Expectations() {
            {
                allowing(entity).getPersistenceTypeValue();
                will(returnValue(persistenceType));

                allowing(entity).getCMPVersionValue();
                will(returnValue(cmpVersion));
            }
        });
        return entity;
    }
}
