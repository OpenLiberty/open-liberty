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
package org.apache.myfaces.el.unified.resolver.implicitobject;

import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.PropertyNotFoundException;
import jakarta.el.PropertyNotWritableException;
import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * See JSF 1.2 spec sections 5.6.1.1 and 5.6.2.1
 * 
 * @author Stan Silvert
 */
public class ImplicitObjectResolver extends ELResolver
{

    private Map<String, ImplicitObject> implicitObjects;

    /**
     * Static factory for an ELResolver for resolving implicit objects in JSPs. See JSF 1.2 spec section 5.6.1.1
     */
    public static ELResolver makeResolverForJSP()
    {
        Map<String, ImplicitObject> forJSPList = new HashMap<>(8); //4
        ImplicitObject io1 = new FacesContextImplicitObject();
        forJSPList.put(io1.getName(), io1);
        ImplicitObject io2 = new ViewImplicitObject();
        forJSPList.put(io2.getName(), io2);
        ImplicitObject io3 = new ResourceImplicitObject();
        forJSPList.put(io3.getName(), io3);
        ImplicitObject io4 = new ViewScopeImplicitObject();
        forJSPList.put(io4.getName(), io4);
        return new ImplicitObjectResolver(forJSPList);
    }

    /**
     * Static factory for an ELResolver for resolving implicit objects in all of Faces. See JSF 1.2 spec section 5.6.1.2
     */
    public static ELResolver makeResolverForFaces()
    {
        Map<String, ImplicitObject> forFacesList = new HashMap<>(30); //19
        ImplicitObject io1 = new ApplicationImplicitObject();
        forFacesList.put(io1.getName(), io1);
        ImplicitObject io2 = new ApplicationScopeImplicitObject();
        forFacesList.put(io2.getName(), io2);
        ImplicitObject io3 = new CookieImplicitObject();
        forFacesList.put(io3.getName(), io3);
        ImplicitObject io4 = new FacesContextImplicitObject();
        forFacesList.put(io4.getName(), io4);
        ImplicitObject io5 = new HeaderImplicitObject();
        forFacesList.put(io5.getName(), io5);
        ImplicitObject io6 = new HeaderValuesImplicitObject();
        forFacesList.put(io6.getName(), io6);
        ImplicitObject io7 = new InitParamImplicitObject();
        forFacesList.put(io7.getName(), io7);
        ImplicitObject io8 = new ParamImplicitObject();
        forFacesList.put(io8.getName(), io8);
        ImplicitObject io9 = new ParamValuesImplicitObject();
        forFacesList.put(io9.getName(), io9);
        ImplicitObject io10 = new RequestImplicitObject();
        forFacesList.put(io10.getName(), io10);
        ImplicitObject io11 = new RequestScopeImplicitObject();
        forFacesList.put(io11.getName(), io11);
        ImplicitObject io12 = new SessionImplicitObject();
        forFacesList.put(io12.getName(), io12);
        ImplicitObject io13 = new SessionScopeImplicitObject();
        forFacesList.put(io13.getName(), io13);
        ImplicitObject io14 = new ViewImplicitObject();
        forFacesList.put(io14.getName(), io14);
        ImplicitObject io15 = new ComponentImplicitObject();
        forFacesList.put(io15.getName(), io15);
        ImplicitObject io16 = new ResourceImplicitObject();
        forFacesList.put(io16.getName(), io16);
        ImplicitObject io17 = new ViewScopeImplicitObject();
        forFacesList.put(io17.getName(), io17);
        ImplicitObject io18 = new CompositeComponentImplicitObject();
        forFacesList.put(io18.getName(), io18);
        ImplicitObject io19 = new FlowScopeImplicitObject();
        forFacesList.put(io19.getName(), io19);
        return new ImplicitObjectResolver(forFacesList);
    }
    
    public static ELResolver makeResolverForFacesCDI()
    {
        Map<String, ImplicitObject> forFacesCDIList = new HashMap<>(4); //2
        ImplicitObject io;
        
        io = new ComponentImplicitObject();
        forFacesCDIList.put(io.getName(), io);
        
        io = new CompositeComponentImplicitObject();
        forFacesCDIList.put(io.getName(), io);
        
        return new ImplicitObjectResolver(forFacesCDIList);
    }

    private ImplicitObjectResolver()
    {
        super();
        this.implicitObjects = new HashMap<>();
    }

    /** Creates a new instance of ImplicitObjectResolverForJSP */
    private ImplicitObjectResolver(Map<String, ImplicitObject> implicitObjects)
    {
        this();
        this.implicitObjects = implicitObjects;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) throws NullPointerException,
        PropertyNotFoundException, PropertyNotWritableException, ELException
    {

        if (base != null)
        {
            return;
        }
        if (property == null)
        {
            throw new PropertyNotFoundException();
        }
        if (!(property instanceof String))
        {
            return;
        }

        String strProperty = property.toString();

        if (implicitObjects.containsKey(strProperty))
        {
            throw new PropertyNotWritableException();
        }
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) throws NullPointerException,
        PropertyNotFoundException, ELException
    {

        if (base != null)
        {
            return false;
        }
        if (property == null)
        {
            throw new PropertyNotFoundException();
        }
        if (!(property instanceof String))
        {
            return false;
        }

        String strProperty = property.toString();

        if (implicitObjects.containsKey(strProperty))
        {
            context.setPropertyResolved(true);
            return true;
        }

        return false;
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property) throws NullPointerException,
        PropertyNotFoundException, ELException
    {

        if (base != null)
        {
            return null;
        }
        if (property == null)
        {
            throw new PropertyNotFoundException();
        }
        if (!(property instanceof String))
        {
            return null;
        }

        String strProperty = property.toString();

        ImplicitObject obj = implicitObjects.get(strProperty);
        if (obj != null)
        {
            context.setPropertyResolved(true);
            return obj.getValue(context);
        }

        return null;
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) throws NullPointerException,
        PropertyNotFoundException, ELException
    {

        if (base != null)
        {
            return null;
        }
        if (property == null)
        {
            throw new PropertyNotFoundException();
        }
        if (!(property instanceof String))
        {
            return null;
        }

        String strProperty = property.toString();

        if (implicitObjects.containsKey(strProperty))
        {
            context.setPropertyResolved(true);
        }

        return null;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base)
    {
        if (base != null)
        {
            return null;
        }

        ArrayList<FeatureDescriptor> descriptors = new ArrayList<>(implicitObjects.size());

        for (ImplicitObject obj : implicitObjects.values())
        {
            descriptors.add(obj.getDescriptor());
        }

        return descriptors.iterator();
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base)
    {
        if (base != null)
        {
            return null;
        }

        return String.class;
    }

}
