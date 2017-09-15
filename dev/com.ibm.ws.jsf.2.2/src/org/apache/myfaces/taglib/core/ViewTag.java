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
package org.apache.myfaces.taglib.core;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.render.RenderKitFactory;
import javax.faces.webapp.UIComponentELTag;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.jstl.core.Config;

import org.apache.myfaces.application.jsp.ServletViewResponseWrapper;
import org.apache.myfaces.shared.util.LocaleUtils;

/**
 * @author Manfred Geiler (latest modification by $Author: struberg $)
 * @author Bruno Aranda (JSR-252)
 * @version $Revision: 1188235 $ $Date: 2011-10-24 17:09:33 +0000 (Mon, 24 Oct 2011) $
 */
public class ViewTag extends UIComponentELTag
{
    //private static final Log log = LogFactory.getLog(ViewTag.class);
    private static final Logger log = Logger.getLogger(ViewTag.class.getName());

    @Override
    public String getComponentType()
    {
        return UIViewRoot.COMPONENT_TYPE;
    }

    @Override
    public String getRendererType()
    {
        return null;
    }

    private ValueExpression _locale;
    private ValueExpression _renderKitId;

    private MethodExpression _beforePhase;
    private MethodExpression _afterPhase;

    public void setLocale(ValueExpression locale)
    {
        _locale = locale;
    }

    public void setRenderKitId(ValueExpression renderKitId)
    {
        _renderKitId = renderKitId;
    }

    public void setBeforePhase(MethodExpression beforePhase)
    {
        _beforePhase = beforePhase;
    }

    public void setAfterPhase(MethodExpression afterPhase)
    {
        _afterPhase = afterPhase;
    }

    @Override
    public int doStartTag() throws JspException
    {
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("entering ViewTag.doStartTag");
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        Object response = facesContext.getExternalContext().getResponse();
        if (response instanceof ServletViewResponseWrapper)
        {
            try
            {
                pageContext.getOut().flush();
                ((ServletViewResponseWrapper)response).flushToWrappedResponse();
            }
            catch (IOException e)
            {
                throw new JspException("Can't write content above <f:view> tag" + " " + e.getMessage());
            }
        }

        int retVal = super.doStartTag();

        Config.set(pageContext.getRequest(), Config.FMT_LOCALE, facesContext.getViewRoot().getLocale());

        if (log.isLoggable(Level.FINEST))
        {
            log.finest("leaving ViewTag.doStartTag");
        }
        return retVal;
    }

    @Override
    public int doEndTag() throws JspException
    {
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("entering ViewTag.doEndTag");
        }
        int retVal = super.doEndTag();

        if (log.isLoggable(Level.FINEST))
        {
            log.finest("leaving ViewTag.doEndTag");
        }
        return retVal;
    }

    @Override
    public int doAfterBody() throws JspException
    {
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("entering ViewTag.doAfterBody");
        }

        UIComponent verbatimComp = createVerbatimComponentFromBodyContent();

        if (verbatimComp != null)
        {
            FacesContext.getCurrentInstance().getViewRoot().getChildren().add(verbatimComp);
        }

        if (log.isLoggable(Level.FINEST))
        {
            log.finest("leaving ViewTag.doAfterBody");
        }
        return EVAL_PAGE;
    }

    @Override
    protected void setProperties(UIComponent component)
    {
        super.setProperties(component);

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ELContext elContext = facesContext.getELContext();

        UIViewRoot viewRoot = (UIViewRoot)component;

        // locale
        if (_locale != null)
        {
            Locale locale;
            if (_locale.isLiteralText())
            {
                locale = LocaleUtils.toLocale(_locale.getValue(elContext).toString());
            }
            else
            {
                component.setValueExpression("locale", _locale);

                Object localeValue = _locale.getValue(elContext);

                if (localeValue instanceof Locale)
                {
                    locale = (Locale)localeValue;
                }
                else if (localeValue instanceof String)
                {
                    locale = LocaleUtils.toLocale((String)localeValue);
                }
                else
                {
                    if (localeValue != null)
                    {
                        throw new IllegalArgumentException("Locale or String class expected. Expression: " + _locale
                                + ". Return class: " + localeValue.getClass().getName());
                    }

                    throw new IllegalArgumentException("Locale or String class expected. Expression: " + _locale
                            + ". Return value null");
                }
            }
            viewRoot.setLocale(locale);
            Config.set(pageContext.getRequest(), Config.FMT_LOCALE, locale);
        }

        // renderkitId
        if (_renderKitId != null)
        {
            if (_renderKitId.isLiteralText())
            {
                viewRoot.setRenderKitId(_renderKitId.getValue(elContext).toString());
            }
            else
            {
                viewRoot.setValueExpression("renderKitId", _renderKitId);
                viewRoot.setRenderKitId(null);
            }
        }
        else if (viewRoot.getRenderKitId() == null)
        {
            String defaultRenderKitId = facesContext.getApplication().getDefaultRenderKitId();

            if (defaultRenderKitId != null)
            {
                viewRoot.setRenderKitId(defaultRenderKitId);
            }
            else
            {
                viewRoot.setRenderKitId(RenderKitFactory.HTML_BASIC_RENDER_KIT);
            }
        }

        // beforePhase
        if (_beforePhase != null)
        {
            if (_beforePhase.isLiteralText())
            {
                throw new FacesException("Invalid method expression for attribute 'beforePhase' in the view tag: "
                        + _beforePhase.getExpressionString());
            }

            viewRoot.setBeforePhaseListener(_beforePhase);

        }

        // afterPhase
        if (_afterPhase != null)
        {
            if (_afterPhase.isLiteralText())
            {
                throw new FacesException("Invalid method expression for attribute 'beforePhase' in the view tag: "
                        + _afterPhase.getExpressionString());
            }

            viewRoot.setAfterPhaseListener(_afterPhase);

        }
    }
}
