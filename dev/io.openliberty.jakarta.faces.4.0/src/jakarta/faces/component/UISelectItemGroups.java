/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package jakarta.faces.component;

import jakarta.el.ValueExpression;
import jakarta.faces.context.FacesContext;
import java.util.Collections;
import org.apache.myfaces.core.api.shared.MessageUtils;
import org.apache.myfaces.core.api.shared.CommonHtmlEvents;
import org.apache.myfaces.core.api.shared.CommonHtmlAttributes;


// Generated from class jakarta.faces.component._UISelectItemGroups.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class UISelectItemGroups extends UISelectItems
{

    static public final String COMPONENT_FAMILY =
        "jakarta.faces.SelectItemGroups";
    static public final String COMPONENT_TYPE =
        "jakarta.faces.SelectItemGroups";

    //BEGIN CODE COPIED FROM jakarta.faces.component._UISelectItemGroups
    public java.lang.Object getValue()
    {
        FacesContext context = getFacesContext();
        java.util.List<jakarta.faces.model.SelectItemGroup> groups = new java.util.ArrayList<>();

        org.apache.myfaces.core.api.shared.SelectItemsUtil.createSelectItems(context,
                this,
                super.getValue(),
                jakarta.faces.model.SelectItemGroup::new,
                selectItemGroup ->
        {
            selectItemGroup.setSelectItems(
                    org.apache.myfaces.core.api.shared.SelectItemsUtil.collectSelectItems(context, this));
            groups.add(selectItemGroup);
        });
        
        return groups;
    }


    //END CODE COPIED FROM jakarta.faces.component._UISelectItemGroups

    public UISelectItemGroups()
    {
        setRendererType(null);
    }

    @Override
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }







    enum PropertyKeys
    {
    }

 }
