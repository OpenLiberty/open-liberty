/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.config;

import org.xml.sax.Attributes;

public class ElementHandler {
     protected StringBuffer characters = new StringBuffer();

     public ElementHandler() {
     }

     public void addRules(RuleHandler ruleHandler) {
     }

     public void startElement (String uri, String name,String qName, Attributes attrs) {
     }
     
     public void endElement (String uri,String name, String qName)
     {
        finished();
        characters = new StringBuffer();
     }

     public void finished() {
     }

     public void characters(char chars[], int start, int length) {
    	String s = new String(chars,start,length);
     	if (!s.equals(" "))
     		s = s.trim();
        if (s.length()==0)
           return;
        characters.append(s);
     }

     public String getCharacters() {
        return characters.toString();
     }

}
