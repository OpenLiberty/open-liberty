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

/**
 * This class contains utility methods to detect special cases to be handled on "script" or
 * "style" tags by HtmlResponseWriterImpl.
 */
public class CommentUtils
{
    public static final String INLINE_SCRIPT_COMMENT = "//";
    public static final String START_SCRIPT_COMMENT = "/*";
    public static final String END_SCRIPT_COMMENT = "*/";
    public static final String CDATA_SIMPLE_START = "<![CDATA[";
    public static final String CDATA_SIMPLE_END = "]]>";
    public static final String COMMENT_SIMPLE_START = "<!--";
    public static final String COMMENT_SIMPLE_END = "-->";

    public static boolean isStartMatchWithCommentedCDATA(String trimmedContent)
    {
        if (trimmedContent.startsWith(START_SCRIPT_COMMENT))
        {
            int offset = 2;
            while (trimmedContent.charAt(offset) <= ' ')
            {
                offset++;
            }
            if (trimmedContent.startsWith(CDATA_SIMPLE_START, offset))
            {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isEndMatchWithCommentedCDATA(String trimmedContent)
    {
        if (trimmedContent.endsWith(END_SCRIPT_COMMENT))
        {
            int offset = trimmedContent.length()-3;
            while (trimmedContent.charAt(offset) <= ' ')
            {
                offset--;
            }
            // CDATA_SIMPLE_END.length() = 3
            // So, -3 +1 = -2
            if (trimmedContent.startsWith(CDATA_SIMPLE_END, offset - 2))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isEndMatchtWithInlineCommentedXmlCommentTag(String trimmedContent)
    {
        if (trimmedContent.endsWith(COMMENT_SIMPLE_END))
        {
            int offset = trimmedContent.length()-4;
            while (trimmedContent.charAt(offset) <= ' ' &&
                    trimmedContent.charAt(offset) != '\n')
            {
                offset--;
            }
            // INLINE_SCRIPT_COMMENT.length() = 2
            // So, -2 +1 = -1
            if (trimmedContent.startsWith(INLINE_SCRIPT_COMMENT, offset - 1))
            {
                return true;
            }
        }
        return false;        
    }
    
    public static boolean isStartMatchWithInlineCommentedCDATA(String trimmedContent)
    {
        if (trimmedContent.startsWith(INLINE_SCRIPT_COMMENT))
        {
            int offset = 2;
            while (trimmedContent.charAt(offset) <= ' ' &&
                    trimmedContent.charAt(offset) != '\n')
            {
                offset++;
            }
            if (trimmedContent.startsWith(CDATA_SIMPLE_START, offset))
            {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isEndMatchWithInlineCommentedCDATA(String trimmedContent)
    {
        if (trimmedContent.endsWith(CDATA_SIMPLE_END))
        {
            int offset = trimmedContent.length()- 4;
            while (trimmedContent.charAt(offset) <= ' ' &&
                    trimmedContent.charAt(offset) != '\n')
            {
                offset--;
            }
            // INLINE_SCRIPT_COMMENT.length() = 2
            // So, -2 +1 = -1
            if (trimmedContent.startsWith(INLINE_SCRIPT_COMMENT, offset - 1))
            {
                return true;
            }
        }
        return false;
    }
}
