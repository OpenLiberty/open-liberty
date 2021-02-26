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

package com.ibm.tra.ann;

import javax.resource.spi.ConfigProperty;

public class ConfigPropertyValidationActivationSuperClass {

    @ConfigProperty(description = "ActSpec Mode",
                    supportsDynamicUpdates = false, confidential = true)
    private Integer mode;

    private String protocol;

    public Integer getMode() {
        return mode;
    }

    public void setMode(Integer mode) {
        this.mode = mode;
    }

    public String getProtocol() {
        return protocol;
    }

    @ConfigProperty(description = "Comm Protocol",
                    supportsDynamicUpdates = false, confidential = true)
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

}
