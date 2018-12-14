/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.service.scope;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
public class ResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        try {
            // sleep 5 seconds to give the server every opportunity to run the @PreDestroy method
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        if (Boolean.parseBoolean(responseContext.getHeaderString("Test-Initial"))
                        && Stage.PREDESTROY_METHOD_EXIT.equals(App.mostRecent)) {
            responseContext.setStatus(555);
            responseContext.setEntity("PreDestroy method invoked before filter exit");
        }
        App.mostRecent = Stage.FILTER_EXIT;
    }
}
