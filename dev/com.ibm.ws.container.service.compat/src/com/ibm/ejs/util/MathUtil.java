/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.util;

/**
 * Provides varous math utility functions to the WebSphere server runtime. <p>
 * 
 * Functions such as isOdd, isEven, and isPrime are provided as static methods.
 * It is not intended that instances of this class should be created.
 **/
public class MathUtil {
    public static final int MAX_PRIME_INTEGER = 2147482661;

    /**
     * Default constructor is private to prevent instances from being created.
     */
    private MathUtil() {}

    /**
     * Returns the next larger positive prime integer. <p>
     * 
     * The integer specified will be returned if it is positive and a prime. <p>
     * 
     * If the integer specified is greater than the maximumn prime integer, then
     * the next odd integer will be returned.
     **/
    public static int findNextPrime(int number) {
        // 2 is the smallest positive prime number.
        if (number <= 2)
            return 2;

        // Even numbers (other than 2) are not prime.
        if (isEven(number))
            ++number;

        if (number > MAX_PRIME_INTEGER)
            return number;

        // For safety, don't loop forever looking for a prime, and skip
        // all of the evens, as they are not prime (except 2).
        for (int next = number; next < number + 1000; next += 2) {
            if (isPrime(next))
                return next;
        }

        return number;
    }

    /**
     * Returns true if the specified integer is a prime number. <p>
     * 
     * This method uses an algorithm to determine the primality of a number,
     * adapted from a sample program in the book "Prime Numbers and Computer
     * Methods for Factorization" by Hans Riesel, 1985, and is based on
     * Euler's criterion for quadratic residues. The algorithm is valid
     * for any number below 25x10^9 (which is only slightly larger than the
     * maximum integer value). <p>
     * 
     * Note that any adaptation of this algorithm for long values needs to
     * take into account the limit for which it is valid.
     **/
    public static boolean isPrime(int number) {
        // For performance, check if the number is divisable by severl of
        // the smaller prime numbers.  Note that a prime is divisible by
        // itself, so check for the smaller primes prior to performing
        // the divion check.
        if (number == 2 || number == 3 || number == 5 || number == 7 ||
            number == 11 || number == 13 || number == 17 || number == 19)
            return true;

        if (number % 2 == 0 || number % 3 == 0 || number % 5 == 0 || number % 7 == 0 ||
            number % 11 == 0 || number % 13 == 0 || number % 17 == 0 || number % 19 == 0)
            return false;

        // Now perform the Euler's criterion check for the bases 2, 3, and 5. This is
        // valid only for odd numbers, so the even must be filtered out above.
        long numberMinusOne = number - 1;
        long s = 0;
        long d = numberMinusOne;

        while (isEven(d)) {
            d = d / 2;
            ++s;
        }

        long d1 = d;

        for (int a = 2; a <= 5; ++a) {
            if (a == 4)
                continue; // Skip the base 4.

            long prod = 1;
            long a2j = a;
            d = d1;
            while (d > 0) {
                if (isOdd(d))
                    prod = (prod * a2j) % number;

                d = d / 2;
                a2j = (a2j * a2j) % number;
            }

            // prod = a^d mod number has been calculated.

            if (prod == 1 || prod == (numberMinusOne))
                continue;

            for (long i = 1; i <= s; ++i) {
                prod = (prod * prod) % number;
                if (prod == (numberMinusOne))
                    break;
            }

            if (prod != (numberMinusOne)) {
                return false;
            }
        }

        // There are a few non-prime numbers below 25x10^9 that pass Euler's
        // criterion, so check for them here.  Note that severl of them are larger
        // than an integer, but they are commented out here for completeness.
        if (number == 25326001 || number == 161304001 ||
            number == 960946321 || number == 1157839381)
            // 3215031751, 3697278427, 5764643587, 6770862367, 14386156093,
            // 15579919981, 18459366157, 19887974881, 21276028621
            return false;

        return true;
    }

    /**
     * Returns true if the specified integer is odd.
     **/
    public static boolean isOdd(int number) {
        return isOdd((long) number);
    }

    /**
     * Returns true if the specified long is odd.
     **/
    public static boolean isOdd(long number) {
        if (number % 2 == 0)
            return false;

        return true;
    }

    /**
     * Returns true if the specified integer is even.
     **/
    public static boolean isEven(int number) {
        return isEven((long) number);
    }

    /**
     * Returns true if the specified long is even.
     **/
    public static boolean isEven(long number) {
        if (number % 2 == 0)
            return true;

        return false;
    }

    /**
     * Provides a mechanism to try out <code>MathUtil</code>
     * with some basic operations and see if it works.
     **/
    //   public static void main(String[] args)
    //   {
    //      System.out.println("Testing out findNextPrime()....................");
    //      java.util.Date start = new java.util.Date();
    //
    //      // Find the first 100,008 prime numbers, and compare the 100th, 1,000th,
    //      // 10,000th and 100,008th againt those found in the table at
    //      // http://www.utm.edu/research/primes/lists/small/10000.txt
    //      int prime = 0;
    //      int primesToFind = 100008;
    //      int columns = 9;
    //      int [] primes = new int[columns];
    //      int index = 0;
    //
    //      for (int i = 0; i < primesToFind; ++i)
    //      {
    //         prime = findNextPrime(prime);
    //         index = i % columns;
    //         primes[index] = prime;
    //
    //         if (index == columns-1)
    //         {
    //            String output = "Primes[" + (i+1) + "] : ";
    //            for (int j=0; j<columns; ++j)
    //            {
    //               output += primes[j];
    //               if (j < columns-1)
    //                  output += ", ";
    //            }
    //            System.out.println(output);
    //         }
    //
    //         switch (i+1)
    //         {
    //           case 100:
    //             if (prime != 541)
    //             {
    //                System.out.println("ERROR - findNextPrime not working!!!!!!!!!!!!!");
    //                System.out.println("100th prime is 541, but found " + prime);
    //                System.exit(-1);
    //             }
    //             break;
    //
    //           case 1000:
    //             if (prime != 7919)
    //             {
    //                System.out.println("ERROR - findNextPrime not working!!!!!!!!!!!!!");
    //                System.out.println("1,000th prime is 7919, but found " + prime);
    //                System.exit(-1);
    //             }
    //             break;
    //
    //           case 10000:
    //             if (prime != 104729)
    //             {
    //                System.out.println("ERROR - findNextPrime not working!!!!!!!!!!!!!");
    //                System.out.println("10,000th prime is 104729, but found " + prime);
    //                System.exit(-1);
    //             }
    //             break;
    //
    //           case 100008:
    //             if (prime != 1299827)
    //             {
    //                System.out.println("ERROR - findNextPrime not working!!!!!!!!!!!!!");
    //                System.out.println("100,008th prime is 1299827, but found " + prime);
    //                System.exit(-1);
    //             }
    //             break;
    //         }
    //
    //         ++prime;
    //      }
    //
    //      java.util.Date end = new java.util.Date();
    //      long findTime = end.getTime() - start.getTime();
    //      java.util.Date find = new java.util.Date(findTime);
    //      java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("mm:ss:SS");
    //      String findTimeStr = formatter.format(find);
    //
    //      System.out.println("Time to find " + primesToFind + " primes : " + findTimeStr);
    //
    //      System.exit(0);
    //   }
}
