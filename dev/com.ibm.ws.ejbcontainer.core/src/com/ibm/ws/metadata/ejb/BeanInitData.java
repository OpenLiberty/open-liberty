/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata.ejb;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.ejbcontainer.facade.EJBConfiguration;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;

/**
 * The data needed to initialize a bean. This object is discarded after a
 * bean's metadata has been fully initialized.
 *
 * <p>The "Legacy", "Endpoint", and "Module start" sections define the minimal
 * set of data that defines a bean to the container. This data is obtained
 * from deployment descriptors and annotation scanning.
 */
public class BeanInitData
{
    /**
     * The EJB name.
     */
    public final String ivName;

    /**
     * The module initialization data.
     */
    public final ModuleInitData ivModuleInitData;

    // -----------------------------------------------------------------------
    // Legacy - TODO: needed for BMD creation for legacy purposes; should be
    // moved later in initialization sequence
    // -----------------------------------------------------------------------

    public J2EEName ivJ2EEName;

    public int ivCMPVersion;

    /**
     * The remote component interface, or null if unspecified.
     */
    public String ivRemoteInterfaceName;

    /**
     * The local component interface, or null if unspecified.
     */
    public String ivLocalInterfaceName;

    // -----------------------------------------------------------------------
    // Endpoint - needed to allow external access to the bean
    // -----------------------------------------------------------------------

    public EnterpriseBean ivEnterpriseBean;

    /**
     * The EJB component type, or {@link com.ibm.ws.ejbcontainer.InternalConstants#TYPE_UNKNOWN} if unknown.
     *
     * @see com.ibm.ws.ejbcontainer.InternalConstants
     */
    public int ivType;

    /**
     * The EJB class name.
     */
    public String ivClassName;

    /**
     * The remote home interface, or null if unspecified.
     */
    public String ivRemoteHomeInterfaceName;

    /**
     * The remote business interfaces, or null if none.
     */
    public String[] ivRemoteBusinessInterfaceNames;

    /**
     * The local home interface, or null if unspecified.
     */
    public String ivLocalHomeInterfaceName;

    /**
     * The local business interfaces, or null if none.
     */
    public String[] ivLocalBusinessInterfaceNames;

    /**
     * True if the bean has a no-interface view.
     */
    public boolean ivLocalBean;

    /**
     * The message listener interface, or null if unspecified.
     */
    public String ivMessageListenerInterfaceName;

    /**
     * The webservice endpoint interface, or null if unspecified.
     */
    public String ivWebServiceEndpointInterfaceName;

    /**
     * True if the bean is a webservice endpoint.
     */
    public boolean ivWebServiceEndpoint;

    /**
     * The message-driven activation configuration properties.
     */
    public Properties ivActivationConfigProperties;

    /**
     * True if this EJB is passivation-capable.
     */
    public boolean ivPassivationCapable = true;

    /**
     * True if the EJB uses bean-managed transactions.
     *
     * <p>This metadata is needed for {@link com.ibm.ejs.container.BeanId},
     * which becomes part of a remote object key and is used by z/OS via {@link com.ibm.ejs.oa.EJBOAKeyImpl#isBeanManagedTransaction}.
     */
    public boolean ivBeanManagedTransaction;

    /**
     * Facade configuration details.
     */
    public EJBConfiguration ivFacadeConfiguration;

    // -----------------------------------------------------------------------
    // Module start - needed to determine if a bean must be initialized at
    // module start rather than being deferred until accessed externally.
    // -----------------------------------------------------------------------

    /**
     * True if the EJB has schedule timers.
     *
     * <p>True if the EJB has schedule timers, which requires timer method
     * metadata to be eagerly loaded.
     */
    public Boolean ivHasScheduleTimers;

    /**
     * True if the EJB is a startup singleton.
     */
    public boolean ivStartup;

    /**
     * The read-only list of dependency EJB links.
     *
     * <p>This metadata is used at application start before the bean is fully
     * initialized to diagnose dependency loops.
     */
    public Set<String> ivDependsOn;

    // -----------------------------------------------------------------------
    // Early initialization data - data saved between module start and
    // deferred initialization
    // -----------------------------------------------------------------------

    public List<TimerMethodData> ivTimerMethods;

    public BeanInitData(String name, ModuleInitData mid)
    {
        ivName = name;
        ivModuleInitData = mid;
    }

    @Override
    public String toString()
    {
        return super.toString() + '[' + ivJ2EEName + ']';
    }

    /**
     * Clear data that is not needed past module start.
     */
    public void unload()
    {
        ivEnterpriseBean = null;
    }

    public BeanMetaData createBeanMetaData()
    {
        return new BeanMetaData(0);
    }

    public boolean supportsInterface(String interfaceName) {
        if (ivLocalBusinessInterfaceNames != null) {
            for (String localInterface : ivLocalBusinessInterfaceNames) {
                if (interfaceName.equals(localInterface)) {
                    return true;
                }
            }
        }

        if (ivRemoteBusinessInterfaceNames != null) {
            for (String remoteInterface : ivRemoteBusinessInterfaceNames) {
                if (interfaceName.equals(remoteInterface)) {
                    return true;
                }
            }
        }

        if (interfaceName.equals(ivLocalHomeInterfaceName)) {
            return true;
        }

        if (interfaceName.equals(ivRemoteHomeInterfaceName)) {
            return true;
        }
        return false;
    }

}
