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
package com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.fallbackbeans;

import javax.enterprise.context.RequestScoped;

import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.interfaces.IExcludedByPropertyBean;

@RequestScoped
public class FallbackForExcludedByPropertyBean implements IExcludedByPropertyBean {
    @Override
    public String getOutput() {
        return "ExcludedByPropertyBean was correctly rejected";
    }

}
