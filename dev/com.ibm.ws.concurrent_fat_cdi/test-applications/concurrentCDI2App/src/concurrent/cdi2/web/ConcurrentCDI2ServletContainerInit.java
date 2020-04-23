/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.cdi2.web;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;

public class ConcurrentCDI2ServletContainerInit implements ServletContainerInitializer {
    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext ctx) {
        Instance<ExecutorService> instance = CDI.current().select(ExecutorService.class);
        ExecutorService executor = instance.get();

        Future<Integer> future = executor.submit(new Callable<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        });

        if (false) // TODO this causes deadlock with com/ibm/ws/webcontainer/webapp/WebApp$1
            try {
                future.get();
            } catch (Exception x) {
                throw new RuntimeException(x);
            }

        Instance<ConcurrentCDI2AppScopedBean> bean = CDI.current().select(ConcurrentCDI2AppScopedBean.class);
        bean.get().setServletContainerInitFuture(future);

        System.out.println("ServletContainerInitializer.onStartup invoked, using " + executor);
    }
}