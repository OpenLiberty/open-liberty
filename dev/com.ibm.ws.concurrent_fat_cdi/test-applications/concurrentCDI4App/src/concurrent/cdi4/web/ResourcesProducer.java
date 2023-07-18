/*******************************************************************************
 * Copyright (c) 2017,2023 IBM Corporation and others.
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
package concurrent.cdi4.web;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
@ManagedExecutorDefinition(name = "java:global/concurrent/allcontextclearedexecutor",
                           context = "java:global/concurrent/allcontextcleared")
public class ResourcesProducer {

    @PostConstruct
    public void init() {
        System.out.println("Initialized bean: " + this);
    }

    @Resource(name = "java:app/env/concurrent/sampleExecutorRef",
              lookup = "concurrent/sampleExecutor")
    @Produces
    private ManagedExecutorService exec;

}
