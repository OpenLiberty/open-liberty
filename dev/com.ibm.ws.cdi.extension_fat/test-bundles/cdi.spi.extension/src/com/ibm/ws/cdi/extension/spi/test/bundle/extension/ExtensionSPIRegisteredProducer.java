/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.spi.test.bundle.extension;

import javax.enterprise.inject.Produces;

public class ExtensionSPIRegisteredProducer {

    @Produces
    public MyExtensionString createMyString() {
        return new MyExtensionString("Injection from a producer registered in a CDI extension that was registered through the SPI");
    }
}
