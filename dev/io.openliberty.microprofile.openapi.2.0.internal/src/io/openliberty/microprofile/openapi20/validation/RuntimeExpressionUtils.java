/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import io.openliberty.microprofile.openapi20.utils.Constants;

/**
 * Utility methods to validate strings as runtime expressions according to the Swagger 3.0 grammar
 * expression = ( "$url" | "$method" | "$statusCode" | "$request." source | "$response." source )
 * source = ( header-reference | query-reference | path-reference | body-reference )
 * header-reference = "header." token
 * query-reference = "query." name
 * path-reference = "path." name
 * body-reference = "body" ["#" fragment]
 * fragment = a JSON Pointer [RFC 6901](https://tools.ietf.org/html/rfc6901)
 * name = *( char )
 * char = as per RFC [7159](https://tools.ietf.org/html/rfc7159#section-7)
 * token = as per RFC [7230](https://tools.ietf.org/html/rfc7230#section-3.2.6)
 */
public class RuntimeExpressionUtils {

    private static final String EXP_URL = "$url";
    private static final String EXP_METHOD = "$method";
    private static final String EXP_STATUS_CODE = "$statusCode";
    private static final String EXP_REQUEST = "$request.";
    private static final String EXP_RESPONSE = "$response.";
    private static final String HEADER_REF = "header.";
    private static final String QUERY_REF = "query.";
    private static final String PATH_REF = "path.";
    private static final String BODY_REF = "body";

    public static boolean isRuntimeExpression(String re) {
        if (re == null)
            return false;
        re = re.trim();
        if (re.equals(EXP_URL))
            return true;
        if (re.equals(EXP_METHOD))
            return true;
        if (re.equals(EXP_STATUS_CODE))
            return true;
        if (re.startsWith(EXP_REQUEST)) {
            return isSource(re.substring(EXP_REQUEST.length()));
        }
        if (re.startsWith(EXP_RESPONSE)) {
            return isSource(re.substring(EXP_RESPONSE.length()));
        }
        return false;
    }

    /**
     * @param substring test to see if it conforms to any of the formats of a source object
     * @return
     */
    private static boolean isSource(String sourceString) {
        if (sourceString == null)
            return false;
        sourceString = sourceString.trim();
        if (sourceString.startsWith(HEADER_REF)) {
            return isToken(sourceString.substring(HEADER_REF.length()));
        }
        if (sourceString.startsWith(QUERY_REF)) {
            return isName(sourceString.substring(QUERY_REF.length()));
        }
        if (sourceString.startsWith(PATH_REF)) {
            return isName(sourceString.substring(PATH_REF.length()));
        }
        if (sourceString.startsWith(BODY_REF)) {
            return isFragment(sourceString.substring(BODY_REF.length()));
        }
        return false;
    }

    /**
     * @param token a string to be validated against RFC 7230 token definition
     * @return true if the characters match the syntax for a header token.
     */
    private static boolean isToken(String token) {
        // Characters from RFC 7230 3.2.6 token
        // Token refers to RFC 5234 which says DIGIT is 0-9 and ALPHA is a-zA-Z
        //"!" / "#" / "$" / "%" / "&" / "'" / "*" / "+" / "-" / "." / "^" / "_" / "`" / "|" / "~" / DIGIT / ALPHA
        return Constants.REGEX_TOKEN_PATTERN.matcher(token).matches();
    }

    /**
     * @param name a string to be validated against RFC 7159 section 7 *( char ).
     * @return true if the characters spell a name.
     */
    private static boolean isName(String name) {
        // Assume the name is 1 or more characters.
        if (name.length() < 1)
            return false;
        //TODO see if the parser decodes encoded characters line \n and \b for us.
        // If parser decodes then return true;
        return true;
    }

    /**
     * @param fragment "#" followed by a JSON pointer from RFC 6901
     * @return true if JSON pointer syntax followed. E.g. #/component/parameter/food
     */
    private static boolean isFragment(String fragment) {
        if (fragment == null || !fragment.startsWith("#/"))
            return false;
        
        Scanner s = new Scanner(fragment.substring(1)).useDelimiter("/");
        if (!s.hasNext())
            return false; // need at least one reference
        while (s.hasNext()) {
            String ref = s.next();
            if (!isReference(ref))
                return false;
        }
        return true;
    }

    /**
     * @param ref A reference token as defined in RFC 6901
     * @return true if RFC 6901 escape system followed
     */
    private static boolean isReference(String ref) {
        if (ref == null || ref.length() < 1)
            return false;
        int tildeLoc = ref.indexOf('~');
        // Validate characters escaped with tilde: ~0 and ~1 allowed.
        // ~ alone is not allowed. / alone is not allowed but will be caught before it gets here.
        while (tildeLoc != -1) {
            if (tildeLoc < ref.length() - 1 && (ref.charAt(tildeLoc + 1) == '0' || ref.charAt(tildeLoc + 1) == '1'))
                tildeLoc = ref.indexOf('~', tildeLoc + 2);
            else
                return false; // ~2 etc not allowed
        }
        return true;
    }

    // Examine the input for zero or more matched pairs of { and }.
    // Return a list of the strings inside the brackets.
    // If there are zero strings then return an empty list.
    // If one of the strings is zero length return an empty string in the list.
    // If there are unmatched brackets then return null.
    // E.g. a{b}c{d}e returns array: { "b", "d" }
    public static List<String> extractURLVars(String input) {
        List<String> list = new ArrayList<String>();
        if (input.startsWith("}"))
            return null;
        int openLoc = input.indexOf('{');
        int closeLoc = 0;
        while (openLoc != -1) {
            closeLoc = input.indexOf('}', closeLoc + 1);
            if (closeLoc <= openLoc)
                return null; // unmatched closing bracket "xxx}xxx{yy}" or missing closing bracket "xx{xx"
            list.add(input.substring(openLoc + 1, closeLoc));
            openLoc = input.indexOf('{', openLoc + 1);
            if (openLoc > 0 && openLoc < closeLoc)
                return null; // unmatched opening bracket "xx{yy{zz}"
        }
        closeLoc = input.indexOf('}', closeLoc + 1);
        if (closeLoc != -1)
            return null; // unmatched closing bracket xx{yy}}
        return list;
    }
}
