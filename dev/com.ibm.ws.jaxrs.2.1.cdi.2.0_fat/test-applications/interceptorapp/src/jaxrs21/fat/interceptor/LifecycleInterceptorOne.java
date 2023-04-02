/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.interceptor;

import javax.annotation.PostConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@LifecycleInterceptableOne
@Interceptor
public class LifecycleInterceptorOne {

    @PostConstruct
    public void interceptPostConstrcut(InvocationContext cxt) {
        BagOfInterceptors.lifecycleInterceptors.get().add(LifecycleInterceptorOne.class.getSimpleName());
        ResourceApplicationScoped.lifecycleInterceptorsInvoked.add(LifecycleInterceptorOne.class.getSimpleName());
    }
}
