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
package com.ibm.ws.sib.processor.gd;

// Import required classes.
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

final public class TickRange extends RangeObject
{

  private static TraceComponent tc =
    SibTr.register(
      TickRange.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);


  // Types represent all posible states
  public static final byte Unknown = 0;
  public static final byte Requested = 1;
  public static final byte Uncommitted = 2;
  public static final byte Value = 3;
  public static final byte Discarded = 4;
  public static final byte Accepted = 5;
  public static final byte Rejected = 6;
  public static final byte Completed = 7;
  public static final byte Error = 8;

  // The current state.
  public byte type;

  //if type = Value, value represents the message
  public Object value;
  public long itemStreamIndex;
  private boolean reallocateOnCommit = false;

  public long valuestamp;

  /** Constructor for the TickRange object
   *
   * @param type
   * @param start
   * @param end
   */
  public TickRange(byte type, long start, long end)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "TickRange",
        new Object[] { Byte.valueOf(type), Long.valueOf(start), Long.valueOf(end)});

    if (start > end)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.debug(this, tc, "Start greater than end");
      // TODO: throw exception?
    }
    else
    {
      this.type = type;
      startstamp = start;
      endstamp = end;
      value = null;
      valuestamp = 0;
      itemStreamIndex = -1;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "TickRange", this);
  }

  // Helper methods
  public static TickRange newValueTick(
    long tick,
    Object value,
    long itemStreamIndex)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry( tc, "newValueTick", new Object[] { Long.valueOf(tick),
                                                      value,
                                                      Long.valueOf(itemStreamIndex)});

    TickRange r = new TickRange(TickRange.Value, tick, tick);
    r.itemStreamIndex = itemStreamIndex;
    r.value = value;
    r.valuestamp = tick;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit( tc, "newValueTick", r);

    return r;
  }

  public static TickRange newUncommittedTick(long tick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry( tc, "newUncommittedTick", Long.valueOf(tick));

    TickRange r = new TickRange(TickRange.Uncommitted, tick, tick);
    r.valuestamp = tick;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit( tc, "newUncommittedTick", r);

    return r;
  }

  /**
   * Indicate that this message was uncommitted when the reallocater was run
   * so reallocation needs to be done again when it commits.
   */
  public void reallocateOnCommit()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "reallocateOnCommit");
      SibTr.exit(tc, "reallocateOnCommit");
    }

    reallocateOnCommit = true;
  }

  /**
   * Is reallocation of the msg required on its postcommit callback
   * @return boolean
   */
  public boolean isReallocationRequired()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "isReallocationRequired");
      SibTr.exit(tc, "isReallocationRequired", Boolean.valueOf(reallocateOnCommit));
    }

    return reallocateOnCommit;
  }

  public String toString()
  {
    String ret =
      "["
        + stateToString(type)
        + " Start:"
        + tickToString(startstamp)
        + ", End:"
        + ((endstamp == RangeList.INFINITY)
          ? "      LAST"
          : tickToString(endstamp))
        + " ValueTick:"
        + tickToString(valuestamp)
        + ", Value:"
        + ((value == null)
          ? "null     "
          : "@" + Integer.toString(value.hashCode(), 16))
        + ", Index:"
        + ((itemStreamIndex == -1)
          ? "none     "
          : Long.toString(itemStreamIndex))
        + ", HashCode:/" + Integer.toHexString(this.hashCode())
        + "]";

    return ret;
  }

  protected static String stateToString(byte state)
  {
    String stype = "*";

    switch (state)
    {
      case Unknown :
        stype = "Unknown    ";
        break;

      case Requested :
        stype = "Requested  ";
        break;

      case Uncommitted :
        stype = "Uncommitted";
        break;

      case Value :
        stype = "Value      ";
        break;
      case Discarded :
        stype = "Discarded  ";
        break;

      case Accepted :
        stype = "Accepted   ";
        break;

      case Rejected :
        stype = "Rejected   ";
        break;

      case Completed :
        stype = "Completed  ";
        break;

      default :
        break;
    }
    return stype;
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

}
