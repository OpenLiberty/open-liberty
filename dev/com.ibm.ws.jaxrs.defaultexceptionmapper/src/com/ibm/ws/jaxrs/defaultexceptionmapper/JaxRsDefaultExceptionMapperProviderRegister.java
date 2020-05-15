/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

 package com.ibm.ws.jaxrs.defaultexceptionmapper;
 
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.TraceObjectField;
import com.ibm.websphere.ras.annotation.TraceOptions;
import com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister;
import com.ibm.ws.ras.instrument.annotation.InjectedFFDC;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component(immediate = true, property = { "service.vendor=IBM" }, configurationPolicy = ConfigurationPolicy.IGNORE, service = {JaxRsProviderRegister.class})
public class JaxRsDefaultExceptionMapperProviderRegister implements JaxRsProviderRegister {

    private List<DefaultExceptionMapperCallback> callbacks = new LinkedList<>();
    @Override
    public void installProvider(boolean clientSide, List<Object> providers, Set<String> features) {
        if (!clientSide) {
            providers.add(new DefaultExceptionMapper(callbacks));
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    public void addCallback(DefaultExceptionMapperCallback callback) {
        callbacks.add(callback);
    }

    public void removeCallback(DefaultExceptionMapperCallback callback) {
        callbacks.remove(callback);
    }

}
