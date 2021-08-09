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

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.view.AttachedObjectHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;

/**
 * @since 2.0
 */
@JSFFaceletTag
public abstract class FaceletsAttachedObjectHandler extends DelegatingMetaTagHandler implements AttachedObjectHandler
{
    /**
     * 
     */
    public FaceletsAttachedObjectHandler(TagConfig config)
    {
        super(config);
    }

    /**
     * {@inheritDoc}
     */
    public final void applyAttachedObject(FacesContext context, UIComponent parent)
    {
        //Just redirect to delegate handler
        getAttachedObjectHandlerHelper().applyAttachedObject(context, parent);
    }
    
    /**
     * Return the delegate handler for this instance. Note that this suppose
     * delegate tag handlers wrapping this class should implement AttachedObjectHandler
     * interface.
     * 
     * @return
     */
    protected final AttachedObjectHandler getAttachedObjectHandlerHelper()
    {
        return (AttachedObjectHandler) getTagHandlerDelegate();
    }

    /**
     * {@inheritDoc}
     */
    @JSFFaceletAttribute
    public final String getFor()
    {
        //Just redirect to delegate handler
        return getAttachedObjectHandlerHelper().getFor();
    }
}
