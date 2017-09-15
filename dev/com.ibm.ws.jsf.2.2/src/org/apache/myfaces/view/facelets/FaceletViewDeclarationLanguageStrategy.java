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
package org.apache.myfaces.view.facelets;

import java.util.regex.Pattern;

import javax.faces.application.ViewHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewDeclarationLanguage;

import org.apache.myfaces.view.ViewDeclarationLanguageStrategy;

/**
 * @author Simon Lessard (latest modification by $Author: bommel $)
 * @version $Revision: 1187701 $ $Date: 2011-10-22 12:21:54 +0000 (Sat, 22 Oct 2011) $
 * 
 * @since 2.0
 */
public class FaceletViewDeclarationLanguageStrategy implements ViewDeclarationLanguageStrategy
{
    private Pattern _acceptPatterns;
    private String _extension;

    private ViewDeclarationLanguage _language;

    public FaceletViewDeclarationLanguageStrategy()
    {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext eContext = context.getExternalContext();

        _acceptPatterns = loadAcceptPattern(eContext);

        _extension = loadFaceletExtension(eContext);

        _language = new FaceletViewDeclarationLanguage(context, this);
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
        if (viewId == null)
        {
            return false;
        }
        // Check extension first as it's faster than mappings
        if (viewId.endsWith(_extension))
        {
            // If the extension matches, it's a Facelet viewId.
            return true;
        }

        // Otherwise, try to match the view identifier with the facelet mappings
        return _acceptPatterns != null && _acceptPatterns.matcher(viewId).matches();
    }

    /**
     * Load and compile a regular expression pattern built from the Facelet view mapping parameters.
     * 
     * @param context
     *            the application's external context
     * 
     * @return the compiled regular expression
     */
    private Pattern loadAcceptPattern(ExternalContext context)
    {
        assert context != null;

        String mappings = context.getInitParameter(ViewHandler.FACELETS_VIEW_MAPPINGS_PARAM_NAME);
        if(mappings == null)    //consider alias facelets.VIEW_MAPPINGS
        {
            mappings = context.getInitParameter("facelets.VIEW_MAPPINGS");
        }
        if (mappings == null)
        {
            return null;
        }

        // Make sure the mappings contain something
        mappings = mappings.trim();
        if (mappings.length() == 0)
        {
            return null;
        }

        return Pattern.compile(toRegex(mappings));
    }

    private String loadFaceletExtension(ExternalContext context)
    {
        assert context != null;

        String suffix = context.getInitParameter(ViewHandler.FACELETS_SUFFIX_PARAM_NAME);
        if (suffix == null)
        {
            suffix = ViewHandler.DEFAULT_FACELETS_SUFFIX;
        }
        else
        {
            suffix = suffix.trim();
            if (suffix.length() == 0)
            {
                suffix = ViewHandler.DEFAULT_FACELETS_SUFFIX;
            }
        }

        return suffix;
    }

    /**
     * Convert the specified mapping string to an equivalent regular expression.
     * 
     * @param mappings
     *            le mapping string
     * 
     * @return an uncompiled regular expression representing the mappings
     */
    private String toRegex(String mappings)
    {
        assert mappings != null;

        // Get rid of spaces
        mappings = mappings.replaceAll("\\s", "");

        // Escape '.'
        mappings = mappings.replaceAll("\\.", "\\\\.");

        // Change '*' to '.*' to represent any match
        mappings = mappings.replaceAll("\\*", ".*");

        // Split the mappings by changing ';' to '|'
        mappings = mappings.replaceAll(";", "|");

        return mappings;
    }
}
