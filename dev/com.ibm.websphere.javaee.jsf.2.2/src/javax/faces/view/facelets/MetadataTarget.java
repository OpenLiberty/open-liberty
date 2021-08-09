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
package javax.faces.view.facelets;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

/**
 * Information used with MetaRule for determining how and what Metadata should be wired.
 */
public abstract class MetadataTarget
{
    /**
     * @param name
     * @return
     */
    public abstract PropertyDescriptor getProperty(String name);

    /**
     * @param name
     * @return
     */
    public abstract Class getPropertyType(String name);

    /**
     * @param name
     * @return
     */
    public abstract Method getReadMethod(String name);

    /**
     * @return
     */
    public abstract Class getTargetClass();

    /**
     * @param name
     * @return
     */
    public abstract Method getWriteMethod(String name);

    /**
     * @param type
     * @return
     */
    public abstract boolean isTargetInstanceOf(Class type);
}
