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
package com.ibm.ws.beanvalidation.accessor;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.validation.ValidatorFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.beanvalidation.service.BeanValidation;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

public class BeanValidationAccessor {

    private static final TraceComponent tc = Tr.register(BeanValidationAccessor.class);

    public static ValidatorFactory getValidatorFactory() {
        Bundle bundle = FrameworkUtil.getBundle(BeanValidationAccessor.class);
        BeanValidation bv = AccessController.doPrivileged(new GetServiceAction(bundle));

        if (bv != null) {
            ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            if (cmd != null) {
                return bv.getValidatorFactoryOrDefault(cmd);
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Unable to get ValidatorFactory because ComponentMetaData was null");
            }
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Unable to get ValidatorFactory because BeanValidation service was null");
        }
        return null;
    }

    /**
     * Privileged action for getting a service.
     */
    private static class GetServiceAction implements PrivilegedAction<BeanValidation> {
        private final Bundle bundle;

        private GetServiceAction(Bundle bundle) {
            this.bundle = bundle;
        }

        @Override
        public BeanValidation run() {
            BundleContext bCtx = bundle.getBundleContext();
            ServiceReference<BeanValidation> svcRef = bCtx.getServiceReference(BeanValidation.class);
            return svcRef == null ? null : bCtx.getService(svcRef);
        }
    }
}
