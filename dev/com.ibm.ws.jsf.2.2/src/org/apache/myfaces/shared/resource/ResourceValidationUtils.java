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
package org.apache.myfaces.shared.resource;

public class ResourceValidationUtils
{
    public static boolean isValidResourceName(String resourceName)
    {
        return validateResourceName(resourceName, true);
    }
    
    public static boolean isValidLibraryName(String libraryName)
    {
        return validate(libraryName, false);
    }
    
    public static boolean isValidLibraryName(String libraryName, boolean allowSlash)
    {
        return validate(libraryName, allowSlash);
    }
    
    public static boolean isValidResourceId(String resourceId)
    {
        // Follow the same rules as for resourceName, but check resourceId does not
        // start with '/'
        return resourceId.length() > 0 && resourceId.charAt(0) != '/' && 
            validateResourceName(resourceId, true); 
    }
    
    public static boolean isValidViewResource(String resourceId)
    {
        // Follow the same rules as for resourceName, but check resourceId does not
        // start with '/'
        return validateResourceName(resourceId, true);
    }
    
    public static boolean isValidContractName(String contractName)
    {
        return validate(contractName, false);
    }    
    
    public static boolean isValidLocalePrefix(String localePrefix)
    {
        for (int i = 0, length = localePrefix.length(); i < length; i++)
        {
            char c = localePrefix.charAt(i);
            if ( (c >='A' && c <='Z') || c == '_' || (c >='a' && c <='z') || (c >='0' && c <='9') )
            {
                continue;
            }
            else
            {
                return false;
            }
        }
        return true;
    }
    
    private static boolean validate(String expression, boolean allowSlash)
    {
        int length = expression.length();
        if (length == 2 && 
            expression.charAt(0) == '.' &&
            expression.charAt(1) == '.')
        {
            return false;
        }
        for (int i = 0; i < length; i++)
        {
            char c = expression.charAt(i);

            // Enforce NameChar convention as specified
            // http://www.w3.org/TR/REC-xml/#NT-NameChar
            // Valid characters for NameChar
            // ":" | [A-Z] | "_" | [a-z] | [#xC0-#xD6] | [#xD8-#xF6] | 
            // [#xF8-#x2FF] | [#x370-#x37D] | [#x37F-#x1FFF] | [#x200C-#x200D] | 
            // [#x2070-#x218F] | [#x2C00-#x2FEF] | [#x3001-#xD7FF] | [#xF900-#xFDCF] 
            // | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
            // "-" | "." | [0-9] | #xB7 | [#x0300-#x036F] | [#x203F-#x2040]
            // Excluding ":" 
            if ( (c >='A' && c <='Z') || c == '_' || (c >='a' && c <='z') || 
                 (c >=0xC0 && c <=0xD6) || (c >=0xD8 && c <=0xF6) || 
                 (c >=0xF8 && c <=0x2FF) || (c >=0x370 && c <=0x37D) || 
                 (c >=0x37F && c <=0x1FFF) || (c >=0x200C && c <=0x200D) ||
                 (c >=0x2070 && c <=0x218F) || (c >=0x2C00 && c <=0x2FEF) || 
                 (c >=0x3001 && c <=0xD7FF) || (c >=0xF900 && c <=0xFDCF) ||
                 (c >=0xFDF0 && c <=0xFFFD) || (c >=0x10000 && c <=0xEFFFF) ||
                 c == '-' || (c >='0' && c <='9') || c == 0xB7 || (c >=0x300 && c <=0x36F) || 
                 (c >=0x203F && c <=0x2040) || (allowSlash && c == '/')
                 )
            {
                continue;
            }
            else if (c == '.')
            {
                if (i+2 < length)
                {
                    char c1 = expression.charAt(i+1);
                    char c2 = expression.charAt(i+2);
                    if (c == c1 && (c2 == '/' || c2 == '\\' ) )
                    {
                        return false;
                    }
                }
                continue;
            }
            else
            {
                return false;
            }
        }
        if (length >= 3)
        {
            if ( (expression.charAt(length-3) == '/' || expression.charAt(length-3) == '\\' ) && 
                  expression.charAt(length-2) == '.' &&
                  expression.charAt(length-1) == '.' )
            {
                return false;
            }
        }
        return true;
    }
    
    private static boolean validateResourceName(String expression, boolean allowSlash)
    {
        int length = expression.length();
        if (length == 2 && 
            expression.charAt(0) == '.' &&
            expression.charAt(1) == '.')
        {
            return false;
        }
        for (int i = 0; i < length; i++)
        {
            char c = expression.charAt(i);

            // Enforce NameChar convention as specified
            // http://www.w3.org/TR/REC-xml/#NT-NameChar
            // Valid characters for NameChar
            // ":" | [A-Z] | "_" | [a-z] | [#xC0-#xD6] | [#xD8-#xF6] | 
            // [#xF8-#x2FF] | [#x370-#x37D] | [#x37F-#x1FFF] | [#x200C-#x200D] | 
            // [#x2070-#x218F] | [#x2C00-#x2FEF] | [#x3001-#xD7FF] | [#xF900-#xFDCF] 
            // | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
            // "-" | "." | [0-9] | #xB7 | [#x0300-#x036F] | [#x203F-#x2040]
            // Excluding ":" 
            
            // Forbidden chars by win
            // < (less than)
            // > (greater than)
            // : (colon)
            // " (double quote)
            // / (forward slash)
            // \ (backslash)
            // | (vertical bar or pipe)
            // ? (question mark)
            // * (asterisk)
            // Do not use chars in UNIX because they have special meaning
            // *&%$|^/\~
            if ( (c >='A' && c <='Z') || c == '_' || (c >='a' && c <='z') || 
                 (c >=0xC0 && c <=0xD6) || (c >=0xD8 && c <=0xF6) || 
                 (c >=0xF8 && c <=0x2FF) || (c >=0x370 && c <=0x37D) || 
                 (c >=0x37F && c <=0x1FFF) || (c >=0x200C && c <=0x200D) ||
                 (c >=0x2070 && c <=0x218F) || (c >=0x2C00 && c <=0x2FEF) || 
                 (c >=0x3001 && c <=0xD7FF) || (c >=0xF900 && c <=0xFDCF) ||
                 (c >=0xFDF0 && c <=0xFFFD) || (c >=0x10000 && c <=0xEFFFF) ||
                 (c == '-') || (c >='0' && c <='9') || c == 0xB7 || (c >=0x300 && c <=0x36F) || 
                 (c >=0x203F && c <=0x2040) || (allowSlash && c == '/') ||
                 (c == '!') || (c == '#') || (c == '\'') || (c == '(') || (c == ')') ||
                 (c == '+') || (c == ',') || (c == ';' ) || (c == '=') || 
                 (c == '@') || (c == '[') || (c == ']' ) || (c == '{') || (c == '}'))
            {
                continue;
            }
            else if (c == '.')
            {
                if (i+2 < length)
                {
                    char c1 = expression.charAt(i+1);
                    char c2 = expression.charAt(i+2);
                    if (c == c1 && (c2 == '/' || c2 == '\\' ) )
                    {
                        return false;
                    }
                }
                continue;
            }
            else
            {
                return false;
            }
        }
        if (length >= 3)
        {
            if ( (expression.charAt(length-3) == '/' || expression.charAt(length-3) == '\\' ) && 
                  expression.charAt(length-2) == '.' &&
                  expression.charAt(length-1) == '.' )
            {
                return false;
            }
        }
        return true;
    }
}
