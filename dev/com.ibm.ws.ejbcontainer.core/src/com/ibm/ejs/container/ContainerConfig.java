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

import com.ibm.ejs.container.passivator.StatefulPassivator;
import com.ibm.ejs.csi.UOWControl;
import com.ibm.websphere.csi.ContainerExtensionFactory;
import com.ibm.websphere.csi.EJBCache;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.csi.OrbUtils;
import com.ibm.websphere.csi.PassivationPolicy;
import com.ibm.websphere.csi.StatefulSessionHandleFactory;
import com.ibm.websphere.csi.StatefulSessionKeyFactory;
import com.ibm.ws.ejbcontainer.EJBPMICollaboratorFactory;
import com.ibm.ws.ejbcontainer.EJBRequestCollaborator;
import com.ibm.ws.ejbcontainer.EJBSecurityCollaborator;
import com.ibm.ws.ejbcontainer.failover.SfFailoverCache;
import com.ibm.ws.ejbcontainer.runtime.EJBRuntime;
import com.ibm.ws.ejbcontainer.util.ObjectCopier;
import com.ibm.ws.ejbcontainer.util.PoolManager;
import com.ibm.ws.util.StatefulBeanEnqDeq;

/**
 * Wrapper for all the config data passed across the CSI.
 */

public class ContainerConfig
{
    public ContainerConfig(EJBRuntime ejbRuntime,
                           ClassLoader classLoader,
                           String containerName,
                           EJBCache ejbCache,
                           WrapperManager wrapperManager,
                           PassivationPolicy passivationPolicy,
                           com.ibm.websphere.cpi.PersisterFactory persisterFactory,
                           EntityHelper entityHelper,
                           EJBPMICollaboratorFactory pmiBeanFactory,
                           EJBSecurityCollaborator<?> securityCollaborator,
                           StatefulPassivator statefulPassivator,
                           StatefulSessionKeyFactory statefulSessionKeyFactory,
                           StatefulSessionHandleFactory
                           statefulSessionHandleFactory,
                           PoolManager poolManager,
                           J2EENameFactory j2eeNameFactory,
                           ObjectCopier objectCopier,
                           OrbUtils orbUtils,
                           UOWControl uowControl,
                           EJBRequestCollaborator<?>[] afterActivationCollaborators,
                           EJBRequestCollaborator<?>[] beforeActivationCollaborators,
                           EJBRequestCollaborator<?>[] beforeActivationAfterCompletionCollaborators,
                           ContainerExtensionFactory containerExtFactory, // d125942
                           StatefulBeanEnqDeq statefulBeanEnqDeq, // d646413.2
                           DispatchEventListenerManager dispatchEventListenerManager, // d646413.2
                           SfFailoverCache failoverCache, //LIDB2018-1
                           boolean sfsbFailoverEnabled //LIDB2018-1

    )
    {
        this.ivEJBRuntime = ejbRuntime;
        this.classLoader = classLoader;
        this.containerName = containerName;
        this.ejbCache = ejbCache;
        this.wrapperManager = wrapperManager;
        this.passivationPolicy = passivationPolicy;
        this.persisterFactory = persisterFactory;
        this.ivEntityHelper = entityHelper;
        this.pmiBeanFactory = pmiBeanFactory;
        this.securityCollaborator = securityCollaborator;
        this.ivStatefulPassivator = statefulPassivator;
        this.statefulSessionKeyFactory = statefulSessionKeyFactory;
        this.statefulSessionHandleFactory = statefulSessionHandleFactory;
        this.poolManager = poolManager;
        this.j2eeNameFactory = j2eeNameFactory;
        this.ivObjectCopier = objectCopier;
        this.orbUtils = orbUtils;
        this.uowControl = uowControl;
        this.afterActivationCollaborators = afterActivationCollaborators;
        this.beforeActivationCollaborators = beforeActivationCollaborators;
        this.beforeActivationAfterCompletionCollaborators =
                        beforeActivationAfterCompletionCollaborators;
        this.containerExtFactory = containerExtFactory; // d125942
        this.ivStatefulBeanEnqDeq = statefulBeanEnqDeq;
        this.ivDispatchEventListenerManager = dispatchEventListenerManager;
        this.ivStatefulFailoverCache = failoverCache; //LIDB2018-1
        this.ivSFSBFailoverEnabled = sfsbFailoverEnabled; //LIDB2018-1
    } // ContainerConfigImpl

