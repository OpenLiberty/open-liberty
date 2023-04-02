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

import javax.annotation.PostConstruct;

//import javax.ejb.Singleton;
//import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.inject.Inject;

// Temporary change while investigating support for EJB types that require
// transactional behavior during app startup, such at @Startup and @Singleton.
//@Startup
//@Singleton
@Stateless
public class TimeoutStartup {

    @Inject
    TimeoutService timeoutTester;

    @PostConstruct
    public void getProperties() {
        System.out.println("TIMEOUT CALLED");
        timeoutTester.spawnTimeoutThread();
    }

}
