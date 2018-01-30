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
package cdi12.classexclusion.test.excludedpackagetree.subpackage;

import javax.enterprise.context.RequestScoped;

import cdi12.classexclusion.test.interfaces.IExcludedPackageTreeBean;

@RequestScoped
public class ExcludedPackageTreeBean implements IExcludedPackageTreeBean {
    @Override
    public String getOutput() {
        return "ExcludedPackageTreeBean was correctly injected";
    }

}
