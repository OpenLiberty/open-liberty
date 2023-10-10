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

import static org.junit.Assert.fail;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.BiPredicate;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 */
public class ThreadPublisher extends SubmissionPublisher<ThreadSnapshot> {

    public ThreadPublisher(Executor ex) {
        super(ex, 3);
    }

    @Override
    public int offer(ThreadSnapshot item, BiPredicate<Subscriber<? super ThreadSnapshot>, ? super ThreadSnapshot> onDrop) {
        try {
            new InitialContext().lookup("java:comp/env/entry1");
        } catch (NamingException e) {
            fail("Could not lookup context on publisher thread");
        }
        return super.offer(item, onDrop);
    }

}
