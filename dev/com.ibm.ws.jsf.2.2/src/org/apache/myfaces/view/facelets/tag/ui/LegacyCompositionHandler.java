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
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.TemplateClient;
import org.apache.myfaces.view.facelets.el.VariableMapperWrapper;
import org.apache.myfaces.view.facelets.tag.TagHandlerUtils;

/**
 * NOTE: This implementation is provided for compatibility reasons and
 * it is considered faulty. It is enabled using
 * org.apache.myfaces.STRICT_JSF_2_FACELETS_COMPATIBILITY web config param.
 * Don't use it if EL expression caching is enabled.
 * 
 * @author Jacob Hookom
 * @version $Id: CompositionHandler.java,v 1.14 2008/07/13 19:01:42 rlubke Exp $
 */
//@JSFFaceletTag(name="ui:composition")
public final class LegacyCompositionHandler extends TagHandler implements TemplateClient
{

    //private static final Logger log = Logger.getLogger("facelets.tag.ui.composition");
    private static final Logger log = Logger.getLogger(CompositionHandler.class.getName());

    public final static String NAME = "composition";

    /**
     * The resolvable URI of the template to use. The content within the composition tag will 
     * be used in populating the template specified.
     */
    //@JSFFaceletAttribute(
    //        name="template",
    //        className="javax.el.ValueExpression",
    //        deferredValueType="java.lang.String")
    protected final TagAttribute _template;

    protected final Map<String, DefineHandler> _handlers;

    protected final LegacyParamHandler[] _params;

    /**
     * @param config
     */
    public LegacyCompositionHandler(TagConfig config)
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

            Collection<LegacyParamHandler> params = TagHandlerUtils.findNextByType(nextHandler, 
                    LegacyParamHandler.class);
            if (!params.isEmpty())
            {
                int i = 0;
                _params = new LegacyParamHandler[params.size()];
                for (LegacyParamHandler handler : params)
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
            VariableMapper orig = ctx.getVariableMapper();
            AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
            actx.extendClient(this);
            if (_params != null)
            {
                VariableMapper vm = new VariableMapperWrapper(orig);
                ctx.setVariableMapper(vm);
                for (int i = 0; i < _params.length; i++)
                {
                    _params[i].apply(ctx, parent);
                }
            }

            try
            {
                ctx.includeFacelet(parent, _template.getValue(ctx));
            }
            finally
            {
                actx.popExtendedClient(this);
                ctx.setVariableMapper(orig);
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
