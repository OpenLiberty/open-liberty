/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.io.Serializable;
import java.rmi.RemoteException;

import javax.ejb.EJBException;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.util.EJBSerializer;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * The state for a wrapper proxy. This object contains a decomposed BeanId,
 * and coordinates with {@link EJSContainer#resolveWrapperProxy} to obtain
 * an actual wrapper instance. To allow proper garbage collection, this object
 * must not hold a reference to an actual BeanId, wrapper, or other object from
 * the started application after {@link #disconnect} has been called.
 */
public abstract class WrapperProxyState
{
    static final TraceComponent tc = Tr.register(WrapperProxyState.class,
                                                 "EJBContainer",
                                                 "com.ibm.ejs.container.container");

    /**
     * Return the state for a wrapper proxy.
     *
     * @param proxy the {@link WrapperProxy}
     * @return the wrapper proxy state
     */
    public static WrapperProxyState getWrapperProxyState(Object proxy)
    {
        // A business interface proxy.
        if (proxy instanceof BusinessLocalWrapperProxy)
        {
            return ((BusinessLocalWrapperProxy) proxy).ivState;
        }

        // An EJBLocalHome or EJBLocalObject proxy.
        if (proxy instanceof EJSLocalWrapperProxy)
        {
            return ((EJSLocalWrapperProxy) proxy).ivState;
        }

        // A no-interface view proxy.
        if (proxy instanceof LocalBeanWrapperProxy)
        {
            return EJSWrapperCommon.getLocalBeanWrapperProxyState((LocalBeanWrapperProxy) proxy);
        }

        if (proxy instanceof WrapperProxy)
        {
            // Something implemented WrapperProxy without updating this method.
            throw new IllegalStateException(Util.identity(proxy));
        }

        // The caller passed something that was not a WrapperProxy.
        throw new IllegalArgumentException(Util.identity(proxy));
    }

    final J2EEName ivJ2EEName;
    private final J2EEName ivUnversionedJ2EEName;
    private final byte[] ivUnversionedSerializerBytes;
    BeanId ivBeanId;
    volatile Object ivWrapper;

    WrapperProxyState(EJSHome home, BeanId beanId, Object wrapper)
    {
        ivJ2EEName = home.getJ2EEName();

        J2EEName unversionedJ2EEName = null;
        byte[] unversionedSerializerBytes = null;

        BeanMetaData bmd = home.beanMetaData;
        if (bmd.ivUnversionedJ2eeName != null)
        {
            unversionedJ2EEName = bmd.ivUnversionedJ2eeName;
            unversionedSerializerBytes = beanId.getByteArrayBytes();
        }

        ivUnversionedJ2EEName = unversionedJ2EEName;
        ivUnversionedSerializerBytes = unversionedSerializerBytes;

        connect(beanId, wrapper);
    }

    @Override
    public final String toString()
    {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append('(');

        builder.append(ivJ2EEName.toString());

        if (ivWrapper == null)
        {
            builder.append(", disconnected");
        }

        if (ivUnversionedJ2EEName != null)
        {
            builder.append(", unversioned: " + ivUnversionedJ2EEName.getModule());
        }

        return builder.append(')').toString();
    }

    /**
     * The implementation of {@link Object#hashCode} for wrapper proxies.
     *
     * @param the hash code
     *
     * @see EJSWrapperBase#hashCode
     */
    @Override
    public abstract int hashCode();

    /**
     * The implementation of {@link Object#equals} for wrapper proxies.
     *
     * @param state the other state
     * @return true this state is equal to the other state
     *
     * @see EJSWrapperBase#equals
     */
    public abstract boolean equals(WrapperProxyState state);

    /**
     * Called when a wrapper is associated with this state and added to the
     * wrapper cache.
     */
    final void connect(BeanId beanId, Object wrapper)
    {
        ivBeanId = beanId;
        ivWrapper = wrapper;
    }

    /**
     * Called when the wrapper associated with this state is evicted from the
     * wrapper cache. All references to objects from the target wrapper must be
     * dropped to allow timely garbage collection.
     */
    final void disconnect()
    {
        ivBeanId = null;
        ivWrapper = null;
    }

    /**
     * Reconnects to an actual wrapper. This method must only be called if {@link #ivWrapper} is null. This method either reconnects this state
     * object, returns a new state object, or throws an EJBException to indicate
     * that the state could not be reconnected. Note that a returned state
     * object might be disconnected by another thread before the caller has an
     * opportunity to obtain the wrapper.
     *
     * @return a possibly disconnected state
     * @throws EJBException if the state cannot be updated
     */
    final WrapperProxyState reconnect()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "reconnect: " + this);

        HomeOfHomes homeOfHomes = EJSContainer.homeOfHomes;

        J2EEName j2eeName;
        if (ivUnversionedJ2EEName == null)
        {
            j2eeName = ivJ2EEName;
        }
        else
        {
            j2eeName = homeOfHomes.getVersionedJ2EEName(ivUnversionedJ2EEName);
        }

        EJSHome home = (EJSHome) homeOfHomes.getHome(j2eeName);
        if (home == null)
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "reconnect: stopped");
            throw new EJBException("The referenced " + j2eeName.getComponent() +
                                   " bean in the " + j2eeName.getModule() +
                                   " module in the " + j2eeName.getApplication() +
                                   " application has been stopped and must be started again to be used.");
        }

        WrapperProxyState state;
        try
        {
            state = reconnect(home);
        } catch (RemoteException ex)
        {
            FFDCFilter.processException(ex, getClass().getName() + ".update", "225", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "reconnect", ex);
            throw new EJBException(ex);
        }

        // Note that if this state was previously disconnected but the
        // application was not actually stopped (e.g., because the wrapper was
        // evicted from the wrapper cache), then the call to update above might
        // have simply reconnected and returned this same state object.

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "reconnect");
        return state;
    }

    abstract WrapperProxyState reconnect(EJSHome home)
                    throws RemoteException;

    /**
     * Returns the object type for this proxy as specified by {@link EJBSerializer#getObjectType}.
     *
     * @return the object type
     */
    public abstract EJBSerializer.ObjectType getSerializerObjectType();

    /**
     * Returns the serialized form of this proxy as specified by {@link EJBSerializer#serialize}.
     *
     * @return
     */
    public final byte[] getSerializerBytes()
    {
        if (ivUnversionedSerializerBytes != null)
        {
            return ivUnversionedSerializerBytes;
        }

        BeanId beanId = ivBeanId;
        if (beanId == null)
        {
            beanId = getSerializerBeanId(ivJ2EEName);
        }

        return getSerializerBytes(beanId);
    }

    abstract BeanId getSerializerBeanId(J2EEName j2eeName);

    byte[] getSerializerBytes(BeanId beanId)
    {
        return beanId.getByteArrayBytes();
    }

    /**
     * The state for an EJBLocalHome.
     */
    static class LocalHome
                    extends WrapperProxyState
    {
        LocalHome(EJSHome home, EJSLocalWrapper wrapper)
        {
            super(home, wrapper.beanId, wrapper);
        }

        @Override
        public int hashCode()
        {
            return ivJ2EEName.hashCode() + 1;
        }

        @Override
        public boolean equals(WrapperProxyState state)
        {
            return state instanceof LocalHome &&
                   ivJ2EEName.equals(state.ivJ2EEName);
        }

        @Override
        WrapperProxyState reconnect(EJSHome home)
                        throws RemoteException
        {
            return home.getWrapper().getLocalWrapperProxyState();
        }

        @Override
        public EJBSerializer.ObjectType getSerializerObjectType()
        {
            return EJBSerializer.ObjectType.EJB_LOCAL_HOME;
        }

        @Override
        public BeanId getSerializerBeanId(J2EEName j2eeName)
        {
            return new BeanId(EJSContainer.homeOfHomes.getJ2EEName(), j2eeName, true);
        }
    }

    /**
     * The base state for non-home wrappers.
     */
    static abstract class AbstractObjectState
                    extends WrapperProxyState
    {
        AbstractObjectState(EJSHome home, BeanId beanId, Object wrapper)
        {
            super(home, beanId, wrapper);
        }

        @Override
        final WrapperProxyState reconnect(EJSHome home)
                        throws RemoteException
        {
            BeanId beanId = new BeanId(home, getPrimaryKey());
            EJSWrapperCommon wrappers = EJSContainer.getDefaultContainer().wrapperManager.getWrapper(beanId);
            return reconnect(wrappers);
        }

        abstract Serializable getPrimaryKey();

        abstract WrapperProxyState reconnect(EJSWrapperCommon wrappers);

        @Override
        public final BeanId getSerializerBeanId(J2EEName j2eeName)
        {
            return new BeanId(j2eeName, getPrimaryKey(), false);
        }
    }

    /**
     * The state for an EJBLocalObject.
     */
    static class LocalObject
                    extends AbstractObjectState
    {
        private final Serializable ivPrimaryKey;

        LocalObject(EJSHome home, EJSLocalWrapper wrapper)
        {
            super(home, wrapper.beanId, wrapper);
            ivPrimaryKey = wrapper.beanId.getPrimaryKey();
        }

        @Override
        public int hashCode()
        {
            return ivJ2EEName.hashCode();
        }

        @Override
        public boolean equals(WrapperProxyState state)
        {
            return state instanceof LocalObject &&
                   ivJ2EEName.equals(state.ivJ2EEName);
        }

        @Override
        Serializable getPrimaryKey()
        {
            return ivPrimaryKey;
        }

        @Override
        WrapperProxyState reconnect(EJSWrapperCommon wrappers)
        {
            return wrappers.getLocalWrapperProxyState();
        }

        @Override
        public EJBSerializer.ObjectType getSerializerObjectType()
        {
            return EJBSerializer.ObjectType.EJB_LOCAL_OBJECT;
        }
    }

    /**
     * The state for a local business interface or no-interface view.
     */
    static class BusinessLocal
                    extends AbstractObjectState
    {
        private final Class<?> ivBusinessInterface;
        private final int ivBusinessInterfaceIndex;

        BusinessLocal(EJSHome home, BeanId beanId, Object wrapper, Class<?> businessInterface, int businessInterfaceIndex)
        {
            super(home, beanId, wrapper);
            ivBusinessInterface = businessInterface;
            ivBusinessInterfaceIndex = businessInterfaceIndex;
        }

        @Override
        public int hashCode()
        {
            return ivJ2EEName.hashCode() + ivBusinessInterface.hashCode();
        }

        @Override
        public boolean equals(WrapperProxyState state)
        {
            return state instanceof BusinessLocal &&
                   ivJ2EEName.equals(state.ivJ2EEName) &&
                   ivBusinessInterface == ((BusinessLocal) state).ivBusinessInterface;
        }

        @Override
        Serializable getPrimaryKey()
        {
            return null;
        }

        @Override
        WrapperProxyState reconnect(EJSWrapperCommon wrappers)
        {
            return wrappers.getLocalBusinessWrapperProxyState(ivBusinessInterface);
        }

        @Override
        public EJBSerializer.ObjectType getSerializerObjectType()
        {
            return EJBSerializer.ObjectType.EJB_BUSINESS_LOCAL;
        }

        @Override
        public byte[] getSerializerBytes(BeanId beanId)
        {
            WrapperId wrapperId = new WrapperId(beanId.getByteArrayBytes(),
                            ivBusinessInterface.getName(),
                            ivBusinessInterfaceIndex);
            return wrapperId.getBytes();
        }
    }
}
