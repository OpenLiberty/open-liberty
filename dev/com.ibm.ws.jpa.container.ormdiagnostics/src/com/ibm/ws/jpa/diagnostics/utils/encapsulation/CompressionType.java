/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

package com.ibm.ws.jpa.diagnostics.utils.encapsulation;

public enum CompressionType {
    NONE,
    GZIP,
    ZIP;
    
    public String toString() {
        switch (this) {
            case GZIP:
                return "GZIP";
            case ZIP:
                return "ZIP";
            default:
                return "NONE";
        }
    }
    
    public static CompressionType fromString(String s) {
        switch (s) {
            case "GZIP":
                return GZIP;
            case "ZIP":
                return ZIP;
            default:
                return NONE;
        }
    }
}
