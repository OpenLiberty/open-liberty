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
package com.ibm.ws.jaxws.metadata.builder;

import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * Configure the EndpointInfo, the interface provides the abstraction for endpoint configurations from
 * different sources, including annotations, web.xml, webservices.xml, and vendor deployment plan.
 */
public interface EndpointInfoConfigurator {
    /**
     * Prepare for parsing.
     * 
     * @param context
     * @UnableToAdaptException
     */
    void prepare(EndpointInfoBuilderContext context, EndpointInfo endpointInfo) throws UnableToAdaptException;

    /**
     * Configure the EndpointInfo.
     * 
     * @param context
     * @UnableToAdaptException
     */
    void config(EndpointInfoBuilderContext context, EndpointInfo endpointInfo) throws UnableToAdaptException;

    /**
     * Return the phase for the configurator
     * 
     * @return
     */
    Phase getPhase();

    /*
     * Predefine Phases
     */
    public enum Phase {
        PRE_PROCESS_ANNOTATION,
        PROCESS_ANNOTATION,
        POST_PROCESS_ANNOTATION,
        PRE_PROCESS_DESCRIPTOR,
        PROCESS_DESCRIPTOR,
        POST_PROCESS_DESCRIPTOR;
    }

}
