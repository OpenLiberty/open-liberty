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
package org.apache.myfaces.view.facelets.tag.jstl.core;

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;

/**
 * Catches any Throwable that occurs in its body and optionally 
 * exposes it.
 * 
 * @author Jacob Hookom
 * @version $Id: CatchHandler.java 1187701 2011-10-22 12:21:54Z bommel $
 */
@JSFFaceletTag(name="c:catch")
public final class CatchHandler extends TagHandler
{

    /**
     * Name of the exported scoped variable for the
     * exception thrown from a nested action. The type of the
     * scoped variable is the type of the exception thrown.
     */
    @JSFFaceletAttribute(className="java.lang.String")
    private final TagAttribute var;

    /**
     * @param config
     */
    public CatchHandler(TagConfig config)
    {
        super(config);
        this.var = this.getAttribute("var");
    }

    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException,
            ELException
    {
        try
        {
            this.nextHandler.apply(ctx, parent);
        }
        catch (Exception e)
        {
            if (this.var != null)
            {
                ctx.setAttribute(this.var.getValue(ctx), e);
            }
        }
    }

}
