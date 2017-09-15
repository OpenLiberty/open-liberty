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
package com.ibm.ws.sib.processor.impl.store.filters;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;

/**
 * @author caseyj
 * 
 * This filter returns true whenever the class given matches that of an Item
 * in the ItemStream it is set to filter.
 */
public final class ClassEqualsFilter implements Filter
{
  private static TraceComponent tc =
    SibTr.register(
      ClassEqualsFilter.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  
  /**
   * The class to use in filter comparisons.
   */
  private Class comparisonClass;
  private Class comparisonClass2 = null;
  
  public ClassEqualsFilter(Class comparisonClass)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "ClassEqualsFilter", comparisonClass);    
    
    this.comparisonClass = comparisonClass;
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "ClassEqualsFilter", this);    
  }

  public ClassEqualsFilter(Class comparisonClass1, Class comparisonClass2)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "ClassEqualsFilter", new Object[] {comparisonClass1, comparisonClass2});    
    
    this.comparisonClass = comparisonClass1;
    this.comparisonClass2 = comparisonClass2;
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "ClassEqualsFilter", this);    
  }

  /**
   * @see com.ibm.ws.sib.msgstore.Filter
   */
  public boolean filterMatches(AbstractItem item)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "filterMatches", item);

    boolean matchResult = false;
    
    if (item.getClass().equals(comparisonClass))
    {
      matchResult = true;
    }
    else
    {
      if (comparisonClass2 != null)
      {
        if (item.getClass().equals(comparisonClass2))
        {
          matchResult = true;
        }
      }
    }
       
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "filterMatches", new Boolean(matchResult));

    return matchResult;
  }
}
