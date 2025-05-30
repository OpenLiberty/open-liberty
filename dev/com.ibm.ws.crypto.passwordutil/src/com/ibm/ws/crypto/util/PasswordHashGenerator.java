/*******************************************************************************
 * Copyright (c) 2013, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

 package com.ibm.ws.crypto.util;

 import java.nio.charset.StandardCharsets;
 import java.security.SecureRandom;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import javax.crypto.SecretKey;
 import javax.crypto.SecretKeyFactory;
 import javax.crypto.spec.PBEKeySpec;
 
 public class PasswordHashGenerator {
     protected final static String SHA_1_ALGORITHM = "PBKDF2WithHmacSHA1";
     protected final static String PARSE_DEFAULT_ALGORITHM = SHA_1_ALGORITHM;
     protected final static String CREATE_DEFAULT_ALGORITHM = "PBKDF2WithHmacSHA512";
     protected final static int PARSE_DEFAULT_ITERATION = 6384;
     protected final static int CREATE_DEFAULT_ITERATION = 210000;
     private final static int DEFAULT_OUTPUT_LENGTH = 256;
     private final static int SALT_LENGTH = 128;
     private static final Class<?> CLASS_NAME = PasswordHashGenerator.class;
     private final static Logger logger = Logger.getLogger(CLASS_NAME.getCanonicalName());
 
     /**
      * generate salt value by using given string.
      * salt was generated as following format
      * String format of current time + given string + hostname
      **/
     public static byte[] generateSalt(String saltString) {
         byte[] output = null;
         if (saltString == null || saltString.length() < 1) {
             // use randomly generated value
             output = new byte[SALT_LENGTH];
             SecureRandom rand = new SecureRandom();
             rand.setSeed(rand.generateSeed(SALT_LENGTH));
             rand.nextBytes(output);
         } else {
             output = saltString.getBytes(StandardCharsets.UTF_8);
         }
         return output;
     }
     
     /**
      * 
      * @return the default iteration value when generating a new hash
      */
     public static int getDefaultIteration() {
         return CREATE_DEFAULT_ITERATION;
     }

     /**
      * 
      * @return the default algorithm when generating a new hash
      */
     public static String getDefaultAlgorithm() {
         return CREATE_DEFAULT_ALGORITHM;
     }

     /**
      * 
      * @return the assumed algorithm when not found while parsing {@link HashedData}
      */
     public static String getParseDefaultAlgorithm() {
         return PARSE_DEFAULT_ALGORITHM;
     }
     /**
      * 
      * @return the assumed iteration count when not found while parsing {@link HashedData}
      */
     public static int getParseDefaultIteration() {
         return PARSE_DEFAULT_ITERATION;
     }

     public static int getDefaultOutputLength() {
         return DEFAULT_OUTPUT_LENGTH;
     }

     public static byte[] digest(HashedData input) throws InvalidPasswordCipherException {
         if (input != null) {
             return digest(input.getPlain(), input.getSalt(), input.getAlgorithm(), input.getIteration(), input.getOutputLength());
         } else {
             throw new InvalidPasswordCipherException("HashedData object is null.");
         }
     }
 
     public static byte[] digest(char[] plainBytes) throws InvalidPasswordCipherException {
         return digest(plainBytes, generateSalt(null), CREATE_DEFAULT_ALGORITHM, CREATE_DEFAULT_ITERATION, DEFAULT_OUTPUT_LENGTH);
     }
 
     /**
      * perform message digest and then append a salt at the end.
      **/
     public static byte[] digest(char[] plainBytes, byte[] salt, String algorithm, int iteration, int length) throws InvalidPasswordCipherException {
         if (logger.isLoggable(Level.FINE)) {
             logger.fine("algorithm : " + algorithm + " iteration : " + iteration);
             logger.fine("input length: " + plainBytes.length);
             logger.fine("salt length: " + salt.length);
             logger.fine("output length: " + length);
         }
         byte[] oBytes = null;
 
         if (plainBytes != null && plainBytes.length > 0 && algorithm != null && algorithm.length() > 0 && iteration > 0) {
             long begin = 0;
             if (logger.isLoggable(Level.FINE)) {
                 begin = System.nanoTime();
             }
             try {
                 SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
                 PBEKeySpec ks = new PBEKeySpec(plainBytes, salt, iteration, length);
                 SecretKey s = skf.generateSecret(ks);
                 oBytes = s.getEncoded();
             } catch (Exception e) {
                 throw (InvalidPasswordCipherException) new InvalidPasswordCipherException(e.getMessage()).initCause(e);
             }
             if (logger.isLoggable(Level.FINE)) {
                 long elapsed = System.nanoTime() - begin;
                 logger.fine("Elapsed time : " + elapsed + " ns " + (elapsed / 1000000) + " ms"); //debug
             }
         }
 
         if ((logger.isLoggable(Level.FINE)) && oBytes != null) {
             logger.fine("digest length: " + oBytes.length);
             logger.fine(hexDump(oBytes));
         }
         return oBytes;
     }
 
     public static String hexDump(byte[] input) {
         String outputString = null;
         if (input != null) {
             int length = input.length;
             StringBuffer output = new StringBuffer();
             output.append("\n");
             for (int i = 0; i < length; i += 16) {
                 output.append(String.format("%04x: ", i));
                 for (int j = 0; j < 16; j++) {
                     if (j + i < length) {
                         output.append(String.format("%02X ", input[j + i]));
                     } else {
                         output.append("   ");
                     }
                     if (j == 7) {
                         output.append(" ");
                     }
                 }
                 output.append(" : ");
                 for (int j = i; j < i + 16 && j < length; j++) {
                     String str = ".";
                     if (input[j] > 0x20 && Character.isDefined(input[j])) {
                         str = new String(input, j, 1);
                     } else {
                         str = ".";
                     }
                     output.append(str);
                 }
                 output.append("\n");
             }
             outputString = output.toString();
         }
         return outputString;
     }
 }
 