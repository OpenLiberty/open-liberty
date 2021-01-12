/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.transport.commons_text;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class holding various entity data for HTML and XML - generally for use with
 * the LookupTranslator.
 * All Maps are generated using {@code java.util.Collections.unmodifiableMap()}.
 *
 * @since 1.0
 */
// CHECKSTYLE:OFF
final class EntityArrays {

   /**
     * A Map&lt;CharSequence, CharSequence&gt; to to escape
     * <a href="https://secure.wikimedia.org/wikipedia/en/wiki/ISO/IEC_8859-1">ISO-8859-1</a>
     * characters to their named HTML 3.x equivalents.
     */
    public static final Map<CharSequence, CharSequence> ISO8859_1_ESCAPE;
    static {
        final Map<CharSequence, CharSequence> initialMap = new HashMap<>();
        initialMap.put("\u00A0", "&nbsp;"); // non-breaking space
        initialMap.put("\u00A1", "&iexcl;"); // inverted exclamation mark
        initialMap.put("\u00A2", "&cent;"); // cent sign
        initialMap.put("\u00A3", "&pound;"); // pound sign
        initialMap.put("\u00A4", "&curren;"); // currency sign
        initialMap.put("\u00A5", "&yen;"); // yen sign = yuan sign
        initialMap.put("\u00A6", "&brvbar;"); // broken bar = broken vertical bar
        initialMap.put("\u00A7", "&sect;"); // section sign
        initialMap.put("\u00A8", "&uml;"); // diaeresis = spacing diaeresis
        initialMap.put("\u00A9", "&copy;"); // © - copyright sign
        initialMap.put("\u00AA", "&ordf;"); // feminine ordinal indicator
        initialMap.put("\u00AB", "&laquo;"); // left-pointing double angle quotation mark = left pointing guillemet
        initialMap.put("\u00AC", "&not;"); // not sign
        initialMap.put("\u00AD", "&shy;"); // soft hyphen = discretionary hyphen
        initialMap.put("\u00AE", "&reg;"); // ® - registered trademark sign
        initialMap.put("\u00AF", "&macr;"); // macron = spacing macron = overline = APL overbar
        initialMap.put("\u00B0", "&deg;"); // degree sign
        initialMap.put("\u00B1", "&plusmn;"); // plus-minus sign = plus-or-minus sign
        initialMap.put("\u00B2", "&sup2;"); // superscript two = superscript digit two = squared
        initialMap.put("\u00B3", "&sup3;"); // superscript three = superscript digit three = cubed
        initialMap.put("\u00B4", "&acute;"); // acute accent = spacing acute
        initialMap.put("\u00B5", "&micro;"); // micro sign
        initialMap.put("\u00B6", "&para;"); // pilcrow sign = paragraph sign
        initialMap.put("\u00B7", "&middot;"); // middle dot = Georgian comma = Greek middle dot
        initialMap.put("\u00B8", "&cedil;"); // cedilla = spacing cedilla
        initialMap.put("\u00B9", "&sup1;"); // superscript one = superscript digit one
        initialMap.put("\u00BA", "&ordm;"); // masculine ordinal indicator
        initialMap.put("\u00BB", "&raquo;"); // right-pointing double angle quotation mark = right pointing guillemet
        initialMap.put("\u00BC", "&frac14;"); // vulgar fraction one quarter = fraction one quarter
        initialMap.put("\u00BD", "&frac12;"); // vulgar fraction one half = fraction one half
        initialMap.put("\u00BE", "&frac34;"); // vulgar fraction three quarters = fraction three quarters
        initialMap.put("\u00BF", "&iquest;"); // inverted question mark = turned question mark
        initialMap.put("\u00C0", "&Agrave;"); // À - uppercase A, grave accent
        initialMap.put("\u00C1", "&Aacute;"); // Á - uppercase A, acute accent
        initialMap.put("\u00C2", "&Acirc;"); // Â - uppercase A, circumflex accent
        initialMap.put("\u00C3", "&Atilde;"); // Ã - uppercase A, tilde
        initialMap.put("\u00C4", "&Auml;"); // Ä - uppercase A, umlaut
        initialMap.put("\u00C5", "&Aring;"); // Å - uppercase A, ring
        initialMap.put("\u00C6", "&AElig;"); // Æ - uppercase AE
        initialMap.put("\u00C7", "&Ccedil;"); // Ç - uppercase C, cedilla
        initialMap.put("\u00C8", "&Egrave;"); // È - uppercase E, grave accent
        initialMap.put("\u00C9", "&Eacute;"); // É - uppercase E, acute accent
        initialMap.put("\u00CA", "&Ecirc;"); // Ê - uppercase E, circumflex accent
        initialMap.put("\u00CB", "&Euml;"); // Ë - uppercase E, umlaut
        initialMap.put("\u00CC", "&Igrave;"); // Ì - uppercase I, grave accent
        initialMap.put("\u00CD", "&Iacute;"); // Í - uppercase I, acute accent
        initialMap.put("\u00CE", "&Icirc;"); // Î - uppercase I, circumflex accent
        initialMap.put("\u00CF", "&Iuml;"); // Ï - uppercase I, umlaut
        initialMap.put("\u00D0", "&ETH;"); // Ð - uppercase Eth, Icelandic
        initialMap.put("\u00D1", "&Ntilde;"); // Ñ - uppercase N, tilde
        initialMap.put("\u00D2", "&Ograve;"); // Ò - uppercase O, grave accent
        initialMap.put("\u00D3", "&Oacute;"); // Ó - uppercase O, acute accent
        initialMap.put("\u00D4", "&Ocirc;"); // Ô - uppercase O, circumflex accent
        initialMap.put("\u00D5", "&Otilde;"); // Õ - uppercase O, tilde
        initialMap.put("\u00D6", "&Ouml;"); // Ö - uppercase O, umlaut
        initialMap.put("\u00D7", "&times;"); // multiplication sign
        initialMap.put("\u00D8", "&Oslash;"); // Ø - uppercase O, slash
        initialMap.put("\u00D9", "&Ugrave;"); // Ù - uppercase U, grave accent
        initialMap.put("\u00DA", "&Uacute;"); // Ú - uppercase U, acute accent
        initialMap.put("\u00DB", "&Ucirc;"); // Û - uppercase U, circumflex accent
        initialMap.put("\u00DC", "&Uuml;"); // Ü - uppercase U, umlaut
        initialMap.put("\u00DD", "&Yacute;"); // Ý - uppercase Y, acute accent
        initialMap.put("\u00DE", "&THORN;"); // Þ - uppercase THORN, Icelandic
        initialMap.put("\u00DF", "&szlig;"); // ß - lowercase sharps, German
        initialMap.put("\u00E0", "&agrave;"); // à - lowercase a, grave accent
        initialMap.put("\u00E1", "&aacute;"); // á - lowercase a, acute accent
        initialMap.put("\u00E2", "&acirc;"); // â - lowercase a, circumflex accent
        initialMap.put("\u00E3", "&atilde;"); // ã - lowercase a, tilde
        initialMap.put("\u00E4", "&auml;"); // ä - lowercase a, umlaut
        initialMap.put("\u00E5", "&aring;"); // å - lowercase a, ring
        initialMap.put("\u00E6", "&aelig;"); // æ - lowercase ae
        initialMap.put("\u00E7", "&ccedil;"); // ç - lowercase c, cedilla
        initialMap.put("\u00E8", "&egrave;"); // è - lowercase e, grave accent
        initialMap.put("\u00E9", "&eacute;"); // é - lowercase e, acute accent
        initialMap.put("\u00EA", "&ecirc;"); // ê - lowercase e, circumflex accent
        initialMap.put("\u00EB", "&euml;"); // ë - lowercase e, umlaut
        initialMap.put("\u00EC", "&igrave;"); // ì - lowercase i, grave accent
        initialMap.put("\u00ED", "&iacute;"); // í - lowercase i, acute accent
        initialMap.put("\u00EE", "&icirc;"); // î - lowercase i, circumflex accent
        initialMap.put("\u00EF", "&iuml;"); // ï - lowercase i, umlaut
        initialMap.put("\u00F0", "&eth;"); // ð - lowercase eth, Icelandic
        initialMap.put("\u00F1", "&ntilde;"); // ñ - lowercase n, tilde
        initialMap.put("\u00F2", "&ograve;"); // ò - lowercase o, grave accent
        initialMap.put("\u00F3", "&oacute;"); // ó - lowercase o, acute accent
        initialMap.put("\u00F4", "&ocirc;"); // ô - lowercase o, circumflex accent
        initialMap.put("\u00F5", "&otilde;"); // õ - lowercase o, tilde
        initialMap.put("\u00F6", "&ouml;"); // ö - lowercase o, umlaut
        initialMap.put("\u00F7", "&divide;"); // division sign
        initialMap.put("\u00F8", "&oslash;"); // ø - lowercase o, slash
        initialMap.put("\u00F9", "&ugrave;"); // ù - lowercase u, grave accent
        initialMap.put("\u00FA", "&uacute;"); // ú - lowercase u, acute accent
        initialMap.put("\u00FB", "&ucirc;"); // û - lowercase u, circumflex accent
        initialMap.put("\u00FC", "&uuml;"); // ü - lowercase u, umlaut
        initialMap.put("\u00FD", "&yacute;"); // ý - lowercase y, acute accent
        initialMap.put("\u00FE", "&thorn;"); // þ - lowercase thorn, Icelandic
        initialMap.put("\u00FF", "&yuml;"); // ÿ - lowercase y, umlaut
        ISO8859_1_ESCAPE = Collections.unmodifiableMap(initialMap);
    }

