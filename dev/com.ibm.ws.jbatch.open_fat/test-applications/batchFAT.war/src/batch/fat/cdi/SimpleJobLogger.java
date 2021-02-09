/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.cdi;

import javax.batch.runtime.context.JobContext;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

//
// This scope might not be the most useful for testing (compared to @Dependent).
// But this is what the customer for PI78436 used, so let's verify it.
// Refrain from testing the 'count', which won't scale easily to >1 test.
// 
@ApplicationScoped 
public class SimpleJobLogger {

    @Inject
    private JobContext jobContext;

    private int count = 0;

    public void log(String message) {
        System.out.println(hashCode() + "|" + jobContext.getJobName() + "[" + jobContext.getExecutionId() + "]: " + message);
        System.out.println("count=" + ++count);
    }

}
