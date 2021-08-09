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
package org.apache.myfaces.shared.renderkit.html.util;

import java.io.IOException;
import java.io.Writer;

/**
 * Converts characters outside of latin-1 set in a string to numeric character references.
 * 
 */
public abstract class UnicodeEncoder
{
    /**
     * Encodes the given string, so that it can be used within a html page.
     * @param string the string to convert
     */
    public static String encode (String string)
    {
        if (string == null)
        {
            return "";
        }

        StringBuilder sb = null;
        char c;
        for (int i = 0; i < string.length (); ++i)
        {
            c = string.charAt(i);
            if (((int)c) >= 0x80)
            {
                if( sb == null )
                {
                    sb = new StringBuilder( string.length()+4 );
                    sb.append( string.substring(0,i) );
                }
                //encode all non basic latin characters
                sb.append("&#");
                sb.append((int)c);
                sb.append(";");
            }
            else if( sb != null )
            {
                sb.append(c);
            }
        }

        return sb != null ? sb.toString() : string;
    }
    
    public static void encode (Writer writer, String string) throws IOException
    {
        if (string == null)
        {
            return;
        }

        int start = 0;
        char c;
        for (int i = 0; i < string.length (); ++i)
        {
            c = string.charAt(i);
            if (((int)c) >= 0x80)
            {
                if (start < i)
                {
                    writer.write(string, start, i-start);
                }
                start = i+1;
                //encode all non basic latin characters
                writer.write("&#");
                writer.write(Integer.toString((int)c));
                writer.write(";");
            }
        }

        if (start == 0)
        {
            writer.write(string);
        }
        else if (start < string.length())
        {
            writer.write(string,start,string.length()-start);
        }
    }

    public static void encode (Writer writer, char[] cbuf, int off, int len) throws IOException
    {
        if (cbuf == null)
        {
            return;
        }

        int start = off;
        char c;
        for (int i = off; i < off+len; ++i)
        {
            c = cbuf[i];
            if (((int)c) >= 0x80)
            {
                if (start < i)
                {
                    writer.write(cbuf, start, i-start);
                }
                start = i+1;
                //encode all non basic latin characters
                writer.write("&#");
                writer.write(Integer.toString((int)c));
                writer.write(";");
            }
        }

        if (start == off)
        {
            writer.write(cbuf, off, len);
        }
        else if (start < off+len)
        {
            writer.write(cbuf,start,off+len-start);
        }
    }
    
    public static void encode (Writer writer, String cbuf, int off, int len) throws IOException
    {
        if (cbuf == null)
        {
            return;
        }

        int start = off;
        char c;
        for (int i = off; i < off+len; ++i)
        {
            c = cbuf.charAt(i);
            if (((int)c) >= 0x80)
            {
                if (start < i)
                {
                    writer.write(cbuf, start, i-start);
                }
                start = i+1;
                //encode all non basic latin characters
                writer.write("&#");
                writer.write(Integer.toString((int)c));
                writer.write(";");
            }
        }

        if (start == off)
        {
            writer.write(cbuf, off, len);
        }
        else if (start < off+len)
        {
            writer.write(cbuf,start,off+len-start);
        }
    }
}
