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
package javax.faces.component;

import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

class _MessageUtils
{
    private static final String DETAIL_SUFFIX = "_detail";

    static void addErrorMessage(FacesContext facesContext,
                                UIComponent component,
                                String messageId)
    {
        facesContext.addMessage(component.getClientId(facesContext),
                                getMessage(facesContext,
                                           facesContext.getViewRoot().getLocale(),
                                           FacesMessage.SEVERITY_ERROR,
                                           messageId,
                                           null));
    }

    static void addErrorMessage(FacesContext facesContext,
                                UIComponent component,
                                String messageId, Object[] args)
    {
        facesContext.addMessage(component.getClientId(facesContext),
                                getMessage(facesContext,
                                           facesContext.getViewRoot().getLocale(),
                                           FacesMessage.SEVERITY_ERROR,
                                           messageId,
                                           args));
    }

    static void addErrorMessage(FacesContext facesContext,
            UIComponent component, Throwable cause)
    {
        facesContext.addMessage(component.getClientId(facesContext),
                new FacesMessage(FacesMessage.SEVERITY_ERROR, cause
                        .getLocalizedMessage(), cause.getLocalizedMessage()));
    }

    
    static FacesMessage getMessage(FacesContext facesContext,
                                   Locale locale,
                                   FacesMessage.Severity severity,
                                   String messageId,
                                   Object args[])
    {
        ResourceBundle appBundle;
        ResourceBundle defBundle;
        String summary;
        String detail;

        appBundle = getApplicationBundle(facesContext, locale);
        summary = getBundleString(appBundle, messageId);
        if (summary != null)
        {
            detail = getBundleString(appBundle, messageId + DETAIL_SUFFIX);
        }
        else
        {
            defBundle = getDefaultBundle(facesContext, locale);
            summary = getBundleString(defBundle, messageId);
            if (summary != null)
            {
                detail = getBundleString(defBundle, messageId + DETAIL_SUFFIX);
            }
            else
            {
                //Try to find detail alone
                detail = getBundleString(appBundle, messageId + DETAIL_SUFFIX);
                if (detail != null)
                {
                    summary = null;
                }
                else
                {
                    detail = getBundleString(defBundle, messageId + DETAIL_SUFFIX);
                    if (detail != null)
                    {
                        summary = null;
                    }
                    else
                    {
                        //Neither detail nor summary found
                        facesContext.getExternalContext().log("No message with id " + messageId
                                                              + " found in any bundle");
                        return new FacesMessage(severity, messageId, null);
                    }
                }
            }
        }

        if (args != null && args.length > 0)
        {
            return new _ParametrizableFacesMessage(severity, summary, detail, args, locale);
        }
        else
        {
            return new FacesMessage(severity, summary, detail);
        }
    }

    private static String getBundleString(ResourceBundle bundle, String key)
    {
        try
        {
            return bundle == null ? null : bundle.getString(key);
        }
        catch (MissingResourceException e)
        {
            return null;
        }
    }


    private static ResourceBundle getApplicationBundle(FacesContext facesContext, Locale locale)
    {
        String bundleName = facesContext.getApplication().getMessageBundle();
        return bundleName != null ? getBundle(facesContext, locale, bundleName) : null;
    }

    private static ResourceBundle getDefaultBundle(FacesContext facesContext,
                                                   Locale locale)
    {
        return getBundle(facesContext, locale, FacesMessage.FACES_MESSAGES);
    }

    private static ResourceBundle getBundle(FacesContext facesContext,
                                            Locale locale,
                                            String bundleName)
    {
        try
        {
            //First we try the JSF implementation class loader
            return ResourceBundle.getBundle(bundleName,
                                            locale,
                                            facesContext.getClass().getClassLoader());
        }
        catch (MissingResourceException ignore1)
        {
            try
            {
                //Next we try the JSF API class loader
                return ResourceBundle.getBundle(bundleName,
                                                locale,
                                                _MessageUtils.class.getClassLoader());
            }
            catch (MissingResourceException ignore2)
            {
                try
                {
                    //Last resort is the context class loader
                    return ResourceBundle.getBundle(bundleName,
                                                    locale,
                                                    _ClassUtils.getContextClassLoader());
                }
                catch (MissingResourceException damned)
                {
                    facesContext.getExternalContext().log("resource bundle " + bundleName + " could not be found");
                    return null;
                }
            }
        }
    }
    
    static Object getLabel(FacesContext facesContext, UIComponent component)
    {
        Object label = component.getAttributes().get("label");
        ValueExpression expression = null;
        if (label != null && 
            label instanceof String && ((String)label).length() == 0 )
        {
            // Note component.getAttributes().get("label") internally try to 
            // evaluate the EL expression for the label, but in some cases, 
            // when PSS is disabled and f:loadBundle is used, when the view is 
            // restored the bundle is not set to the EL expression returns an 
            // empty String. It is not possible to check if there is a 
            // hardcoded label, but we can check if there is
            // an EL expression set, so the best in this case is use that, and if
            // there is an EL expression set, use it, otherwise use the hardcoded
            // value. See MYFACES-3591 for details.
            expression = component.getValueExpression("label");
            if (expression != null)
            {
                // Set the label to null and use the EL expression instead.
                label = null;
            }
        }
            
        if(label != null)
        {
            return label;
        }
        
        expression = (expression == null) ? component.getValueExpression("label") : expression;
        if(expression != null)
        {
            return expression;
        }
        
        //If no label is not specified, use clientId
        return component.getClientId( facesContext );
    }
}
