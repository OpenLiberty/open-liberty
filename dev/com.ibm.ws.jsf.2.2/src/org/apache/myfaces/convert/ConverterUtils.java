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
package org.apache.myfaces.convert;


/**
 * TODO: Move to util package and rename to better name
 *
 * @author Manfred Geiler (latest modification by $Author: bommel $)
 * @version $Revision: 1187701 $ $Date: 2011-10-22 12:21:54 +0000 (Sat, 22 Oct 2011) $
 */
public final class ConverterUtils
{
    //private static final Log log = LogFactory.getLog(ConverterUtils.class);

    private ConverterUtils() {}


    public static int convertToInt(Object value)
    {
        if (value instanceof Number)
        {
            return ((Number) value).intValue();
        }
        else if (value instanceof String)
        {
            try
            {
                return Integer.parseInt((String) value);
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("Cannot convert " + value.toString() + " to int");
            }
        }
        else
        {
            throw new IllegalArgumentException("Cannot convert " + value.toString() + " to int");
        }
    }

    public static boolean convertToBoolean(Object value)
    {
        if (value instanceof Boolean)
        {
            return ((Boolean) value);
        }
        else if (value instanceof String)
        {
            try
            {
                return Boolean.parseBoolean((String) value);
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("Cannot convert " + value.toString() + " to boolean");
            }
        }
        else
        {
            throw new IllegalArgumentException("Cannot convert " + value.toString() + " to boolean");
        }
    }    

    public static long convertToLong(Object value)
    {
        if (value instanceof Number)
        {
            return ((Number) value).longValue();
        }
        else if (value instanceof String)
        {
            try
            {
                return Long.parseLong((String) value);
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("Cannot convert " + value.toString() + " to long");
            }
        }
        else
        {
            throw new IllegalArgumentException("Cannot convert " + value.toString() + " to long");
        }
    }

    public static double convertToDouble(Object value)
    {
        if (value instanceof Number)
        {
            return ((Number) value).doubleValue();
        }
        else if (value instanceof String)
        {
            try
            {
                return Double.parseDouble((String) value);
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("Cannot convert " + value.toString() + " to double");
            }
        }
        else
        {
            throw new IllegalArgumentException("Cannot convert " + value.toString() + " to double");
        }
    }


}