    /**
     * Reverse of {@link #ISO8859_1_ESCAPE} for unescaping purposes.
     */
    public static final Map<CharSequence, CharSequence> ISO8859_1_UNESCAPE;
    static {
        ISO8859_1_UNESCAPE = Collections.unmodifiableMap(invert(ISO8859_1_ESCAPE));
    }

    /**
     * A Map&lt;CharSequence, CharSequence&gt; to escape additional
     * <a href="http://www.w3.org/TR/REC-html40/sgml/entities.html">character entity
     * references</a>. Note that this must be used with {@link #ISO8859_1_ESCAPE} to get the full list of
     * HTML 4.0 character entities.
     */
    public static final Map<CharSequence, CharSequence> HTML40_EXTENDED_ESCAPE;
    static {
        final Map<CharSequence, CharSequence> initialMap = new HashMap<>();
        // <!-- Latin Extended-B -->
        initialMap.put("\u0192", "&fnof;"); // latin small f with hook = function= florin, U+0192 ISOtech -->
        // <!-- Greek -->
        initialMap.put("\u0391", "&Alpha;"); // greek capital letter alpha, U+0391 -->
        initialMap.put("\u0392", "&Beta;"); // greek capital letter beta, U+0392 -->
        initialMap.put("\u0393", "&Gamma;"); // greek capital letter gamma,U+0393 ISOgrk3 -->
        initialMap.put("\u0394", "&Delta;"); // greek capital letter delta,U+0394 ISOgrk3 -->
        initialMap.put("\u0395", "&Epsilon;"); // greek capital letter epsilon, U+0395 -->
        initialMap.put("\u0396", "&Zeta;"); // greek capital letter zeta, U+0396 -->
        initialMap.put("\u0397", "&Eta;"); // greek capital letter eta, U+0397 -->
        initialMap.put("\u0398", "&Theta;"); // greek capital letter theta,U+0398 ISOgrk3 -->
        initialMap.put("\u0399", "&Iota;"); // greek capital letter iota, U+0399 -->
        initialMap.put("\u039A", "&Kappa;"); // greek capital letter kappa, U+039A -->
        initialMap.put("\u039B", "&Lambda;"); // greek capital letter lambda,U+039B ISOgrk3 -->
        initialMap.put("\u039C", "&Mu;"); // greek capital letter mu, U+039C -->
        initialMap.put("\u039D", "&Nu;"); // greek capital letter nu, U+039D -->
        initialMap.put("\u039E", "&Xi;"); // greek capital letter xi, U+039E ISOgrk3 -->
        initialMap.put("\u039F", "&Omicron;"); // greek capital letter omicron, U+039F -->
        initialMap.put("\u03A0", "&Pi;"); // greek capital letter pi, U+03A0 ISOgrk3 -->
        initialMap.put("\u03A1", "&Rho;"); // greek capital letter rho, U+03A1 -->
        // <!-- there is no Sigmaf, and no U+03A2 character either -->
        initialMap.put("\u03A3", "&Sigma;"); // greek capital letter sigma,U+03A3 ISOgrk3 -->
        initialMap.put("\u03A4", "&Tau;"); // greek capital letter tau, U+03A4 -->
        initialMap.put("\u03A5", "&Upsilon;"); // greek capital letter upsilon,U+03A5 ISOgrk3 -->
        initialMap.put("\u03A6", "&Phi;"); // greek capital letter phi,U+03A6 ISOgrk3 -->
        initialMap.put("\u03A7", "&Chi;"); // greek capital letter chi, U+03A7 -->
        initialMap.put("\u03A8", "&Psi;"); // greek capital letter psi,U+03A8 ISOgrk3 -->
        initialMap.put("\u03A9", "&Omega;"); // greek capital letter omega,U+03A9 ISOgrk3 -->
        initialMap.put("\u03B1", "&alpha;"); // greek small letter alpha,U+03B1 ISOgrk3 -->
        initialMap.put("\u03B2", "&beta;"); // greek small letter beta, U+03B2 ISOgrk3 -->
        initialMap.put("\u03B3", "&gamma;"); // greek small letter gamma,U+03B3 ISOgrk3 -->
        initialMap.put("\u03B4", "&delta;"); // greek small letter delta,U+03B4 ISOgrk3 -->
        initialMap.put("\u03B5", "&epsilon;"); // greek small letter epsilon,U+03B5 ISOgrk3 -->
        initialMap.put("\u03B6", "&zeta;"); // greek small letter zeta, U+03B6 ISOgrk3 -->
        initialMap.put("\u03B7", "&eta;"); // greek small letter eta, U+03B7 ISOgrk3 -->
        initialMap.put("\u03B8", "&theta;"); // greek small letter theta,U+03B8 ISOgrk3 -->
        initialMap.put("\u03B9", "&iota;"); // greek small letter iota, U+03B9 ISOgrk3 -->
        initialMap.put("\u03BA", "&kappa;"); // greek small letter kappa,U+03BA ISOgrk3 -->
        initialMap.put("\u03BB", "&lambda;"); // greek small letter lambda,U+03BB ISOgrk3 -->
        initialMap.put("\u03BC", "&mu;"); // greek small letter mu, U+03BC ISOgrk3 -->
        initialMap.put("\u03BD", "&nu;"); // greek small letter nu, U+03BD ISOgrk3 -->
        initialMap.put("\u03BE", "&xi;"); // greek small letter xi, U+03BE ISOgrk3 -->
        initialMap.put("\u03BF", "&omicron;"); // greek small letter omicron, U+03BF NEW -->
        initialMap.put("\u03C0", "&pi;"); // greek small letter pi, U+03C0 ISOgrk3 -->
        initialMap.put("\u03C1", "&rho;"); // greek small letter rho, U+03C1 ISOgrk3 -->
        initialMap.put("\u03C2", "&sigmaf;"); // greek small letter final sigma,U+03C2 ISOgrk3 -->
        initialMap.put("\u03C3", "&sigma;"); // greek small letter sigma,U+03C3 ISOgrk3 -->
        initialMap.put("\u03C4", "&tau;"); // greek small letter tau, U+03C4 ISOgrk3 -->
        initialMap.put("\u03C5", "&upsilon;"); // greek small letter upsilon,U+03C5 ISOgrk3 -->
        initialMap.put("\u03C6", "&phi;"); // greek small letter phi, U+03C6 ISOgrk3 -->
        initialMap.put("\u03C7", "&chi;"); // greek small letter chi, U+03C7 ISOgrk3 -->
        initialMap.put("\u03C8", "&psi;"); // greek small letter psi, U+03C8 ISOgrk3 -->
        initialMap.put("\u03C9", "&omega;"); // greek small letter omega,U+03C9 ISOgrk3 -->
        initialMap.put("\u03D1", "&thetasym;"); // greek small letter theta symbol,U+03D1 NEW -->
        initialMap.put("\u03D2", "&upsih;"); // greek upsilon with hook symbol,U+03D2 NEW -->
        initialMap.put("\u03D6", "&piv;"); // greek pi symbol, U+03D6 ISOgrk3 -->
        // <!-- General Punctuation -->
        initialMap.put("\u2022", "&bull;"); // bullet = black small circle,U+2022 ISOpub -->
        // <!-- bullet is NOT the same as bullet operator, U+2219 -->
        initialMap.put("\u2026", "&hellip;"); // horizontal ellipsis = three dot leader,U+2026 ISOpub -->
        initialMap.put("\u2032", "&prime;"); // prime = minutes = feet, U+2032 ISOtech -->
        initialMap.put("\u2033", "&Prime;"); // double prime = seconds = inches,U+2033 ISOtech -->
        initialMap.put("\u203E", "&oline;"); // overline = spacing overscore,U+203E NEW -->
        initialMap.put("\u2044", "&frasl;"); // fraction slash, U+2044 NEW -->
        // <!-- Letterlike Symbols -->
        initialMap.put("\u2118", "&weierp;"); // script capital P = power set= Weierstrass p, U+2118 ISOamso -->
        initialMap.put("\u2111", "&image;"); // blackletter capital I = imaginary part,U+2111 ISOamso -->
        initialMap.put("\u211C", "&real;"); // blackletter capital R = real part symbol,U+211C ISOamso -->
        initialMap.put("\u2122", "&trade;"); // trade mark sign, U+2122 ISOnum -->
        initialMap.put("\u2135", "&alefsym;"); // alef symbol = first transfinite cardinal,U+2135 NEW -->
        // <!-- alef symbol is NOT the same as hebrew letter alef,U+05D0 although the
        // same glyph could be used to depict both characters -->
        // <!-- Arrows -->
        initialMap.put("\u2190", "&larr;"); // leftwards arrow, U+2190 ISOnum -->
        initialMap.put("\u2191", "&uarr;"); // upwards arrow, U+2191 ISOnum-->
        initialMap.put("\u2192", "&rarr;"); // rightwards arrow, U+2192 ISOnum -->
        initialMap.put("\u2193", "&darr;"); // downwards arrow, U+2193 ISOnum -->
        initialMap.put("\u2194", "&harr;"); // left right arrow, U+2194 ISOamsa -->
        initialMap.put("\u21B5", "&crarr;"); // downwards arrow with corner leftwards= carriage return, U+21B5 NEW -->
        initialMap.put("\u21D0", "&lArr;"); // leftwards double arrow, U+21D0 ISOtech -->
        // <!-- ISO 10646 does not say that lArr is the same as the 'is implied by'
        // arrow but also does not have any other character for that function.
        // So ? lArr canbe used for 'is implied by' as ISOtech suggests -->
        initialMap.put("\u21D1", "&uArr;"); // upwards double arrow, U+21D1 ISOamsa -->
        initialMap.put("\u21D2", "&rArr;"); // rightwards double arrow,U+21D2 ISOtech -->
        // <!-- ISO 10646 does not say this is the 'implies' character but does not
        // have another character with this function so ?rArr can be used for
        // 'implies' as ISOtech suggests -->
        initialMap.put("\u21D3", "&dArr;"); // downwards double arrow, U+21D3 ISOamsa -->
        initialMap.put("\u21D4", "&hArr;"); // left right double arrow,U+21D4 ISOamsa -->
        // <!-- Mathematical Operators -->
        initialMap.put("\u2200", "&forall;"); // for all, U+2200 ISOtech -->
        initialMap.put("\u2202", "&part;"); // partial differential, U+2202 ISOtech -->
        initialMap.put("\u2203", "&exist;"); // there exists, U+2203 ISOtech -->
        initialMap.put("\u2205", "&empty;"); // empty set = null set = diameter,U+2205 ISOamso -->
        initialMap.put("\u2207", "&nabla;"); // nabla = backward difference,U+2207 ISOtech -->
        initialMap.put("\u2208", "&isin;"); // element of, U+2208 ISOtech -->
        initialMap.put("\u2209", "&notin;"); // not an element of, U+2209 ISOtech -->
        initialMap.put("\u220B", "&ni;"); // contains as member, U+220B ISOtech -->
        // <!-- should there be a more memorable name than 'ni'? -->
        initialMap.put("\u220F", "&prod;"); // n-ary product = product sign,U+220F ISOamsb -->
        // <!-- prod is NOT the same character as U+03A0 'greek capital letter pi'
        // though the same glyph might be used for both -->
        initialMap.put("\u2211", "&sum;"); // n-ary summation, U+2211 ISOamsb -->
        // <!-- sum is NOT the same character as U+03A3 'greek capital letter sigma'
        // though the same glyph might be used for both -->
        initialMap.put("\u2212", "&minus;"); // minus sign, U+2212 ISOtech -->
        initialMap.put("\u2217", "&lowast;"); // asterisk operator, U+2217 ISOtech -->
        initialMap.put("\u221A", "&radic;"); // square root = radical sign,U+221A ISOtech -->
        initialMap.put("\u221D", "&prop;"); // proportional to, U+221D ISOtech -->
        initialMap.put("\u221E", "&infin;"); // infinity, U+221E ISOtech -->
        initialMap.put("\u2220", "&ang;"); // angle, U+2220 ISOamso -->
        initialMap.put("\u2227", "&and;"); // logical and = wedge, U+2227 ISOtech -->
        initialMap.put("\u2228", "&or;"); // logical or = vee, U+2228 ISOtech -->
        initialMap.put("\u2229", "&cap;"); // intersection = cap, U+2229 ISOtech -->
        initialMap.put("\u222A", "&cup;"); // union = cup, U+222A ISOtech -->
        initialMap.put("\u222B", "&int;"); // integral, U+222B ISOtech -->
        initialMap.put("\u2234", "&there4;"); // therefore, U+2234 ISOtech -->
        initialMap.put("\u223C", "&sim;"); // tilde operator = varies with = similar to,U+223C ISOtech -->
        // <!-- tilde operator is NOT the same character as the tilde, U+007E,although
        // the same glyph might be used to represent both -->
        initialMap.put("\u2245", "&cong;"); // approximately equal to, U+2245 ISOtech -->
        initialMap.put("\u2248", "&asymp;"); // almost equal to = asymptotic to,U+2248 ISOamsr -->
        initialMap.put("\u2260", "&ne;"); // not equal to, U+2260 ISOtech -->
        initialMap.put("\u2261", "&equiv;"); // identical to, U+2261 ISOtech -->
        initialMap.put("\u2264", "&le;"); // less-than or equal to, U+2264 ISOtech -->
        initialMap.put("\u2265", "&ge;"); // greater-than or equal to,U+2265 ISOtech -->
        initialMap.put("\u2282", "&sub;"); // subset of, U+2282 ISOtech -->
        initialMap.put("\u2283", "&sup;"); // superset of, U+2283 ISOtech -->
        // <!-- note that nsup, 'not a superset of, U+2283' is not covered by the
        // Symbol font encoding and is not included. Should it be, for symmetry?
        // It is in ISOamsn -->,
        initialMap.put("\u2284", "&nsub;"); // not a subset of, U+2284 ISOamsn -->
        initialMap.put("\u2286", "&sube;"); // subset of or equal to, U+2286 ISOtech -->
        initialMap.put("\u2287", "&supe;"); // superset of or equal to,U+2287 ISOtech -->
        initialMap.put("\u2295", "&oplus;"); // circled plus = direct sum,U+2295 ISOamsb -->
        initialMap.put("\u2297", "&otimes;"); // circled times = vector product,U+2297 ISOamsb -->
        initialMap.put("\u22A5", "&perp;"); // up tack = orthogonal to = perpendicular,U+22A5 ISOtech -->
        initialMap.put("\u22C5", "&sdot;"); // dot operator, U+22C5 ISOamsb -->
        // <!-- dot operator is NOT the same character as U+00B7 middle dot -->
        // <!-- Miscellaneous Technical -->
        initialMap.put("\u2308", "&lceil;"); // left ceiling = apl upstile,U+2308 ISOamsc -->
        initialMap.put("\u2309", "&rceil;"); // right ceiling, U+2309 ISOamsc -->
        initialMap.put("\u230A", "&lfloor;"); // left floor = apl downstile,U+230A ISOamsc -->
        initialMap.put("\u230B", "&rfloor;"); // right floor, U+230B ISOamsc -->
        initialMap.put("\u2329", "&lang;"); // left-pointing angle bracket = bra,U+2329 ISOtech -->
        // <!-- lang is NOT the same character as U+003C 'less than' or U+2039 'single left-pointing angle quotation
        // mark' -->
        initialMap.put("\u232A", "&rang;"); // right-pointing angle bracket = ket,U+232A ISOtech -->
        // <!-- rang is NOT the same character as U+003E 'greater than' or U+203A
        // 'single right-pointing angle quotation mark' -->
        // <!-- Geometric Shapes -->
        initialMap.put("\u25CA", "&loz;"); // lozenge, U+25CA ISOpub -->
        // <!-- Miscellaneous Symbols -->
        initialMap.put("\u2660", "&spades;"); // black spade suit, U+2660 ISOpub -->
        // <!-- black here seems to mean filled as opposed to hollow -->
        initialMap.put("\u2663", "&clubs;"); // black club suit = shamrock,U+2663 ISOpub -->
        initialMap.put("\u2665", "&hearts;"); // black heart suit = valentine,U+2665 ISOpub -->
        initialMap.put("\u2666", "&diams;"); // black diamond suit, U+2666 ISOpub -->

        // <!-- Latin Extended-A -->
        initialMap.put("\u0152", "&OElig;"); // -- latin capital ligature OE,U+0152 ISOlat2 -->
        initialMap.put("\u0153", "&oelig;"); // -- latin small ligature oe, U+0153 ISOlat2 -->
        // <!-- ligature is a misnomer, this is a separate character in some languages -->
        initialMap.put("\u0160", "&Scaron;"); // -- latin capital letter S with caron,U+0160 ISOlat2 -->
        initialMap.put("\u0161", "&scaron;"); // -- latin small letter s with caron,U+0161 ISOlat2 -->
        initialMap.put("\u0178", "&Yuml;"); // -- latin capital letter Y with diaeresis,U+0178 ISOlat2 -->
        // <!-- Spacing Modifier Letters -->
        initialMap.put("\u02C6", "&circ;"); // -- modifier letter circumflex accent,U+02C6 ISOpub -->
        initialMap.put("\u02DC", "&tilde;"); // small tilde, U+02DC ISOdia -->
        // <!-- General Punctuation -->
        initialMap.put("\u2002", "&ensp;"); // en space, U+2002 ISOpub -->
        initialMap.put("\u2003", "&emsp;"); // em space, U+2003 ISOpub -->
        initialMap.put("\u2009", "&thinsp;"); // thin space, U+2009 ISOpub -->
        initialMap.put("\u200C", "&zwnj;"); // zero width non-joiner,U+200C NEW RFC 2070 -->
        initialMap.put("\u200D", "&zwj;"); // zero width joiner, U+200D NEW RFC 2070 -->
        initialMap.put("\u200E", "&lrm;"); // left-to-right mark, U+200E NEW RFC 2070 -->
        initialMap.put("\u200F", "&rlm;"); // right-to-left mark, U+200F NEW RFC 2070 -->
        initialMap.put("\u2013", "&ndash;"); // en dash, U+2013 ISOpub -->
        initialMap.put("\u2014", "&mdash;"); // em dash, U+2014 ISOpub -->
        initialMap.put("\u2018", "&lsquo;"); // left single quotation mark,U+2018 ISOnum -->
        initialMap.put("\u2019", "&rsquo;"); // right single quotation mark,U+2019 ISOnum -->
        initialMap.put("\u201A", "&sbquo;"); // single low-9 quotation mark, U+201A NEW -->
        initialMap.put("\u201C", "&ldquo;"); // left double quotation mark,U+201C ISOnum -->
        initialMap.put("\u201D", "&rdquo;"); // right double quotation mark,U+201D ISOnum -->
        initialMap.put("\u201E", "&bdquo;"); // double low-9 quotation mark, U+201E NEW -->
        initialMap.put("\u2020", "&dagger;"); // dagger, U+2020 ISOpub -->
        initialMap.put("\u2021", "&Dagger;"); // double dagger, U+2021 ISOpub -->
        initialMap.put("\u2030", "&permil;"); // per mille sign, U+2030 ISOtech -->
        initialMap.put("\u2039", "&lsaquo;"); // single left-pointing angle quotation mark,U+2039 ISO proposed -->
        // <!-- lsaquo is proposed but not yet ISO standardized -->
        initialMap.put("\u203A", "&rsaquo;"); // single right-pointing angle quotation mark,U+203A ISO proposed -->
        // <!-- rsaquo is proposed but not yet ISO standardized -->
        initialMap.put("\u20AC", "&euro;"); // -- euro sign, U+20AC NEW -->
        HTML40_EXTENDED_ESCAPE = Collections.unmodifiableMap(initialMap);
    }

