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

import java.io.*;

public class Invalidation {
   public String    baseName;
   public String    invalidationGenerator;
   public Component components[];

   public Object    invalidationGeneratorImpl;

      public String toString() {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.println("baseName: "+baseName);
      pw.println("invalidationGenerator: " + invalidationGenerator);  
      for (int i=0;components != null && i<components.length;i++) {
         pw.println("Inval. Component "+i);
         pw.println(components[i]);
      }
      return sw.toString();
   }


   public String fancyFormat(int level) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      for (int i =level;i >0; i--) pw.print("\t");
      pw.println("baseName: "+baseName);
      pw.println("invalidationGenerator: " + invalidationGenerator);
      for (int i=0;components != null && i<components.length;i++) {
         for (int ii = level;ii >0; ii--) pw.print("\t");
         pw.println("Inval. Component "+i);
         pw.println(components[i].fancyFormat(level+1));
      }
      return sw.toString();
   }




   public Object clone() {
      Invalidation c =  new Invalidation();
      c.invalidationGenerator = invalidationGenerator;  
      if (components != null)  {
         c.components = new Component[components.length];
         for (int i = 0; i < components.length;i++) {
            c.components[i] = (Component) components[i].clone();
         }
      }
      c.baseName = baseName;
      return c;
   }

}
