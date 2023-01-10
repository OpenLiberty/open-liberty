/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.apps.spi;

import javax.inject.Inject;

import com.ibm.ws.cdi.extension.spi.test.bundle.annotations.NewBDA;

@NewBDA
public class WellPlacedBean {

    @Inject
    MissPlacedBean two;

    public String toString() {
        return two.toString();
    }

}
