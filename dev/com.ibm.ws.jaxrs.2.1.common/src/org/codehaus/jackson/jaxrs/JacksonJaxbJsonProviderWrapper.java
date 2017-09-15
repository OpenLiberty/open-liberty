/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.codehaus.jackson.jaxrs;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * this provider is a wrapper of the codehaus jackson provider
 * to make the customized the fastxml jackson with high priority.
 */
@Provider
@Consumes({ "*/*" })
@Produces({ "*/*" })
public class JacksonJaxbJsonProviderWrapper extends JacksonJaxbJsonProvider {

    public JacksonJaxbJsonProviderWrapper()
    {
        this(null, DEFAULT_ANNOTATIONS);
    }

    public JacksonJaxbJsonProviderWrapper(Annotations[] annotationsToUse)
    {
        this(null, annotationsToUse);
    }

    public JacksonJaxbJsonProviderWrapper(ObjectMapper mapper, Annotations[] annotationsToUse)
    {
        super(mapper, annotationsToUse);
    }

}
