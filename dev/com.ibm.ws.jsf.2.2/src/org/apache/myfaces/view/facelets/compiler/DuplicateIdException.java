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
package org.apache.myfaces.view.facelets.compiler;

import javax.faces.FacesException;
import javax.faces.component.UIComponent;

/**
 * Indicates duplicate id as specified in spec 7.7.3 State Saving Methods.
 * 
 * @author martinkoci
 */
public class DuplicateIdException extends FacesException
{

    private final UIComponent firstComponent;

    private final UIComponent secondComponent;

    public DuplicateIdException(String message, UIComponent firstComponent, UIComponent secondComponent)
    {
        super(message);
        this.firstComponent = firstComponent;
        this.secondComponent = secondComponent;
    }

    public UIComponent getFirstComponent()
    {
        return firstComponent;
    }

    public UIComponent getSecondComponent()
    {
        return secondComponent;
    }

}
