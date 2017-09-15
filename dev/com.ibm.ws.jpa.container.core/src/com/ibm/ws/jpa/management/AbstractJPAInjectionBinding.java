/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.management;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;

/**
 * Base class for PersistenceUnit and PersistenceContext injection binding.
 */
abstract class AbstractJPAInjectionBinding<A extends Annotation>
                extends InjectionBinding<A>
{
    private static final TraceComponent tc = Tr.register(AbstractJPAInjectionBinding.class,
                                                         JPAConstants.JPA_TRACE_GROUP,
                                                         JPAConstants.JPA_RESOURCE_BUNDLE_NAME);

    /**
     * True if this binding was created during injection processing that
     * included multiple components.
     */
    private boolean ivMultipleComponents; // d662814.2

    /**
     * The list of components that have declared a dependency on this binding.
     */
    private Set<String> ivComponents;

    JPAPuId ivPuId;

    AbstractJPAInjectionBinding(A annotation, String name, String unitName, ComponentNameSpaceConfiguration nameSpaceConfig)
        throws InjectionConfigurationException
    {
        super(annotation, nameSpaceConfig);

        if (name.startsWith("java:global/")) // d696076
        {
            Tr.error(tc, "INVALID_JAVA_GLOBAL_REF_CWWJP0043E",
                     name,
                     nameSpaceConfig.getDisplayName(),
                     nameSpaceConfig.getModuleName(),
                     nameSpaceConfig.getApplicationName());
            throw new InjectionConfigurationException(
                            "The " + name +
                                            " persistence reference is declared by the " + nameSpaceConfig.getDisplayName() +
                                            " component in the " + nameSpaceConfig.getModuleName() +
                                            " module of the " + nameSpaceConfig.getApplicationName() +
                                            " application, but java:global persistence references are not valid.");
        }

        setJndiName(name);

        // Note that this will create a JPAPuId with a null module name for
        // java:app persistence references.
        J2EEName j2eeName = ivNameSpaceConfig.getJ2EEName();
        this.ivPuId = new JPAPuId(j2eeName.getApplication(),
                        j2eeName.getModule(), // d410616
                        unitName);
    }

    /**
     * Returns application name associated to a specific persistence reference.
     */
    final String getApplName()
    {
        return ivPuId.getApplName();
    }

    /**
     * Returns module jar name associated to a specific persistence reference.
     */
    final String getModJarName()
    {
        return ivPuId.getModJarName();
    }

    /**
     * Sets the persistence unit name.
     */
    final void setPuName(String puName)
    {
        ivPuId.setPuName(puName);
    }

    /**
     * Returns the persistence unit name.
     */
    final String getPuName()
    {
        return ivPuId.getPuName();
    }

    @Override
    public void addInjectionClass(Class<?> klass)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "addInjectionClass: " + getJndiName() + ": " + klass);

        Map<Class<?>, Collection<String>> classesToComponents = ivNameSpaceConfig.getClassesToComponents();
        if (classesToComponents != null)
        {
            addComponents(classesToComponents.get(klass));
        }
    }

    @Override
    public void addInjectionTarget(Member member)
                    throws InjectionException
    {
        super.addInjectionTarget(member);
        addInjectionClass(member.getDeclaringClass());
    }

    protected void addRefComponents(String jndiName)
    {
        Map<String, Collection<String>> refsToComponents = ivNameSpaceConfig.getPersistenceRefsToComponents();
        if (refsToComponents != null)
        {
            addComponents(refsToComponents.get(jndiName));
        }
    }

    private void addComponents(Collection<String> components)
    {
        // d662814.2 - Set a variable to track whether or not refs-to-comps or
        // classes-to-comps was non-null.  WAR classes and web.xml are not
        // tracked, but containsComponent should not return true for them.
        ivMultipleComponents = true;

        if (components != null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "addComponents: " + getJndiName() + ": " + components);

            if (ivComponents == null)
            {
                ivComponents = new HashSet<String>();
            }
            ivComponents.addAll(components);
        }
    }

    /**
     * Returns true if the specified component declared a dependency on this
     * binding.
     */
    public boolean containsComponent(String compName)
    {
        // d662814.2 - If this binding was for a single component, then we can
        // return true unconditionally.  Otherwise, check if the component
        // contributed to this binding.
        return !ivMultipleComponents ||
               (ivComponents != null && ivComponents.contains(compName));
    }
}
