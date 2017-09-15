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
package jsf.flow.beans;

import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;

@ManagedBean
public class InitializerBean {

    public InitializerBean() {}

    public void initialize() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<Object, Object> inboundParameters = facesContext.getApplication().getFlowHandler().getCurrentFlowScope();

        // See if the inbound-parameter is set correctly.
        String testParameter = (String) inboundParameters.get("testValue");
        if ((testParameter == null) || !testParameter.contains("test string"))
            throw new IllegalArgumentException("initializer:   did NOT find inbound-parameter");
    }
}