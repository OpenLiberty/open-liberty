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
package com.ibm.ws.cache;


public class UncacheableStateException extends Exception {
   private static final long serialVersionUID = 852878369455272764L;
    
   protected String uri = null;
   protected String parentURI = null;

   public UncacheableStateException(String message, String uri) {
      super(message);
      this.uri = uri;
   }

   public String getURI() {
      return uri;
   }

   public String getParentURI() {
      return parentURI;
   }

   public void setParentURI(String parentURI) {
      this.parentURI = parentURI;
   }
}
