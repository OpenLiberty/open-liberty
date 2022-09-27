/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.cdi.extensions;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.security.jakartasec.cdi.beans.OpenIdContextBean;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;

/**
 * Produces the OpenIdContext from the CallerSubject for Injection annotations.
 */
public class OpenIdContextProducer {

    private static final TraceComponent tc = Tr.register(OpenIdContextProducer.class);

    @Inject
    OpenIdContextBean openIdContextBean;

    /**
     * Fetch the OpenIdContext from the @OpenIdContextBean. The OpenIdContext must be SessionScoped we so
     * need to load it from a bean.
     *
     * @param injectionPoint
     * @return
     */
    @Produces
    @Dependent
    public OpenIdContext getOpenIdContext(InjectionPoint injectionPoint) {

        if (openIdContextBean == null) {
            // TODO -- log nls message here or is this impossible?

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "openIdContextBean was null, cannot get openIdContext, returning null ");
            }
            return null;
        }

        return openIdContextBean.getOpenIdContext();
    }

}
