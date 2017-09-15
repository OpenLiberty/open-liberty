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
package com.ibm.ws.sib.matchspace.selector.impl;

import java.util.List;

import com.ibm.ws.sib.matchspace.impl.Matching;
import com.ibm.ws.sib.matchspace.Operator;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.utils.FFDC;

/** Contains miscellaneous utilities needed during parsing */

public final class ParseUtil {
  
  private static final Class cclass = ParseUtil.class;
  
  private ParseUtil() {} // static only

  /** Parse a string literal
   *
   * @param image a String containing the image of the parse token for the literal
   *
   * @return a Selector representing the literal
   **/
  static Selector parseStringLiteral(String image) {
    return new LiteralImpl(reduceStringLiteralToken(image));
  }

  // Subroutine to reduce a string literal token image to a string literal value by
  // processing ' characters in the image
  private static String reduceStringLiteralToken(String image) {
    // First, remove leading and trailing ' character
    image = image.substring(1, image.length()-1);
    // Next, de-double any doubled occurances of the ' character
    for (int i = 0; i < image.length(); i++)
      if (image.charAt(i) == '\'')
        // Here's a ', which we retain, but the character after it, also a ', is elided
        image = image.substring(0,i+1) + image.substring(i+2);
    return image;
  }

  /** Parse an integer literal
   *
   * @param image a String containing the image of the parse token for the literal
   *
   * @return a Selector representing the literal
   **/
  static Selector parseIntegerLiteral(String val) {
    // Determine if this is a long constant by checking the suffix
    char tag = val.charAt(val.length()-1);
    boolean mustBeLong = false;
    if (tag == 'l' || tag == 'L') 
    {
      val = val.substring(0, val.length()-1);
      mustBeLong = true;
    }
    long longVal = Long.decode(val).longValue();
    if (mustBeLong || longVal > Integer.MAX_VALUE || longVal < Integer.MIN_VALUE)
      return new LiteralImpl(new Long(longVal));
    else
      return new LiteralImpl(new Integer((int) longVal));
  }

  /** Parse a floating point literal
   *
   * @param image a String containing the image of the parse token for the literal
   *
   * @return a Selector representing the literal
   **/
  static Selector parseFloatingLiteral(String val) {
    // Determine if this is a float constant by checking the suffix
    Number value; // was NumericValue
    char tag = val.charAt(val.length()-1);
    if (tag == 'f' || tag == 'F')
      value = new Float(val);
    else
      value = new Double(val);
    return new LiteralImpl(value);
  }

  /** Convert a partially parsed set expression into its more primitive form as a
   * disjunction of equalities.
   *
   * @param expr the expression whose set membership is being tested
   *
   * @param set the set itself, as a FastVector containing Selector trees representing
   * expressions
   *
   * @return a Selector representing the set expression its more primitive form
   **/
  static Selector convertSet(Selector expr, List set) {
    Selector ans = null;
    for (int i = 0; i < set.size(); i++) {
      Selector comparand = (Selector) set.get(i);
      Selector comparison = new OperatorImpl(Operator.EQ, (Selector) expr.clone(), comparand);
      if (ans == null)
        ans = comparison;
      else
        ans = new OperatorImpl(Operator.OR, ans, comparison);
    }
    return ans;
  }

  /** Convert a partially parsed BETWEEN expression into its more primitive form as a
   * conjunction of inequalities.
   *
   * @param expr the expression whose range is being tested
   *
   * @param bound1 the lower bound
   *
   * @param bound2 the upper bound
   *
   * @return a Selector representing the BETWEEN expression its more primitive form
   **/
  static Selector convertRange(Selector expr, Selector bound1, Selector bound2) {
    return new OperatorImpl(Operator.AND,
                        new OperatorImpl(Operator.GE, (Selector) expr.clone(), bound1),
                        new OperatorImpl(Operator.LE, (Selector) expr.clone(), bound2));
  }
  
  /** Convert a partially parsed LIKE expression, in which the pattern and escape are
   *   in the form of token images, to the proper Selector expression to represent
   *   the LIKE expression
   * @param arg the argument of the like
   * @param pattern the pattern image (still containing leading and trailing ')
   * @param escape the escape (still containing leading and trailing ') or null
   * @return an appropriate Selector or null if the pattern and/or escape are
   *   syntactically invalid
   */
  static Selector convertLike(Selector arg, String pattern, String escape) {
    try
    {
      pattern = reduceStringLiteralToken(pattern);
      boolean escaped = false;
      char esc = 0;
      if (escape != null) {
        escape = reduceStringLiteralToken(escape);
        if (escape.length() != 1)
          return null;
        escaped = true;
        esc = escape.charAt(0);
      }
      return Matching.getInstance().createLikeOperator(arg, pattern, escaped, esc);
    }
    catch (Exception e)
    {
      // No FFDC Code Needed.
      // FFDC driven by wrapper class.
      FFDC.processException(cclass,
          "com.ibm.ws.sib.matchspace.selector.impl.ParseUtil.convertLike",
          e,
          "1:183:1.19");              
      // This should never occur as to get into this we should be missing
      // the sib.matchspace jar file, but we are already in it.
      
      throw new RuntimeException(e);
    }
  }
}
