/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.management;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.JPAAccessor;
import com.ibm.ws.jpa.JPAPuId;

/**
 * This class is a proxy/wrapper to enable serialization of
 * EntityManagerFactory. This class is exposed as a public API because the
 * EntityManagerFactory interface does not have an unwrap method. The only
 * supported use of this class is to call {@link #unwrap}.
 * 
 * @ibm-api
 */
public class JPAEMFactory
                implements EntityManagerFactory,
                Serializable
{
    private static final long serialVersionUID = 5790871719838228801L;

    private static final TraceComponent tc = Tr.register(JPAEMFactory.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    private JPAPuId ivPuId;

    private J2EEName ivJ2eeName; // d510184

    protected transient EntityManagerFactory ivFactory;

    protected JPAEMFactory(JPAPuId puId, J2EEName j2eeName, EntityManagerFactory emf) {
        ivPuId = puId;
        ivJ2eeName = j2eeName; // d510184
        ivFactory = emf;
    }

    public JPAEMFactory(JPAEMFactory wrapper) {
        ivPuId = wrapper.ivPuId;
        ivJ2eeName = wrapper.ivJ2eeName;
        ivFactory = wrapper.ivFactory;
    }

    /**
     * Return an object of the specified type to allow access to
     * provider-specific API.
     * 
     * @param cls the class of the object to be returned
     * @return an instance of the specified class
     * @throws PersistenceException if the class is not supported
     */
    public <T> T unwrap(Class<T> cls) // d706751
    {
        if (cls.isInstance(ivFactory))
        {
            return cls.cast(ivFactory);
        }

        throw new PersistenceException(cls.toString());
    }

    @Override
    public void close()
    {
        ivFactory.close();
    }

    @Override
    public EntityManager createEntityManager()
    {
        return ivFactory.createEntityManager();
    }

    @Override
    @SuppressWarnings("unchecked")
    public EntityManager createEntityManager(Map arg0)
    {
        return ivFactory.createEntityManager(arg0);
    }

    @Override
    public boolean isOpen()
    {
        return ivFactory.isOpen();
    }

    @Override
    public String toString()
    {
        return super.toString() + '[' + ivPuId +
               ", " + ivJ2eeName +
               ", " + ivFactory + ']';
    }

    /*
     * Instance serialization.
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "writeObject : " + ivPuId + ", " + ivJ2eeName);

        out.writeObject(ivPuId);
        out.writeObject(ivJ2eeName); // d510184

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "writeObject");
    }

    /*
     * Instance de-serialization.
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException,
                    ClassNotFoundException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "readObject");

        ivPuId = (JPAPuId) in.readObject();
        ivJ2eeName = (J2EEName) in.readObject(); // d510184

        // restore the provider factory from JPA Service via puInfo object.
        //F743-16027 - using JPAAccessor to get JPAComponent, rather than using cached (possibly stale) static reference
        JPAPUnitInfo puInfo = ((AbstractJPAComponent) JPAAccessor.getJPAComponent()).findPersistenceUnitInfo(ivPuId); // d416151.3.5, F743-18776

        ivFactory = puInfo.getEntityManagerFactory(ivJ2eeName); // d510184

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "readObject : " + this);
    }

    private Object readResolve() // d706751
    {
        // If necessary, create a JPARuntime for an uplevel JPA version.
        JPARuntime jpaRuntime = ((AbstractJPAComponent) JPAAccessor.getJPAComponent()).getJPARuntime();
        Object wrapper = jpaRuntime.isDefault() ? this : jpaRuntime.createJPAEMFactory(ivPuId, ivJ2eeName, ivFactory);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "readResolve: " + wrapper);
        return wrapper;
    }

    // New JPA 2.0 methods   //F743-954 F743-954.1

    @Override
    public Cache getCache() {
        return ivFactory.getCache();
    }

    @Override
    public Map<String, Object> getProperties() {
        return ivFactory.getProperties();
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return ivFactory.getCriteriaBuilder();
    }

    @Override
    public Metamodel getMetamodel() {
        return ivFactory.getMetamodel();
    }

    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        return ivFactory.getPersistenceUnitUtil();
    }
}
