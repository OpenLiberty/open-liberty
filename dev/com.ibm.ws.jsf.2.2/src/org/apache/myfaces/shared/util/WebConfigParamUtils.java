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
package org.apache.myfaces.shared.util;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;

/**
 * Utility class to handle web config parameters
 * 
 * @since 2.0.10 (4.0.4 in shared, 1.0.1 in commons)
 */
public final class WebConfigParamUtils
{
    public final static String[] COMMON_TRUE_VALUES = {"true", "on", "yes"};
    public final static String[] COMMON_FALSE_VALUES = {"false", "off", "no"};

    /**
     * Gets the String init parameter value from the specified context. If the parameter is an empty String or a String
     * containing only white space, this method returns <code>null</code>
     * 
     * @param context
     *            the application's external context
     * @param name
     *            the init parameter's name
     *            
     * @return the parameter if it was specified and was not empty, <code>null</code> otherwise
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */    
    public static String getStringInitParameter(ExternalContext context, String name)
    {
        return getStringInitParameter(context,name,null);
    }

    /**
     * Gets the String init parameter value from the specified context. If the parameter is an empty String or a String
     * containing only white space, this method returns <code>null</code>
     * 
     * @param context
     *            the application's external context
     * @param name
     *            the init parameter's name
     * @param defaultValue
     *            the value by default if null or empty
     *            
     * @return the parameter if it was specified and was not empty, <code>null</code> otherwise
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */    
    public static String getStringInitParameter(ExternalContext context, String name, String defaultValue)
    {
        if (name == null)
        {
            throw new NullPointerException();
        }
        
        String param = context.getInitParameter(name);
        
        if (param == null)
        {
            return defaultValue;
        }

        param = param.trim();
        if (param.length() == 0)
        {
            return defaultValue;
        }

        return param;
    }
    
    /**
     * Gets the String init parameter value from the specified context. If the parameter is an 
     * empty String or a String
     * containing only white space, this method returns <code>null</code>
     * 
     * @param context
     *            the application's external context
     * @param names
     *            the init parameter's names, the first one is scanned first. Usually used when a 
     *            param has multiple aliases
     *            
     * @return the parameter if it was specified and was not empty, <code>null</code> otherwise
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */    
    public static String getStringInitParameter(ExternalContext context, String[] names)
    {
        return getStringInitParameter(context, names, null);
    }
    
    /**
     * Gets the String init parameter value from the specified context. If the parameter is an empty 
     * String or a String containing only white space, this method returns <code>null</code>
     * 
     * @param context
     *            the application's external context
     * @param names
     *            the init parameter's names, the first one is scanned first. Usually used when a param has 
     *            multiple aliases
     * @param defaultValue
     *            the value by default if null or empty
     *            
     * @return the parameter if it was specified and was not empty, <code>null</code> otherwise
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */    
    public static String getStringInitParameter(ExternalContext context, String[] names, String defaultValue)
    {
        if (names == null)
        {
            throw new NullPointerException();
        }
        
        String param = null;
        
        for (String name : names)
        {
            if (name == null)
            {
                throw new NullPointerException();
            }
            
            param = context.getInitParameter(name);
            if (param != null)
            {
                break;
            }
        }
        
        if (param == null)
        {
            return defaultValue;
        }

        param = param.trim();
        if (param.length() == 0)
        {
            return defaultValue;
        }

        return param;
    }
    
    /**
     * Gets the boolean init parameter value from the specified context. If the parameter was not specified, the default
     * value is used instead.
     * 
     * @param context
     *            the application's external context
     * @param name
     *            the init parameter's name
     * @param deprecatedName
     *            the init parameter's deprecated name.
     * @param defaultValue
     *            the default value to return in case the parameter was not set
     * 
     * @return the init parameter value as a boolean
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */
    public static boolean getBooleanInitParameter(ExternalContext context, String name)
    {
        return getBooleanInitParameter(context, name, false);
    }
    
    /**
     * Gets the boolean init parameter value from the specified context. If the parameter was not specified, the default
     * value is used instead.
     * 
     * @param context
     *            the application's external context
     * @param name
     *            the init parameter's name
     * @param deprecatedName
     *            the init parameter's deprecated name.
     * @param defaultValue
     *            the default value to return in case the parameter was not set
     * 
     * @return the init parameter value as a boolean
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */
    public static boolean getBooleanInitParameter(ExternalContext context, String name, boolean defaultValue)
    {
        if (name == null)
        {
            throw new NullPointerException();
        }

        String param = getStringInitParameter(context, name);
        if (param == null)
        {
            return defaultValue;
        }
        else
        {
            return Boolean.parseBoolean(param.toLowerCase());
        }
    }
    