    /**
     * Reverse of {@link #HTML40_EXTENDED_ESCAPE} for unescaping purposes.
     */
    public static final Map<CharSequence, CharSequence> HTML40_EXTENDED_UNESCAPE;
    static {
        HTML40_EXTENDED_UNESCAPE = Collections.unmodifiableMap(invert(HTML40_EXTENDED_ESCAPE));
    }

    /**
     * A Map&lt;CharSequence, CharSequence&gt; to escape the basic XML and HTML
     * character entities.
     *
     * Namely: {@code " & < >}
     */
    public static final Map<CharSequence, CharSequence> BASIC_ESCAPE;
    static {
        final Map<CharSequence, CharSequence> initialMap = new HashMap<>();
        initialMap.put("\"", "&quot;"); // " - double-quote
        initialMap.put("&", "&amp;");   // & - ampersand
        initialMap.put("<", "&lt;");    // < - less-than
        initialMap.put(">", "&gt;");    // > - greater-than
        BASIC_ESCAPE = Collections.unmodifiableMap(initialMap);
    }

    /**
     * Reverse of {@link #BASIC_ESCAPE} for unescaping purposes.
     */
    public static final Map<CharSequence, CharSequence> BASIC_UNESCAPE;
    static {
        BASIC_UNESCAPE = Collections.unmodifiableMap(invert(BASIC_ESCAPE));
    }

