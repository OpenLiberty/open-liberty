/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
#*******************************************************************************
# Copyright (c) 2025 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#*******************************************************************************
*/
package org.apache.wss4j.common.util;

// Liberty Change: This is a backport with no Liberty specific changes. 
import java.security.AccessController;
import java.security.PrivilegedAction;


public final class FIPSUtils {

    private static boolean isFIPSEnabled = false;
    private static final String FIPS_ENABLED = "fips.enabled";
    
    static {
        isFIPSEnabled = Boolean.parseBoolean(AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty(FIPS_ENABLED);
            }
        }));
        
    }
    
    public static boolean isFIPSEnabled() {
        return isFIPSEnabled;
    }
}
