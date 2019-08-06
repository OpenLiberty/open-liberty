/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package failover1serv.web;

import java.io.Serializable;
import java.util.concurrent.Callable;

import com.ibm.websphere.concurrent.persistent.TaskIdAccessor;

/**
 * A simple task that increments a counter each time it runs.
 */
public class IncTask implements Callable<Integer>, Serializable {
    private static final long serialVersionUID = 1L;

    int counter;

    @Override
    public Integer call() throws Exception {
        ++counter;
        System.out.println("IncTask " + TaskIdAccessor.get() + " execution attempt #" + counter);
        return counter;
    }
}
