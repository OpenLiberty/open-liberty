/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.osgi.internal;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.naming.JavaColonNamespaceBindings;

@Trivial
public class NonCompBindingClassNameProvider implements JavaColonNamespaceBindings.ClassNameProvider<NonCompBinding> {
    public static final JavaColonNamespaceBindings.ClassNameProvider<NonCompBinding> instance = new NonCompBindingClassNameProvider();

    @Override
    public String getBindingClassName(NonCompBinding binding) {
        return binding.binding.getInjectionClassTypeName();
    }
}
