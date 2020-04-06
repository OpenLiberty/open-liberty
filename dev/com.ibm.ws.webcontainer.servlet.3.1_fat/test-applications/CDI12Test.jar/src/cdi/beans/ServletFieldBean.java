/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.beans;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import javax.interceptor.Interceptors;

@RequestScoped
@Named
@ServletType
public class ServletFieldBean extends FieldBean {

    @Override
    @Interceptors(BeanInterceptor.class)
    public void setData(String data) {
        super.setData(data);
    }

    @Override
    public String getData() {
        return this.getClass() + (value == null ? ":" : ":" + value);
    }

}
