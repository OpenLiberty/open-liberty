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

package com.ibm.websphere.sib;

/**
 * Constants useful for API message programmers
 *
 * @ibm-was-base
 * @ibm-api
 */
public class SIApiConstants {

  /* **************************************************************************/
  /* Constants for Report messages                                            */
  /* **************************************************************************/

  /** The report byte for reports without data */
  public final static Byte REPORT_NO_DATA           = Byte.valueOf((byte)0);
  /** The report byte for reports with data */
  public final static Byte REPORT_WITH_DATA         = Byte.valueOf((byte)1);
  /** The report byte for reports with full data */
  public final static Byte REPORT_WITH_FULL_DATA    = Byte.valueOf((byte)2);

  /** The report integer for report on expiry */
  public final static Integer REPORT_EXPIRY            = Integer.valueOf(3);
  /** The report integer for report on exception */
  public final static Integer REPORT_EXCEPTION         = Integer.valueOf(4);
  /** The report integer for report on arrival */
  public final static Integer REPORT_COA               = Integer.valueOf(5);
  /** The report integer for report on delivery */
  public final static Integer REPORT_COD               = Integer.valueOf(6);
  /** The report integer for positive action notification */
  public final static Integer REPORT_PAN               = Integer.valueOf(7);
  /** The report integer for negative action notification */
  public final static Integer REPORT_NAN               = Integer.valueOf(8);

  /*
   * Format strings needed to construct JMS DataGraphs.
   */
  /** The format string for a bodyless JMS message */
  public final static String JMS_FORMAT        = "JMS:";
  /** The format string for a BytesMessage */
  public final static String JMS_FORMAT_BYTES  = JMS_FORMAT+"bytes";
  /** The format string for a TextMessage */
  public final static String JMS_FORMAT_TEXT   = JMS_FORMAT+"text";
  /** The format string for an ObjectMessage */
  public final static String JMS_FORMAT_OBJECT = JMS_FORMAT+"object";
  /** The format string for a StreamMessage */
  public final static String JMS_FORMAT_STREAM = JMS_FORMAT+"stream";
  /** The format string for a MapMessage */
  public final static String JMS_FORMAT_MAP    = JMS_FORMAT+"map";
}
