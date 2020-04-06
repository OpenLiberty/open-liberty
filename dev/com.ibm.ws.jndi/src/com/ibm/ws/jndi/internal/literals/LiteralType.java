/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.internal.literals;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import java.math.BigInteger;
import java.util.regex.Pattern;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

/**
 * Parses string values into primitive wrapper types, such as {@link Integer}.
 * Any Java primitive literal will be converted into the
 * appropriate wrapper type. String literals will be converted into String objects.
 * Any other string will be returned unmodified.
 */
@Trivial
enum LiteralType {
    BOOLEAN_TRUE("true") {
        @Override
        Object parse(String s) {
            return true;
        }
    },
    BOOLEAN_FALSE("false") {
        @Override
        Object parse(String s) {
            return false;
        }
    },
    CHARACTER("'.*") {
        @Override
        Object parse(String s) {
            return StringParser.parseChar(s);
        }
    },
    DOUBLE(Patterns.DOUBLE) {
        @Override
        Object parse(String s) {
            try {
                return Double.valueOf(s);
            } catch (NumberFormatException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Failed to parse double", s);
                return s;
            }
        }
    },
    FLOAT(Patterns.FLOAT) {
        @Override
        Object parse(String s) {
            try {
                return Float.valueOf(s);
            } catch (NumberFormatException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Failed to parse float", s);
                return s;
            }
        }
    },
    INTEGER_BINARY("0[Bb][01](?:_*[01]){0,31}") {
        @Override
        Object parse(String s) {
            // manipulate the string into a parsable form
            s = s.replaceAll("_*", "").substring(2);
            try {
                return parseUnsigned(s, 2, MAX_UNSIGNED_INTEGER).intValue();
            } catch (NumberFormatException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Failed to parse binary int", s);
                return s;
            }
        }
    },
    INTEGER_OCTAL("0(?:_*[0-7]){1,11}") {
        @Override
        Object parse(String s) {
            // manipulate the string into a parsable form
            s = s.replaceAll("_*", "");
            try {
                return parseUnsigned(s, 8, MAX_UNSIGNED_INTEGER).intValue();
            } catch (NumberFormatException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Failed to parse octal int", s);
                return s;
            }
        }
    },
    INTEGER_DENARY("[+-]?(0|[1-9](?:_*[0-9]){0,9})") {
        @Override
        Object parse(String s) {
            s = s.replaceAll("_*", "");
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Failed to parse int", s);
                return s;
            }
        }
    },
    INTEGER_HEX("0[Xx][0-9A-Fa-f](?:_*[0-9A-Fa-f]){0,7}") {
        @Override
        Object parse(String s) {
            // manipulate the string into a parsable form
            s = s.replaceAll("_*", "").substring(2);
            try {
                return parseUnsigned(s, 16, MAX_UNSIGNED_INTEGER).intValue();
            } catch (NumberFormatException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Failed to parse hex int", s);
                return s;
            }
        }
    },
    LONG_BINARY("0[Bb][01](?:_*[01]){0,63}[Ll]") {
        @Override
        Object parse(String s) {
            // manipulate the string into a parsable form
            s = s.replaceAll("_*", "").replaceAll("[Ll]$", "").substring(2);
            try {
                return parseUnsigned(s, 2, MAX_UNSIGNED_LONG).longValue();
            } catch (NumberFormatException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Failed to parse binary long", s);
                return s;
            }
        }
    },
    LONG_OCTAL("0(?:_*[0-7]){1,23}[Ll]") {
        @Override
        Object parse(String s) {
            // manipulate the string into a parsable form
            s = s.replaceAll("_*", "").replaceAll("[Ll]$", "");
            try {
                return parseUnsigned(s, 8, MAX_UNSIGNED_LONG).longValue();
            } catch (NumberFormatException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Failed to parse octal long", s);
                return s;
            }
        }
    },
    LONG_DENARY("[+-]?(0|[1-9](?:_*[0-9]){0,18})[Ll]") {
        @Override
        Object parse(String s) {
            s = s.replaceAll("_*", "").replaceAll("[Ll]$", "");
            try {
                return Long.valueOf(s);
            } catch (NumberFormatException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Failed to parse long", s);
                return s;
            }
        }
    },
    LONG_HEX("0[Xx][0-9A-Fa-f](?:_*[0-9A-Fa-f]){0,15}[Ll]") {
        @Override
        Object parse(String s) {
            // manipulate the string into a parsable form
            s = s.replaceAll("_*", "").replaceAll("[Ll]$", "").substring(2);
            try {
                return parseUnsigned(s, 16, MAX_UNSIGNED_LONG).longValue();
            } catch (NumberFormatException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Failed to parse hex long", s);
                return s;
            }
        }
    },
    STRING("\".*") {
        @Override
        Object parse(String s) {
            return StringParser.parse(s);
        }
    };

    static final TraceComponent tc = Tr.register(LiteralType.class);
    static final BigInteger MAX_UNSIGNED_INTEGER = new BigInteger("2").pow(32).subtract(ONE);
    static final BigInteger MAX_UNSIGNED_LONG = new BigInteger("2").pow(64).subtract(ONE);
    final Pattern pattern;

    LiteralType(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    boolean matches(String s) {
        return pattern.matcher(s).matches();
    }

    abstract Object parse(String s);

    private static BigInteger parseUnsigned(String s, int radix, BigInteger maxValue) {
        BigInteger i = new BigInteger(s, radix);
        // check range
        if (i.compareTo(ZERO) < 0 || i.compareTo(maxValue) > 0)
            throw new NumberFormatException();
        return i;
    }
}
