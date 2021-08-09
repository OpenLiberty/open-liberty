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
package javax.faces.application;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.faces.FacesWrapper;
import javax.faces.context.FacesContext;

/**
 * @since 2.2
 */
public abstract class NavigationCaseWrapper extends NavigationCase
    implements FacesWrapper<NavigationCase>
{
    public NavigationCaseWrapper()
    {
        super(null, null, null, null, null, null, false, false);
    }

    public boolean equals(Object o)
    {
        return getWrapped().equals(o);
    }

    public int hashCode()
    {
        return getWrapped().hashCode();
    }

    public URL getActionURL(FacesContext context) throws MalformedURLException
    {
        return getWrapped().getActionURL(context);
    }

    public Boolean getCondition(FacesContext context)
    {
        return getWrapped().getCondition(context);
    }

    public String getFromAction()
    {
        return getWrapped().getFromAction();
    }

    public String getFromOutcome()
    {
        return getWrapped().getFromOutcome();
    }

    public String getFromViewId()
    {
        return getWrapped().getFromViewId();
    }

    public URL getBookmarkableURL(FacesContext context) throws MalformedURLException
    {
        return getWrapped().getBookmarkableURL(context);
    }

    public URL getResourceURL(FacesContext context) throws MalformedURLException
    {
        return getWrapped().getResourceURL(context);
    }

    public URL getRedirectURL(FacesContext context) throws MalformedURLException
    {
        return getWrapped().getRedirectURL(context);
    }

    public Map<String, List<String>> getParameters()
    {
        return getWrapped().getParameters();
    }

    public String getToViewId(FacesContext context)
    {
        return getWrapped().getToViewId(context);
    }

    public boolean hasCondition()
    {
        return getWrapped().hasCondition();
    }

    public boolean isIncludeViewParams()
    {
        return getWrapped().isIncludeViewParams();
    }

    public boolean isRedirect()
    {
        return getWrapped().isRedirect();
    }

    public String getToFlowDocumentId()
    {
        return getWrapped().getToFlowDocumentId();
    }

    public String toString()
    {
        return getWrapped().toString();
    }
    
    public abstract NavigationCase getWrapped();
}
