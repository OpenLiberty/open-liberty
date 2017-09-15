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
package org.apache.myfaces.view.facelets.tag.jsf.core;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TextHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.view.facelets.tag.TagHandlerUtils;

/**
 * Handler for f:verbatim
 * 
 * @author Adam Winer
 * @version $Id: VerbatimHandler.java 1187701 2011-10-22 12:21:54Z bommel $
 */
@JSFFaceletTag(
        name = "f:verbatim",
        bodyContent = "empty", 
        tagClass="org.apache.myfaces.taglib.core.VerbatimTag")
public final class VerbatimHandler extends ComponentHandler
{
    public VerbatimHandler(ComponentConfig config)
    {
        super(config);
    }

    public void onComponentCreated(FaceletContext ctx, UIComponent c, UIComponent parent)
    {
        StringBuffer content = new StringBuffer();
        for (TextHandler handler : TagHandlerUtils.findNextByType(nextHandler, TextHandler.class))
        {
            content.append(handler.getText(ctx));
        }

        c.getAttributes().put("value", content.toString());
        c.getAttributes().put("escape", Boolean.FALSE);
        c.setTransient(true);
    }

    public void applyNextHandler(FaceletContext ctx, UIComponent c)
    {
    }
}
