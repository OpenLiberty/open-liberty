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
package com.ibm.ws.request.interrupt.status;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.request.interrupt.internal.InterruptibleThreadInfrastructureImpl;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM", service = { InterruptibleThreadStatus.class })
public class InterruptibleThreadStatusImpl implements InterruptibleThreadStatus {
	
	private InterruptibleThreadInfrastructureImpl iti = null;
	
    /** OSGi set method */
    @Reference
    protected void setInterruptibleThreadInfrastructure(InterruptibleThreadInfrastructureImpl iti) {
        this.iti = iti;
    }

    /** OSGi unset method */
    protected void unsetInterruptibleThreadInfrastructure(InterruptibleThreadInfrastructureImpl iti) {
        this.iti = null;
    }

    /** {@inheritDoc} */
	@Override
	public List<InterruptibleThreadObjectStatus> getStatusArray() {
		return iti.getStatusArray();
	}

}
