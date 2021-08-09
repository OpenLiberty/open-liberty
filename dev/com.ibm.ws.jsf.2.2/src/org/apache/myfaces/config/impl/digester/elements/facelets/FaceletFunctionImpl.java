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
package org.apache.myfaces.config.impl.digester.elements.facelets;

import java.io.Serializable;
import org.apache.myfaces.config.element.facelets.FaceletFunction;

/**
 *
 */
public class FaceletFunctionImpl extends FaceletFunction implements Serializable
{
    private String functionName;
    private String functionClass;
    private String functionSignature;

    public FaceletFunctionImpl()
    {
    }

    public FaceletFunctionImpl(String functionName, String functionClass, String functionSignature)
    {
        this.functionName = functionName;
        this.functionClass = functionClass;
        this.functionSignature = functionSignature;
    }

    public String getFunctionName()
    {
        return functionName;
    }

    public void setFunctionName(String functionName)
    {
        this.functionName = functionName;
    }

    public String getFunctionClass()
    {
        return functionClass;
    }

    public void setFunctionClass(String functionClass)
    {
        this.functionClass = functionClass;
    }

    public String getFunctionSignature()
    {
        return functionSignature;
    }

    public void setFunctionSignature(String functionSignature)
    {
        this.functionSignature = functionSignature;
    }
    
}
