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
package cdi12.classexclusion.test.excludedpackage;

import javax.enterprise.context.RequestScoped;

import cdi12.classexclusion.test.interfaces.IExcludedPackageBean;

@RequestScoped
public class ExcludedPackageBean implements IExcludedPackageBean {
    @Override
    public String getOutput() {
        return "ExcludedPackageBean was correctly injected";
    }

}
