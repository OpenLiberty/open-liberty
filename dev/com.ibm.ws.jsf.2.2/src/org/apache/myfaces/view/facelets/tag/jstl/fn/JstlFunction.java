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
package org.apache.myfaces.view.facelets.tag.jstl.fn;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletFunction;

/**
 * Implementations of JSTL Functions
 * 
 * @author Jacob Hookom
 * @version $Id: JstlFunction.java 1197653 2011-11-04 16:53:39Z lu4242 $
 */
public final class JstlFunction
{
    private JstlFunction()
    {
    }

    @JSFFaceletFunction(name="fn:contains")
    public static boolean contains(String name, String searchString)
    {
        if (name == null || searchString == null)
        {
            return false;
        }

        return -1 != name.indexOf(searchString);
    }

    @JSFFaceletFunction(name="fn:containsIgnoreCase")
    public static boolean containsIgnoreCase(String name, String searchString)
    {
        if (name == null || searchString == null)
        {
            return false;
        }
        
        return -1 != name.toUpperCase().indexOf(searchString.toUpperCase());
    }

    @JSFFaceletFunction(name="fn:endsWith")
    public static boolean endsWith(String name, String searchString)
    {
        if (name == null || searchString == null)
        {
            return false;
        }
        
        return name.endsWith(searchString);
    }

    @JSFFaceletFunction(name="fn:escapeXml")
    public static String escapeXml(String value)
    {
        if (value == null)
        {
            return "";
        }
        if (value.length() == 0)
        {
            return "";
        }
        
        StringBuilder sb = null;    //create later on demand
        String app;
        char c;
        for (int i = 0; i < value.length (); ++i)
        {
            app = null;
            c = value.charAt(i);
            
            // All characters before letters
            if ((int)c < 0x41)
            {
                switch (c)
                {
                    case '<' : app = "&lt;"; break;      //<
                    case '>' : app = "&gt;"; break;      //>
                    case '\'': app = "&#039;"; break;    //'
                    case '"' : app = "&#034;"; break;    //"
                    case '&' : //&
                        if (i+4 < value.length() )
                        {
                            if ('a' == value.charAt(i+1) &&
                                'm' == value.charAt(i+2) &&
                                'p' == value.charAt(i+3) &&
                                ';' == value.charAt(i+4))
                            {
                                //Skip
                            }
                            else
                            {
                                app = "&amp;";
                            }
                        }
                        else
                        {
                            app = "&amp;";
                        }
                        break;
                    default: // all fine
                }
            } 
            if (app != null)
            {
                if (sb == null)
                {
                    sb = new StringBuilder(value.substring(0, i));
                }
                sb.append(app);
            }
            else
            {
                if (sb != null)
                {
                    sb.append(c);
                }
            }
        }

        if (sb == null)
        {
            return value;
        }
        else
        {
            return sb.toString();
        }
    }

    @JSFFaceletFunction(name="fn:indexOf")
    public static int indexOf(String name, String searchString)
    {
        if (name == null || searchString == null)
        {
            return -1;
        }
        
        return name.indexOf(searchString);
    }

    @JSFFaceletFunction(name="fn:join")
    public static String join(String[] a, String delim)
    {
        if (a == null || delim == null)
        {
            return "";
        }
        
        if (a.length == 0)
        {
            return "";
        }
        
        StringBuilder sb = new StringBuilder(a.length * (a[0].length() + delim.length()));
        for (int i = 0; i < a.length; i++)
        {
            sb.append(a[i]);
            if (i < (a.length - 1))
            {
                sb.append(delim);
            }
        }
        
        return sb.toString();
    }

    @JSFFaceletFunction(name="fn:length")
    public static int length(Object obj)
    {
        if (obj == null)
        {
            return 0;
        }
        
        if (obj instanceof Collection)
        {
            return ((Collection<?>) obj).size();
        }
        
        if (obj.getClass().isArray())
        {
            return Array.getLength(obj);
        }
        
        if (obj instanceof String)
        {
            return ((String) obj).length();
        }
        
        if (obj instanceof Map)
        {
            return ((Map<?, ?>) obj).size();
        }
        
        throw new IllegalArgumentException("Object type not supported: " + obj.getClass().getName());
    }

    @JSFFaceletFunction(name="fn:replace")
    public static String replace(String value, String a, String b)
    {
        if (value == null)
        {
            return "";
        }
        if (value.length() == 0)
        {
            return "";
        }
        if (a == null)
        {
            return value;
        }
        if (a.length() == 0)
        {
            return value;
        }
        if (b == null)
        {
            b = "";
        }
        
        return value.replaceAll(a, b);
    }

    @JSFFaceletFunction(name="fn:split")
    public static String[] split(String value, String d)
    {
        if (value == null)
        {
            return new String[]{""};
        }
        if (value.length() == 0)
        {
            return new String[]{""};
        }
        if (d == null)
        {
            return new String[]{value};
        }
        if (d.length() == 0)
        {
            return new String[]{value};
        }        
        
        StringTokenizer st = new StringTokenizer(value, d);
        int numTokens = st.countTokens();
        if (numTokens == 0)
        {
            return new String[]{value};
        }
        String[] array = new String[numTokens];
        int i = 0;
        while (st.hasMoreTokens())
        {
            array[i] = st.nextToken();
            i++;
        }
        return array;
    }

    @JSFFaceletFunction(name="fn:startsWith")
    public static boolean startsWith(String value, String p)
    {
        if (value == null || p == null)
        {
            return false;
        }
        
        return value.startsWith(p);
    }

    @JSFFaceletFunction(name="fn:substring")
    public static String substring(String v, int s, int e)
    {
        if (v == null)
        {
            return "";
        }
        if (v.length() == 0)
        {
            return "";
        }
        if (s >= v.length())
        {
            return "";
        }
        if (s < 0)
        {
            s = 0;
        }
        if (e >= v.length())
        {
            e = v.length();
        }
        if (e < s)
        {
            return ""; 
        }
        
        return v.substring(s, e);
    }

    @JSFFaceletFunction(name="fn:substringAfter")
    public static String substringAfter(String v, String p)
    {
        if (v == null)
        {
            return "";
        }
        if (v.length() == 0)
        {
            return "";
        }
        
        int i = v.indexOf(p);
        if (i >= 0)
        {
            return v.substring(i + p.length());
        }
        
        return "";
    }

    @JSFFaceletFunction(name="fn:substringBefore")
    public static String substringBefore(String v, String s)
    {
        if (v == null)
        {
            return "";
        }
        if (v.length() == 0)
        {
            return "";
        }
        
        int i = v.indexOf(s);
        if (i > 0)
        {
            return v.substring(0, i);
        }
        
        return "";
    }

    @JSFFaceletFunction(name="fn:toLowerCase")
    public static String toLowerCase(String v)
    {
        if (v == null)
        {
            return "";
        }
        if (v.length() == 0)
        {
            return "";
        }
        
        return v.toLowerCase();
    }

    @JSFFaceletFunction(name="fn:toUpperCase")
    public static String toUpperCase(String v)
    {
        if (v == null)
        {
            return "";
        }
        if (v.length() == 0)
        {
            return "";
        }
        
        return v.toUpperCase();
    }

    @JSFFaceletFunction(name="fn:trim")
    public static String trim(String v)
    {
        if (v == null)
        {
            return "";
        }
        if (v.length() == 0)
        {
            return "";
        }
        return v.trim();
    }

}
