/*******************************************************************************
 * Copyright (c) 1998, 2013 IBM Corporation and others.
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
import java.rmi.NoSuchObjectException;

import javax.ejb.EJBHome;
import javax.ejb.EJBMetaData;
import javax.rmi.PortableRemoteObject;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ffdc.FFDCFilter;

//     ***** Obsolete ****
//     This class was replaced by com.ibm.ejb.portable.EJBMetaDataImpl,
//     so do not update this class, update the new class.
//     ***** Obsolete ****
/**
 * This class is designed to be used as the EJBMetaData instance returned
 * by getEJBMetaData on EJS homes. <p>
 */

public class EJBMetaDataImpl
                implements EJBMetaData,
                Serializable
{
    private EJBHome home;
    private final String beanClassName;
    private final String remoteInterfaceClassName;
    private final String homeInterfaceClassName;
    private final String primaryKeyClassName;
    private final boolean session;
    private final boolean statelessSession;
    private static final long serialVersionUID = 4092588565014573628L;
    private static final TraceComponent tc =
                    Tr.register(EJBMetaDataImpl.class
                                , "EJBContainer"
                                , "com.ibm.ejs.container.container"); //d118336

    private static final String CLASS_NAME = "com.ibm.ejs.container.EJBMetaDataImpl";

    public EJBMetaDataImpl(BeanMetaData beanMetaData,
                           EJSWrapper home, J2EEName j2eeName) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "<init>");

        // Store the stub for the home
        try {
            this.home = (EJBHome) (PortableRemoteObject.toStub(home));
        } catch (NoSuchObjectException ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".EJBMetaDataImpl",
                                        "85", this);
            Tr.warning(tc, "UNABLE_CONVERT_REMOTE_2_STUB_CNTR0045W", ex.toString());
        }

        beanClassName = beanMetaData.enterpriseBeanAbstractClass.getName();//d150685.1
        remoteInterfaceClassName =
                        beanMetaData.remoteInterfaceClass.getName();
        homeInterfaceClassName = beanMetaData.homeInterfaceClass.getName();

        if (beanMetaData.pKeyClass != null) {
            primaryKeyClassName = beanMetaData.pKeyClass.getName();
        } else {
            primaryKeyClassName = null;
        }

        if (beanMetaData.type == InternalConstants.TYPE_STATEFUL_SESSION) { // 126512
            session = true;
            statelessSession = false;
        } else if (beanMetaData.type == InternalConstants.TYPE_STATELESS_SESSION) { // 126512
            session = true;
            statelessSession = true;
        } else {
            session = false;
            statelessSession = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "<init>");
    }

    /**
     * Objtain the home interface of the enterprise Bean associated with
     * this meta data instance. <p>
     */

    public EJBHome getEJBHome()
    {
        return home;
    } // getEJBHome

    public Class<?> getHomeInterfaceClass() {

        Class<?> homeInterfaceClass = null;

        try {

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            homeInterfaceClass = cl.loadClass(homeInterfaceClassName);

        } catch (ClassNotFoundException ex) {

            FFDCFilter.processException(ex, CLASS_NAME + ".getHomeInterfaceClass",
                                        "140", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) // d144064
                Tr.event(tc, "Failed to load home interface class",
                         homeInterfaceClassName);
        }

        return homeInterfaceClass;
    }

    public Class<?> getPrimaryKeyClass() {

        Class<?> primaryKeyClass = null;

        if (session) {
            throw new RuntimeException("No PrimaryKeyClass for Session Bean");
        }

        try {

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            primaryKeyClass = cl.loadClass(primaryKeyClassName);

        } catch (ClassNotFoundException ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".getPrimaryKeyClass",
                                        "163", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) // d144064
                Tr.event(tc, "Failed to load primary key class",
                         primaryKeyClassName);
        }

        return primaryKeyClass;
    }

    public Class<?> getRemoteInterfaceClass() {

        Class<?> remoteInterfaceClass = null;

        try {

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            remoteInterfaceClass =
                            cl.loadClass(remoteInterfaceClassName);
        } catch (ClassNotFoundException ex) {

            FFDCFilter.processException(ex, CLASS_NAME + ".getRemoteInterfaceClass",
                                        "183", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) // d144064
                Tr.event(tc, "Failed to load remote interface class",
                         remoteInterfaceClassName);
        }

        return remoteInterfaceClass;
    }

    public String getBeanClassName() {
        return beanClassName;
    }

    public boolean isSession() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isSession = " + session);

        return session;
    }

    public boolean isStatelessSession() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isStatelessSession = " + statelessSession);

        return statelessSession;
    }
} // EJBMetaData
