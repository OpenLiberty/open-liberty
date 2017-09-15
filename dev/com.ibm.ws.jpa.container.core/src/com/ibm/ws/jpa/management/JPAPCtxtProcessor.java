/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContexts;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.InjectionTarget;
import com.ibm.ws.javaee.dd.common.PersistenceContextRef;
import com.ibm.ws.jpa.JPAAccessor;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessor;

/**
 * A subclass of the InjectionProcessor to handle @PersistenceContext and
 * 
 * @PersistenceContexts annotations.
 */
public class JPAPCtxtProcessor extends InjectionProcessor<PersistenceContext, PersistenceContexts>
{
    private static final TraceComponent tc = Tr.register
                    (JPAPCtxtProcessor.class,
                     JPA_TRACE_GROUP,
                     JPA_RESOURCE_BUNDLE_NAME);

    private final JPAPCtxtAttributeAccessor ivAttributeAccessor;

    public JPAPCtxtProcessor(JPAPCtxtAttributeAccessor attributeAccessor) {
        super(PersistenceContext.class, PersistenceContexts.class);
        ivAttributeAccessor = attributeAccessor;
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
            Tr.entry(tc, "processXML : " + ivNameSpaceConfig.getPersistenceContextRefs());

        // d416151.3 Begins
        List<? extends PersistenceContextRef> pCtxtRefs = ivNameSpaceConfig.getPersistenceContextRefs();
        if (pCtxtRefs != null && pCtxtRefs.size() > 0)
        {
            for (PersistenceContextRef pCtxtRef : pCtxtRefs)
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, " pCtxtRef = " + pCtxtRef);

                // If XML has previously been read and an injection binding with the same
                // jndi name has been created, get the current injection binding and merge
                // the new PersistenceContext Ref into it.
                String jndiName = pCtxtRef.getName();
                InjectionBinding<PersistenceContext> injectionBinding = ivAllAnnotationsCollection.get(jndiName);
                if (injectionBinding != null)
                {
                    ((JPAPCtxtInjectionBinding) injectionBinding).merge(pCtxtRef);
                }
                else
                {
                    injectionBinding = new JPAPCtxtInjectionBinding(pCtxtRef, ivNameSpaceConfig, ivAttributeAccessor); // d662814
                    addInjectionBinding(injectionBinding);
                }

                ((AbstractJPAInjectionBinding<?>) injectionBinding).addRefComponents(jndiName); // F743-30682

                // Process any injection-targets that may be specified.    d429866.1
                // The code takes into account the possibility of duplicate InjectionTargets
                // and will only add if not already present, regardless of whether this is
                // a newly created binding, or a second one being merged.
                List<InjectionTarget> targets = pCtxtRef.getInjectionTargets();
                if (targets != null && !targets.isEmpty())
                {
                    for (InjectionTarget target : targets)
                    {
                        String injectionClassName = target.getInjectionTargetClassName();
                        String injectionName = target.getInjectionTargetName();
                        injectionBinding.addInjectionTarget(EntityManager.class,
                                                            injectionName,
                                                            injectionClassName);
                    }
                }
            }
        }
        // d416151.3 Ends
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processXML : " + ivAllAnnotationsCollection.size());
    }

    /**
     * Returns an annotation specific InjectionBinding associated with
     * the input annotation. <p>
     * 
     * Provides a 'factory' to the base InjectionProcessor for creating
     * annotation specific Binding objects. <p>
     * 
     * @param annotation the annotation to create a binding for.
     * @param compNSConfig component configuration data.
     * 
     * @throws InjectionException when a problem occurs processing
     *             the annotation.
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionProcessor#createInjectionBinding
     **/
    @Override
    public InjectionBinding<PersistenceContext> createInjectionBinding
                    (PersistenceContext annotation, Class<?> instanceClass, Member member, String jndiName)
                                    throws InjectionException
    {
        return new JPAPCtxtInjectionBinding(annotation, ivNameSpaceConfig, ivAttributeAccessor);
    }

    @Override
    /**
     * Resolve input binding's injection data for @ PersistenceContext.
     */
    public void resolve(InjectionBinding<PersistenceContext> binding)
                    throws InjectionException
    {
        JPAPCtxtInjectionBinding pCtxtBinding = (JPAPCtxtInjectionBinding) binding;

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "resolve : " + pCtxtBinding);

        String applName = pCtxtBinding.getApplName();
        String modJarName = pCtxtBinding.getModJarName();
        String puName = pCtxtBinding.getPuName();

        // If this is a WAR module with EJBs, then the isSFSB setting is not reliable,
        // so use a different object factory which knows how to look at the metadata
        // on the thread to determine if running in a stateful bean context.   d658638
        boolean isEJBinWar = ivNameSpaceConfig.getOwningFlow() == HYBRID;
        JPAPuId puId = new JPAPuId(applName, modJarName, puName);

        AbstractJPAComponent jpaComponent = (AbstractJPAComponent) JPAAccessor.getJPAComponent();
        Reference ref = jpaComponent.createPersistenceContextReference
                        (isEJBinWar,
                         puId,
                         ivNameSpaceConfig.getJ2EEName(), // d510184
                         pCtxtBinding.getJndiName(), // d510184
                         pCtxtBinding.isExtendedType(),
                         ivNameSpaceConfig.isSFSB(), // d658638
                         pCtxtBinding.getProperties(),
                         pCtxtBinding.isUnsynchronized());

        pCtxtBinding.setObjects(null, ref); // d446013

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "resolve : " + pCtxtBinding);
    }

    /**
     * Returns the 'name' attribute of the EJB annotation. <p>
     * 
     * The name attribute, if present, is the Jndi Name where the
     * injection object is bound into naming. <p>
     * 
     * Although all injection annotations have a 'name' attribute,
     * the attribute is not present in the base annotation class,
     * so each subclass processor must extract the value. <p>
     * 
     * @param annotation the EJB annotation to extract the name from.
     **/
    // d432816
    @Override
    public String getJndiName(PersistenceContext annotation)
    {
        return annotation.name();
    }

    @Override
    public PersistenceContext[] getAnnotations(PersistenceContexts annotation)
    {
        return annotation.value();
    }

}
