/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.spi.test.app;

import javax.inject.Inject;

import com.ibm.ws.cdi.misplaced.spi.test.bundle.annotations.NewBDA;

@NewBDA
public class MissPlacedBean {

    public String toString() {
        return "A bean created by an annotation defined by the SPI in a different bundle, injected into a bean created by an annotation defined by the spi in the same bundle, intercepted by two interceptors defined by the SPI one from each bundle";
    }

}
