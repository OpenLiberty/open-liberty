/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.invalid;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.invalid.subpackage.OutOfPackageSearchB;

@ApplicationScoped
public class OutOfPackageSearchA extends OutOfPackageSearchB {

    @Fallback(fallbackMethod = "target")
    public String source(int a, Long b) {
        return "source";
    }

}
