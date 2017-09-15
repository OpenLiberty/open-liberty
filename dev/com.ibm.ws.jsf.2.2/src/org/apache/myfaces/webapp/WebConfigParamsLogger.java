// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
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
package org.apache.myfaces.webapp;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.ProjectStage;
import javax.faces.context.FacesContext;

import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.util.StringUtils;
import org.apache.myfaces.shared.util.WebConfigParamUtils;

public class WebConfigParamsLogger
{
    private static final Logger log = Logger.getLogger(WebConfigParamsLogger.class.getName());

    public static void logWebContextParams(FacesContext facesContext)
    {
        String logCommand = WebConfigParamUtils.getStringInitParameter(
                facesContext.getExternalContext(), 
                AbstractFacesInitializer.INIT_PARAM_LOG_WEB_CONTEXT_PARAMS, AbstractFacesInitializer.INIT_PARAM_LOG_WEB_CONTEXT_PARAMS_DEFAULT);
        
        if ( logCommand.equals("false") || 
             (logCommand.equals("auto") && !facesContext.isProjectStage(ProjectStage.Development) && !facesContext.isProjectStage(ProjectStage.Production) )
           )
        {
            //No log if is disabled or is in auto mode and project stage is UnitTest or SystemTest
            return;
        }
        
        MyfacesConfig myfacesConfig = MyfacesConfig.getCurrentInstance(facesContext.getExternalContext());
        
        if (myfacesConfig.isTomahawkAvailable())
        {
            if(myfacesConfig.isMyfacesImplAvailable())
            {
                if(log.isLoggable(Level.INFO))
                {
                    log.info("Starting up Tomahawk on the MyFaces-JSF-Implementation");
                }
            }

            if(myfacesConfig.isRiImplAvailable())
            {
                if(log.isLoggable(Level.INFO))
                {
                    log.info("Starting up Tomahawk on the RI-JSF-Implementation.");
                }
            }
        }
        else
        {
            if (log.isLoggable(Level.INFO))
            {
                log.info("Tomahawk jar not available. Autoscrolling, DetectJavascript, AddResourceClass and CheckExtensionsFilter are disabled now.");
            }
        }

        if(myfacesConfig.isRiImplAvailable() && myfacesConfig.isMyfacesImplAvailable())
        {
            log.severe("Both MyFaces and the RI are on your classpath. Please make sure to use only one of the two JSF-implementations.");
        }
        
        

        if (log.isLoggable(Level.INFO))
        {
            log.info("Scanning for context init parameters not defined. It is not necessary to define them all into your web.xml, " +
                     "they are just provided here for informative purposes. To disable this messages set " +
                     AbstractFacesInitializer.INIT_PARAM_LOG_WEB_CONTEXT_PARAMS + " config param to 'false'");
            String paramValue = null;

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.RESOURCE_EXCLUDES");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.RESOURCE_EXCLUDES' found, using default value '.class .jsp .jspx .properties .xhtml .groovy'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.STATE_SAVING_METHOD");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.STATE_SAVING_METHOD' found, using default value 'server'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("server,client",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equalsIgnoreCase(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'javax.faces.STATE_SAVING_METHOD' (='" + paramValue + "'), using default value 'server'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.FULL_STATE_SAVING_VIEW_IDS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.FULL_STATE_SAVING_VIEW_IDS' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.PARTIAL_STATE_SAVING");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.PARTIAL_STATE_SAVING' found, using default value 'true (false with 1.2 webapps)'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true,false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'javax.faces.PARTIAL_STATE_SAVING' (='" + paramValue + "'), using default value 'true (false with 1.2 webapps)'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.SERIALIZE_SERVER_STATE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.SERIALIZE_SERVER_STATE' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true,false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'javax.faces.SERIALIZE_SERVER_STATE' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.DEFAULT_SUFFIX");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.DEFAULT_SUFFIX' found, using default value '.xhtml .view.xml .jsp'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.FACELETS_SUFFIX");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.FACELETS_SUFFIX' found, using default value '.xhtml'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.FACELETS_VIEW_MAPPINGS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.FACELETS_VIEW_MAPPINGS' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("DISABLE_FACELET_JSF_VIEWHANDLER");
            if (paramValue == null)
            {
                log.info("No context init parameter 'DISABLE_FACELET_JSF_VIEWHANDLER' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.FACELETS_BUFFER_SIZE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.FACELETS_BUFFER_SIZE' found, using default value '1024'.");
            }
            else
            {
                try
                {
                    java.lang.Integer.valueOf(paramValue);
                }
                catch(Exception e)
                {
                    if (log.isLoggable(Level.WARNING))
                    {
                        log.warning("Wrong value in context init parameter 'javax.faces.FACELETS_BUFFER_SIZE' (='" + paramValue + "'), using default value '1024'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.FACELETS_DECORATORS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.FACELETS_DECORATORS' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.FACELETS_LIBRARIES");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.FACELETS_LIBRARIES' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.FACELETS_REFRESH_PERIOD");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.FACELETS_REFRESH_PERIOD' found, using default value '-1'.");
            }
            else
            {
                try
                {
                    java.lang.Long.valueOf(paramValue);
                }
                catch(Exception e)
                {
                    if (log.isLoggable(Level.WARNING))
                    {
                        log.warning("Wrong value in context init parameter 'javax.faces.FACELETS_REFRESH_PERIOD' (='" + paramValue + "'), using default value '-1'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.FACELETS_SKIP_COMMENTS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.FACELETS_SKIP_COMMENTS' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.HONOR_CURRENT_COMPONENT_ATTRIBUTES");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.HONOR_CURRENT_COMPONENT_ATTRIBUTES' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'javax.faces.HONOR_CURRENT_COMPONENT_ATTRIBUTES' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.VALIDATE_EMPTY_FIELDS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.VALIDATE_EMPTY_FIELDS' found, using default value 'auto'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("auto, true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'javax.faces.VALIDATE_EMPTY_FIELDS' (='" + paramValue + "'), using default value 'auto'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'javax.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'javax.faces.DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.ENUM_CONVERTER_ALLOW_STRING_PASSTROUGH");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.ENUM_CONVERTER_ALLOW_STRING_PASSTROUGH' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true,false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.ENUM_CONVERTER_ALLOW_STRING_PASSTROUGH' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.validator.DISABLE_DEFAULT_BEAN_VALIDATOR");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.validator.DISABLE_DEFAULT_BEAN_VALIDATOR' found, using default value 'true'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'javax.faces.validator.DISABLE_DEFAULT_BEAN_VALIDATOR' (='" + paramValue + "'), using default value 'true'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.CONFIG_FILES");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.CONFIG_FILES' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.LIFECYCLE_ID");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.LIFECYCLE_ID' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.ERROR_HANDLER");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.ERROR_HANDLER' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.CHECKED_VIEWID_CACHE_SIZE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.CHECKED_VIEWID_CACHE_SIZE' found, using default value '500'.");
            }
            else
            {
                try
                {
                    java.lang.Integer.valueOf(paramValue);
                }
                catch(Exception e)
                {
                    if (log.isLoggable(Level.WARNING))
                    {
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.CHECKED_VIEWID_CACHE_SIZE' (='" + paramValue + "'), using default value '500'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.CHECKED_VIEWID_CACHE_ENABLED");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.CHECKED_VIEWID_CACHE_ENABLED' found, using default value 'true'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.CHECKED_VIEWID_CACHE_ENABLED' (='" + paramValue + "'), using default value 'true'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.PRETTY_HTML");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.PRETTY_HTML' found, using default value 'true'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false, on, off, yes, no",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equalsIgnoreCase(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.PRETTY_HTML' (='" + paramValue + "'), using default value 'true'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.ALLOW_JAVASCRIPT");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.ALLOW_JAVASCRIPT' found, using default value 'true'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false, on, off, yes, no",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equalsIgnoreCase(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.ALLOW_JAVASCRIPT' (='" + paramValue + "'), using default value 'true'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.CONFIG_REFRESH_PERIOD");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.CONFIG_REFRESH_PERIOD' found, using default value '2'.");
            }
            else
            {
                try
                {
                    java.lang.Long.valueOf(paramValue);
                }
                catch(Exception e)
                {
                    if (log.isLoggable(Level.WARNING))
                    {
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.CONFIG_REFRESH_PERIOD' (='" + paramValue + "'), using default value '2'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VIEWSTATE_JAVASCRIPT");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.VIEWSTATE_JAVASCRIPT' found, using default value 'false'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RENDER_VIEWSTATE_ID");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RENDER_VIEWSTATE_ID' found, using default value 'true'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false, on, off, yes, no",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equalsIgnoreCase(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.RENDER_VIEWSTATE_ID' (='" + paramValue + "'), using default value 'true'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.STRICT_XHTML_LINKS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.STRICT_XHTML_LINKS' found, using default value 'true'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false, on, off, yes, no",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equalsIgnoreCase(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.STRICT_XHTML_LINKS' (='" + paramValue + "'), using default value 'true'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false, on, off, yes, no",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equalsIgnoreCase(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RENDER_HIDDEN_FIELDS_FOR_LINK_PARAMS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RENDER_HIDDEN_FIELDS_FOR_LINK_PARAMS' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false, on, off, yes, no",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equalsIgnoreCase(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.RENDER_HIDDEN_FIELDS_FOR_LINK_PARAMS' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.SAVE_FORM_SUBMIT_LINK_IE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.SAVE_FORM_SUBMIT_LINK_IE' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false, on, off, yes, no",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equalsIgnoreCase(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.SAVE_FORM_SUBMIT_LINK_IE' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.DELEGATE_FACES_SERVLET");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.DELEGATE_FACES_SERVLET' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS' found, using default value 'auto'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true,false,auto",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equalsIgnoreCase(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS' (='" + paramValue + "'), using default value 'auto'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false, on, off, yes, no",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equalsIgnoreCase(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VALIDATE_XML");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.VALIDATE_XML' found.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false, on, off, yes, no",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equalsIgnoreCase(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.VALIDATE_XML' (='" + paramValue + "'), using default value 'null'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false, on, off, yes, no",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equalsIgnoreCase(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RENDER_FORM_SUBMIT_SCRIPT_INLINE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RENDER_FORM_SUBMIT_SCRIPT_INLINE' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false, on, off, yes, no",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equalsIgnoreCase(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.RENDER_FORM_SUBMIT_SCRIPT_INLINE' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.DEBUG_PHASE_LISTENER");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.DEBUG_PHASE_LISTENER' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.STRICT_JSF_2_REFRESH_TARGET_AJAX");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.STRICT_JSF_2_REFRESH_TARGET_AJAX' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.STRICT_JSF_2_REFRESH_TARGET_AJAX' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.STRICT_JSF_2_CC_EL_RESOLVER");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.STRICT_JSF_2_CC_EL_RESOLVER' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.STRICT_JSF_2_CC_EL_RESOLVER' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE' found, using default value 'text/html'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("text/html, application/xhtml+xml",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE' (='" + paramValue + "'), using default value 'text/html'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VIEW_UNIQUE_IDS_CACHE_ENABLED");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.VIEW_UNIQUE_IDS_CACHE_ENABLED' found, using default value 'true'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.VIEW_UNIQUE_IDS_CACHE_ENABLED' (='" + paramValue + "'), using default value 'true'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.COMPONENT_UNIQUE_IDS_CACHE_SIZE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.COMPONENT_UNIQUE_IDS_CACHE_SIZE' found, using default value '100'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.SUPPORT_JSP_AND_FACES_EL");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.SUPPORT_JSP_AND_FACES_EL' found, using default value 'true'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true,false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.SUPPORT_JSP_AND_FACES_EL' (='" + paramValue + "'), using default value 'true'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.GAE_JSF_JAR_FILES");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.GAE_JSF_JAR_FILES' found.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("none, myfavoritejsflib-*.jar",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.GAE_JSF_JAR_FILES' (='" + paramValue + "'), using default value 'null'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.GAE_JSF_ANNOTATIONS_JAR_FILES");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.GAE_JSF_ANNOTATIONS_JAR_FILES' found.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("none, myfavoritejsflib-*.jar",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.GAE_JSF_ANNOTATIONS_JAR_FILES' (='" + paramValue + "'), using default value 'null'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.STRICT_JSF_2_VIEW_NOT_FOUND");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.STRICT_JSF_2_VIEW_NOT_FOUND' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true,false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.STRICT_JSF_2_VIEW_NOT_FOUND' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.EARLY_FLUSH_ENABLED");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.EARLY_FLUSH_ENABLED' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.EARLY_FLUSH_ENABLED' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.CDI_MANAGED_CONVERTERS_ENABLED");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.CDI_MANAGED_CONVERTERS_ENABLED' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.CDI_MANAGED_CONVERTERS_ENABLED' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.CDI_MANAGED_VALIDATORS_ENABLED");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.CDI_MANAGED_VALIDATORS_ENABLED' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.CDI_MANAGED_VALIDATORS_ENABLED' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.STRICT_JSF_2_FACELETS_COMPATIBILITY");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.STRICT_JSF_2_FACELETS_COMPATIBILITY' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true,false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.STRICT_JSF_2_FACELETS_COMPATIBILITY' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RENDER_FORM_VIEW_STATE_AT_BEGIN");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RENDER_FORM_VIEW_STATE_AT_BEGIN' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true,false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.RENDER_FORM_VIEW_STATE_AT_BEGIN' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.FLASH_SCOPE_DISABLED");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.FLASH_SCOPE_DISABLED' found, using default value 'false'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.NUMBER_OF_VIEWS_IN_SESSION");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.NUMBER_OF_VIEWS_IN_SESSION' found, using default value '20'.");
            }
            else
            {
                try
                {
                    java.lang.Integer.valueOf(paramValue);
                }
                catch(Exception e)
                {
                    if (log.isLoggable(Level.WARNING))
                    {
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.NUMBER_OF_VIEWS_IN_SESSION' (='" + paramValue + "'), using default value '20'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION' found, using default value '4'.");
            }
            else
            {
                try
                {
                    java.lang.Integer.valueOf(paramValue);
                }
                catch(Exception e)
                {
                    if (log.isLoggable(Level.WARNING))
                    {
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION' (='" + paramValue + "'), using default value '4'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.NUMBER_OF_FLASH_TOKENS_IN_SESSION");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.NUMBER_OF_FLASH_TOKENS_IN_SESSION' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.FACES_FLOW_CLIENT_WINDOW_IDS_IN_SESSION");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.FACES_FLOW_CLIENT_WINDOW_IDS_IN_SESSION' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RESOURCE_MAX_TIME_EXPIRES");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RESOURCE_MAX_TIME_EXPIRES' found, using default value '604800000'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RESOURCE_HANDLER_CACHE_SIZE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RESOURCE_HANDLER_CACHE_SIZE' found, using default value '500'.");
            }
            else
            {
                try
                {
                    java.lang.Integer.valueOf(paramValue);
                }
                catch(Exception e)
                {
                    if (log.isLoggable(Level.WARNING))
                    {
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.RESOURCE_HANDLER_CACHE_SIZE' (='" + paramValue + "'), using default value '500'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RESOURCE_HANDLER_CACHE_ENABLED");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RESOURCE_HANDLER_CACHE_ENABLED' found, using default value 'true'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true,false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.RESOURCE_HANDLER_CACHE_ENABLED' (='" + paramValue + "'), using default value 'true'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.USE_ENCRYPTION");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.USE_ENCRYPTION' found, using default value 'true'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true,false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.USE_ENCRYPTION' (='" + paramValue + "'), using default value 'true'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.SECRET");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.SECRET' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.ALGORITHM");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.ALGORITHM' found, using default value 'DES'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.SECRET.CACHE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.SECRET.CACHE' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.ALGORITHM.IV");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.ALGORITHM.IV' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.ALGORITHM.PARAMETERS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.ALGORITHM.PARAMETERS' found, using default value 'ECB/PKCS5Padding'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.SERIAL_FACTORY");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.SERIAL_FACTORY' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.COMPRESS_STATE_IN_CLIENT");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.COMPRESS_STATE_IN_CLIENT' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true,false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.COMPRESS_STATE_IN_CLIENT' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.MAC_ALGORITHM");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.MAC_ALGORITHM' found, using default value 'HmacSHA1'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.MAC_SECRET");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.MAC_SECRET' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.MAC_SECRET.CACHE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.MAC_SECRET.CACHE' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.PROJECT_STAGE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.PROJECT_STAGE' found, using default value 'Production'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("Development, Production, SystemTest, UnitTest",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'javax.faces.PROJECT_STAGE' (='" + paramValue + "'), using default value 'Production'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.LAZY_LOAD_CONFIG_OBJECTS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.LAZY_LOAD_CONFIG_OBJECTS' found, using default value 'true'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.STRICT_JSF_2_ALLOW_SLASH_LIBRARY_NAME");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.STRICT_JSF_2_ALLOW_SLASH_LIBRARY_NAME' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.STRICT_JSF_2_ALLOW_SLASH_LIBRARY_NAME' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RESOURCE_BUFFER_SIZE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RESOURCE_BUFFER_SIZE' found, using default value '2048'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN' found, using default value 'none'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("secureRandom, random",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN' (='" + paramValue + "'), using default value 'none'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_LENGTH");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_LENGTH' found, using default value '16'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_CLASS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_CLASS' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_PROVIDER");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_PROVIDER' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_ALGORITM");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_ALGORITM' found, using default value 'SHA1PRNG'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.CLIENT_VIEW_STATE_TIMEOUT");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.CLIENT_VIEW_STATE_TIMEOUT' found, using default value '0'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.SERIALIZE_STATE_IN_SESSION");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.SERIALIZE_STATE_IN_SESSION' found, using default value 'false'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.COMPRESS_STATE_IN_SESSION");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.COMPRESS_STATE_IN_SESSION' found, using default value 'true'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true,false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.COMPRESS_STATE_IN_SESSION' (='" + paramValue + "'), using default value 'true'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.CACHE_OLD_VIEWS_IN_SESSION_MODE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.CACHE_OLD_VIEWS_IN_SESSION_MODE' found, using default value 'off'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.USE_FLASH_SCOPE_PURGE_VIEWS_IN_SESSION");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.USE_FLASH_SCOPE_PURGE_VIEWS_IN_SESSION' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.USE_FLASH_SCOPE_PURGE_VIEWS_IN_SESSION' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN' found, using default value 'none'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("secureRandom, random, none",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN' (='" + paramValue + "'), using default value 'none'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_LENGTH");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_LENGTH' found, using default value '8'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_CLASS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_CLASS' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_PROVIDER");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_PROVIDER' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_ALGORITM");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_ALGORITM' found, using default value 'SHA1PRNG'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("facelets.LIBRARIES");
            if (paramValue == null)
            {
                log.info("No context init parameter 'facelets.LIBRARIES' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VALIDATE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.VALIDATE' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.VALIDATE' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.annotation.SCAN_PACKAGES");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.annotation.SCAN_PACKAGES' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.config.annotation.LifecycleProvider");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.config.annotation.LifecycleProvider' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.EL_RESOLVER_COMPARATOR");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.EL_RESOLVER_COMPARATOR' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.EL_RESOLVER_PREDICATE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.EL_RESOLVER_PREDICATE' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.DEFAULT_WINDOW_MODE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.DEFAULT_WINDOW_MODE' found, using default value 'url'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.CHECKED_VIEWID_CACHE_SIZE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.CHECKED_VIEWID_CACHE_SIZE' found, using default value '500'.");
            }
            else
            {
                try
                {
                    java.lang.Integer.valueOf(paramValue);
                }
                catch(Exception e)
                {
                    if (log.isLoggable(Level.WARNING))
                    {
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.CHECKED_VIEWID_CACHE_SIZE' (='" + paramValue + "'), using default value '500'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.CHECKED_VIEWID_CACHE_ENABLED");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.CHECKED_VIEWID_CACHE_ENABLED' found, using default value 'true'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.CHECKED_VIEWID_CACHE_ENABLED' (='" + paramValue + "'), using default value 'true'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.ERROR_TEMPLATE_RESOURCE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.ERROR_TEMPLATE_RESOURCE' found, using default value 'META-INF/rsc/myfaces-dev-error.xml'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.DEBUG_TEMPLATE_RESOURCE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.DEBUG_TEMPLATE_RESOURCE' found, using default value 'META-INF/rsc/myfaces-dev-debug.xml'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.ERROR_HANDLING");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.ERROR_HANDLING' found, using default value 'false, on Development Project stage: true'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true,false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.ERROR_HANDLING' (='" + paramValue + "'), using default value 'false, on Development Project stage: true'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.HANDLE_STATE_CACHING_MECHANICS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.HANDLE_STATE_CACHING_MECHANICS' found, using default value 'true'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.AUTOCOMPLETE_OFF_VIEW_STATE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.AUTOCOMPLETE_OFF_VIEW_STATE' found, using default value 'true'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.AUTOCOMPLETE_OFF_VIEW_STATE' (='" + paramValue + "'), using default value 'true'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.USE_MULTIPLE_JS_FILES_FOR_JSF_UNCOMPRESSED_JS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.USE_MULTIPLE_JS_FILES_FOR_JSF_UNCOMPRESSED_JS' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true,false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.USE_MULTIPLE_JS_FILES_FOR_JSF_UNCOMPRESSED_JS' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.JSF_JS_MODE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.JSF_JS_MODE' found, using default value 'normal'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("normal, minimal-modern, minimal",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.JSF_JS_MODE' (='" + paramValue + "'), using default value 'normal'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.TEMPORAL_RESOURCEHANDLER_CACHE_ENABLED");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.TEMPORAL_RESOURCEHANDLER_CACHE_ENABLED' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.TEMPORAL_RESOURCEHANDLER_CACHE_ENABLED' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.SERVICE_PROVIDER_FINDER");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.SERVICE_PROVIDER_FINDER' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.spi.InjectionProvider");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.spi.InjectionProvider' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.DISABLE_FACELET_JSF_VIEWHANDLER");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.DISABLE_FACELET_JSF_VIEWHANDLER' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true,false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'javax.faces.DISABLE_FACELET_JSF_VIEWHANDLER' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.SAVE_STATE_WITH_VISIT_TREE_ON_PSS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.SAVE_STATE_WITH_VISIT_TREE_ON_PSS' found, using default value 'true'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.CHECK_ID_PRODUCTION_MODE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.CHECK_ID_PRODUCTION_MODE' found, using default value 'auto'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, auto, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.CHECK_ID_PRODUCTION_MODE' (='" + paramValue + "'), using default value 'auto'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("facelets.BUFFER_SIZE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'facelets.BUFFER_SIZE' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("facelets.REFRESH_PERIOD");
            if (paramValue == null)
            {
                log.info("No context init parameter 'facelets.REFRESH_PERIOD' found, using default value '-1'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("javax.faces.FACELETS_RESOURCE_RESOLVER");
            if (paramValue == null)
            {
                log.info("No context init parameter 'javax.faces.FACELETS_RESOURCE_RESOLVER' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("facelets.RESOURCE_RESOLVER");
            if (paramValue == null)
            {
                log.info("No context init parameter 'facelets.RESOURCE_RESOLVER' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.MARK_INITIAL_STATE_WHEN_APPLY_BUILD_VIEW");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.MARK_INITIAL_STATE_WHEN_APPLY_BUILD_VIEW' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.MARK_INITIAL_STATE_WHEN_APPLY_BUILD_VIEW' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("facelets.DECORATORS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'facelets.DECORATORS' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("facelets.SKIP_COMMENTS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'facelets.SKIP_COMMENTS' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.WRAP_TAG_EXCEPTIONS_AS_CONTEXT_AWARE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.WRAP_TAG_EXCEPTIONS_AS_CONTEXT_AWARE' found, using default value 'true'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.WRAP_TAG_EXCEPTIONS_AS_CONTEXT_AWARE' (='" + paramValue + "'), using default value 'true'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.CACHE_EL_EXPRESSIONS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.CACHE_EL_EXPRESSIONS' found, using default value 'noCache'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("noCache, strict, allowCset, always, alwaysRecompile",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.CACHE_EL_EXPRESSIONS' (='" + paramValue + "'), using default value 'noCache'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VIEW_POOL_MAX_POOL_SIZE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.VIEW_POOL_MAX_POOL_SIZE' found, using default value '5'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VIEW_POOL_MAX_DYNAMIC_PARTIAL_LIMIT");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.VIEW_POOL_MAX_DYNAMIC_PARTIAL_LIMIT' found, using default value '2'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VIEW_POOL_ENTRY_MODE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.VIEW_POOL_ENTRY_MODE' found, using default value 'soft'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("weak,soft",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.VIEW_POOL_ENTRY_MODE' (='" + paramValue + "'), using default value 'soft'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VIEW_POOL_DEFERRED_NAVIGATION");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.VIEW_POOL_DEFERRED_NAVIGATION' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.VIEW_POOL_DEFERRED_NAVIGATION' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.EXPRESSION_FACTORY");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.EXPRESSION_FACTORY' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.INITIALIZE_ALWAYS_STANDALONE");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.INITIALIZE_ALWAYS_STANDALONE' found, using default value 'false'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.LOG_WEB_CONTEXT_PARAMS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.LOG_WEB_CONTEXT_PARAMS' found, using default value 'auto'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, auto, false",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equals(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.LOG_WEB_CONTEXT_PARAMS' (='" + paramValue + "'), using default value 'auto'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.FACES_INITIALIZER");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.FACES_INITIALIZER' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.FACES_INIT_PLUGINS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.FACES_INIT_PLUGINS' found.");
            }
        }
        if (log.isLoggable(Level.INFO) && myfacesConfig.isTomahawkAvailable())
        {
            String paramValue = null;

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RESOURCE_VIRTUAL_PATH");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.RESOURCE_VIRTUAL_PATH' found.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.DETECT_JAVASCRIPT");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.DETECT_JAVASCRIPT' found, using default value 'false'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.AUTO_SCROLL");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.AUTO_SCROLL' found, using default value 'false'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false, on, off, yes, no",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equalsIgnoreCase(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.AUTO_SCROLL' (='" + paramValue + "'), using default value 'false'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.ADD_RESOURCE_CLASS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.ADD_RESOURCE_CLASS' found, using default value 'org.apache.myfaces. renderkit.html.util. DefaultAddResource'.");
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.CHECK_EXTENSIONS_FILTER");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.CHECK_EXTENSIONS_FILTER' found, using default value 'for JSF 2.0 since 1.1.11 false, otherwise true'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false, on, off, yes, no",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equalsIgnoreCase(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.CHECK_EXTENSIONS_FILTER' (='" + paramValue + "'), using default value 'for JSF 2.0 since 1.1.11 false, otherwise true'");
                    }
                }
            }

            paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.READONLY_AS_DISABLED_FOR_SELECTS");
            if (paramValue == null)
            {
                log.info("No context init parameter 'org.apache.myfaces.READONLY_AS_DISABLED_FOR_SELECTS' found, using default value 'true'.");
            }
            else
            {
                boolean found = false;
                String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false, on, off, yes, no",','));
                for (int i = 0; i < expectedValues.length; i++)
                {
                    if (paramValue.equalsIgnoreCase(expectedValues[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    if (log.isLoggable(Level.WARNING))
                    { 
                        log.warning("Wrong value in context init parameter 'org.apache.myfaces.READONLY_AS_DISABLED_FOR_SELECTS' (='" + paramValue + "'), using default value 'true'");
                    }
                }
            }
        }
    }
}
