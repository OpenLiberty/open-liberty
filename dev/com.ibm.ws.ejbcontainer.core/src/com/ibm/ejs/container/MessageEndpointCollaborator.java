/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.util.Map;

/**
 * Collaborator interface that allows implementers to be called for pre and post MDB invocations.
 */
public interface MessageEndpointCollaborator {

    /** Map key to the Activation specification ID */
    public static final String KEY_ACTIVATION_SPEC_ID = "activationSpecification";

    /** Map key to the j2ee name. */
    public static final String KEY_J2EE_NAME = "j2eeName";

    /**
     * Processes pre MDB invocation logic.
     *
     * @param contextData The context data associated with the MDB dispatch.
     * @return The context data to be used when processing postInvoke.
     */
    public Map<String, Object> preInvoke(Map<String, Object> contextData);

    /**
     * Processes post MDB invocation logic.
     *
     * @param contextData The context data associated with the MDB dispatch.
     */
    public void postInvoke(Map<String, Object> contextData);
}