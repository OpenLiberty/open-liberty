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
 
package com.ibm.ws.sib.processor;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;


/**
 MPSelectorEvaluatorFactory is used to create MPSelectorEvaluator objects. It is 
 implemented by SIB.processor. 
*/

public abstract class MPSelectorEvaluatorFactory 
{
  private  final static String MP_SELECTOR_EVALUATOR_FACTORY_CLASS = "com.ibm.ws.sib.processor.matching.MPSelectorEvaluatorFactoryImpl";  
  private static MPSelectorEvaluatorFactory instance = null;
  private static SIErrorException createException = null;

  static 
  {
    /* Create the singleton factory instance                                  */
    try {
      createFactoryInstance();
    }
    catch (SIErrorException e) {
      // No FFDC code needed
      createException = e;
    }
  }  

  /**
   *  Get the singleton MPSelectorEvaluatorFactory which is to be used for
   *  creating MPSelectorEvaluator instances.
   *
   *  @return The MPSelectorEvaluatorFactory
   *
   *  @exception SIErrorException The method rethrows any Exception caught during
   *                       creaton of the singleton factory.
   */
  public static MPSelectorEvaluatorFactory getInstance()  {

    /* If instance creation failed, throw on the Exception                    */
    if (instance == null) {
      throw createException;
    }

    /* Otherwise, return the singleton                                        */
    return instance;
  }
  
  /**
   Creates a new default MPSelectorEvaluator, that can be used to parse selectors
   and evaluate selectors against messages.

   @return a new MPSelectorEvaluator
   
   @see com.ibm.ws.sib.processor.MPSelectorEvaluator
  */
  public abstract MPSelectorEvaluator createMPSelectorEvaluator();
    
  /**
   *  Create the singleton Factory instance.
   *
   *  @exception Exception The method rethrows any Exception caught during
   *                       creaton of the singleton factory.
   */
  private static void createFactoryInstance() 
  {

    try {
      Class cls = Class.forName(MP_SELECTOR_EVALUATOR_FACTORY_CLASS);
      instance = (MPSelectorEvaluatorFactory) cls.newInstance();
    }
    catch (Exception e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.processor.MPSelectorEvaluatorFactory.createFactoryInstance", "100");
      throw new SIErrorException(e);
    }
  }    
    
}
