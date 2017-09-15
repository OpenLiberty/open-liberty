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

import javax.faces.view.ViewDeclarationLanguage;

import org.apache.myfaces.view.ViewDeclarationLanguageStrategy;

/**
 * @author Simon Lessard (latest modification by $Author: bommel $)
 * @version $Revision: 1187701 $ $Date: 2011-10-22 12:21:54 +0000 (Sat, 22 Oct 2011) $
 *
 * @since 2.0
 */
public class JspViewDeclarationLanguageStrategy implements ViewDeclarationLanguageStrategy
{
    private ViewDeclarationLanguage _language;
    //private LinkedList<String> _suffixes;
    
    public JspViewDeclarationLanguageStrategy()
    {
        // TODO: IMPLEMENT HERE
        
        _language = new JspViewDeclarationLanguage();
        
        //_suffixes = loadSuffixes (FacesContext.getCurrentInstance().getExternalContext());
    }
    
    /**
     * {@inheritDoc}
     */
    public ViewDeclarationLanguage getViewDeclarationLanguage()
    {
        return _language;
    }

    /**
     * {@inheritDoc}
     */
    public boolean handles(String viewId)
    {
        return true;
        /*
        for (String suffix : _suffixes) {
            if (viewId.endsWith (suffix)) {
                return true;
            }
        }
        
        return false;
        */
    }
    
    /*
    private LinkedList<String> loadSuffixes (ExternalContext context) {
        LinkedList<String> result = new LinkedList<String>();
        String definedSuffixes = context.getInitParameter (ViewHandler.DEFAULT_SUFFIX_PARAM_NAME);
        StringTokenizer tokenizer;
        
        if (definedSuffixes == null) {
            definedSuffixes = ViewHandler.DEFAULT_SUFFIX;
        }
        
        // This is a space-separated list of suffixes, so parse them out.
        
        tokenizer = new StringTokenizer (definedSuffixes, " ");
        
        while (tokenizer.hasMoreTokens()) {
            result.add (tokenizer.nextToken());
        }
        
        return result;
    }
    */
}
