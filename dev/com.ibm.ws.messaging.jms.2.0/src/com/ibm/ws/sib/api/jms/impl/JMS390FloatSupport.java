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
package com.ibm.ws.sib.api.jms.impl;

import java.io.IOException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/******************************************************************/
/*                   S390 Float Support                           */
/******************************************************************/

public class JMS390FloatSupport {

  // ---------------- S390 Float routines ------------------------------------

  // IEEE 754 Floating point format represents double precision numbers as follows:
  // seee eeee | eeee ffff | ffff ffff | ffff ffff | ......
  // where s = sign bit
  // e = exponent bit
  // f = fraction bit
  // The decimal point is assumed at the left of the second byte.
  // When the exponent is non-zero, the MSB of the second byte is assumed to be
  // value 1 (2^-1 = 0.5).
  private final static long DOUBLE_SIGN_MASK = 0x8000000000000000L;
  private final static long DOUBLE_EXPONENT_MASK = 0x7ff0000000000000L;
  private final static long DOUBLE_MANTISSA_MASK = 0x000fffffffffffffL;
  private final static long DOUBLE_MANTISSA_MSB_MASK = 0x0010000000000000L;

  // The exponent is treated as an unisgned number
  // A bias of 1022 is subtracted from it to give the power used.
  private final static long DOUBLE_BIAS = 1022;

  // The value of the IEEE Float is then given by mantissa * (2^(exp-1022)), the
  // sign bit giving the sign

  // S390 double precision floats have the following format:
  // seee eee | ffff ffff | ffff ffff | ffff ffff | ....
  // The exponent a power of 16, not 2.  Hence the best we can guarantee is that one
  // of the top four bits of the mantissa are non-zero - therefore there is no implied
  // bit as with IEEE
  // The exponent has a bias of 64, giving a range of -63..64
  private final static int S390_DOUBLE_BIAS = 64;
  private final static long S390_DOUBLE_EXPONENT_MASK = 0x7f00000000000000L;
  private final static long S390_DOUBLE_MANTISSA_MASK = 0x00ffffffffffffffL;

  // IEEE 754 Floating point format represents numbers as follows:
  // seee eeee | efff ffff | ffff ffff | ffff ffff
  // where s = sign bit
  // e = eponent bit
  // f = fraction bit
  // The decimal point is assumed at the left of the second byte.
  // When the exponent is non-zero, the MSB of the second byte is assumed to be
  // value 1 (2^-1 = 0.5).
  private final static int FLOAT_SIGN_MASK = 0x80000000;
  private final static int FLOAT_EXPONENT_MASK = 0x7f800000;
  private final static int FLOAT_MANTISSA_MASK = 0x007fffff;
  private final static int FLOAT_MANTISSA_MSB_MASK = 0x00800000;

  // The exponent is treated as an unisgned number, giving a range of 0..255.
  // A bias of 126 is subtracted from it to give the power used.
  private final static int FLOAT_BIAS = 126;

  // The value of the IEEE Float is then given by mantissa * (2^(exp-126)), the
  // sign bit giving the sign

  // S390 floats have the following format:
  // seee eee | ffff ffff | ffff ffff | ffff ffff
  // The exponent a power of 16, not 2.  Hence the best we can guarantee is that one
  // of the top four bits of the mantissa are non-zero - therefore there is no implied
  // bit as with IEEE
  // The exponent has a bias of 64, giving a range of -63..64
  private final static int S390_FLOAT_BIAS = 64;
  private final static int S390_FLOAT_EXPONENT_MASK = 0x7f000000;
  private final static int S390_FLOAT_MANTISSA_MASK = 0x00ffffff;

  // *************************** TRACE INITIALIZATION **************************
  private static TraceComponent tc = SibTr.register(JMS390FloatSupport.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

  /**
   * Utility function to convert between IEEE double and S390.<p>
   *
   * To convert from IEEE to S390 we use the following formula:
   *   let r = exponent % 4;  q = exponent / 4;
   *   if q == 0 then m.2^x = m.16^q
   *   if q != 0 then m.2^x = (m.2^(r-4)).16^(q+1)  for positive q,
   *                        = (m.2^-r).16^q) for negative q
   */
  protected static final long doubleToS390LongBits(double ieeeDouble) throws IOException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "doubleToS390LongBits", ieeeDouble);

