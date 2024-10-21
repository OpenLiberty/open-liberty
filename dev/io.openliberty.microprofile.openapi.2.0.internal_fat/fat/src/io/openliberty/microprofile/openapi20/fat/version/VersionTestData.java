/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.fat.version;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "VersionTestData")
public class VersionTestData {

    @Schema(maximum = "16", exclusiveMaximum = true)
    private int hexDigit;

    public VersionTestData(int hexDigit) {
        super();
        this.hexDigit = hexDigit;
    }

    public int getHexDigit() {
        return hexDigit;
    }

}
