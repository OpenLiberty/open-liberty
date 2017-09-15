/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

import com.ibm.wsspi.injectionengine.factory.InjectionObjectFactory;

/**
 * Provides a Container a mechanism to include associated context data
 * with the Object that is the target of injection, for use in {@link InjectionObjectFactory} implementations provided by the
 * corresponding Container. <p>
 *
 * This permits object factories to be sensitive to the target of the
 * injection; such as the ability to associate the object being
 * injected with the target of the injection. <p>
 *
 * The ability to pass context data to the Object factories is largely
 * for performance, to avoid passing information via thread context
 * data; however, this may also be useful for processing that is
 * unique to injection processing as opposed to a naming lookup. <p>
 *
 * Note that this context data will only be available to an
 * InjectionObjectFactory implementation during injection, and will not
 * be available during a Naming lookup. <p>
 */
public interface InjectionTargetContext
{
    /**
     * Returns the requested context data associated with the injection target
     * instance, or null if unavailable. <p>
     *
     * The available context data will depend on the Container that is
     * requesting injection. For example, EJB Container may provide
     * EJBContext and TimerService, etc. <p>
     *
     * The provided context data is intended for use in the {@link InjectionObjectFactory} implementations provided by the
     * corresponding Container. <p>
     */
    <T> T getInjectionTargetContextData(Class<T> data);
}
