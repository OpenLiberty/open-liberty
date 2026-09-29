/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.jmx;

import java.io.ObjectInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.misc.ObjectInputFilter;

import com.ibm.ws.jmx.connector.client.rest.internal.DeserializationHelper;

public class Java8HelperImpl implements DeserializationHelper {

   private static final Logger logger = Logger.getLogger(Java8HelperImpl.class.getName());

   public void addInterfaceFilter(ObjectInputStream ois, Class type) {
      ObjectInputFilter f = new Java8HelperImpl.InterfaceFilter(type);
      ObjectInputFilter.Config.setObjectInputFilter(ois, f);
      return;
   }

   // Although this code is identical to the Java 9 code, it is implementing
   // an interface in a different package, so I don't think it can be
   // easily merged.
   static class InterfaceFilter implements ObjectInputFilter {

      private Class interfaceClass;

      public InterfaceFilter(Class interfaceClass) {
         this.interfaceClass = interfaceClass;
      }

      @Override
      public Status checkInput(FilterInfo filterInfo) {
         Class<?> serialClass = filterInfo.serialClass();

         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Filtering : " + serialClass + " against filter class : "
               + interfaceClass + " and depth is " + filterInfo.depth() + " and references are " + filterInfo.references());
         }
         
         if (serialClass == null) {
            return Status.UNDECIDED;
         }
         
         // This should be the top level class in the tree, the one that must implement
         // the specified interface
         if (filterInfo.depth() == 1) {
            if (interfaceClass.isAssignableFrom(serialClass)) {
                return Status.ALLOWED;
            } else {
               return Status.REJECTED;
            }
         }
         
         return Status.UNDECIDED;
      }

   }
}
