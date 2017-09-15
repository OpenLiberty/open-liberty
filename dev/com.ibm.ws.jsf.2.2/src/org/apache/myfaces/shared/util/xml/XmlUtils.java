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
package org.apache.myfaces.shared.util.xml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class XmlUtils
{
    private XmlUtils()
    {
        // hide from public access
    }

    public static String getElementText(Element elem)
    {
        StringBuilder buf = new StringBuilder();
        NodeList nodeList = elem.getChildNodes();
        for (int i = 0, len = nodeList.getLength(); i < len; i++)
        {
            Node n = nodeList.item(i);
            if (n.getNodeType() == Node.TEXT_NODE)
            {
                buf.append(n.getNodeValue());
            }
            else
            {
                //TODO see jsf-samples
                //throw new FacesException("Unexpected node type " + n.getNodeType());
            }
        }
        return buf.toString();
    }



    /**
     * Return content of child element with given tag name.
     * If more than one children with this name are present, the content of the last
     * element is returned.
     *
     * @param elem
     * @param childTagName
     * @return content of child element or null if no child element with this name was found
     */
    public static String getChildText(Element elem, String childTagName)
    {
        NodeList nodeList = elem.getElementsByTagName(childTagName);
        int len = nodeList.getLength();
        if (len == 0)
        {
            return null;
        }

        return getElementText((Element)nodeList.item(len - 1));
        
   }


    /**
     * Return list of content Strings of all child elements with given tag name.
     * @param elem
     * @param childTagName
     * @return List 
     */
    public static List getChildTextList(Element elem, String childTagName)
    {
        NodeList nodeList = elem.getElementsByTagName(childTagName);
        int len = nodeList.getLength();
        if (len == 0)
        {
            return Collections.EMPTY_LIST;
        }

        List list = new ArrayList(len);
        for (int i = 0; i < len; i++)
        {
            list.add(getElementText((Element)nodeList.item(i)));
        }
        return list;
        
   }

}
