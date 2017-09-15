/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.config;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class Value {
   public String   value;
   public ArrayList ranges;
   
   public String toString() {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.println("value:  "+value);
      if (ranges != null){
      	Iterator it = ranges.iterator();
      	while (it.hasNext())
      		pw.println(it.next());
      }
      return sw.toString();
   }

   public String fancyFormat(int level) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      for (int i = level;i>0;i--) pw.print("\t");
      pw.println("value:  "+value);
      if (ranges != null){
      	Iterator it = ranges.iterator();
      	while (it.hasNext())
      		pw.println(it.next());
      }
      return sw.toString();
   }



   public Object clone() {
      Value c =  new Value();
      c.value = value;
      if (ranges != null){
      	c.ranges = new ArrayList();
      	for (Iterator i = ranges.iterator(); i.hasNext();) {
      		c.ranges.add(((Range)i.next()).clone());
      	}
      }
      return c;
   }
}
