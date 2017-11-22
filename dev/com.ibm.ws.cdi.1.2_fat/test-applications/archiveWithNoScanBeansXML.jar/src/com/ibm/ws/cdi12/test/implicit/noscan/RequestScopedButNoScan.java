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
package com.ibm.ws.cdi12.test.implicit.noscan;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import com.ibm.ws.cdi12.test.beansXML.UnannotatedBeanInAllModeBeanArchive;

@RequestScoped
public class RequestScopedButNoScan {

    @Inject
    private UnannotatedBeanInAllModeBeanArchive unannotatedBean;

    public void setData(String data) {
        unannotatedBean.setData(data);
    }
}
