/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.runtime;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ejs.container.DispatchEventListenerManager;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EntityHelper;
import com.ibm.ejs.container.WrapperManager;
import com.ibm.ejs.container.passivator.StatefulPassivator;
import com.ibm.websphere.cpi.PersisterFactory;
import com.ibm.websphere.csi.ContainerExtensionFactory;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.csi.OrbUtils;
import com.ibm.websphere.csi.PassivationPolicy;
import com.ibm.websphere.csi.StatefulSessionHandleFactory;
import com.ibm.websphere.csi.StatefulSessionKeyFactory;
import com.ibm.ws.ejbcontainer.EJBPMICollaboratorFactory;
import com.ibm.ws.ejbcontainer.EJBRequestCollaborator;
import com.ibm.ws.ejbcontainer.EJBSecurityCollaborator;
import com.ibm.ws.ejbcontainer.failover.SfFailoverCache;
import com.ibm.ws.ejbcontainer.util.ObjectCopier;
import com.ibm.ws.metadata.ejb.EJBMDOrchestrator;
import com.ibm.ws.util.StatefulBeanEnqDeq;
import com.ibm.wsspi.ejbcontainer.WSEJBHandlerResolver;

public class EJBRuntimeConfig
{
    private EJSContainer ivContainer;
    private WrapperManager ivWrapperManager;
    private EJBMDOrchestrator ivEJBMDOrchestrator;
    private String ivName;

    private ContainerExtensionFactory ivContainerExtensionFactory;
    private EJBSecurityCollaborator<?> ivSecurityCollaborator;
    private StatefulPassivator ivStatefulPassivator;
    private EJBPMICollaboratorFactory ivPmiBeanFactory;
    private PersisterFactory ivPersisterFactory;
    private EntityHelper ivEntityHelper;
    private PassivationPolicy ivPassivationPolicy;
    private SfFailoverCache ivSfFailoverCache;
    private boolean ivSFSBFailoverEnabled;
    private J2EENameFactory ivJ2EENameFactory;
    private OrbUtils ivOrbUtils;
    private ObjectCopier ivObjectCopier;
    private StatefulSessionKeyFactory ivStatefulSessionKeyFactory;
    private StatefulSessionHandleFactory ivStatefulSessionHandleFactory;
    private WSEJBHandlerResolver ivWSEJBHandlerResolver;
    private StatefulBeanEnqDeq ivStatefulBeanEnqDeq;
    private DispatchEventListenerManager ivDispatchEventListenerManager;

    private String ivDefaultDataSourceJNDIName;
    private long ivInactivePoolCleanupInterval = 30000;
    private long ivCacheSize = 2053;
    private long ivCacheSweepInterval = 3000;

    private List<EJBRequestCollaborator<?>> ivAfterActivationCollaborators = new ArrayList<EJBRequestCollaborator<?>>();
    private List<EJBRequestCollaborator<?>> ivBeforeActivationCollaborators = new ArrayList<EJBRequestCollaborator<?>>();
    private List<EJBRequestCollaborator<?>> ivBeforeActivationAfterCompletionCollaborators = new ArrayList<EJBRequestCollaborator<?>>();

    public EJSContainer getContainer()
    {
        return ivContainer;
    }

    public void setContainer(EJSContainer container)
    {
        ivContainer = container;
    }

    public WrapperManager getWrapperManager()
    {
        return ivWrapperManager;
    }

    public void setWrapperManager(WrapperManager wrapperManager)
    {
        ivWrapperManager = wrapperManager;
    }

    public EJBMDOrchestrator getEJBMDOrchestrator()
    {
        return ivEJBMDOrchestrator;
    }

    public void setEJBMDOrchestrator(EJBMDOrchestrator ejbMDOrchestrator)
    {
        ivEJBMDOrchestrator = ejbMDOrchestrator;
    }

    public String getName()
    {
        return ivName;
    }

    public void setName(String name)
    {
        ivName = name;
    }

    public String getDefaultDataSourceJNDIName()
    {
        return ivDefaultDataSourceJNDIName;
    }

    public void setDefaultDataSourceJNDIName(String jndiName)
    {
        ivDefaultDataSourceJNDIName = jndiName;
    }

    public ContainerExtensionFactory getContainerExtensionFactory()
    {
        return ivContainerExtensionFactory;
    }

    public void setContainerExtensionFactory(ContainerExtensionFactory containerExtensionFactory)
    {
        ivContainerExtensionFactory = containerExtensionFactory;
    }

    public EJBSecurityCollaborator<?> getSecurityCollaborator()
    {
        return ivSecurityCollaborator;
    }

    public void setSecurityCollaborator(EJBSecurityCollaborator<?> securityCollaborator)
    {
        ivSecurityCollaborator = securityCollaborator;
    }

    public StatefulPassivator getStatefulPassivator()
    {
        return ivStatefulPassivator;
    }

    public void setStatefulPassivator(StatefulPassivator statefulPassivator)
    {
        ivStatefulPassivator = statefulPassivator;
    }

    public EJBPMICollaboratorFactory getPmiBeanFactory()
    {
        return ivPmiBeanFactory;
    }

    public void setPmiBeanFactory(EJBPMICollaboratorFactory pmiBeanFactory)
    {
        ivPmiBeanFactory = pmiBeanFactory;
    }

    public PersisterFactory getPersisterFactory()
    {
        return ivPersisterFactory;
    }

