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
package org.apache.myfaces.shared.test;

import java.util.List;
import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @see AbstractClassElementTestCase
 */

public class ClassElementHandler extends DefaultHandler
{
    
    private boolean clazz ;
    private List elementName = new ArrayList();
    private List className = new ArrayList();
    private StringBuffer buffer ;
    
    public ClassElementHandler()
    {
        
        elementName.add("component-class");
        elementName.add("tag-class");
        elementName.add("renderer-class");
        elementName.add("validator-class");
        elementName.add("converter-class");
        elementName.add("action-listener");
        elementName.add("navigation-handler");
        elementName.add("variable-resolver");
        elementName.add("property-resolver");
        elementName.add("phase-listener");
        
    }

    public void characters(char[] ch, int start, int length)
    throws SAXException
    {
        if (clazz)
        {
            String string = new String(ch, start, length);
            if(string != null)
            {
                buffer.append(string.trim());
            }
        }
    }
    
    public void startElement(
            String ns, String local, String qName, Attributes atts) 
            throws SAXException
    {
       
         clazz = elementName.contains(qName);
         
         if(clazz)
         {
             buffer = new StringBuffer();
         }
        
    }

    public void endElement(String ns, String local, String qName) 
        throws SAXException
    {
        
        if(clazz)
        {
            className.add(buffer.toString());
            clazz = false;
        }
        
    }

    public List getClassName()
    {
        return className;
    }
    
}
