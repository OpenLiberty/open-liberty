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
package configuratorApp.web.observerMethod;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessObserverMethod;

public class ProcessObserverMethodObserver implements Extension {

    void observer(@Observes ProcessObserverMethod<Triangle, ShapeObserver> event) {

        event.configureObserverMethod().priority(ObserverMethod.DEFAULT_PRIORITY + 1);
    }

    void vetoDodecagonObserver(@Observes ProcessObserverMethod<Dodecagon, ShapeObserver> event) {
        event.veto();
    }
}