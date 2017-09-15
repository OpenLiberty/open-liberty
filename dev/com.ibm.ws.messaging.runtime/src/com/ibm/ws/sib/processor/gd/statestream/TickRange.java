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
package com.ibm.ws.sib.processor.gd.statestream;

// Import required classes.
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.utils.linkedlist2.Entry;
import com.ibm.ws.sib.utils.ras.SibTr;

final public class TickRange extends Entry implements Cloneable
{

  private static TraceComponent tc =
    SibTr.register(
      TickRange.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);


  /**
   * NLS for component
   */
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  public static final long MAX = Long.MAX_VALUE;
  public static final long MIN = 0;

  // The current state.
  protected TickRangeType type;
  protected long start;
  protected long end;  
  protected TickData data;
  
  /**
   * Constructor for the TickRange object
   */
  public TickRange()
  {        
  }

  /** Constructor for the TickRange object
   * 
   * @param type
   * @param start
   * @param end
   */
  public TickRange(TickRangeType type, long start, long end)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "TickRange",
        new Object[] { type, new Long(start), new Long(end)});

    if (start > end)
    {
      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0008",
          new Object[] {
            "com.ibm.ws.sib.processor.gd.statestream.TickRange",
            "1:92:1.2",
            new Long(start),
            new Long(end) },
          null));
    }
    
    reset(type,start,end);    

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "TickRange", this);
  }
  
  /** Constructor for the TickRange object
   * 
   * @param type
   * @param start
   * @param end
   */
  public TickRange(TickRangeType type,
                   long start,
                   long end,
                   TickData data)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "TickRange",
        new Object[] { type,
                       new Long(start),
                       new Long(end),
                       data });

    if (start > end)
    {
      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0008",
          new Object[] {
            "com.ibm.ws.sib.processor.gd.statestream.TickRange",
            "1:131:1.2",
            new Long(start),
            new Long(end) },
          null));
    }
  
    reset(type,start,end,data);    

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "TickRange", this);
  }

  /**
   * @param type
   * @param start
   * @param end
   */
  public void reset(TickRangeType type, long start, long end)
  {
    reset(type,start,end,null);    
  }
  
  /**
   * @param type
   * @param start
   * @param end
   */
  public void reset(TickRangeType type,
                    long start,
                    long end,
                    TickData data)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "reset", new Object[] { type, new Long(start),
          new Long(end), data });
    this.type = type;
    this.start = start;
    this.end = end;
    this.data = data;
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "reset");
  }
  
  public boolean contains(long tick)
  {
    return start <= tick && end >= tick;
  }
  
  public boolean isInUse()
  {
    return parentList != null;
  }
  
  public long diff(long tick)
  {
    long diff = tick - start;
    if(contains(tick))
    {
      diff = 0;
    }
    else if(diff > 0)
    {
      diff = tick - end;
    }
    return diff;
  }

  public String toString(String indent)
  {
    String ret = indent +
      "["
        + type
        + " Start:"
        + tickToString(start)
        + ", End:"
        + ((end == MAX)
          ? "      LAST"
          : tickToString(end))
        + ", Data:"
        + ((data == null)
          ? "null     "
          : "@" + Integer.toString(data.hashCode(), 16))
        + "]";
    return ret;
  }

  protected static String tickToString(long tick)
  {
    String blanks = "          "; // For padding.
    // Set the high order 32 signed bits.
    String highBits = Long.toString(tick >>> 32);
    highBits =
      blanks.substring(0, blanks.length() - highBits.length()) + highBits;
    // Set the low 32 bits.
    String lowBits = Long.toString(tick & 0x00000000ffffffffL);
    lowBits = blanks.substring(0, blanks.length() - lowBits.length()) + lowBits;
    //return highBits+" "+lowBits;
    return lowBits;
  }
  /**
   * @return Returns the data.
   */
  public TickData getData()
  {
    return data;
  }
  /**
   * @return Returns the end.
   */
  public long getEnd()
  {
    return end;
  }
  /**
   * @return Returns the start.
   */
  public long getStart()
  {
    return start;
  }
  /**
   * @return Returns the type.
   */
  public TickRangeType getType()
  {
    return type;
  }
}
