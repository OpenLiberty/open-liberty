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

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;

/**
 * Register a named facet on the UIComponent associated with the closest parent UIComponent custom action. <p/> See <a
 * target="_new" href="http://java.sun.com/j2ee/javaserverfaces/1.1_01/docs/tlddocs/f/facet.html">tag documentation</a>.
 * 
 * @author Jacob Hookom
 * @version $Id: FacetHandler.java 1187701 2011-10-22 12:21:54Z bommel $
 */
@JSFFaceletTag(
        name = "f:facet",
        bodyContent = "JSP", 
        tagClass="javax.faces.webapp.FacetTag")
public final class FacetHandler extends TagHandler 
    implements javax.faces.view.facelets.FacetHandler
{

    public static final String KEY = "facelets.FACET_NAME";

    protected final TagAttribute name;

    public FacetHandler(TagConfig config)
    {
        super(config);
        this.name = this.getRequiredAttribute("name");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.faces.view.facelets.FaceletHandler#apply(javax.faces.view.facelets.FaceletContext, javax.faces.component.UIComponent)
     */
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException,
            ELException
    {
        if (parent == null)
        {
            throw new TagException(this.tag, "Parent UIComponent was null");
        }
        parent.getAttributes().put(KEY, this.name.getValue(ctx));
        try
        {
            this.nextHandler.apply(ctx, parent);
        }
        finally
        {
            parent.getAttributes().remove(KEY);
        }
    }

    public String getFacetName(FaceletContext ctx)
    {
        return this.name.getValue(ctx);
    }
}
