/*
 * Copyright 2013 International Business Machines Corp.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.jbatch.container.services.impl;

import javax.security.auth.Subject;

import com.ibm.jbatch.spi.BatchSecurityHelper;

public class NoOpBatchSecurityHelper implements BatchSecurityHelper {

    private static final String SUBMITTER = "NOTSET";

    public NoOpBatchSecurityHelper() {}

    @Override
    public String getRunAsUser() {
        return SUBMITTER;
    }

    @Override
    public Subject getRunAsSubject() {
        return null;
    }

}
