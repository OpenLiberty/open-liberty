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
package test.concurrent.work;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * An oversimplified mock work item implementation
 */
public class WorkItem {
    private final Future<Work> future;

    public WorkItem(Future<Work> future) {
        this.future = future;
    }

    public Work getResult() throws WorkCompletedException {
        if (future.isDone())
            try {
                return future.get();
            } catch (ExecutionException x) {
                throw new WorkCompletedException(x);
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            }
        else
            return null;
    }
}
