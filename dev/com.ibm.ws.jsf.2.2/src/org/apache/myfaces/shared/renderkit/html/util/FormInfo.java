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
package org.apache.myfaces.shared.renderkit.html.util;

import javax.faces.component.UIComponent;

public class FormInfo
{
    private final UIComponent form;
    private final String formName;
    
    public FormInfo(final UIComponent form, final String formName)
    {
        this.form = form;
        this.formName = formName;
    }

    public UIComponent getForm()
    {
        return form;
    }

    public String getFormName()
    {
        return formName;
    }
}
