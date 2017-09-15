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
import org.apache.myfaces.config.element.facelets.FaceletValidatorTag;

/**
 *
 */
public class FaceletValidatorTagImpl extends FaceletValidatorTag implements Serializable
{
    private String validatorId;
    private String handlerClass;

    public FaceletValidatorTagImpl()
    {
    }

    public FaceletValidatorTagImpl(String validatorId)
    {
        this.validatorId = validatorId;
    }

    public FaceletValidatorTagImpl(String validatorId, String handlerClass)
    {
        this.validatorId = validatorId;
        this.handlerClass = handlerClass;
    }

    public String getValidatorId()
    {
        return validatorId;
    }

    public void setValidatorId(String validatorId)
    {
        this.validatorId = validatorId;
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
