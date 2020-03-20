/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.liberty;

import javax.enterprise.inject.spi.Extension;

import org.jboss.weld.bootstrap.spi.Metadata;

public class ExtensionMetaData implements Metadata<Extension> {

    private final Extension extension;

    public ExtensionMetaData(Extension extension) {
        this.extension = extension;
    }

    @Override
    public String getLocation() {
        return "A SPI class registered through WebSphereCDIExtensionMetaData";
    }

    @Override
    public Extension getValue() {
        return extension;
    }

}
