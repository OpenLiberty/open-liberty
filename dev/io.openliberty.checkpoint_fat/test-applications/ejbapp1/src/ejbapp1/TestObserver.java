/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package ejbapp1;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.ejb.Stateful;
import javax.enterprise.event.Observes;

@Stateful
public class TestObserver implements RemoteInterface {

    static AtomicBoolean observed = new AtomicBoolean(false);

    public static void observeRemote(@Observes EJBEvent e) {
        observed.set(true);
    }

    @Override
    public boolean observed() {
        return observed.get();
    }

}
