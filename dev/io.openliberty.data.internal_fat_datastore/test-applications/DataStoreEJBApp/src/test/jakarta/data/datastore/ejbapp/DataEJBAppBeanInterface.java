/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.datastore.ejbapp;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;

/**
 * Interface that allows DataEJBAppBean to have an Observes Startup method
 * without getting the error:
 * WELD-000088: Observer method must be static or local business method
 */
public interface DataEJBAppBeanInterface {

    /**
     * Add some data on startup.
     */
    void startup(@Observes Startup event);
}
