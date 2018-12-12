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
package com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class GenericSearchA extends GenericSearchB<Long> {

    @Fallback(fallbackMethod = "target")
    public String source(int a, Long b) {
        throw new RuntimeException("source");
    }

}
