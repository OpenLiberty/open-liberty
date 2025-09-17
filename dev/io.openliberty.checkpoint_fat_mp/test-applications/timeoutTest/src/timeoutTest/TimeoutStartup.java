/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package timeoutTest;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class TimeoutStartup {

    @Inject
    TimeoutService timeoutTester;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        System.out.println("TIMEOUT CALLED");
        timeoutTester.spawnTimeoutThread();
    }

}