    /**
     * Gets the boolean init parameter value from the specified context. If the parameter 
     * was not specified, the default
     * value is used instead.
     * 
     * @param context
     *            the application's external context
     * @param name
     *            the init parameter's name
     * @param deprecatedName
     *            the init parameter's deprecated name.
     * @param defaultValue
     *            the default value to return in case the parameter was not set
     * @param valuesIgnoreCase
     *            an array of valid values to match
     * @param returnOnValueEqualsIgnoreCase
     *            the value to return in case the parameter match with valuesIgnoreCase
     * 
     * @return the init parameter value as a boolean
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */
    public static boolean getBooleanInitParameter(ExternalContext context, String name, 
            boolean defaultValue, String [] valuesIgnoreCase, boolean returnOnValueEqualsIgnoreCase)
    {
        if (name == null)
        {
            throw new NullPointerException();
        }

        String param = getStringInitParameter(context, name);
        if (param == null)
        {
            return defaultValue;
        }
        else
        {
            if (valuesIgnoreCase != null)
            {
                for (String trueValue : valuesIgnoreCase)
                {
                    if (trueValue.equalsIgnoreCase(param))
                    {
                        return returnOnValueEqualsIgnoreCase;
                    }
                }
                return defaultValue;
            }
            else 
            {
                return Boolean.parseBoolean(param.toLowerCase());
            }
        }
    }    

    /**
     * Gets the boolean init parameter value from the specified context. If the parameter was not specified, 
     * the default value is used instead.
     * 
     * @param context
     *            the application's external context
     * @param names
     *            the init parameter's names
     * 
     * @return the init parameter value as a boolean
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */
    
    public static boolean getBooleanInitParameter(ExternalContext context, String[] names)
    {
        return getBooleanInitParameter(context, names, false);
    }

    /**
     * Gets the boolean init parameter value from the specified context. If the parameter was not specified,
     * the default value is used instead.
     * 
     * @param context
     *            the application's external context
     * @param names
     *            the init parameter's names
     * @param defaultValue
     *            the default value to return in case the parameter was not set
     * 
     * @return the init parameter value as a boolean
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */
    public static boolean getBooleanInitParameter(ExternalContext context, String[] names, boolean defaultValue)
    {
        if (names == null)
        {
            throw new NullPointerException();
        }
        
        String param = null;
        for (String name : names)
        {
            if (name == null)
            {
                throw new NullPointerException();
            }
            
            param = getStringInitParameter(context, name);
            if (param != null)
            {
                break;
            }
        }
        if (param == null)
        {
            return defaultValue;
        }
        else
        {
            return Boolean.parseBoolean(param.toLowerCase());
        }
    }
    
    /**
     * Gets the boolean init parameter value from the specified context. If the parameter was not specified,
     * the default value is used instead.
     * 
     * @param context
     *            the application's external context
     * @param names
     *            the init parameter's names
     * @param defaultValue
     *            the default value to return in case the parameter was not set
     * @param valuesIgnoreCase
     *            an array of valid values to match
     * @param returnOnValueEqualsIgnoreCase
     *            the value to return in case the parameter match with valuesIgnoreCase
     * 
     * @return the init parameter value as a boolean
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */
    
    public static boolean getBooleanInitParameter(ExternalContext context, String[] names, boolean defaultValue,
            String [] valuesIgnoreCase, boolean returnOnValueEqualsIgnoreCase)
    {
        if (names == null)
        {
            throw new NullPointerException();
        }
        
        String param = null;
        for (String name : names)
        {
            if (name == null)
            {
                throw new NullPointerException();
            }
            
            param = getStringInitParameter(context, name);
            if (param != null)
            {
                break;
            }
        }
        if (param == null)
        {
            return defaultValue;
        }
        else
        {
            if (valuesIgnoreCase != null)
            {
                for (String trueValue : valuesIgnoreCase)
                {
                    if (trueValue.equalsIgnoreCase(param))
                    {
                        return returnOnValueEqualsIgnoreCase;
                    }
                }
                return defaultValue;
            }
            else 
            {
                return Boolean.parseBoolean(param.toLowerCase());
            }
        }
    }
    
    /**
     * Gets the int init parameter value from the specified context. If the parameter was not 
     * specified, the default value is used instead.
     * 
     * @param context
     *            the application's external context
     * @param name
     *            the init parameter's name
     * @param deprecatedName
     *            the init parameter's deprecated name.
     * @param defaultValue
     *            the default value to return in case the parameter was not set
     * 
     * @return the init parameter value as a int
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */
    public static int getIntegerInitParameter(ExternalContext context, String name)
    {
        return getIntegerInitParameter(context, name, 0);
    }
    
    /**
     * Gets the int init parameter value from the specified context. If the parameter was not specified,
     * the default value is used instead.
     * 
     * @param context
     *            the application's external context
     * @param name
     *            the init parameter's name
     * @param deprecatedName
     *            the init parameter's deprecated name.
     * @param defaultValue
     *            the default value to return in case the parameter was not set
     * 
     * @return the init parameter value as a int
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */
    public static int getIntegerInitParameter(ExternalContext context, String name, int defaultValue)
    {
        if (name == null)
        {
            throw new NullPointerException();
        }

        String param = getStringInitParameter(context, name);
        if (param == null)
        {
            return defaultValue;
        }
        else
        {
            return Integer.parseInt(param.toLowerCase());
        }
    }

