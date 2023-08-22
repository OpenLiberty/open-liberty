/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.cdi.extension.spi.test.bundle.with.internals.buildcompatible;

//Even though this is only run in EE10 we use javax otherwise the MANIFEST.MF imports both javax and jakarta.enterprise.context; then blows up when the transformer doesn't filter out the duplicate
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BCExtraBean {

    @Override
    public String toString() {
        return "bean registered in a BCE registered via SPI";
    }
}
