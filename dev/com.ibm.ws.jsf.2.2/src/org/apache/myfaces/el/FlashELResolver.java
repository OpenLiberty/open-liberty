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
package org.apache.myfaces.el;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Resolver for Flash object 
 * 
 * @author Leonardo Uribe (latest modification by $Author: struberg $)
 * @version $Revision: 1188895 $ $Date: 2011-10-25 20:31:51 +0000 (Tue, 25 Oct 2011) $
 */
public class FlashELResolver extends ELResolver
{

    private final static String FLASH = "flash";

    private final static String KEEP = "keep";

    private final static String NOW = "now";

    public FlashELResolver()
    {
        super();
    }

    @Override
    public void setValue(ELContext context, Object base, Object property,
            Object value) throws NullPointerException,
            PropertyNotFoundException, PropertyNotWritableException,
            ELException
    {
        if (property == null)
        {
            throw new PropertyNotFoundException();
        }
        if (!(property instanceof String))
        {
            return;
        }

        String strProperty = property.toString();

        if (FLASH.equals(strProperty))
        {
            throw new PropertyNotWritableException();
        }
        else if (base instanceof Flash)
        {
            context.setPropertyResolved(true);
            try
            {
                ((Flash) base).put(strProperty, value);
            }
            catch (UnsupportedOperationException e)
            {
                throw new PropertyNotWritableException(e);
            }
        }
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property)
            throws NullPointerException, PropertyNotFoundException, ELException
    {

        if (property == null)
        {
            throw new PropertyNotFoundException();
        }
        if (!(property instanceof String))
        {
            return false;
        }

        String strProperty = property.toString();

        if (FLASH.equals(strProperty))
        {
            context.setPropertyResolved(true);
            return true;
        }
        else if (base instanceof Flash)
        {
            context.setPropertyResolved(true);
        }

        return false;
    }

    @Override
    public Object getValue(ELContext elContext, Object base, Object property)
            throws NullPointerException, PropertyNotFoundException, ELException
    {

        if (property == null)
        {
            throw new PropertyNotFoundException();
        }
        if (!(property instanceof String))
        {
            return null;
        }

        String strProperty = property.toString();

        if (base == null)
        {
            if (FLASH.equals(strProperty))
            {
                FacesContext facesContext = facesContext(elContext);
                if (facesContext == null)
                {
                    return null;
                }
                ExternalContext externalContext = facesContext.getExternalContext();
                if (externalContext == null)
                {
                    return null;
                }

                //Access to flash object
                elContext.setPropertyResolved(true);
                Flash flash = externalContext.getFlash();
                //This is just to make sure after this point
                //we are not in "keep" promotion.
                setDoKeepPromotion(false, facesContext);
                
                // Note that after this object is returned, Flash.get() and Flash.put()
                // methods are called from javax.el.MapELResolver, since 
                // Flash is instance of Map.
                return flash;
            }
        }
        else if (base instanceof Flash)
        {
            FacesContext facesContext = facesContext(elContext);
            if (facesContext == null)
            {
                return null;
            }
            ExternalContext externalContext = facesContext.getExternalContext();
            if (externalContext == null)
            {
                return null;
            }
            Flash flash = (Flash) base;
            if (KEEP.equals(strProperty))
            {
                setDoKeepPromotion(true, facesContext);
                // Since we returned a Flash instance getValue will 
                // be called again but this time the property name
                // to be resolved will be called, so we can do keep
                // promotion.
                elContext.setPropertyResolved(true);
                return base;
            }
            else if (NOW.equals(strProperty))
            {
                //Prevent invalid syntax #{flash.keep.now.someKey}
                if (!isDoKeepPromotion(facesContext))
                {
                    // According to the javadoc of Flash.putNow() and 
                    // Flash.keep(), this is an alias to requestMap, used
                    // as a "buffer" to promote vars to flash scope using
                    // "keep" method
                    elContext.setPropertyResolved(true);
                    return externalContext.getRequestMap();
                }
            }
            else if (isDoKeepPromotion(facesContext))
            {
                //Resolve property calling get or keep
                elContext.setPropertyResolved(true);
                //promote it to flash scope
                flash.keep(strProperty);
                //Obtain the value on requestMap if any
                Object value = externalContext.getRequestMap().get(strProperty);
                return value;
            }
            else
            {
                //Just get the value
                elContext.setPropertyResolved(true);
                return flash.get(strProperty);
            }
        }
        return null;
    }
    
