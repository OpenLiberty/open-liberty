/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.cdi.dependent;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import com.ibm.ws.cdi.CDIServiceUtils;

public class DependentBeanExtension implements Extension
{
    @SuppressWarnings("UnusedDeclaration")
    public void registerAnnotatedTypes(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager)
    {
        AnnotatedType at = beanManager.createAnnotatedType(RequestDependentBeanStorage.class);
        beforeBeanDiscovery.addAnnotatedType(at, CDIServiceUtils.getAnnotatedTypeIdentifier(at, this.getClass()));
    }
}
