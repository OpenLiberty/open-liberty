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
import org.apache.myfaces.config.element.facelets.FaceletBehaviorTag;

/**
 *
 */
public class FaceletBehaviorTagImpl extends FaceletBehaviorTag implements Serializable
{
    private String behaviorId;
    private String handlerClass;

    public FaceletBehaviorTagImpl()
    {
    }

    public FaceletBehaviorTagImpl(String behaviorId)
    {
        this.behaviorId = behaviorId;
    }

    public FaceletBehaviorTagImpl(String behaviorId, String handlerClass)
    {
        this.behaviorId = behaviorId;
        this.handlerClass = handlerClass;
    }

    public String getBehaviorId()
    {
        return behaviorId;
    }

    public void setBehaviorId(String behaviorId)
    {
        this.behaviorId = behaviorId;
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
