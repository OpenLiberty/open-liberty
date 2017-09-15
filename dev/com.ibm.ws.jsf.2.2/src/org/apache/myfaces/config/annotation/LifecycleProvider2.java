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
package org.apache.myfaces.config.annotation;

import java.lang.reflect.InvocationTargetException;

import javax.naming.NamingException;

/**
 * Proposed interface to annotation service. An implementation of this class needs to know the appropriate classloader,
 * dependencies to be injected, and lifecycle methods to be called.
 *
 * @version $Rev: 1188686 $ $Date: 2011-10-25 14:59:52 +0000 (Tue, 25 Oct 2011) $
 */
public interface LifecycleProvider2 extends LifecycleProvider
{
    /**
     * Create an object of the class with the supplied name, inject dependencies as appropriate.
     *
     * @param className name of the class of the desired object
     * @return a fully constructed, dependency-injected, and initialized object.
     */
    Object newInstance(String className)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException,
            NamingException, InvocationTargetException;

    /**
     * Call a postConstruct method as appropriate.
     *
     * @param o object to initialize
     */
    void postConstruct(Object o) throws IllegalAccessException, InvocationTargetException;
}
