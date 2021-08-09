/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.application.viewstate;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NOTE: Class taken from tomcat 7 org.apache.catalina.util.SessionIdGenerator
 * and used here as an alternative for server side state token encryption.
 * 
 */
class SessionIdGenerator
{

    private static Logger log = Logger.getLogger(SessionIdGenerator.class.getName());

    /**
     * Queue of random number generator objects to be used when creating session
     * identifiers. If the queue is empty when a random number generator is
     * required, a new random number generator object is created. This is
     * designed this way since random number generators use a sync to make them
     * thread-safe and the sync makes using a a single object slow(er).
     */
    private Queue<SecureRandom> randoms =
            new ConcurrentLinkedQueue<SecureRandom>();
    /**
     * The Java class name of the secure random number generator class to be
     * used when generating session identifiers. The random number generator
     * class must be self-seeding and have a zero-argument constructor. If not
     * specified, an instance of {@link SecureRandom} will be generated.
     */
    private String secureRandomClass = null;
    /**
     * The name of the algorithm to use to create instances of
     * {@link SecureRandom} which are used to generate session IDs. If no
     * algorithm is specified, SHA1PRNG is used. To use the platform default
     * (which may be SHA1PRNG), specify the empty string. If an invalid
     * algorithm and/or provider is specified the {@link SecureRandom} instances
     * will be created using the defaults. If that fails, the {@link
     * SecureRandom} instances will be created using platform defaults.
     */
    private String secureRandomAlgorithm = "SHA1PRNG";
    /**
     * The name of the provider to use to create instances of
     * {@link SecureRandom} which are used to generate session IDs. If no
     * algorithm is specified the of SHA1PRNG default is used. If an invalid
     * algorithm and/or provider is specified the {@link SecureRandom} instances
     * will be created using the defaults. If that fails, the {@link
     * SecureRandom} instances will be created using platform defaults.
     */
    private String secureRandomProvider = null;
    /**
     * Node identifier when in a cluster. Defaults to the empty string.
     */
    private String jvmRoute = "";
    /**
     * Number of bytes in a session ID. Defaults to 16.
     */
    private int sessionIdLength = 16;

    /**
     * Specify a non-default @{link {@link SecureRandom} implementation to use.
     *
     * @param secureRandomClass The fully-qualified class name
     */
    public void setSecureRandomClass(String secureRandomClass)
    {
        this.secureRandomClass = secureRandomClass;
    }

    /**
     * Specify a non-default algorithm to use to generate random numbers.
     *
     * @param secureRandomAlgorithm The name of the algorithm
     */
    public void setSecureRandomAlgorithm(String secureRandomAlgorithm)
    {
        this.secureRandomAlgorithm = secureRandomAlgorithm;
    }

    /**
     * Specify a non-default provider to use to generate random numbers.
     *
     * @param secureRandomProvider The name of the provider
     */
    public void setSecureRandomProvider(String secureRandomProvider)
    {
        this.secureRandomProvider = secureRandomProvider;
    }

    /**
     * Specify the node identifier associated with this node which will be
     * included in the generated session ID.
     *
     * @param jvmRoute The node identifier
     */
    public void setJvmRoute(String jvmRoute)
    {
        this.jvmRoute = jvmRoute;
    }

    /**
     * Specify the number of bytes for a session ID
     *
     * @param sessionIdLength Number of bytes
     */
    public void setSessionIdLength(int sessionIdLength)
    {
        this.sessionIdLength = sessionIdLength;
    }

    /**
     * Generate and return a new session identifier.
     */
    public String generateSessionId()
    {

        byte random[] = new byte[16];

        // Render the result as a String of hexadecimal digits
        StringBuilder buffer = new StringBuilder();

        int resultLenBytes = 0;

        while (resultLenBytes < sessionIdLength)
        {
            getRandomBytes(random);
            for (int j = 0;
                    j < random.length && resultLenBytes < sessionIdLength;
                    j++)
            {
                byte b1 = (byte) ((random[j] & 0xf0) >> 4);
                byte b2 = (byte) (random[j] & 0x0f);
                if (b1 < 10)
                {
                    buffer.append((char) ('0' + b1));
                }
                else
                {
                    buffer.append((char) ('A' + (b1 - 10)));
                }
                if (b2 < 10)
                {
                    buffer.append((char) ('0' + b2));
                }
                else
                {
                    buffer.append((char) ('A' + (b2 - 10)));
                }
                resultLenBytes++;
            }
        }

        if (jvmRoute != null && jvmRoute.length() > 0)
        {
            buffer.append('.').append(jvmRoute);
        }

        return buffer.toString();
    }

    public void getRandomBytes(byte bytes[])
    {
        SecureRandom random = randoms.poll();
        if (random == null)
        {
            random = createSecureRandom();
        }
        random.nextBytes(bytes);
        randoms.add(random);
    }

    /**
     * Create a new random number generator instance we should use for
     * generating session identifiers.
     */
    private SecureRandom createSecureRandom()
    {

        SecureRandom result = null;

        long t1 = System.currentTimeMillis();
        if (secureRandomClass != null)
        {
            try
            {
                // Construct and seed a new random number generator
                Class<?> clazz = Class.forName(secureRandomClass);
                result = (SecureRandom) clazz.newInstance();
            }
            catch (Exception e)
            {
                log.log(Level.SEVERE, "Exception initializing random number generator of class "+ 
                        secureRandomClass + ". Falling back to java.secure.SecureRandom", e);
            }
        }

        if (result == null)
        {
            // No secureRandomClass or creation failed. Use SecureRandom.
            try
            {
                if (secureRandomProvider != null
                        && secureRandomProvider.length() > 0)
                {
                    result = SecureRandom.getInstance(secureRandomAlgorithm,
                            secureRandomProvider);
                }
                else
                {
                    if (secureRandomAlgorithm != null
                            && secureRandomAlgorithm.length() > 0)
                    {
                        result = SecureRandom.getInstance(secureRandomAlgorithm);
                    }
                }
            }
            catch (NoSuchAlgorithmException e)
            {
                log.log(Level.SEVERE, "Exception initializing random number generator using algorithm: "+
                        secureRandomAlgorithm, e);
            }
            catch (NoSuchProviderException e)
            {
                log.log(Level.SEVERE, "Exception initializing random number generator using provider: " + 
                        secureRandomProvider, e);
            }
        }

        if (result == null)
        {
            // Invalid provider / algorithm
            try
            {
                result = SecureRandom.getInstance("SHA1PRNG");
            }
            catch (NoSuchAlgorithmException e)
            {
                log.log(Level.SEVERE, "Invalid provider / algoritm SHA1PRNG for generate secure random token", e);
            }
        }

        if (result == null)
        {
            // Nothing works - use platform default
            result = new SecureRandom();
        }

        // Force seeding to take place
        result.nextInt();

        long t2 = System.currentTimeMillis();
        if ((t2 - t1) > 100)
        {
            if (log.isLoggable(Level.FINEST))
            {
                log.info("Creation of SecureRandom instance for session ID generation using ["
                        +result.getAlgorithm()+"] took ["+Long.valueOf(t2 - t1)+"] milliseconds.");
            }
        }
        return result;
    }
}
