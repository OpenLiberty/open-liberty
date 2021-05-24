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
package com.ibm.ws.cdi.beansxml.fat.apps.webBeansBeansXmlInterceptors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * A bean that will be intercepted
 */
@RequestScoped
@Named("interceptedBean")
@BasicInterceptorBinding
public class InterceptedBean {

    private String lastInterceptedBy = null;

    public void setLastInterceptedBy(String interceptorClassName) {
        this.lastInterceptedBy = interceptorClassName;
    }

    public String getMessage() {
        if (this.lastInterceptedBy == null) {
            return "Not Intercepted";
        }
        return "Last Intercepted by: " + this.lastInterceptedBy;
    }

}
