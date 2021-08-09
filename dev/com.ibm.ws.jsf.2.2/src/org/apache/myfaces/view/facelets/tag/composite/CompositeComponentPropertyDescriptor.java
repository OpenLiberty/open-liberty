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
package org.apache.myfaces.view.facelets.tag.composite;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Serializable implementation of PropertyDescriptor
 * 
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 881558 $ $Date: 2009-11-17 21:55:58 +0000 (Tue, 17 Nov 2009) $
 */
public class CompositeComponentPropertyDescriptor extends PropertyDescriptor 
    implements Externalizable
{
    
    /**
     * Used for serialization only
     * 
     * @throws IntrospectionException
     */
    public CompositeComponentPropertyDescriptor() throws IntrospectionException
    {
        super("a",null,null);
    }
    
    public CompositeComponentPropertyDescriptor(String propertyName)
            throws IntrospectionException
    {
        super(propertyName, null, null);
    }

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException
    {
        setName((String) in.readObject());
        setDisplayName((String) in.readObject());
        setExpert(in.readBoolean());
        setPreferred(in.readBoolean());
        setShortDescription((String)in.readObject());
        
        Map<String,Object> map = (Map) in.readObject();
        
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            setValue(entry.getKey(), entry.getValue());
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(getName());
        out.writeObject(getDisplayName());
        out.writeBoolean(isExpert());
        out.writeBoolean(isPreferred());
        out.writeObject(getShortDescription());
        
        // Properties that comes here are targets, default, required,
        // method-signature and type.
        Map<String,Object> map = new HashMap<String, Object>(6,1);
        
        for (Enumeration<String> e = attributeNames(); e.hasMoreElements();)
        {
            String name = e.nextElement();
            map.put(name, getValue(name));
        }
        out.writeObject(map);
    }
}