    /**
     * Gets the int init parameter value from the specified context. If the parameter was not specified,
     * the default value is used instead.
     * 
     * @param context
     *            the application's external context
     * @param names
     *            the init parameter's names
     * 
     * @return the init parameter value as a int
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */
    public static int getIntegerInitParameter(ExternalContext context, String[] names)
    {
        return getIntegerInitParameter(context, names, 0);
    }

    /**
     * Gets the int init parameter value from the specified context. If the parameter was not specified, the default
     * value is used instead.
     * 
     * @param context
     *            the application's external context
     * @param names
     *            the init parameter's names
     * @param defaultValue
     *            the default value to return in case the parameter was not set
     * 
     * @return the init parameter value as a int
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */
    
    public static int getIntegerInitParameter(ExternalContext context, String[] names, int defaultValue)
    {
        if (names == null)
        {
            throw new NullPointerException();
        }
        
        String param = null;
        for (String name : names)
        {
            if (name == null)
            {
                throw new NullPointerException();
            }
            
            param = getStringInitParameter(context, name);
            if (param != null)
            {
                break;
            }
        }
        if (param == null)
        {
            return defaultValue;
        }
        else
        {
            return Integer.parseInt(param.toLowerCase());
        }
    }
    
    /**
     * Gets the long init parameter value from the specified context. If the parameter was not specified, the default
     * value is used instead.
     * 
     * @param context
     *            the application's external context
     * @param name
     *            the init parameter's name
     * @param deprecatedName
     *            the init parameter's deprecated name.
     * @param defaultValue
     *            the default value to return in case the parameter was not set
     * 
     * @return the init parameter value as a long
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */
    public static long getLongInitParameter(ExternalContext context, String name)
    {
        return getLongInitParameter(context, name, 0);
    }
    
    /**
     * Gets the long init parameter value from the specified context. If the parameter was not specified, the default
     * value is used instead.
     * 
     * @param context
     *            the application's external context
     * @param name
     *            the init parameter's name
     * @param deprecatedName
     *            the init parameter's deprecated name.
     * @param defaultValue
     *            the default value to return in case the parameter was not set
     * 
     * @return the init parameter value as a long
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */
    public static long getLongInitParameter(ExternalContext context, String name, long defaultValue)
    {
        if (name == null)
        {
            throw new NullPointerException();
        }

        String param = getStringInitParameter(context, name);
        if (param == null)
        {
            return defaultValue;
        }
        else
        {
            return Long.parseLong(param.toLowerCase());
        }
    }

    /**
     * Gets the long init parameter value from the specified context. If the parameter was not specified, the default
     * value is used instead.
     * 
     * @param context
     *            the application's external context
     * @param names
     *            the init parameter's names
     * 
     * @return the init parameter value as a long
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */
    
    public static long getLongInitParameter(ExternalContext context, String[] names)
    {
        return getLongInitParameter(context, names, 0);
    }
    
    /**
     * Gets the long init parameter value from the specified context. If the parameter was not specified, the default
     * value is used instead.
     * 
     * @param context
     *            the application's external context
     * @param names
     *            the init parameter's names
     * @param defaultValue
     *            the default value to return in case the parameter was not set
     * 
     * @return the init parameter value as a long
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */
    
    public static long getLongInitParameter(ExternalContext context, String[] names, long defaultValue)
    {
        if (names == null)
        {
            throw new NullPointerException();
        }
        
        String param = null;
        for (String name : names)
        {
            if (name == null)
            {
                throw new NullPointerException();
            }
            
            param = getStringInitParameter(context, name);
            if (param != null)
            {
                break;
            }
        }
        if (param == null)
        {
            return defaultValue;
        }
        else
        {
            return Long.parseLong(param.toLowerCase());
        }
    }

    /**
     * Gets the init parameter value from the specified context and instanciate it. 
     * If the parameter was not specified, the default value is used instead.
     * 
     * @param context
     *            the application's external context
     * @param name
     *            the init parameter's name
     * @param deprecatedName
     *            the init parameter's deprecated name.
     * @param defaultValue
     *            the default value to return in case the parameter was not set
     * 
     * @return the init parameter value as an object instance
     * 
     * @throws NullPointerException
     *             if context or name is <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public static <T> T getInstanceInitParameter(ExternalContext context, String name, 
            String deprecatedName, T defaultValue)
    {
        String param = getStringInitParameter(context, name, deprecatedName);
        if (param == null)
        {
            return defaultValue;
        }
        else
        {
            try
            {
                return (T) ClassUtils.classForName(param).newInstance();
            }
            catch (Exception e)
            {
                throw new FacesException("Error Initializing Object[" + param + "]", e);
            }
        }
    }
}
