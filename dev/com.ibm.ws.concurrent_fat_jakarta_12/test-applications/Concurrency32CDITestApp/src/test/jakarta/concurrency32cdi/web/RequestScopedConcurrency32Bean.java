/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jakarta.concurrency32cdi.web;

import java.util.concurrent.LinkedBlockingQueue;

import jakarta.enterprise.concurrent.Schedule;
import jakarta.enterprise.context.RequestScoped;

/**
 * A RequestScoped CDI managed bean.
 */
@RequestScoped
public class RequestScopedConcurrency32Bean {

    /**
     * Receives the identity hash code of the RequestScopedConcurrency32Bean
     * instance each time onThirdSecondsFrom1() fires.
     */
    static final LinkedBlockingQueue<Integer> onThirdSecondsFrom1Queue = //
                    new LinkedBlockingQueue<>();

    /**
     * Fires every 3 seconds (at seconds 1, 4, 7, 10, ...). Each invocation
     * records this instance's identity hash code so the test can verify that
     * a fresh bean instance is obtained on each execution (as a @RequestScoped
     * bean should provide), and so the test can confirm the method ran.
     */
    @Schedule(cron = "1/3 * * * * *")
    public void onThirdSecondsFrom1() {
        onThirdSecondsFrom1Queue.add(System.identityHashCode(this));
    }
}
