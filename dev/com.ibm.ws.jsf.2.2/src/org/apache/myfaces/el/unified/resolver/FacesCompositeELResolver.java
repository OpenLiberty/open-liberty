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
package org.apache.myfaces.el.unified.resolver;

import javax.el.ELContext;
import javax.faces.context.FacesContext;
import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.Map;
import java.util.Arrays;

/**
 * <p>
 * This composite el resolver will be used at the top level resolver for faces
 * ({@link javax.faces.application.Application#getELResolver()})
 * and jsp (the one we add with {@link javax.servlet.jsp.JspApplicationContext#addELResolver(javax.el.ELResolver)}.
 * It keeps track of its scope to let the variable resolver {@link org.apache.myfaces.el.VariableResolverImpl}
 * know in which scope it is executed. This is
 * necessarry to call either the faces or the jsp resolver head.
 * </p>
 * <p>
 * This implementation does nothing if there is no actual faces context. This is necessarry since we registered our
 * resolvers into the jsp engine. Therefore we have to make sure that jsp only pages where no faces context is available
 * are still working
 * </p>
 *
 * @author Mathias Broekelmann (latest modification by $Author: lu4242 $)
 * @version $Revision: 1341400 $ $Date: 2012-05-22 10:57:55 +0000 (Tue, 22 May 2012) $
 */
public final class FacesCompositeELResolver extends org.apache.myfaces.el.CompositeELResolver
{
    private final Scope _scope;

    public enum Scope
    {
        Faces, JSP, NONE
    }
    
    public static final String SCOPE = "org.apache.myfaces.el.unified.resolver.FacesCompositeELResolver.Scope";
    
    public FacesCompositeELResolver(final Scope scope)
    {
        if (scope == null)
        {
            throw new IllegalArgumentException("scope must not be one of " + Arrays.toString(Scope.values()));
        }
        _scope = scope;
    }

    private static FacesContext facesContext(final ELContext context)
    {
        FacesContext facesContext = (FacesContext)context.getContext(FacesContext.class);
        if (facesContext == null)
        {
            facesContext = FacesContext.getCurrentInstance();
        }
        return facesContext;
    }
    
    @Override
    public Class<?> getCommonPropertyType(final ELContext context, final Object base)
    {
        final FacesContext facesContext = facesContext(context);
        if (facesContext == null)
        {
            return null;
        }
        final Map<Object, Object> requestMap = facesContext.getAttributes();
        Scope prevScope = null;
        try
        {
            prevScope = getScope(requestMap);
            setScope(requestMap);
            return super.getCommonPropertyType(context, base);
        }
        finally
        {
            if(prevScope != null)
            {
                setScope(requestMap, prevScope);
            }
            else
            {
                unsetScope(requestMap);
            }
        }

    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(final ELContext context, final Object base)
    {
        final FacesContext facesContext = facesContext(context);
        if (facesContext == null)
        {
            return null;
        }
        final Map<Object, Object> requestMap = facesContext.getAttributes();
        Scope prevScope = null;
        try
        {
            prevScope = getScope(requestMap);
            setScope(requestMap);
            return super.getFeatureDescriptors(context, base);

        }
        finally
        {
            if(prevScope != null)
            {
                setScope(requestMap, prevScope);
            }
            else
            {
                unsetScope(requestMap);
            }
        }
    }

    @Override
    public Class<?> getType(final ELContext context, final Object base, final Object property)
    {
        final FacesContext facesContext = facesContext(context);
        if (facesContext == null)
        {
            return null;
        }
        final Map<Object, Object> requestMap = facesContext.getAttributes();
        Scope prevScope = null;
        try
        {
            prevScope = getScope(requestMap);
            setScope(requestMap);
            return super.getType(context, base, property);
        }
        finally
        {
            if(prevScope != null)
            {
                setScope(requestMap, prevScope);
            }
            else
            {
                unsetScope(requestMap);
            }
        }
    }

    @Override
    public Object getValue(final ELContext context, final Object base, final Object property)
    {
        final FacesContext facesContext = facesContext(context);
        if (facesContext == null)
        {
            return null;
        }
        final Map<Object, Object> requestMap = facesContext.getAttributes();
        Scope prevScope = null;
        try
        {
            prevScope = getScope(requestMap);
            setScope(requestMap);
            return super.getValue(context, base, property);
        }
        finally
        {
            if(prevScope != null)
            {
                setScope(requestMap, prevScope);
            }
            else
            {
                unsetScope(requestMap);
            }
        }
    }

    @Override
    public boolean isReadOnly(final ELContext context, final Object base, final Object property)
    {
        final FacesContext facesContext = facesContext(context);
        if (facesContext == null)
        {
            return false;
        }
        final Map<Object, Object> requestMap = facesContext.getAttributes();
        Scope prevScope = null;
        try
        {
            prevScope = getScope(requestMap);
            setScope(requestMap);
            return super.isReadOnly(context, base, property);
        }
        finally
        {
            if(prevScope != null)
            {
                setScope(requestMap, prevScope);
            }
            else
            {
                unsetScope(requestMap);
            }
        }
    }

    @Override
    public void setValue(final ELContext context, final Object base, final Object property, final Object val)
    {
        final FacesContext facesContext = facesContext(context);
        if (facesContext == null)
        {
            return;
        }
        final Map<Object, Object> requestMap = facesContext.getAttributes();
        Scope prevScope = null;
        try
        {
            prevScope = getScope(requestMap);
            setScope(requestMap);
            super.setValue(context, base, property, val);

        }
        finally
        {
            if(prevScope != null)
            {
                setScope(requestMap, prevScope);
            }
            else
            {
                unsetScope(requestMap);
            }
        }
    }

    private void setScope(final Map<Object, Object> attributes)
    {
        attributes.put(SCOPE, _scope);
    }
    
    private Scope getScope(final Map<Object, Object> attributes)
    {
        return (Scope) attributes.get(SCOPE);
    }

    private void setScope(final Map<Object, Object> attributes, Scope prevScope)
    {
        attributes.put(SCOPE, prevScope);
    }

    private static void unsetScope(final Map<Object, Object> attributes)
    {
        //attributes.remove(SCOPE);
        attributes.put(SCOPE, Scope.NONE);
    }
}