    /**
     * A Map&lt;CharSequence, CharSequence&gt; to escape the apostrophe character to
     * its XML character entity.
     */
    public static final Map<CharSequence, CharSequence> APOS_ESCAPE;
    static {
        final Map<CharSequence, CharSequence> initialMap = new HashMap<>();
        initialMap.put("'", "&apos;"); // XML apostrophe
        APOS_ESCAPE = Collections.unmodifiableMap(initialMap);
    }

    /**
     * Reverse of {@link #APOS_ESCAPE} for unescaping purposes.
     */
    public static final Map<CharSequence, CharSequence> APOS_UNESCAPE;
    static {
        APOS_UNESCAPE = Collections.unmodifiableMap(invert(APOS_ESCAPE));
    }

    /**
     * A Map&lt;CharSequence, CharSequence&gt; to escape the Java
     * control characters.
     *
     * Namely: {@code \b \n \t \f \r}
     */
    public static final Map<CharSequence, CharSequence> JAVA_CTRL_CHARS_ESCAPE;
    static {
        final Map<CharSequence, CharSequence> initialMap = new HashMap<>();
        initialMap.put("\b", "\\b");
        initialMap.put("\n", "\\n");
        initialMap.put("\t", "\\t");
        initialMap.put("\f", "\\f");
        initialMap.put("\r", "\\r");
        JAVA_CTRL_CHARS_ESCAPE = Collections.unmodifiableMap(initialMap);
    }

    /**
     * Reverse of {@link #JAVA_CTRL_CHARS_ESCAPE} for unescaping purposes.
     */
    public static final Map<CharSequence, CharSequence> JAVA_CTRL_CHARS_UNESCAPE;
    static {
        JAVA_CTRL_CHARS_UNESCAPE = Collections.unmodifiableMap(invert(JAVA_CTRL_CHARS_ESCAPE));
    }

    private EntityArrays() {
        // complete
    }

    /**
     * Used to invert an escape Map into an unescape Map.
     * @param map Map&lt;String, String&gt; to be inverted
     * @return Map&lt;String, String&gt; inverted array
     */
    public static Map<CharSequence, CharSequence> invert(final Map<CharSequence, CharSequence> map) {
        final Map<CharSequence, CharSequence> newMap = new HashMap<>();
        for (final Map.Entry<CharSequence, CharSequence> pair : map.entrySet()) {
            newMap.put(pair.getValue(), pair.getKey());
        }
        return newMap;
    }

}