    /**
     * This var indicate if we are inside a keep operation
     * or not. We go into keep status in two cases:
     * 
     * - A direct call to Flash.keep(String key)
     * - A lookup to keep map using a value expression #{flash.keep.someKey}.
     *   This occur when the ELResolver try to get the keep object.
     *   
     * Note that when "keep" is resolved by FlashELResolver,
     * we need a way to comunicate that the current lookup is 
     * for keep promotion.
     * 
     * This var do the job.
     */
    private static final String KEEP_STATUS_KEY = "org.apache.myfaces.el.FlashELResolver.KEEP_STATUS";

    private static boolean isDoKeepPromotion(FacesContext facesContext)
    {
        Boolean doKeepPromotion = (Boolean) facesContext.getAttributes().get(KEEP_STATUS_KEY);

        if (doKeepPromotion == null)
        {
            doKeepPromotion = false;
        }

        return doKeepPromotion;
    }

    private static void setDoKeepPromotion(boolean value, FacesContext facesContext)
    {
        facesContext.getAttributes().put(KEEP_STATUS_KEY, Boolean.valueOf(value));
    }
    
    // get the FacesContext from the ELContext
    protected FacesContext facesContext(ELContext context)
    {
        return (FacesContext) context.getContext(FacesContext.class);
    }

    protected ExternalContext externalContext(ELContext context)
    {
        return facesContext(context).getExternalContext();
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property)
            throws NullPointerException, PropertyNotFoundException, ELException
    {

        if (property == null)
        {
            throw new PropertyNotFoundException();
        }
        if (!(property instanceof String))
        {
            return null;
        }

        String strProperty = property.toString();

        if (FLASH.equals(strProperty))
        {
            context.setPropertyResolved(true);
        }
        else if (base instanceof Flash)
        {
            context.setPropertyResolved(true);
            Object obj = ((Flash) base).get(property);
            return (obj != null) ? obj.getClass() : null;
        }

        return null;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context,
            Object base)
    {
        ArrayList<FeatureDescriptor> descriptors = new ArrayList<FeatureDescriptor>(1);

        descriptors.add(makeDescriptor(FLASH,
                "Represents the current flash scope", Object.class));

        if (base instanceof Flash)
        {
            Iterator itr = ((Flash) base).keySet().iterator();
            Object key;
            FeatureDescriptor desc;
            while (itr.hasNext())
            {
                key = itr.next();
                desc = makeDescriptor(key.toString(), key.toString(), key.getClass());
                descriptors.add(desc);
            }
        }
        return descriptors.iterator();
    }

    protected FeatureDescriptor makeDescriptor(String name, String description,
            Class<?> elResolverType)
    {
        FeatureDescriptor fd = new FeatureDescriptor();
        fd.setValue(ELResolver.RESOLVABLE_AT_DESIGN_TIME, Boolean.TRUE);
        fd.setValue(ELResolver.TYPE, elResolverType);
        fd.setName(name);
        fd.setDisplayName(name);
        fd.setShortDescription(description);
        fd.setExpert(false);
        fd.setHidden(false);
        fd.setPreferred(true);
        return fd;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base)
    {
        if (base == null)
        {
            return null;
        }

        if (base instanceof Flash)
        {
            return Object.class;
        }
        else if (FLASH.equals(base.toString()))
        {
            return Object.class;
        }

        return null;
    }

}