    // Get the bit pattern
    long ieeeLongBits = Double.doubleToLongBits(ieeeDouble);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "IEEE double bit pattern= " + Long.toString(ieeeLongBits, 16));

    // Test the sign bit (0 = positive, 1 = negative)
    boolean positive = ((ieeeLongBits & DOUBLE_SIGN_MASK) == 0);

    // Deal with zero straight away...
    if ((ieeeLongBits & 0x7fffffffffffffffL) == 0) {
      // + or - 0.0
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "zero");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "doubleToS390LongBits", ieeeLongBits);
      return ieeeLongBits;
    }

    // Extract the exponent
    long exponent = ieeeLongBits & DOUBLE_EXPONENT_MASK;
    // shift right 52 bits to get exponent in least significant byte
    exponent = exponent >>> 52;
    // subtract the bias to get the true value
    exponent = exponent - DOUBLE_BIAS;

    // Extract the mantissa
    long mantissa = ieeeLongBits & DOUBLE_MANTISSA_MASK;

    // Now begin the conversion to S390

    long remainder = Math.abs(exponent) % 4;
    long quotient = Math.abs(exponent) / 4;

    long s390Exponent = quotient;
    if ((exponent > 0) && (remainder != 0)) {
      s390Exponent = s390Exponent + 1;
    }

    // put the sign back in
    if (exponent < 0) {
      s390Exponent = -s390Exponent;
    }

    // Add the bias
    s390Exponent += S390_DOUBLE_BIAS;

    // Now adjust the mantissa part
    long s390Mantissa = mantissa;
    // for an exponent greater than -DOUBLE_BIAS, add in the implicit bit
    if (exponent > (-DOUBLE_BIAS)) {
      s390Mantissa = s390Mantissa | DOUBLE_MANTISSA_MSB_MASK;
    }
    else {
      // there is no implicit bit, so the mantissa is one bit to the right of what
      // we would normally expect.  We need to fix this for S390
      s390Mantissa = s390Mantissa << 1;
    }

    // S390 Mantissa starts 4 bits left of ieee one. The first of these is implied in
    // IEEE so only shift 3 places
    s390Mantissa = s390Mantissa << 3;

    if (remainder > 0) {
      if (exponent > 0) {
        // the next two lines perform the (m.2^(r-4)) part of the conversion
        int shift_places = (int) (4 - remainder);
        s390Mantissa = s390Mantissa >>> shift_places;
      }
      else {
        // to avoid loss of precision when the exponent is at a minimum,
        // we may need to shift the mantissa four places left and decrease the
        // s390 exponent by one before shifting right
        if ((exponent == - (DOUBLE_BIAS)) && ((s390Mantissa & 0x00f0000000000000L) == 0)) {
          s390Mantissa = s390Mantissa << 4;
          s390Exponent = s390Exponent - 1;
        }
        // the next two lines perform the m.2-r part of the conversion
        s390Mantissa = s390Mantissa >>> remainder;
      }
    }

    // An exponent of -DOUBLE_BIAS is the smallest that IEEE can do.  S390 has
    // a wider range, and hence may be able to normalise the mantissa more than
    // is possible for IEEE
    // Each shift left of four bits is equivalent to multiplying by 16,
    // so the exponent must be reduced by 1
    if (exponent == - (DOUBLE_BIAS)) {
      while ((s390Mantissa != 0) && ((s390Mantissa & 0x00f0000000000000L) == 0)) {
        s390Mantissa = s390Mantissa << 4;
        s390Exponent = s390Exponent - 1;
      }
    }

    // if the exponent is now > 127, we have an overflow since IEEE can handle larger numbers
    // than S390 can.
    if (s390Exponent > 127) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Exponent = " + s390Exponent);
      throw new IOException("Number outside of range for double precision S/390 Float");
    }
    else if (s390Exponent < 0) {
      // the number is too small to represent, set it to zero
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Number too small to represent, rounding to zero");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "doubleToS390LongBits", 0L);
      return 0L;
    }

    // Assemble the s390BitPattern
    long s390Double = 0L;
    long s390ExponentBits = s390Exponent & 0x000000000000007FL;
    // make sure we only deal with 7 bits
    // add the exponent
    s390Double = s390ExponentBits << 56; // shift to MSB
    // add the sign
    if (!positive) {
      s390Double = s390Double | DOUBLE_SIGN_MASK;
    }
    // add the mantissa
    s390Double = s390Double | s390Mantissa;
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "S390 bit pattern = " + Long.toString(s390Double, 16));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "doubleToS390LongBits",  s390Double);
    return s390Double;
  }

  /**
   * Utility function to convert from IEEE float to S390.<p>
   *
   * To convert from IEEE to S390 we use the following formula:
   * let r = exponent % 4;  q = exponent / 4;
   * if q == 0 then m.2^x = m.16^q
   * if q != 0 then m.2^x = (m.2^(r-4)).16^(q+1)  for positive q,
   *                      = (m.2^-r).16^q) for negative q
  */
  protected static final int floatToS390IntBits(float ieeeFloat) throws IOException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "floatToS390IntBits", ieeeFloat);

    // Get the bit pattern
    int ieeeIntBits = Float.floatToIntBits(ieeeFloat);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "IEEE bit pattern = " + Integer.toString(ieeeIntBits, 16));

    // Test the sign bit (0 = positive, 1 = negative)
    boolean positive = ((ieeeIntBits & FLOAT_SIGN_MASK) == 0);

    // Deal with zero straight away...
    if ((ieeeIntBits & 0x7fffffff) == 0) {
      // + or - 0.0
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "zero");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "floatToS390IntBits",  ieeeIntBits);
      return ieeeIntBits;
    }

    // Extract the exponent
    int exponent = ieeeIntBits & FLOAT_EXPONENT_MASK;
    // shift right 23 bits to get exponent in least significant byte
    exponent = exponent >>> 23;
    // subtract the bias to get the true value
    exponent = exponent - FLOAT_BIAS;

    // Extract the mantissa
    int mantissa = ieeeIntBits & FLOAT_MANTISSA_MASK;
    // for an exponent greater than -FLOAT_BIAS, add in the implicit bit
    if (exponent > (-FLOAT_BIAS)) {
      mantissa = mantissa | FLOAT_MANTISSA_MSB_MASK;
    }

    // Now begin the conversion to S390

    int remainder = Math.abs(exponent) % 4;
    int quotient = Math.abs(exponent) / 4;

    int s390Exponent = quotient;
    if ((exponent > 0) && (remainder != 0)) {
      s390Exponent = s390Exponent + 1;
    }

    // put the sign back in
    if (exponent < 0) {
      s390Exponent = -s390Exponent;
    }

    // Add the bias
    s390Exponent += S390_FLOAT_BIAS;

    // Now adjust the mantissa part
    int s390Mantissa = mantissa;
    if (remainder > 0) {
      if (exponent > 0) {
        // the next two lines perform the (m.2^(r-4)) part of the conversion
        int shift_places = 4 - remainder;
        s390Mantissa = s390Mantissa >>> shift_places;
      }
      else {
        // to avoid loss of precision when the exponent is at a minimum,
        // we may need to shift the mantissa four places left, and decrease the
        // s390Exponent by one before shifting right
        if ((exponent == - (FLOAT_BIAS)) && ((s390Mantissa & 0x00f00000) == 0)) {
          s390Mantissa = s390Mantissa << 4;
          s390Exponent = s390Exponent - 1;
        }
        // the next two line perform the (m.2^-r) part of the conversion
        int shift_places = remainder;
        s390Mantissa = s390Mantissa >>> shift_places;
      }
    }

    // An exponent of -FLOAT_BIAS is the smallest that IEEE can do.  S390 has
    // a wider range, and hence may be able to normalise the mantissa more than
    // is possible for IEEE
    // Also, since an exponent of -FLOAT_BIAS has no implicit bit set, the mantissa
    // starts with a value of 2^-1 at the second bit of the second byte.  We thus need
    // to shift one place left to move the mantissa to the S390 position
    // Follwoing that, we notmalise as follows:
    // Each shift left of four bits is equivalent to multiplying by 16,
    // so the exponent must be reduced by 1
    if (exponent == - (FLOAT_BIAS)) {
      s390Mantissa = s390Mantissa << 1;
      while ((s390Mantissa != 0) && ((s390Mantissa & 0x00f00000) == 0)) {
        s390Mantissa = s390Mantissa << 4;
        s390Exponent = s390Exponent - 1;
      }
    }

    // Assemble the s390BitPattern
    int s390Float = 0;
    int s390ExponentBits = s390Exponent & 0x0000007F; // make sure we only deal with 7 bits
    // add the exponent
    s390Float = s390ExponentBits << 24; // shift to MSB
    // add the sign
    if (!positive) {
      s390Float = s390Float | FLOAT_SIGN_MASK;
    }
    // add the mantissa
    s390Float = s390Float | s390Mantissa;

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "S390 Bit pattern = " + Integer.toString(s390Float, 16));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "floatToS390IntBits",  s390Float);
    return s390Float;
  }

  /** To convert from S390 to IEEE we use the fomula:
  // m.16^x = m.2^4x, and then normalise by shifting the mantissa up to three
  // places left
  */
  protected static final float intS390BitsToFloat(int floatBits) throws IOException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "intS390BitsToFloat", floatBits);

    // Test the sign bit (0 = positive, 1 = negative)
    boolean positive = ((floatBits & FLOAT_SIGN_MASK) == 0);

    // Deal with zero straight away...
    if ((floatBits & 0x7fffffff) == 0) {
      // + or - 0.0
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "zero");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "intS390BitsToFloat");
      if (positive) {
        return 0.0F;
      } else {
        return - (0.0F);
      }
    }

    int mantissa = floatBits & S390_FLOAT_MANTISSA_MASK;
    int exponent = floatBits & S390_FLOAT_EXPONENT_MASK;

    // move the exponent into the LSB
    exponent = exponent >> 24;
    // subtract the bias
    exponent = exponent - S390_FLOAT_BIAS;

    // caculate the IEEE exponent
    int ieeeExponent = exponent * 4;

    // Normalise the mantissa
    int ieeeMantissa = mantissa;
    // Deal with exponents <= -FLOAT_BIAS
    if (ieeeExponent <= - (FLOAT_BIAS)) {
      // ieeeMantissa is one place to the right since there is no implicit bit set
      ieeeMantissa = ieeeMantissa >> 1;
      // now increase the exponent until it reaches -FLOAT_BIAS, shifting right one
      // place at each stage to compensate
      while (ieeeExponent < - (FLOAT_BIAS)) {
        ieeeExponent = ieeeExponent + 1;
        ieeeMantissa = ieeeMantissa >> 1;
      }
    }

    // Deal with exponents greater than -FLOAT_BIAS
    while ((ieeeMantissa != 0)
      && ((ieeeMantissa & FLOAT_MANTISSA_MSB_MASK) == 0)
      && (ieeeExponent > - (FLOAT_BIAS))) {
      ieeeMantissa = ieeeMantissa << 1; // *2
      ieeeExponent = ieeeExponent - 1; // /2
    }

    // s390 has a wider range than IEEE, so deal with over and underflows
    if (ieeeExponent < -149) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "underflow, returning zero");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "intS390BitsToFloat",  0.0F);
      return 0.0F; // underflow
    }
    else if (ieeeExponent > 128) {
      if (positive) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "overflow, returning +INFINITY");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "intS390BitsToFloat", "+infinity");
        return (Float.MAX_VALUE * 2); // + infinity
      }
      else {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "overflow, returning -INFINITY");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "intS390BitsToFloat", "-infinity");
        return - (Float.MAX_VALUE * 2); // -infinity
      }
    }

    // Build the IEEE float
    int ieeeBits = 0;
    if (!positive) {
      ieeeBits = ieeeBits | FLOAT_SIGN_MASK;
    }

    // add the bias to the exponent
    ieeeExponent = ieeeExponent + FLOAT_BIAS;
    // move it to the IEEE exponent position
    ieeeExponent = ieeeExponent << 23;
    // add to the result
    ieeeBits = ieeeBits | ieeeExponent;

    // mask the top bit of the mantissa (implicit in IEEE)
    ieeeMantissa = ieeeMantissa & FLOAT_MANTISSA_MASK;
    // add to the result
    ieeeBits = ieeeBits | ieeeMantissa;

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "IEEE Bit pattern = " + Integer.toString(ieeeBits, 16));
    float result = Float.intBitsToFloat(ieeeBits);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "intS390BitsToFloat",  result);
    return result;
  }

  /** To convert from S390 to IEEE we use the fomula:
  // m.16^x = m.2^4x, and then normalise by shifting the mantissa up to three
  // places left
  */
  protected static final double longS390BitsToDouble(long doubleBits) throws IOException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "longS390BitsToDouble", doubleBits);

    // Test the sign bit (0 = positive, 1 = negative)
    boolean positive = ((doubleBits & DOUBLE_SIGN_MASK) == 0);

    // Deal with zero straight away...
    if ((doubleBits & 0x7fffffffffffffffL) == 0) {
      // + or - 0.0
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "zero");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "longS390BitsToDouble");
      if (positive) {
        return 0.0D;
      }
      else {
        return - (0.0D);
      }
    }

    long mantissa = doubleBits & S390_DOUBLE_MANTISSA_MASK;
    long exponent = doubleBits & S390_DOUBLE_EXPONENT_MASK;

    // move the exponent into the LSB
    exponent = exponent >> 56;
    // subtract the bias
    exponent = exponent - S390_DOUBLE_BIAS;

    // caculate the IEEE exponent
    long ieeeExponent = exponent * 4;

    // Normalise the mantissa
    long ieeeMantissa = mantissa;
    // IEEE mantissa starts three places right of S390 (+ implicit bit)
    ieeeMantissa = ieeeMantissa >> 3;
    // if this is the samllest possible exponent, then there is no implicit bit,
    // and so we need to shift an extra bit
    if (ieeeExponent <= - (DOUBLE_BIAS)) {
      ieeeMantissa = ieeeMantissa >> 1;
      // now increase the exponent until it reaches -DOUBLE_BIAS, shifting
      // right one place at each stage to compensate
      while (ieeeExponent < - (DOUBLE_BIAS)) {
        ieeeExponent = ieeeExponent + 1;
        ieeeMantissa = ieeeMantissa >> 1;
      }
    }

    // complete the normalisation for exponents > -DOUBLE_BIAS
    while (  (ieeeMantissa != 0)
          && ((ieeeMantissa & DOUBLE_MANTISSA_MSB_MASK) == 0)
          && (ieeeExponent > - (DOUBLE_BIAS))
          ) {
      ieeeMantissa = ieeeMantissa << 1; // *2
      ieeeExponent = ieeeExponent - 1; // /2
    }

    // s390 has a wider range than IEEE, so deal with over and underflows
    if (ieeeExponent < -1045) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "longS390BitsToDouble",  "underflow");
      return 0.0F; // underflow
    }
    else if (ieeeExponent > 1024) {
      if (positive) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "overflow - returning +INFINITY");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "longS390BitsToDouble", "+infinity");
        return (Double.MAX_VALUE * 2); // + infinity
      }
      else {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "overflow - returning -INFINITY");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "longS390BitsToDouble", "-infinity");
        return - (Double.MAX_VALUE * 2); // -infinity
      }
    }

    // Built the IEEE double
    long ieeeBits = 0;
    if (!positive) {
      ieeeBits = ieeeBits | DOUBLE_SIGN_MASK;
    }

    // add the bias to the exponent
    ieeeExponent = ieeeExponent + DOUBLE_BIAS;
    // move it to the IEEE exponent position
    ieeeExponent = ieeeExponent << 52;
    // add to the result
    ieeeBits = ieeeBits | ieeeExponent;

    // mask the top bit of the mantissa (implicit in IEEE)
    ieeeMantissa = ieeeMantissa & DOUBLE_MANTISSA_MASK;
    // add to the result
    ieeeBits = ieeeBits | ieeeMantissa;
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "IEEE bit pattern = " + Long.toString(ieeeBits, 16));
    double result = Double.longBitsToDouble(ieeeBits);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "longS390BitsToDouble",  result);
    return result;
  }
}
