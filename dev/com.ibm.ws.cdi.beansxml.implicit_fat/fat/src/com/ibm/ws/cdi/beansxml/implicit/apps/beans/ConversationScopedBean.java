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
package com.ibm.ws.cdi.beansxml.implicit.apps.beans;

import java.io.Serializable;

import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;

import com.ibm.ws.cdi.beansxml.implicit.apps.unannotated.UnannotatedBeanInAllModeBeanArchive;

@ConversationScoped
public class ConversationScopedBean implements Serializable {

    @Inject
    private UnannotatedBeanInAllModeBeanArchive unannotatedBean;

    public void setData(String data) {
        unannotatedBean.setData(data);
    }

}
