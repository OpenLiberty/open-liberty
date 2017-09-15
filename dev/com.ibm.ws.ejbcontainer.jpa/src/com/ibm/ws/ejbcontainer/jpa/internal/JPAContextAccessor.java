/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jpa.internal;

import javax.ejb.EJBException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.ejbcontainer.osgi.EJBContainer;
import com.ibm.ws.jpa.JPAExPcBindingContext;
import com.ibm.ws.jpa.JPAExPcBindingContextAccessor;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public class JPAContextAccessor 
   implements JPAExPcBindingContextAccessor
{

   private AtomicServiceReference<EJBContainer> ejbContainerSR = new AtomicServiceReference<EJBContainer>("ejbContainer");
   
   @Override
   public JPAExPcBindingContext getExPcBindingContext()
   {
      return (JPAExPcBindingContext) ejbContainerSR.getServiceWithException().getExPcBindingContext();
   }
   
   public void activate(ComponentContext cc) {
       ejbContainerSR.activate(cc);
   }

   public void deactivate(ComponentContext cc) {
       ejbContainerSR.deactivate(cc);
   }
   
   public void setEJBContainer(ServiceReference<EJBContainer> reference) {
       ejbContainerSR.setReference(reference);
   }

   public void unsetEJBContainer(ServiceReference<EJBContainer> reference) {
       ejbContainerSR.unsetReference(reference);
   }

   @Override
   public RuntimeException newEJBException(String msg) {
	   return new EJBException(msg);
   }
  
}