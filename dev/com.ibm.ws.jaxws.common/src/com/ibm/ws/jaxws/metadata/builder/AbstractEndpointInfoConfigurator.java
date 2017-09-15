/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.metadata.builder;

/**
 * Provide basic implementation for the EndpointInfoConfigurator
 */
public abstract class AbstractEndpointInfoConfigurator implements EndpointInfoConfigurator {

    protected Phase phase;

    public AbstractEndpointInfoConfigurator() {
        //if phase if not specified, use PRE_PROCESS_ANNOTATION as default
        this.phase = EndpointInfoConfigurator.Phase.PRE_PROCESS_ANNOTATION;
    }

    public AbstractEndpointInfoConfigurator(Phase phase) {
        this.phase = phase;
    }

    @Override
    public Phase getPhase() {
        return phase;
    }

}
