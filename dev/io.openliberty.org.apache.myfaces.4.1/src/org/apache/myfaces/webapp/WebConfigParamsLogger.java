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

import jakarta.faces.application.ProjectStage;
import jakarta.faces.context.FacesContext;

import org.apache.myfaces.config.webparameters.MyfacesConfig;
import org.apache.myfaces.util.WebConfigParamUtils;
import org.apache.myfaces.util.lang.StringUtils;

public class WebConfigParamsLogger
{
    private final Logger log = Logger.getLogger(WebConfigParamsLogger.class.getName());

    private Boolean LOG_WEB_CONTEXT_PARAM = false;

    public void logWebContextParams(FacesContext facesContext)
    {
        MyfacesConfig myfacesConfig = MyfacesConfig.getCurrentInstance(facesContext.getExternalContext());

        LOG_WEB_CONTEXT_PARAM = myfacesConfig.isLogWebContextParams();

        if(myfacesConfig.isRiImplAvailable() && myfacesConfig.isMyfacesImplAvailable())
        {
            log.severe("Both MyFaces and the RI are on your classpath. Please make sure to use only one of the two JSF-implementations.");
        }

        if(LOG_WEB_CONTEXT_PARAM)
        {
            log.info("Scanning for context init parameters not defined. It is not necessary to define them all into your web.xml, " +
                "they are just provided here for informative purposes. To disable this messages set " +
                MyfacesConfig.LOG_WEB_CONTEXT_PARAMS + " config param to 'false'");
        }
            String paramValue = null;

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.PROJECT_STAGE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.PROJECT_STAGE' found, using default value 'Production'.");
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
                    log.warning("Wrong value in context init parameter 'jakarta.faces.PROJECT_STAGE' (='" + paramValue + "'), using default value 'Production'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'jakarta.faces.PROJECT_STAGE' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.RESOURCE_EXCLUDES");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.RESOURCE_EXCLUDES' found, using default value '.class .jsp .jspx .properties .xhtml .groovy'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.STATE_SAVING_METHOD");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.STATE_SAVING_METHOD' found, using default value 'server'.");
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
                    log.warning("Wrong value in context init parameter 'jakarta.faces.STATE_SAVING_METHOD' (='" + paramValue + "'), using default value 'server'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'jakarta.faces.STATE_SAVING_METHOD' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.FULL_STATE_SAVING_VIEW_IDS");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.FULL_STATE_SAVING_VIEW_IDS' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.PARTIAL_STATE_SAVING");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.PARTIAL_STATE_SAVING' found, using default value 'true (false with 1.2 webapps)'.");
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
                    log.warning("Wrong value in context init parameter 'jakarta.faces.PARTIAL_STATE_SAVING' (='" + paramValue + "'), using default value 'true (false with 1.2 webapps)'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'jakarta.faces.PARTIAL_STATE_SAVING' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.SERIALIZE_SERVER_STATE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.SERIALIZE_SERVER_STATE' found, using default value 'false'.");
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
                    log.warning("Wrong value in context init parameter 'jakarta.faces.SERIALIZE_SERVER_STATE' (='" + paramValue + "'), using default value 'false'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'jakarta.faces.SERIALIZE_SERVER_STATE' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.DEFAULT_SUFFIX");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.DEFAULT_SUFFIX' found, using default value '.xhtml'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.FACELETS_SUFFIX");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.FACELETS_SUFFIX' found, using default value '.xhtml'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.FACELETS_VIEW_MAPPINGS");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.FACELETS_VIEW_MAPPINGS' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.FACELETS_BUFFER_SIZE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.FACELETS_BUFFER_SIZE' found, using default value '1024'.");
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
                    log.warning("Wrong value in context init parameter 'jakarta.faces.FACELETS_BUFFER_SIZE' (='" + paramValue + "'), using default value '1024'");
                }
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.FACELETS_DECORATORS");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.FACELETS_DECORATORS' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.FACELETS_LIBRARIES");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.FACELETS_LIBRARIES' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.FACELETS_REFRESH_PERIOD");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.FACELETS_REFRESH_PERIOD' found, using default value '0, -1 in Production'.");
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
                    log.warning("Wrong value in context init parameter 'jakarta.faces.FACELETS_REFRESH_PERIOD' (='" + paramValue + "'), using default value '0, -1 in Production'");
                }
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.FACELETS_SKIP_COMMENTS");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.FACELETS_SKIP_COMMENTS' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.VALIDATE_EMPTY_FIELDS");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.VALIDATE_EMPTY_FIELDS' found, using default value 'auto'.");
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
                    log.warning("Wrong value in context init parameter 'jakarta.faces.VALIDATE_EMPTY_FIELDS' (='" + paramValue + "'), using default value 'auto'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'jakarta.faces.VALIDATE_EMPTY_FIELDS' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL' found, using default value 'false'.");
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
                    log.warning("Wrong value in context init parameter 'jakarta.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL' (='" + paramValue + "'), using default value 'false'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'jakarta.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.CLEAR_INPUT_WHEN_SUBMITTED_VALUE_IS_NULL_OR_EMPTY");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.CLEAR_INPUT_WHEN_SUBMITTED_VALUE_IS_NULL_OR_EMPTY' found, using default value 'true'.");
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
                    log.warning("Wrong value in context init parameter 'org.apache.myfaces.CLEAR_INPUT_WHEN_SUBMITTED_VALUE_IS_NULL_OR_EMPTY' (='" + paramValue + "'), using default value 'true'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.CLEAR_INPUT_WHEN_SUBMITTED_VALUE_IS_NULL_OR_EMPTY' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED_IS_TRUE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED_IS_TRUE' found, using default value 'false'.");
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
                    log.warning("Wrong value in context init parameter 'jakarta.faces.ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED_IS_TRUE' (='" + paramValue + "'), using default value 'false'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'jakarta.faces.ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED_IS_TRUE' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.VIEWROOT_PHASE_LISTENER_QUEUES_EXCEPTIONS");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.VIEWROOT_PHASE_LISTENER_QUEUES_EXCEPTIONS' found, using default value 'false'.");
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
                    log.warning("Wrong value in context init parameter 'jakarta.faces.VIEWROOT_PHASE_LISTENER_QUEUES_EXCEPTIONS' (='" + paramValue + "'), using default value 'false'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'jakarta.faces.VIEWROOT_PHASE_LISTENER_QUEUES_EXCEPTIONS' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE' found, using default value 'false'.");
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
                    log.warning("Wrong value in context init parameter 'jakarta.faces.DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE' (='" + paramValue + "'), using default value 'false'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'jakarta.faces.DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.ENUM_CONVERTER_ALLOW_STRING_PASSTROUGH");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.ENUM_CONVERTER_ALLOW_STRING_PASSTROUGH' found, using default value 'false'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.ENUM_CONVERTER_ALLOW_STRING_PASSTROUGH' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.CLIENT_WINDOW_MODE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.CLIENT_WINDOW_MODE' found, using default value 'none'.");
        }
        else
        {
            boolean found = false;
            String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("none, url, url-redirect, client",','));
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
                    log.warning("Wrong value in context init parameter 'jakarta.faces.CLIENT_WINDOW_MODE' (='" + paramValue + "'), using default value 'none'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'jakarta.faces.CLIENT_WINDOW_MODE' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.NUMBER_OF_CLIENT_WINDOWS");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.NUMBER_OF_CLIENT_WINDOWS' found, using default value '10'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.validator.DISABLE_DEFAULT_BEAN_VALIDATOR");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.validator.DISABLE_DEFAULT_BEAN_VALIDATOR' found, using default value 'true'.");
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
                    log.warning("Wrong value in context init parameter 'jakarta.faces.validator.DISABLE_DEFAULT_BEAN_VALIDATOR' (='" + paramValue + "'), using default value 'true'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'jakarta.faces.validator.DISABLE_DEFAULT_BEAN_VALIDATOR' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.validator.ENABLE_VALIDATE_WHOLE_BEAN");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.validator.ENABLE_VALIDATE_WHOLE_BEAN' found, using default value 'false'.");
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
                    log.warning("Wrong value in context init parameter 'jakarta.faces.validator.ENABLE_VALIDATE_WHOLE_BEAN' (='" + paramValue + "'), using default value 'false'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'jakarta.faces.validator.ENABLE_VALIDATE_WHOLE_BEAN' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.CONFIG_FILES");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.CONFIG_FILES' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.LIFECYCLE_ID");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.LIFECYCLE_ID' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.DISABLE_FACESSERVLET_TO_XHTML");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.DISABLE_FACESSERVLET_TO_XHTML' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.AUTOMATIC_EXTENSIONLESS_MAPPING");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.AUTOMATIC_EXTENSIONLESS_MAPPING' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.USE_LAMBDA_METAFACTORY");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.USE_LAMBDA_METAFACTORY' found, using default value 'false'.");
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
                    log.warning("Wrong value in context init parameter 'org.apache.myfaces.USE_LAMBDA_METAFACTORY' (='" + paramValue + "'), using default value 'false'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.USE_LAMBDA_METAFACTORY' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.USE_ENCRYPTION");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.USE_ENCRYPTION' found, using default value 'true'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.USE_ENCRYPTION' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.SECRET");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.SECRET' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.ALGORITHM");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.ALGORITHM' found, using default value 'AES'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.SECRET.CACHE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.SECRET.CACHE' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.ALGORITHM.IV");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.ALGORITHM.IV' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.ALGORITHM.PARAMETERS");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.ALGORITHM.PARAMETERS' found, using default value 'ECB/PKCS5Padding'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.SERIAL_FACTORY");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.SERIAL_FACTORY' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.COMPRESS_STATE_IN_CLIENT");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.COMPRESS_STATE_IN_CLIENT' found, using default value 'false'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.COMPRESS_STATE_IN_CLIENT' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.MAC_ALGORITHM");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.MAC_ALGORITHM' found, using default value 'HmacSHA256'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.MAC_SECRET");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.MAC_SECRET' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.MAC_SECRET.CACHE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.MAC_SECRET.CACHE' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.CONFIG_REFRESH_PERIOD");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.CONFIG_REFRESH_PERIOD' found, using default value '2'.");
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

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RENDER_VIEWSTATE_ID");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RENDER_VIEWSTATE_ID' found, using default value 'true'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.RENDER_VIEWSTATE_ID' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.STRICT_XHTML_LINKS");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.STRICT_XHTML_LINKS' found, using default value 'true'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.STRICT_XHTML_LINKS' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON' found, using default value 'false'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS' found, using default value 'auto'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE' found, using default value 'false'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VALIDATE_XML");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.VALIDATE_XML' found.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.VALIDATE_XML' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG' found, using default value 'false'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.DEBUG_PHASE_LISTENER");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.DEBUG_PHASE_LISTENER' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.STRICT_JSF_2_CC_EL_RESOLVER");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.STRICT_JSF_2_CC_EL_RESOLVER' found, using default value 'false'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.STRICT_JSF_2_CC_EL_RESOLVER' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE' found, using default value 'text/html'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VIEW_UNIQUE_IDS_CACHE_ENABLED");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.VIEW_UNIQUE_IDS_CACHE_ENABLED' found, using default value 'true'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.VIEW_UNIQUE_IDS_CACHE_ENABLED' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.COMPONENT_UNIQUE_IDS_CACHE_SIZE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.COMPONENT_UNIQUE_IDS_CACHE_SIZE' found, using default value '100'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.STRICT_JSF_2_VIEW_NOT_FOUND");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.STRICT_JSF_2_VIEW_NOT_FOUND' found, using default value 'false'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.STRICT_JSF_2_VIEW_NOT_FOUND' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.EARLY_FLUSH_ENABLED");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.EARLY_FLUSH_ENABLED' found, using default value 'false'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.EARLY_FLUSH_ENABLED' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.STRICT_JSF_2_FACELETS_COMPATIBILITY");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.STRICT_JSF_2_FACELETS_COMPATIBILITY' found, using default value 'false'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.STRICT_JSF_2_FACELETS_COMPATIBILITY' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RENDER_FORM_VIEW_STATE_AT_BEGIN");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RENDER_FORM_VIEW_STATE_AT_BEGIN' found, using default value 'false'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.RENDER_FORM_VIEW_STATE_AT_BEGIN' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.FLASH_SCOPE_DISABLED");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.FLASH_SCOPE_DISABLED' found, using default value 'false'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.NUMBER_OF_VIEWS_IN_SESSION");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.NUMBER_OF_VIEWS_IN_SESSION' found, using default value '20'.");
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
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION' found, using default value '4'.");
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
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.NUMBER_OF_FLASH_TOKENS_IN_SESSION' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.SUPPORT_EL_3_IMPORT_HANDLER");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.SUPPORT_EL_3_IMPORT_HANDLER' found, using default value 'false'.");
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
                    log.warning("Wrong value in context init parameter 'org.apache.myfaces.SUPPORT_EL_3_IMPORT_HANDLER' (='" + paramValue + "'), using default value 'false'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.SUPPORT_EL_3_IMPORT_HANDLER' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.STRICT_JSF_2_ORIGIN_HEADER_APP_PATH");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.STRICT_JSF_2_ORIGIN_HEADER_APP_PATH' found, using default value 'false'.");
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
                    log.warning("Wrong value in context init parameter 'org.apache.myfaces.STRICT_JSF_2_ORIGIN_HEADER_APP_PATH' (='" + paramValue + "'), using default value 'false'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.STRICT_JSF_2_ORIGIN_HEADER_APP_PATH' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.STRICT_JSF_2_ALLOW_SLASH_LIBRARY_NAME");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.STRICT_JSF_2_ALLOW_SLASH_LIBRARY_NAME' found, using default value 'false'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.STRICT_JSF_2_ALLOW_SLASH_LIBRARY_NAME' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RESOURCE_BUFFER_SIZE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RESOURCE_BUFFER_SIZE' found, using default value '2048'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VALIDATE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.VALIDATE' found, using default value 'false'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.VALIDATE' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.annotation.USE_CDI_FOR_ANNOTATION_SCANNING");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.annotation.USE_CDI_FOR_ANNOTATION_SCANNING' found, using default value 'false'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RESOURCE_HANDLER_CACHE_SIZE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RESOURCE_HANDLER_CACHE_SIZE' found, using default value '500'.");
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
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RESOURCE_HANDLER_CACHE_ENABLED' found, using default value 'true'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.RESOURCE_HANDLER_CACHE_ENABLED' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.annotation.SCAN_PACKAGES");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.annotation.SCAN_PACKAGES' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("jakarta.faces.WEBSOCKET_ENDPOINT_PORT");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'jakarta.faces.WEBSOCKET_ENDPOINT_PORT' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.WEBSOCKET_MAX_IDLE_TIMEOUT");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.WEBSOCKET_MAX_IDLE_TIMEOUT' found, using default value '300000'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_WEBSOCKET_SESSION_TOKEN");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_WEBSOCKET_SESSION_TOKEN' found, using default value 'secureRandom'.");
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
                    log.warning("Wrong value in context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_WEBSOCKET_SESSION_TOKEN' (='" + paramValue + "'), using default value 'secureRandom'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.RANDOM_KEY_IN_WEBSOCKET_SESSION_TOKEN' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.CLIENT_VIEW_STATE_TIMEOUT");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.CLIENT_VIEW_STATE_TIMEOUT' found, using default value '0'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN' found, using default value 'secureRandom'.");
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
                    log.warning("Wrong value in context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN' (='" + paramValue + "'), using default value 'secureRandom'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_LENGTH");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_LENGTH' found, using default value '8'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_CLASS");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_CLASS' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_PROVIDER");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_PROVIDER' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_ALGORITHM");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_ALGORITHM' found, using default value 'SHA1PRNG'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN' found, using default value 'secureRandom'.");
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
                    log.warning("Wrong value in context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN' (='" + paramValue + "'), using default value 'secureRandom'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.COMPRESS_STATE_IN_SESSION");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.COMPRESS_STATE_IN_SESSION' found, using default value 'true'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.COMPRESS_STATE_IN_SESSION' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.USE_FLASH_SCOPE_PURGE_VIEWS_IN_SESSION");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.USE_FLASH_SCOPE_PURGE_VIEWS_IN_SESSION' found, using default value 'false'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.USE_FLASH_SCOPE_PURGE_VIEWS_IN_SESSION' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.AUTOCOMPLETE_OFF_VIEW_STATE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.AUTOCOMPLETE_OFF_VIEW_STATE' found, using default value 'one-time-code'.");
        }
        else
        {
            boolean found = false;
            String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, false, one-time-code",','));
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
                    log.warning("Wrong value in context init parameter 'org.apache.myfaces.AUTOCOMPLETE_OFF_VIEW_STATE' (='" + paramValue + "'), using default value 'one-time-code'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.AUTOCOMPLETE_OFF_VIEW_STATE' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RESOURCE_MAX_TIME_EXPIRES");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RESOURCE_MAX_TIME_EXPIRES' found, using default value '604800000'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.LAZY_LOAD_CONFIG_OBJECTS");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.LAZY_LOAD_CONFIG_OBJECTS' found, using default value 'true'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.EL_RESOLVER_COMPARATOR");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.EL_RESOLVER_COMPARATOR' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.EL_RESOLVER_PREDICATE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.EL_RESOLVER_PREDICATE' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VIEWID_CACHE_SIZE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.VIEWID_CACHE_SIZE' found, using default value '500'.");
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
                    log.warning("Wrong value in context init parameter 'org.apache.myfaces.VIEWID_CACHE_SIZE' (='" + paramValue + "'), using default value '500'");
                }
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VIEWID_EXISTS_CACHE_ENABLED");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.VIEWID_EXISTS_CACHE_ENABLED' found, using default value 'true'.");
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
                    log.warning("Wrong value in context init parameter 'org.apache.myfaces.VIEWID_EXISTS_CACHE_ENABLED' (='" + paramValue + "'), using default value 'true'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.VIEWID_EXISTS_CACHE_ENABLED' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VIEWID_PROTECTED_CACHE_ENABLED");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.VIEWID_PROTECTED_CACHE_ENABLED' found, using default value 'true'.");
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
                    log.warning("Wrong value in context init parameter 'org.apache.myfaces.VIEWID_PROTECTED_CACHE_ENABLED' (='" + paramValue + "'), using default value 'true'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.VIEWID_PROTECTED_CACHE_ENABLED' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VIEWID_DERIVE_CACHE_ENABLED");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.VIEWID_DERIVE_CACHE_ENABLED' found, using default value 'true'.");
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
                    log.warning("Wrong value in context init parameter 'org.apache.myfaces.VIEWID_DERIVE_CACHE_ENABLED' (='" + paramValue + "'), using default value 'true'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.VIEWID_DERIVE_CACHE_ENABLED' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.validator.BEAN_BEFORE_JSF_VALIDATION");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.validator.BEAN_BEFORE_JSF_VALIDATION' found, using default value 'false'.");
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
                    log.warning("Wrong value in context init parameter 'org.apache.myfaces.validator.BEAN_BEFORE_JSF_VALIDATION' (='" + paramValue + "'), using default value 'false'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.validator.BEAN_BEFORE_JSF_VALIDATION' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.FACES_INIT_PLUGINS");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.FACES_INIT_PLUGINS' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.INITIALIZE_SKIP_JAR_FACES_CONFIG_SCAN");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.INITIALIZE_SKIP_JAR_FACES_CONFIG_SCAN' found, using default value 'false'.");
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
                    log.warning("Wrong value in context init parameter 'org.apache.myfaces.INITIALIZE_SKIP_JAR_FACES_CONFIG_SCAN' (='" + paramValue + "'), using default value 'false'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.INITIALIZE_SKIP_JAR_FACES_CONFIG_SCAN' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.INITIALIZE_ALWAYS_STANDALONE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.INITIALIZE_ALWAYS_STANDALONE' found, using default value 'false'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.EXPRESSION_FACTORY");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.EXPRESSION_FACTORY' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.CHECK_ID_PRODUCTION_MODE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.CHECK_ID_PRODUCTION_MODE' found, using default value 'auto'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.CHECK_ID_PRODUCTION_MODE' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.MARK_INITIAL_STATE_WHEN_APPLY_BUILD_VIEW");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.MARK_INITIAL_STATE_WHEN_APPLY_BUILD_VIEW' found, using default value 'false'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.MARK_INITIAL_STATE_WHEN_APPLY_BUILD_VIEW' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.CACHE_EL_EXPRESSIONS");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.CACHE_EL_EXPRESSIONS' found, using default value 'noCache'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.CACHE_EL_EXPRESSIONS' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.WRAP_TAG_EXCEPTIONS_AS_CONTEXT_AWARE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.WRAP_TAG_EXCEPTIONS_AS_CONTEXT_AWARE' found, using default value 'true'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.WRAP_TAG_EXCEPTIONS_AS_CONTEXT_AWARE' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RESOURCE_CACHE_LAST_MODIFIED");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RESOURCE_CACHE_LAST_MODIFIED' found, using default value 'true'.");
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
                    log.warning("Wrong value in context init parameter 'org.apache.myfaces.RESOURCE_CACHE_LAST_MODIFIED' (='" + paramValue + "'), using default value 'true'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.RESOURCE_CACHE_LAST_MODIFIED' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.LOG_WEB_CONTEXT_PARAMS");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.LOG_WEB_CONTEXT_PARAMS' found, using default value 'dev-only'.");
        }
        else
        {
            boolean found = false;
            String[] expectedValues = StringUtils.trim(StringUtils.splitShortString("true, dev-only, false",','));
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
                    log.warning("Wrong value in context init parameter 'org.apache.myfaces.LOG_WEB_CONTEXT_PARAMS' (='" + paramValue + "'), using default value 'dev-only'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.LOG_WEB_CONTEXT_PARAMS' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.FACES_INITIALIZER");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.FACES_INITIALIZER' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.spi.InjectionProvider");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.spi.InjectionProvider' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.WEBSOCKET_MAX_CONNECTIONS");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.WEBSOCKET_MAX_CONNECTIONS' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RENDER_CLIENTBEHAVIOR_SCRIPTS_AS_STRING");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RENDER_CLIENTBEHAVIOR_SCRIPTS_AS_STRING' found, using default value 'false'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.ALWAYS_FORCE_SESSION_CREATION");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.ALWAYS_FORCE_SESSION_CREATION' found, using default value 'false'.");
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
                    log.warning("Wrong value in context init parameter 'org.apache.myfaces.ALWAYS_FORCE_SESSION_CREATION' (='" + paramValue + "'), using default value 'false'");
                }
            }
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.ALWAYS_FORCE_SESSION_CREATION' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RESOURCE_BUNDLE_CONTROL");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RESOURCE_BUNDLE_CONTROL' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.EL_RESOLVER_TRACING");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.EL_RESOLVER_TRACING' found, using default value 'false'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.EXCEPTION_TYPES_TO_IGNORE_IN_LOGGING");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.EXCEPTION_TYPES_TO_IGNORE_IN_LOGGING' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.ERROR_TEMPLATE_RESOURCE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.ERROR_TEMPLATE_RESOURCE' found, using default value 'META-INF/rsc/myfaces-dev-error.xml'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.DEBUG_TEMPLATE_RESOURCE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.DEBUG_TEMPLATE_RESOURCE' found, using default value 'META-INF/rsc/myfaces-dev-debug.xml'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.ERROR_HANDLING");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.ERROR_HANDLING' found, using default value 'false, on Development Project stage: true'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.ERROR_HANDLING' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.TEMPORAL_RESOURCEHANDLER_CACHE_ENABLED");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.TEMPORAL_RESOURCEHANDLER_CACHE_ENABLED' found, using default value 'false'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.TEMPORAL_RESOURCEHANDLER_CACHE_ENABLED' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.SERVICE_PROVIDER_FINDER");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.SERVICE_PROVIDER_FINDER' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_LENGTH");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_LENGTH' found, using default value '16'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_CLASS");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_CLASS' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_PROVIDER");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_PROVIDER' found.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_ALGORITM");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_ALGORITM' found, using default value 'SHA1PRNG'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VIEW_POOL_MAX_POOL_SIZE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.VIEW_POOL_MAX_POOL_SIZE' found, using default value '5'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VIEW_POOL_MAX_DYNAMIC_PARTIAL_LIMIT");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.VIEW_POOL_MAX_DYNAMIC_PARTIAL_LIMIT' found, using default value '2'.");
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VIEW_POOL_ENTRY_MODE");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.VIEW_POOL_ENTRY_MODE' found, using default value 'soft'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.VIEW_POOL_ENTRY_MODE' set to '" + paramValue + "'");
            }
        }

        paramValue = facesContext.getExternalContext().getInitParameter("org.apache.myfaces.VIEW_POOL_DEFERRED_NAVIGATION");
        if (paramValue == null)
        {
            logMessageToAppropriateLevel("No context init parameter 'org.apache.myfaces.VIEW_POOL_DEFERRED_NAVIGATION' found, using default value 'false'.");
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
            else
            {
                logMessageToAppropriateLevel("Init context parameter found 'org.apache.myfaces.VIEW_POOL_DEFERRED_NAVIGATION' set to '" + paramValue + "'");
            }
        }
    }

    private void logMessageToAppropriateLevel(String text)
    {
        if(LOG_WEB_CONTEXT_PARAM && log.isLoggable(Level.INFO))
        {
            log.info(text);
        }
        else if (log.isLoggable(Level.FINEST))
        {
            log.finest(text);
        }
    }
}
