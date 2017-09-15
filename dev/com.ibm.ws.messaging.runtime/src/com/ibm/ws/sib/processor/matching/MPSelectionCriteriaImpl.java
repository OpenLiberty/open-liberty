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
package com.ibm.ws.sib.processor.matching;

import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.MPSelectionCriteria;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectorDomain;

public class MPSelectionCriteriaImpl implements MPSelectionCriteria
{
  //trace
  private static final TraceComponent tc =
    SibTr.register(
        MPSelectionCriteriaImpl.class,
        SIMPConstants.MP_TRACE_GROUP,
        SIMPConstants.RESOURCE_BUNDLE);


  private String discriminator = null;
  private String selectorString = null;
  private SelectorDomain domain = null;
  // A map of properties associated with a selector
  private Map<String, Object> selectorProperties = null;

  /**
   * MPSelectionCriteria additionally (to SelectionCriteria) support a selectorProperties map
   */
  public MPSelectionCriteriaImpl(String discriminator,
                                 String selectorString,
                                 SelectorDomain selectorDomain,
                                 Map<String, Object> selectorProperties)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "MPSelectionCriteriaImpl",
        new Object[]{discriminator, selectorString, selectorDomain, selectorProperties});

    this.discriminator = discriminator;
    this.selectorString = selectorString;
    domain = selectorDomain;
    this.selectorProperties = selectorProperties;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MPSelectionCriteriaImpl");
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.SelectionCriteria#getSelectorDomain()
   */
  public SelectorDomain getSelectorDomain()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getSelectorDomain");
      SibTr.exit(tc, "getSelectorDomain", domain);
    }
    return domain;
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.SelectionCriteria#getSelectorString()
   */
  public String getSelectorString()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getSelectorString");
      SibTr.exit(tc, "getSelectorString", selectorString);
    }
    return selectorString;
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.SelectionCriteria#setSelectorDomain(com.ibm.wsspi.sib.core.SelectorDomain)
   */
  public void setSelectorDomain(SelectorDomain domain)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setSelectorDomain", domain);

    this.domain = domain;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setSelectorDomain");
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.SelectionCriteria#setSelectorString(java.lang.String)
   */
  public void setSelectorString(String selector)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setSelectorString", selector);

    this.selectorString = selector;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setSelectorString");
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.SelectionCriteria#getDiscriminator()
   */
  public String getDiscriminator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getDiscriminator");
      SibTr.exit(tc, "getDiscriminator", discriminator);
    }
    return discriminator;
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.SelectionCriteria#setDiscriminator(java.lang.String)
   */
  public void setDiscriminator(String discriminator)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setDiscriminator", discriminator);

    this.discriminator = discriminator;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setDiscriminator");
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object other)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "equals", other);
    boolean res = false;
    if(other instanceof MPSelectionCriteriaImpl)
    {
      MPSelectionCriteriaImpl otherCriteria = (MPSelectionCriteriaImpl)other;
      if(otherCriteria.discriminator == null)
      {
        if(discriminator == null)
        {
          if(otherCriteria.selectorString == null)
          {
            if(selectorString == null)
              res = true; // Both are null
          }
          else
          {
            // Non null other selector
            if(selectorString != null && selectorString.equals(otherCriteria.selectorString))
            {
              // Go on to check selector domain
              if(domain.equals(otherCriteria.domain))
                res = true;  // Null discriminator, nonnull string
            }
          }
        }
      }
      else // other discriminator is non null
      {
        if(discriminator != null && discriminator.equals(otherCriteria.discriminator))
        {
          // Discriminators are equal
          if(otherCriteria.selectorString == null)
          {
            if(selectorString == null)
              res = true; // Both selectors are null
          }
          else
          {
            // Non null other selector
            if(selectorString != null && selectorString.equals(otherCriteria.selectorString))
            {
              // Go on to check selector domain
              if(domain.equals(otherCriteria.domain))
                res = true;  // nonnull discriminator, nonnull string
            }
          }
        }
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "equals", Boolean.valueOf(res));
    return res;
  }


  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode()
  {
    if(discriminator == null)
    {
      if(selectorString != null)
      {
        return selectorString.hashCode();
      }
      else
      {
        return 0;
      }
    }
    else
    {
      return discriminator.hashCode();
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.MPSelectionCriteria#getSelectorProperties()
   */
  public Map<String, Object> getSelectorProperties()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getSelectorProperties");
      SibTr.exit(tc, "getSelectorProperties", selectorProperties);
    }
    return selectorProperties;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.MPSelectionCriteria#setSelectorProperties(java.util.Map)
   */
  public void setSelectorProperties(Map<String, Object> selectorProperties)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setSelectorProperties", selectorProperties);

    this.selectorProperties = selectorProperties;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setSelectorProperties");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.core.impl.SelectionCriteriaImpl#toString()
   */
  public String toString()
  {
    String retString = discriminator + " : " + selectorString + " : " + domain;
    if(selectorProperties != null)
    {
      retString = retString + selectorProperties;
    }
    return retString;
  }
}
