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
package org.apache.myfaces.view.facelets.tag.composite;

import javax.faces.view.EditableValueHolderAttachedObjectTarget;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;

/**
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 808707 $ $Date: 2009-08-28 00:59:52 +0000 (Fri, 28 Aug 2009) $
 */
@JSFFaceletTag(name="composite:editableValueHolder")
public class EditableValueHolderHandler extends AttachedObjectTargetHandler<EditableValueHolderAttachedObjectTarget>
{

    public EditableValueHolderHandler(TagConfig config)
    {
        super(config);
    }

    @Override
    protected EditableValueHolderAttachedObjectTarget createAttachedObjectTarget(
            FaceletContext ctx)
    {
        EditableValueHolderAttachedObjectTargetImpl target = new EditableValueHolderAttachedObjectTargetImpl();
        
        if (_name != null)
        {
            target.setName(_name.getValueExpression(ctx, String.class));
        }
        if (_targets != null)
        {
            target.setTargets(_targets.getValueExpression(ctx, String.class));
        }
        return target;
    }
}