    public void setPersisterFactory(PersisterFactory persisterFactory)
    {
        ivPersisterFactory = persisterFactory;
    }

    public EntityHelper getEntityHelper()
    {
        return ivEntityHelper;
    }

    public void setEntityHelper(EntityHelper entityHelper)
    {
        ivEntityHelper = entityHelper;
    }

    public PassivationPolicy getPassivationPolicy()
    {
        return ivPassivationPolicy;
    }

    public void setPassivationPolicy(PassivationPolicy passivationPolicy)
    {
        ivPassivationPolicy = passivationPolicy;
    }

    public SfFailoverCache getSfFailoverCache()
    {
        return ivSfFailoverCache;
    }

    public void setSfFailoverCache(SfFailoverCache sfFailoverCache)
    {
        ivSfFailoverCache = sfFailoverCache;
    }

    public J2EENameFactory getJ2EENameFactory()
    {
        return ivJ2EENameFactory;
    }

    public void setOrbUtils(OrbUtils orbUtils)
    {
        ivOrbUtils = orbUtils;
    }

    public OrbUtils getOrbUtils()
    {
        return ivOrbUtils;
    }

    public void setObjectCopier(ObjectCopier objectCopier)
    {
        ivObjectCopier = objectCopier;
    }

    public ObjectCopier getObjectCopier()
    {
        return ivObjectCopier;
    }

    public void setJ2EENameFactory(J2EENameFactory j2eeNameFactory)
    {
        ivJ2EENameFactory = j2eeNameFactory;
    }

    public StatefulSessionKeyFactory getStatefulSessionKeyFactory()
    {
        return ivStatefulSessionKeyFactory;
    }

    public void setStatefulSessionKeyFactory(StatefulSessionKeyFactory sessionKeyFactory)
    {
        ivStatefulSessionKeyFactory = sessionKeyFactory;
    }

    public StatefulSessionHandleFactory getStatefulSessionHandleFactory()
    {
        return ivStatefulSessionHandleFactory;
    }

    public void setStatefulSessionHandleFactory(StatefulSessionHandleFactory sessionHandleFactory)
    {
        ivStatefulSessionHandleFactory = sessionHandleFactory;
    }

    public WSEJBHandlerResolver getWSEJBHandlerResolver()
    {
        return ivWSEJBHandlerResolver;
    }

    public void setWSEJBHandlerResolver(WSEJBHandlerResolver resolver)
    {
        ivWSEJBHandlerResolver = resolver;
    }

    public StatefulBeanEnqDeq getStatefulBeanEnqDeq()
    {
        return ivStatefulBeanEnqDeq;
    }

    public void setStatefulBeanEnqDeq(StatefulBeanEnqDeq enqDeq)
    {
        ivStatefulBeanEnqDeq = enqDeq;
    }

    public DispatchEventListenerManager getDispatchEventListenerManager()
    {
        return ivDispatchEventListenerManager;
    }

    public void setDispatchEventListenerManager(DispatchEventListenerManager manager)
    {
        ivDispatchEventListenerManager = manager;
    }

    public boolean isSFSBFailoverEnabled()
    {
        return ivSFSBFailoverEnabled;
    }

    public void setSFSBFailoverEnabled(boolean enabled)
    {
        ivSFSBFailoverEnabled = enabled;
    }

    public long getInactivePoolCleanupInterval()
    {
        return ivInactivePoolCleanupInterval;
    }

    public void setInactivePoolCleanupInterval(long interval)
    {
        ivInactivePoolCleanupInterval = interval;
    }

    public long getCacheSize()
    {
        return ivCacheSize;
    }

    public void setCacheSize(long size)
    {
        ivCacheSize = size;
    }

    public long getCacheSweepInterval()
    {
        return ivCacheSweepInterval;
    }

    public void setCacheSweepInterval(long interval)
    {
        ivCacheSweepInterval = interval;
    }

    private EJBRequestCollaborator<?>[] getEJBRequestCollaborators(List<EJBRequestCollaborator<?>> collaborators)
    {
        int size = collaborators.size();
        if (size == 0)
        {
            return null;
        }

        EJBRequestCollaborator<?>[] result = new EJBRequestCollaborator<?>[collaborators.size()];
        collaborators.toArray(result);
        return result;
    }

    public EJBRequestCollaborator<?>[] getAfterActivationCollaborators()
    {
        return getEJBRequestCollaborators(ivAfterActivationCollaborators);
    }

    public void addAfterActivationCollaborator(EJBRequestCollaborator<?> collaborator)
    {
        ivAfterActivationCollaborators.add(collaborator);
    }

    public EJBRequestCollaborator<?>[] getBeforeActivationCollaborators()
    {
        return getEJBRequestCollaborators(ivBeforeActivationCollaborators);
    }

    public void addBeforeActivationCollaborator(EJBRequestCollaborator<?> collaborator)
    {
        ivBeforeActivationCollaborators.add(collaborator);
    }

    public EJBRequestCollaborator<?>[] getBeforeActivationAfterCompletionCollaborators()
    {
        return getEJBRequestCollaborators(ivBeforeActivationAfterCompletionCollaborators);
    }

    public void addBeforeActivationAfterCompletionCollaborator(EJBRequestCollaborator<?> collaborator)
    {
        ivBeforeActivationAfterCompletionCollaborators.add(collaborator);
    }
}
