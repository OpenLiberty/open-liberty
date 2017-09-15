/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
import static com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration.ReferenceFlowKind.HYBRID;

import java.lang.reflect.Member;
import java.util.List;

import javax.naming.Reference;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.PersistenceUnits;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.InjectionTarget;
import com.ibm.ws.javaee.dd.common.PersistenceUnitRef;
import com.ibm.ws.jpa.JPAAccessor;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessor;

/**
 * A subclass of the InjectionProcessor to handle @PersistenceUnit and
 * 
 * @PersistenceUnits annotations.
 */
public class JPAPUnitProcessor extends InjectionProcessor<PersistenceUnit, PersistenceUnits>
{
    private static final TraceComponent tc = Tr.register(JPAPUnitProcessor.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    public JPAPUnitProcessor()
    {
        super(PersistenceUnit.class, PersistenceUnits.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionProcessor#createInjectionBinding(java.lang.annotation.Annotation, com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration)
     */
    @Override
    public InjectionBinding<PersistenceUnit> createInjectionBinding
                    (PersistenceUnit annotation, Class<?> instanceClass, Member member, String jndiName)
                                    throws InjectionException
    {
        JPAPUnitInjectionBinding injectionBindingResource = new JPAPUnitInjectionBinding(annotation, ivNameSpaceConfig);
        return injectionBindingResource;
    }

    /**
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionProcessor#processXML
     */
    @Override
    public void processXML() throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processXML : " + ivNameSpaceConfig.getPersistenceUnitRefs());

        // d416151.3 Begins
        List<? extends PersistenceUnitRef> pUnitRefs = ivNameSpaceConfig.getPersistenceUnitRefs();
        if (pUnitRefs != null && pUnitRefs.size() > 0)
        {
            for (PersistenceUnitRef pUnitRef : pUnitRefs)
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, " pUnitRef = " + pUnitRef);

                // If XML has previously been read and an injection binding with the same
                // jndi name has been created, get the current injection binding and merge
                // the new PersistenceUnit Ref into it.
                String jndiName = pUnitRef.getName();
                InjectionBinding<PersistenceUnit> injectionBinding = ivAllAnnotationsCollection.get(jndiName);
                if (injectionBinding != null)
                {
                    ((JPAPUnitInjectionBinding) injectionBinding).merge(pUnitRef);
                }
                else
                {
                    injectionBinding = new JPAPUnitInjectionBinding(pUnitRef, ivNameSpaceConfig); // d662814
                    addInjectionBinding(injectionBinding);
                }

                ((AbstractJPAInjectionBinding<?>) injectionBinding).addRefComponents(jndiName); // F743-30682

                // Process any injection-targets that may be specified.    d429866.1
                // The code takes into account the possibility of duplicate InjectionTargets
                // and will only add if not already present, regardless of whether this is
                // a newly created binding, or a second one being merged.
                List<InjectionTarget> targets = pUnitRef.getInjectionTargets();
                if (targets != null && !targets.isEmpty())
                {
                    for (InjectionTarget target : targets)
                    {
                        String injectionClassName = target.getInjectionTargetClassName();
                        String injectionName = target.getInjectionTargetName();
                        injectionBinding.addInjectionTarget(EntityManagerFactory.class, injectionName, injectionClassName);
                    }
                }
            }
        }
        // d416151.3 Ends
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processXML : " + ivAllAnnotationsCollection.size());
    }

    @Override
    public void resolve(InjectionBinding<PersistenceUnit> injectionBinding)
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "resolve : " + injectionBinding);
        JPAPUnitInjectionBinding pUnitBinding = (JPAPUnitInjectionBinding) injectionBinding;

        String applName = pUnitBinding.getApplName();
        String modJarName = pUnitBinding.getModJarName();
        String puName = pUnitBinding.getAnnotation().unitName();

        // If this is a WAR module with EJBs, then the isSFSB setting is not reliable,
        // so use a different object factory which knows how to look at the metadata
        // on the thread to determine if running in a stateful bean context.   F743-30682
        boolean isEJBinWar = ivNameSpaceConfig.getOwningFlow() == HYBRID;
        JPAPuId puId = new JPAPuId(applName, modJarName, puName);

        AbstractJPAComponent jpaComponent = (AbstractJPAComponent) JPAAccessor.getJPAComponent();
        Reference ref = jpaComponent.createPersistenceUnitReference
                        (isEJBinWar,
                         puId,
                         ivNameSpaceConfig.getJ2EEName(), // d510184
                         pUnitBinding.getJndiName(), // d510184
                         ivNameSpaceConfig.isSFSB()); // d416151.3.1

        pUnitBinding.setObjects(null, ref);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "resolve : " + pUnitBinding);
    }

    @Override
    public String getJndiName(PersistenceUnit annotation)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "annotation.name=" + annotation.name());
        return annotation.name();
    }

    @Override
    public PersistenceUnit[] getAnnotations(PersistenceUnits annotation)
    {
        return annotation.value();
    }
}
