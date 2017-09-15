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
package org.apache.myfaces.view;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.ViewDeclarationLanguageFactory;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguageStrategy;
import org.apache.myfaces.view.jsp.JspViewDeclarationLanguageStrategy;

/**
 * This is the default VDL factory used as of JSF 2.0, it tries to use Facelet VDL whenever possible, 
 * but fallback on JSP if required.
 * 
 * @author Simon Lessard (latest modification by $Author: lu4242 $)
 * @version $Revision: 1545492 $ $Date: 2013-11-26 01:19:50 +0000 (Tue, 26 Nov 2013) $
 *
 * @since 2.0
 */
public class ViewDeclarationLanguageFactoryImpl extends ViewDeclarationLanguageFactory
{
    /**
     * Disable facelets VDL from the current application project. 
     */
    @JSFWebConfigParam(since="2.0", defaultValue="false", expectedValues="true,false", group="viewhandler")
    public static final String PARAM_DISABLE_JSF_FACELET = "javax.faces.DISABLE_FACELET_JSF_VIEWHANDLER";

    private static final String FACELETS_1_VIEW_HANDLER = "com.sun.facelets.FaceletViewHandler";

    private static final Logger LOGGER = Logger.getLogger(ViewDeclarationLanguageFactoryImpl.class.getName());
    
    private volatile boolean _initialized;
    private volatile ViewDeclarationLanguageStrategy[] _supportedLanguages;
    
    /**
     * 
     */
    public ViewDeclarationLanguageFactoryImpl()
    {
        _initialized = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ViewDeclarationLanguage getViewDeclarationLanguage(String viewId)
    {
        if (!_initialized)
        {
            initialize();
        }
        
        for (ViewDeclarationLanguageStrategy strategy : _supportedLanguages)
        {
            if (strategy.handles(viewId))
            {
                return strategy.getViewDeclarationLanguage();
            }
        }
        
        return null;
    }
    
    /**
     * Initialize the supported view declaration languages.
     */
    private synchronized void initialize()
    {
        if (!_initialized)
        {
            FacesContext context = FacesContext.getCurrentInstance();

            if (isFacelets2Enabled(context))
            {
                logWarningIfLegacyFaceletViewHandlerIsPresent(context);

                if (MyfacesConfig.getCurrentInstance(
                        context.getExternalContext()).isSupportJSPAndFacesEL())
                {
                    _supportedLanguages = new ViewDeclarationLanguageStrategy[2];
                    _supportedLanguages[0] = new FaceletViewDeclarationLanguageStrategy();
                    _supportedLanguages[1] = new JspViewDeclarationLanguageStrategy();
                }
                else
                {
                    _supportedLanguages = new ViewDeclarationLanguageStrategy[1];
                    _supportedLanguages[0] = new FaceletViewDeclarationLanguageStrategy();
                }
            }
            else
            {
                // Support JSP only
                _supportedLanguages = new ViewDeclarationLanguageStrategy[1];
                _supportedLanguages[0] = new JspViewDeclarationLanguageStrategy();
            }

            _initialized = true;
        }
    }
    
    /**
     * Determines if the current application uses Facelets-2.
     * To accomplish that it looks at the init param javax.faces.DISABLE_FACELET_JSF_VIEWHANDLER,
     * 
     * @param context the <code>FacesContext</code>
     * @return <code>true</code> if the current application uses the built in Facelets-2,
     *         <code>false</code> otherwise (e.g. it uses Facelets-1 or only JSP).
     */
    private boolean isFacelets2Enabled(FacesContext context)
    {
        String param = context.getExternalContext().getInitParameter(PARAM_DISABLE_JSF_FACELET);
        boolean facelets2ParamDisabled = (param != null && Boolean.parseBoolean(param.toLowerCase()));
        
        return !facelets2ParamDisabled;
    }
    
    /**
     * If the Facelets-1 ViewHandler com.sun.facelets.FaceletViewHandler is present <b>AND</b>
     * the new Facelets-2 is <b>NOT</b> disabled, we log a <code>WARNING</code>. 
     * 
     * @param context the <code>FacesContext</code>
     */
    private void logWarningIfLegacyFaceletViewHandlerIsPresent(FacesContext context)
    {
        boolean facelets1ViewHandlerPresent
                = context.getApplication().getViewHandler().getClass().getName().equals(FACELETS_1_VIEW_HANDLER);

        if (facelets1ViewHandlerPresent)
        {
            if (LOGGER.isLoggable(Level.WARNING))
            {
                LOGGER.log(Level.WARNING, "Your faces-config.xml contains the " + FACELETS_1_VIEW_HANDLER + " class."
                    + "\nYou need to remove it since you have not disabled the \"new\" Facelets-2 version with the "
                    + PARAM_DISABLE_JSF_FACELET + " context parameter");
            }
        }
    }
}