    public EJBRuntime getEJBRuntime() {
        return ivEJBRuntime;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public EJBCache getEJBCache() {
        return ejbCache;
    }

    public WrapperManager getWrapperManager()
    {
        return wrapperManager;
    }

    public PoolManager getPoolManager() {
        return poolManager;
    }

    public J2EENameFactory getJ2EENameFactory() {
        return j2eeNameFactory;
    }

    public String getContainerName() {
        return containerName;
    }

    public EJBRequestCollaborator<?>[] getAfterActivationCollaborators()
    {
        return afterActivationCollaborators;
    }

    public EJBRequestCollaborator<?>[] getBeforeActivationCollaborators()
    {
        return beforeActivationCollaborators;
    }

    public EJBRequestCollaborator<?>[] getBeforeActivationAfterCompletionCollaborators()
    {
        return beforeActivationAfterCompletionCollaborators;
    }

    public PassivationPolicy getPassivationPolicy() {
        return passivationPolicy;
    }

    public com.ibm.websphere.cpi.PersisterFactory getPersisterFactory() {
        return persisterFactory;
    }

    public EntityHelper getEntityHelper()
    {
        return ivEntityHelper;
    }

    public EJBPMICollaboratorFactory getPmiBeanFactory() {
        return pmiBeanFactory;
    }

    public EJBSecurityCollaborator<?> getSecurityCollaborator() {
        return securityCollaborator;
    }

    public StatefulPassivator getStatefulPassivator() {
        return ivStatefulPassivator;
    }

    public StatefulSessionKeyFactory getSessionKeyFactory() {
        return statefulSessionKeyFactory;
    }

    public StatefulSessionHandleFactory getStatefulSessionHandleFactory() {
        return statefulSessionHandleFactory;
    }

    public ObjectCopier getObjectCopier()
    {
        return ivObjectCopier;
    }

    public OrbUtils getOrbUtils() {
        return orbUtils;
    }

    public UOWControl getUOWControl() {
        return uowControl;
    }

    public ContainerExtensionFactory getContainerExtensionFactory() { // d125942
        return containerExtFactory;
    }

    public StatefulBeanEnqDeq getStatefulBeanEnqDeq()
    {
        return ivStatefulBeanEnqDeq;
    }

    public DispatchEventListenerManager getDispatchEventListenerManager()
    {
        return ivDispatchEventListenerManager;
    }

    /**
     * Get the default Settings object to use for EJB container.
     * 
     * @return default Settings or null if EJB container is not
     *         configured for SFSB failover.
     */
    public SfFailoverCache getStatefulFailoverCache() //LIDB2018-1
    {
        return ivStatefulFailoverCache;
    }

    /**
     * Get whether SFSB failover is enabled for all SFSB in this
     * container by default.
     * 
     * @return true if SFSB failover is enabled.
     */
    public boolean isEnabledSFSBFailover() //LIDB2018-1
    {
        return ivSFSBFailoverEnabled;
    }

    private EJBRuntime ivEJBRuntime; // F743-12528
    private ClassLoader classLoader;
    private String containerName;
    private EJBCache ejbCache;
    private WrapperManager wrapperManager;
    private PassivationPolicy passivationPolicy;
    private com.ibm.websphere.cpi.PersisterFactory persisterFactory;
    private EntityHelper ivEntityHelper;
    private EJBPMICollaboratorFactory pmiBeanFactory;
    private EJBSecurityCollaborator<?> securityCollaborator;
    private StatefulPassivator ivStatefulPassivator;
    private StatefulSessionKeyFactory statefulSessionKeyFactory;
    private StatefulSessionHandleFactory statefulSessionHandleFactory;
    private PoolManager poolManager;
    private J2EENameFactory j2eeNameFactory;
    private ObjectCopier ivObjectCopier;
    private OrbUtils orbUtils;
    private EJBRequestCollaborator<?>[] afterActivationCollaborators;
    private EJBRequestCollaborator<?>[] beforeActivationCollaborators;
    private EJBRequestCollaborator<?>[] beforeActivationAfterCompletionCollaborators;
    private UOWControl uowControl;
    private ContainerExtensionFactory containerExtFactory; // d125942
    private final StatefulBeanEnqDeq ivStatefulBeanEnqDeq; // d646413.2
    private final DispatchEventListenerManager ivDispatchEventListenerManager; // d646413.2

    /**
     * If SFSB failover is enabled, this is the SfFailoverCache
     * object to be used by this application server.
     */
    private SfFailoverCache ivStatefulFailoverCache; //LIDB2018-1

    /**
     * Indicates whether SFSB failover is enabled for all SFSB
     * in the container by default (e.g. can override at module
     * or application level).
     */
    private boolean ivSFSBFailoverEnabled; //LIDB2018-1

} // ContainerConfigImpl

