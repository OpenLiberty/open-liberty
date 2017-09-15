/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.Phaser;

/**
 * Task that submits another task to a completion service.
 * A phaser can optionally be used to encourage multiple submits to occur at the same time.
 * 
 * @param <T>
 */
public class CompletionServiceTask<T> implements Callable<Future<T>> {
    private final Callable<T> callable;
    private final CompletionService<T> completionSvc;
    private final Phaser phaser;

    public CompletionServiceTask(CompletionService<T> completionSvc, Callable<T> callable, Phaser phaser) {
        this.callable = callable;
        this.completionSvc = completionSvc;
        this.phaser = phaser;
    }

    @Override
    public Future<T> call() throws InterruptedException {
        System.out.println("> call " + toString());
        try {
            if (phaser != null)
                phaser.arriveAndAwaitAdvance();
            Future<T> future = completionSvc.submit(callable);
            System.out.println("< call " + toString() + " " + future);
            return future;
        } catch (RuntimeException x) {
            System.out.println("< call " + toString() + " " + x);
            throw x;
        }
    }
}
