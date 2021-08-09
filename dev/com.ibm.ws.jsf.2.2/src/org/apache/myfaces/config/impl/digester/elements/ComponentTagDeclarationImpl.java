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
package org.apache.myfaces.config.impl.digester.elements;

import java.io.Serializable;
import org.apache.myfaces.config.element.ComponentTagDeclaration;

/**
 *
 * @author lu4242
 */
public class ComponentTagDeclarationImpl extends ComponentTagDeclaration implements Serializable
{
    private String componentType;
    private String namespace;
    private String tagName;

    public ComponentTagDeclarationImpl(String componentType, String namespace, String tagName)
    {
        this.componentType = componentType;
        this.namespace = namespace;
        this.tagName = tagName;
    }

    /**
     * @return the namespace
     */
    public String getNamespace()
    {
        return namespace;
    }

    /**
     * @param namespace the namespace to set
     */
    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }

    /**
     * @return the tagName
     */
    public String getTagName()
    {
        return tagName;
    }

    /**
     * @param tagName the tagName to set
     */
    public void setTagName(String tagName)
    {
        this.tagName = tagName;
    }

    /**
     * @return the componentType
     */
    public String getComponentType()
    {
        return componentType;
    }

    /**
     * @param componentType the componentType to set
     */
    public void setComponentType(String componentType)
    {
        this.componentType = componentType;
    }
}
