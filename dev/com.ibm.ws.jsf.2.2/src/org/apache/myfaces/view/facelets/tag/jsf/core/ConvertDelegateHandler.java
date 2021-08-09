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

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.convert.Converter;
import javax.faces.view.facelets.ConverterConfig;
import javax.faces.view.facelets.ConverterHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.TagAttribute;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;

/**
 * Register a named Converter instance on the UIComponent associated with the closest parent UIComponent custom action.
 * <p/> See <a target="_new" href="http://java.sun.com/j2ee/javaserverfaces/1.1_01/docs/tlddocs/f/converter.html">tag
 * documentation</a>.
 * 
 * @author Jacob Hookom
 * @version $Id: ConvertDelegateHandler.java 1188694 2011-10-25 15:07:44Z struberg $
 */
@JSFFaceletTag(
        name = "f:converter",
        bodyContent = "empty", 
        tagClass="org.apache.myfaces.taglib.core.ConverterImplTag")
public final class ConvertDelegateHandler extends ConverterHandler
{

    private final TagAttribute converterId;

    /**
     * @param config
     */
    public ConvertDelegateHandler(ConverterConfig config)
    {
        super(config);
        this.converterId = this.getAttribute("converterId");
    }

    /**
     * Uses the specified "converterId" to pull an instance from the Application
     * 
     * @see javax.faces.application.Application#createComponent(java.lang.String)
     * @see org.apache.myfaces.view.facelets.tag.jsf.ConverterHandler#createConverter(javax.faces.view.facelets.FaceletContext)
     */
    protected Converter createConverter(FaceletContext ctx) throws FacesException, ELException, FaceletException
    {
        return ctx.getFacesContext().getApplication().createConverter(this.getConverterId(ctx));
    }

    protected MetaRuleset createMetaRuleset(Class type)
    {
        return super.createMetaRuleset(type).ignoreAll();
    }

    @Override
    public String getConverterId(FaceletContext ctx)
    {
        if (converterId == null)
        {
            return null;
        }
        return converterId.getValue(ctx);
    }
}
