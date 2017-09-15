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
import org.apache.myfaces.config.element.facelets.FaceletConverterTag;

/**
 *
 */
public class FaceletConverterTagImpl extends FaceletConverterTag implements Serializable
{
    private String converterId;
    private String handlerClass;

    public FaceletConverterTagImpl()
    {
    }

    public FaceletConverterTagImpl(String converterId)
    {
        this.converterId = converterId;
    }

    public FaceletConverterTagImpl(String converterId, String handlerClass)
    {
        this.converterId = converterId;
        this.handlerClass = handlerClass;
    }

    public String getConverterId()
    {
        return converterId;
    }

    public void setConverterId(String converterId)
    {
        this.converterId = converterId;
    }

    public String getHandlerClass()
    {
        return handlerClass;
    }

    public void setHandlerClass(String handlerClass)
    {
        this.handlerClass = handlerClass;
    }
    
}
