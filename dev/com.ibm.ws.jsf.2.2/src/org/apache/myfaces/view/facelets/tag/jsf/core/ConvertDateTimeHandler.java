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

import java.util.TimeZone;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.convert.Converter;
import javax.faces.convert.DateTimeConverter;
import javax.faces.view.facelets.ConverterConfig;
import javax.faces.view.facelets.ConverterHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributeException;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport;

/**
 * Register a DateTimeConverter instance on the UIComponent associated with the closest parent UIComponent custom
 * action. <p/> See <a target="_new"
 * href="http://java.sun.com/j2ee/javaserverfaces/1.1_01/docs/tlddocs/f/convertDateTime.html">tag documentation</a>.
 * 
 * @deprecated use 
 * @author Jacob Hookom
 * @version $Id: ConvertDateTimeHandler.java 1194861 2011-10-29 10:02:34Z struberg $
 */
@JSFFaceletTag(
        name = "f:convertDateTime",
        bodyContent = "empty", 
        converterClass="javax.faces.convert.DateTimeConverter")
public final class ConvertDateTimeHandler extends ConverterHandler
{

    private final TagAttribute dateStyle;

    private final TagAttribute locale;

    private final TagAttribute pattern;

    private final TagAttribute timeStyle;

    private final TagAttribute timeZone;

    private final TagAttribute type;

    /**
     * @param config
     */
    public ConvertDateTimeHandler(ConverterConfig config)
    {
        super(config);
        this.dateStyle = this.getAttribute("dateStyle");
        this.locale = this.getAttribute("locale");
        this.pattern = this.getAttribute("pattern");
        this.timeStyle = this.getAttribute("timeStyle");
        this.timeZone = this.getAttribute("timeZone");
        this.type = this.getAttribute("type");
    }

    /**
     * Returns a new DateTimeConverter
     * 
     * @see DateTimeConverter
     * @see javax.faces.view.facelets.ConverterHandler#createConverter(javax.faces.view.facelets.FaceletContext)
     */
    protected Converter createConverter(FaceletContext ctx) throws FacesException, ELException, FaceletException
    {
        return ctx.getFacesContext().getApplication().createConverter(DateTimeConverter.CONVERTER_ID);

    }

    /**
     * Implements tag spec, see taglib documentation.
     * 
     * @see org.apache.myfaces.view.facelets.tag.ObjectHandler#setAttributes(javax.faces.view.facelets.FaceletContext, java.lang.Object)
     */
    public void setAttributes(FaceletContext ctx, Object obj)
    {
        DateTimeConverter c = (DateTimeConverter) obj;
        if (this.locale != null)
        {
            c.setLocale(ComponentSupport.getLocale(ctx, this.locale));
        }
        if (this.pattern != null)
        {
            c.setPattern(this.pattern.getValue(ctx));
        }
        else
        {
            if (this.type != null)
            {
                c.setType(this.type.getValue(ctx));
            }
            if (this.dateStyle != null)
            {
                c.setDateStyle(this.dateStyle.getValue(ctx));
            }
            if (this.timeStyle != null)
            {
                c.setTimeStyle(this.timeStyle.getValue(ctx));
            }
        }

        if (this.timeZone != null)
        {
            Object t = this.timeZone.getObject(ctx);
            if (t != null)
            {
                if (t instanceof TimeZone)
                {
                    c.setTimeZone((TimeZone) t);
                }
                else if (t instanceof String)
                {
                    TimeZone tz = TimeZone.getTimeZone((String) t);
                    c.setTimeZone(tz);
                }
                else
                {
                    throw new TagAttributeException(this.tag, this.timeZone,
                                "Illegal TimeZone, must evaluate to either a java.util.TimeZone or String, is type: "
                                + t.getClass());
                }
            }
        }
    }

    protected MetaRuleset createMetaRuleset(Class type)
    {
        return super.createMetaRuleset(type).ignoreAll();
    }
}
