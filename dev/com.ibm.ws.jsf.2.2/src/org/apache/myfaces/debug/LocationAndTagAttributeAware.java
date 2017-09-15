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
package org.apache.myfaces.debug;



/**
 * Identification inferface for types that know about {@link javax.faces.view.Location}
 * and XML attribute name/value pair.
 * 
 *  <ol>
 *      <li>Location -  location instance - see {@link LocationAware}</li>
 *      <li>expressionString - expression String {@link javax.el.Expression#getExpressionString()}</li>
 *      <li>qName - the qualified name for attribute {@link javax.faces.view.facelets.TagAttribute#getQName()}</li>
 *  </ol>   
 * 
 *  If type implements this interface, we can say that it knows where instance implementing this
 *  interface is located in facelets view (line/column)
 *  and what XML attribute (name/value pair) makes it.
 *  
 *  Also you can think about this type as about ligthweight version of {@link TagAttributeAware}
 *  for cases, where full TagAttribute instance is not available
 *  
 * @author martinkoci
 */
public interface LocationAndTagAttributeAware extends LocationAware
{

    
    /**
     * @return expression string, for example "#{bean.actionMethod}" or "success"
     */
    public abstract String getExpressionString();

    /**
     * @return qName of XML attribute, for example "action" or "value"
     */
    public abstract String getQName();

}
