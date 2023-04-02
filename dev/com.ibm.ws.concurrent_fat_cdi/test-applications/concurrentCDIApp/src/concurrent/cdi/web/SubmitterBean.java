/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
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
package concurrent.cdi.web;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import jakarta.enterprise.context.RequestScoped;

import javax.naming.InitialContext;

@RequestScoped
public class SubmitterBean {
    public Future<?> submit(Callable<?> task) throws Exception {
        ExecutorService executor = (ExecutorService) new InitialContext().lookup("java:comp/DefaultManagedExecutorService");
        return executor.submit(task);
    }
}
