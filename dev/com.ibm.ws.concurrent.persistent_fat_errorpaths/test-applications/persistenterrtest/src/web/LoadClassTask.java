/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.concurrent.Callable;

public class LoadClassTask implements Callable<Class<?>> {

    @Override
    public Class<?> call() throws ClassNotFoundException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        System.out.println("Task thread class loader " + loader);
        return loader.loadClass(PersistentErrorTestServlet.class.getName());
    }
}
