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
package com.ibm.ws.webcontainer.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.InjectionTarget;
import com.ibm.ws.javaee.dd.common.ResourceBaseGroup;

/**
 *
 */
public abstract class AbstractResourceBaseGroup implements ResourceBaseGroup, InjectionTargetsEditable {

    private String name;

    private String mappedName;

    private List<InjectionTarget> injectionTargets = new ArrayList<InjectionTarget>();

    public AbstractResourceBaseGroup(ResourceBaseGroup resourceGroup) {
        this.name = resourceGroup.getName();
        this.mappedName = resourceGroup.getMappedName();
        this.injectionTargets.addAll(resourceGroup.getInjectionTargets());
    }

    /** {@inheritDoc} */
    @Override
    public String getMappedName() {
        return mappedName;
    }

    /** {@inheritDoc} */
    @Override
    public List<InjectionTarget> getInjectionTargets() {
        return Collections.unmodifiableList(injectionTargets);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public void addInjectionTarget(InjectionTarget injectionTarget) {
        injectionTargets.add(injectionTarget);
    }

    /** {@inheritDoc} */
    @Override
    public void addInjectionTargets(List<InjectionTarget> _injectionTargets) {
        injectionTargets.addAll(_injectionTargets);
    }
}
