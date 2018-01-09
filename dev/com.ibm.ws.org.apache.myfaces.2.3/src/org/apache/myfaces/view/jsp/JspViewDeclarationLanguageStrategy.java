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
package org.apache.myfaces.view.jsp;

import java.util.LinkedList;
import java.util.StringTokenizer;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewDeclarationLanguage;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;

import org.apache.myfaces.view.ViewDeclarationLanguageStrategy;

/**
 * @author Simon Lessard (latest modification by $Author: tandraschko $)
 * @version $Revision: 1808330 $ $Date: 2017-09-14 12:29:31 +0000 (Thu, 14 Sep 2017) $
 *
 * @since 2.0
 */
public class JspViewDeclarationLanguageStrategy implements ViewDeclarationLanguageStrategy
{
    private ViewDeclarationLanguage _language;
    private LinkedList<String> _suffixes;
    
    /**
     * 
     */
    @JSFWebConfigParam(defaultValue=".jsp", since="2.3", group="viewhandler")
    public static final String JSP_SUFFIX_PARAM_NAME = "org.apache.myfaces.JSP_SUFFIX";
    public static final String JSP_SUFFIX_DEFAULT = ".jsp";
    
    public JspViewDeclarationLanguageStrategy()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        
        _suffixes = loadSuffixes (facesContext.getExternalContext());
        _language = new JspViewDeclarationLanguage(facesContext, this, _suffixes);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ViewDeclarationLanguage getViewDeclarationLanguage()
    {
        return _language;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean handles(String viewId)
    {
        /*
        for (String suffix : _suffixes)
        {
            if (viewId != null && viewId.endsWith (suffix)) 
            {
                return true;
            }
        }
        */
        
        return true;
    }
    
    static LinkedList<String> loadSuffixes (ExternalContext context) 
    {
        LinkedList<String> result = new LinkedList<String>();
        String definedSuffixes = context.getInitParameter (JSP_SUFFIX_PARAM_NAME);
        StringTokenizer tokenizer;
        
        if (definedSuffixes == null) 
        {
            definedSuffixes = JSP_SUFFIX_DEFAULT;
        }
        
        // This is a space-separated list of suffixes, so parse them out.
        
        tokenizer = new StringTokenizer (definedSuffixes, " ");
        
        while (tokenizer.hasMoreTokens()) 
        {
            result.add (tokenizer.nextToken());
        }
        
        return result;
    }

    @Override
    public String getMinimalImplicitOutcome(String viewId)
    {
        for (String suffix : _suffixes) 
        {
            if (viewId != null && viewId.endsWith(suffix))
            {
                return viewId.substring(0, viewId.length()-suffix.length());
            }
        }
        return viewId;
    }
}
