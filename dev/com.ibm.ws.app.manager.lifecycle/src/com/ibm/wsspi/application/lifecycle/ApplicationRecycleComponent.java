/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.application.lifecycle;

import java.util.Set;

/**
 *
 */
public interface ApplicationRecycleComponent {
    /**
     * Returns the context which this recycle component is a part of, or null if it is an independent component
     */
    public ApplicationRecycleContext getContext();

    /**
     * Stops the requested applications and restarts them when a corresponding startApplications()
     * call is invoked. The stop is done in coordination with configuration updates and other users
     * of the application recycler such that no applications are started until all users have driven
     * startApplications() and any in-flight configuration updates are complete, whichever comes
     * last.
     */
    public Set<String> getDependentApplications();
}
