/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package reactiveapp.web;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import javax.naming.NamingException;

/**
 *
 */
public class ThreadPublisher extends SubmissionPublisher<ContextCDL> {

    public ThreadPublisher(Executor ex, BiConsumer<? super Subscriber<? super ContextCDL>, ? super Throwable> handler) {
        super(ex, 3, handler);
    }

    @Override
    public int offer(ContextCDL item, BiPredicate<Subscriber<? super ContextCDL>, ? super ContextCDL> onDrop) {
        try {
            item.checkContext();
            return super.offer(item, onDrop);
        } catch (NamingException e) {
            closeExceptionally(e);
        }
        return -1;
    }

}
