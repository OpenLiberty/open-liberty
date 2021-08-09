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
package org.apache.myfaces.view.facelets.tag.ui;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.TemplateClient;
import org.apache.myfaces.view.facelets.tag.TagHandlerUtils;

/**
 * TODO: REFACTOR - This class could easily use a common parent with DecoratorHandler
 * 
 * @author Jacob Hookom
 * @version $Id: CompositionHandler.java 1545903 2013-11-27 01:26:02Z lu4242 $
 */
@JSFFaceletTag(name="ui:composition")
public final class CompositionHandler extends TagHandler implements TemplateClient
{

    //private static final Logger log = Logger.getLogger("facelets.tag.ui.composition");
    private static final Logger log = Logger.getLogger(CompositionHandler.class.getName());

    public final static String NAME = "composition";

    /**
     * The resolvable URI of the template to use. The content within the composition tag will 
     * be used in populating the template specified.
     */
    @JSFFaceletAttribute(
            name="template",
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String")
    protected final TagAttribute _template;

    protected final Map<String, DefineHandler> _handlers;

    protected final ParamHandler[] _params;

    /**
     * @param config
     */
    public CompositionHandler(TagConfig config)
    {
        super(config);
        _template = getAttribute("template");
        if (_template != null)
        {
            _handlers = new HashMap<String, DefineHandler>();
            for (DefineHandler handler : TagHandlerUtils.findNextByType(nextHandler, DefineHandler.class))
            {
                _handlers.put(handler.getName(), handler);
                if (log.isLoggable(Level.FINE))
                {
                    log.fine(tag + " found Define[" + handler.getName() + "]");
                }
            }

            Collection<ParamHandler> params = TagHandlerUtils.findNextByType(nextHandler, ParamHandler.class);
            if (!params.isEmpty())
            {
                int i = 0;
                _params = new ParamHandler[params.size()];
                for (ParamHandler handler : params)
                {
                    _params[i++] = handler;
                }
            }
            else
            {
                _params = null;
            }
        }
        else
        {
            _params = null;
            _handlers = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.faces.view.facelets.FaceletHandler#apply(javax.faces.view.facelets.FaceletContext,
     * javax.faces.component.UIComponent)
     */
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException,
            ELException
    {
        if (_template != null)
        {
            //VariableMapper orig = ctx.getVariableMapper();
            //if (_params != null)
            //{
            //    VariableMapper vm = new VariableMapperWrapper(orig);
            //    ctx.setVariableMapper(vm);
            //    for (int i = 0; i < _params.length; i++)
            //    {
            //        _params[i].apply(ctx, parent);
            //    }
            //}
            AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
            FaceletCompositionContext fcc = FaceletCompositionContext.getCurrentInstance(ctx);
            actx.extendClient(this);
            if (_params != null)
            {
                String uniqueId = fcc.generateUniqueComponentId();
                //VariableMapper vm = new VariableMapperWrapper(orig);
                //ctx.setVariableMapper(vm);
                for (int i = 0; i < _params.length; i++)
                {
                    _params[i].apply(ctx, parent, _params[i].getName(ctx), _params[i].getValue(ctx), uniqueId);
                }
            }

            try
            {
                ctx.includeFacelet(parent, _template.getValue(ctx));
            }
            finally
            {
                actx.popExtendedClient(this);
                //ctx.setVariableMapper(orig);
            }
        }
        else
        {
            this.nextHandler.apply(ctx, parent);
        }
    }

    public boolean apply(FaceletContext ctx, UIComponent parent, String name) throws IOException, FacesException,
            FaceletException, ELException
    {
        if (name != null)
        {
            if (_handlers == null)
            {
                return false;
            }
            
            DefineHandler handler = _handlers.get(name);
            if (handler != null)
            {
                handler.applyDefinition(ctx, parent);
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            this.nextHandler.apply(ctx, parent);
            return true;
        }
    }

}
