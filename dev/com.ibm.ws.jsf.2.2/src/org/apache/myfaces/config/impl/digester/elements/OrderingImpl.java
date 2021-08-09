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
package org.apache.myfaces.config.impl.digester.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OrderingImpl extends org.apache.myfaces.config.element.Ordering implements Serializable
{
    
    private List<org.apache.myfaces.config.element.OrderSlot> beforeList
            = new ArrayList<org.apache.myfaces.config.element.OrderSlot>();
    private List<org.apache.myfaces.config.element.OrderSlot> afterList
            = new ArrayList<org.apache.myfaces.config.element.OrderSlot>();

    public void addBeforeSlot(org.apache.myfaces.config.element.OrderSlot slot)
    {
        beforeList.add(slot);
    }
    
    public List<org.apache.myfaces.config.element.OrderSlot> getBeforeList()
    {
        return beforeList;
    }
    
    public void addAfterSlot(org.apache.myfaces.config.element.OrderSlot slot)
    {
        afterList.add(slot);
    }
    
    public List<org.apache.myfaces.config.element.OrderSlot> getAfterList()
    {
        return afterList;
    }    
}
