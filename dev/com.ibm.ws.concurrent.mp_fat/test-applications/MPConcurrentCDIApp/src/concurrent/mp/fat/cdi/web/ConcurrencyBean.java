/*******************************************************************************
 * Copyright (c) 2018,2019 IBM Corporation and others.

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
package concurrent.mp.fat.cdi.web;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;

@ApplicationScoped
public class ConcurrencyBean {
    @Inject
    @MyQualifier
    ManagedExecutor myQualifier;

    public ManagedExecutor getMyQualifier() {
        return myQualifier;
    }

    @Produces
    @ApplicationScoped
    @MyQualifier
    public ManagedExecutor appDefinedQualifier() {
        ManagedExecutor exec = ManagedExecutor.builder().maxAsync(5).build();
        System.out.println("Application produced ManagedExecutor with @MyQualifier qualifier: " + exec);
        return exec;
    }

}
