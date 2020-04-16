/*******************************************************************************
 * Copyright (c) 2013,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.context.app;

import java.io.Serializable;
import java.util.Map;

import javax.enterprise.concurrent.ContextService;
import javax.naming.InitialContext;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Simple implementation of both AppTask and AppWork
 */
public class AppWorkerTask implements AppTask, AppWork, Serializable {
    private static final long serialVersionUID = -246709994859918391L;

    // hacky way to get bundle context for the app to use
    private static final BundleContext bundleContext = FrameworkUtil.getBundle(AppWorkerTask.class.getClassLoader().getClass()).getBundleContext();

    // get the value for each of the parameters from the map service
    @Override
    public Object doTask(Object... params) throws Exception {
        String[] results = new String[params.length];
        @SuppressWarnings("rawtypes")
        ServiceReference<Map> mapSvcRef = bundleContext.getServiceReference(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, String> mapSvc = bundleContext.getService(mapSvcRef);
        try {
            for (int i = 0; i < params.length; i++)
                results[i] = mapSvc.get(params[i]);
        } finally {
            bundleContext.ungetService(mapSvcRef);
        }
        return results.length == 1 ? results[0] : results;
    }

    // get the execution properties for the first parameter (which must be a contextual proxy)
    @Override
    public Object doWork(Object... params) throws Exception {
        ContextService contextSvc = (ContextService) new InitialContext().lookup("concurrent/NumContextSvc2");
        Map<String, String> execProps = contextSvc.getExecutionProperties(params[0]);
        return execProps;
    }
}
