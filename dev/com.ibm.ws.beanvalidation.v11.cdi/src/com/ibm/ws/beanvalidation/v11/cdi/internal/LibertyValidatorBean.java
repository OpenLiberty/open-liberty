/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.v11.cdi.internal;

import javax.enterprise.context.spi.CreationalContext;
import javax.validation.Validator;

import org.apache.bval.cdi.ValidatorBean;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.beanvalidation.BVNLSConstants;

/**
 * This class is used to extend the Apache ValidatorBean for the sole purpose
 * of overriding the create method. Instead of passing in the Validator object
 * when the bean is initialized, we delay the creation of the Validator until
 * create is called. The delay is needed since the server thread doesn't have its
 * metadata and context initialized to a point where creating the Validator will succeed.
 * 
 */
public class LibertyValidatorBean extends ValidatorBean {
    private static final TraceComponent tc = Tr.register(LibertyValidatorBean.class, "BeanValidation", BVNLSConstants.BV_RESOURCE_BUNDLE);

    protected String id = null;

    public LibertyValidatorBean() {
        super(null, null);
    }

    @Override
    public Validator create(CreationalContext<Validator> context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "create");
        Validator validator = ValidationExtensionService.instance().getDefaultValidator();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "create", new Object[] { validator });
        return validator;
    }

    /*
     * Override this method so that a LibertyValidatorBean is stored in the WELD
     * Bean Store keyed on its classname. This allows an injected Validator Bean to
     * be retrieved in both local and server failover scenarios as per defect 774504.
     */
    @Override
    public String getId() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getId");
        if (id == null)
        {
            // Set id to the class name
            id = this.getClass().getName();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getId", new Object[] { id });
        return id;
    }
}
