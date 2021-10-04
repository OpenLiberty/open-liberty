/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.defaultexceptionmapper.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.jaxrs.defaultexceptionmapper.DefaultExceptionMapperCallback;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class DefaultExceptionMapperCallbackTracker {

    private static DefaultExceptionMapperCallbackTracker instance;

    public static Collection<DefaultExceptionMapperCallback> getCallbacks() {
        List<DefaultExceptionMapperCallback> result = null;

        DefaultExceptionMapperCallbackTracker instance = DefaultExceptionMapperCallbackTracker.instance;
        if (instance != null) {
            result = instance.callbacks;
        }

        if (result == null) {
            result = Collections.emptyList();
        }

        return result;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile List<DefaultExceptionMapperCallback> callbacks;

    @Activate
    private void activate() {
        instance = this;
    }

    @Deactivate
    private void deactivate() {
        if (instance == this) {
            instance = null;
        }
    }

}
