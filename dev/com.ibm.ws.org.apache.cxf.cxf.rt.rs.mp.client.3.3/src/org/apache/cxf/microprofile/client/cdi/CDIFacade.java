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
 */

package org.apache.cxf.microprofile.client.cdi;

import java.util.Optional;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public final class CDIFacade {

    private CDIFacade() {}

    @FFDCIgnore({Throwable.class})
    public static <T> Optional<T> getInstanceFromCDI(Class<T> clazz) {
        T bean;
        try {
            bean = CDIUtils.getInstanceFromCDI(clazz);
        } catch (Throwable t) {
            // ExceptionInInitializerError | NoClassDefFoundError | IllegalStateException expected if CDI is not enabled
            bean = null;
        }
        return Optional.ofNullable(bean);
    }
}