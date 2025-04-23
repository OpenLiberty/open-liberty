/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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

package ejbapp1;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.ejb.Stateful;
import jakarta.enterprise.event.Observes;

@Stateful
public class TestObserver implements LocalInterface {

    static AtomicBoolean observed = new AtomicBoolean(false);

    public static void observeRemote(@Observes EJBEvent e) {
        observed.set(true);
    }

    @Override
    public boolean observed() {
        return observed.get();
    }

}
