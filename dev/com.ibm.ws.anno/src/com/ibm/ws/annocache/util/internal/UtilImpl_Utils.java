/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.util.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.ibm.wsspi.annocache.util.Util_InternMap;

public class UtilImpl_Utils {
    private static final String CLASS_NAME = "UtilImpl_Utils";

    //

    public static long getSystemTime() {
        return System.nanoTime();
    }

    //

    public static String getSystemProperty(final String propertyName) {
        return AccessController.doPrivileged( new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(propertyName);
                }
            }
        );
    }

    public static String getSystemProperty(Logger logger,
                                           String propertyName, String defaultValue) {
        String methodName = "getSystemProperty";

        String propertyValue = getSystemProperty(propertyName);

        String reason;
        String result;
        if ( propertyValue == null ) {
            reason = "Defaulted";
            result = defaultValue;
        } else {
            reason = "Read From Property";
            result = propertyValue;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Property [ {0} ] Read [ {1} ] Returned [ {2} ] ({3})",
                new Object[] { propertyName, propertyValue, result, reason });
        }
        return result;
    }

    public static boolean getSystemProperty(Logger logger,
                                            String propertyName, boolean defaultValue) {
        String methodName = "getSystemProperty";

        String propertyValue = getSystemProperty(propertyName);

        String reason;
        boolean result;
        if ( propertyValue == null ) {
            reason = "Defaulted";
            result = defaultValue;
        } else {
            reason = "Read From Property";
            result = Boolean.parseBoolean(propertyValue);
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Property [ {0} ] Read [ {1} ] Returned [ {2} ] ({3})",
                new Object[] { propertyName, propertyValue, Boolean.valueOf(result), reason });
        }
        return result;
    }

    public static int getSystemProperty(Logger logger,
                                        String propertyName, int defaultValue) {
        String methodName = "getSystemProperty";

        String propertyValue = getSystemProperty(propertyName);

        String reason;
        int result;
        if ( propertyValue == null ) {
            reason = "Defaulted";
            result = defaultValue;
        } else {
            reason = "Read From Property";
            result = Integer.parseInt(propertyValue);
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Property [ {0} ] Read [ {1} ] Returned [ {2} ] ({3})",
                new Object[] { propertyName, propertyValue, Integer.valueOf(result), reason });
        }
        return result;
    }
    
    //

    public static Thread createThread(final Runnable runnable, final String threadName) {
        return AccessController.doPrivileged( new PrivilegedAction<Thread>() {
            @Override
            public Thread run() {
                return new Thread(runnable, threadName);
            }
        } );
    }

    //

    public static Set<String> restrict(Set<String> candidates, Set<String> allowed) {
        if ( (candidates == null) || (allowed == null) ||
             candidates.isEmpty() || allowed.isEmpty() ) {
            return Collections.emptySet();
        }

        Set<String> restricted = new HashSet<String>();

        for ( String candidate : candidates ) {
            if ( allowed.contains(candidate) ) {
                restricted.add(candidate);
            }
        }

        return restricted;
    }

    //

    // Filtering is supported for archives, packages, classes, and annotations.
    //
    // The meaning of the filtering options is to restrict the results of annotation scans to
    // selected values, according to the particular specified options.  The options are usually
    // referred to as "filtering" options instead of as "selection" options because the default
    // for scanning is to select all available values.  Then, the options are only ever used
    // used to restrict, or "filter" scanned values.
    //
    // Filtering provides two values, a regular expression used to select values, and a regular
    // expression used to omit values.
    //
    // Regular expression processing is according to the current java level.  See:
    //   java 6: http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html
    //   java 7: http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
    //   java 8: http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
    //
    // For filtering, package and classes are processed using the same filter expressions.
    // Archives and annotations use different filter expressions, resulting in three overall
    // sets of filter expressions.
    //
    // Selection and omission expressions are specified in four combinations: Select All
    // Omit None, Select Per Regular Expression / Omit None, Select All / Omit Per Regular
    // Expression, or Select Per Regular Expression / Omit Per Regular Expression.
    //
    // In all cases, the two regular expressions are used to select values according to the rule:
    //
    // select(aValue) if-and-only-if
    //     matches(selectionPattern, aValue) and
    //     !matches(omissionPattern, aValue)
    //
    // Using this rule, the "Select All / Omit None" combination evaluates to true for
    // all values.  The "Select Per Regular Expression / Omit None" combination evaluates
    // according to "matches(selectionPattern, aValue)", with the test against the omission
    // pattern making no contribution to the result.  Similarly, the "Select All / Omit Per
    // Regular Expression" evaluates according to "!matches(omissionPattern, aValue)", with
    // the selection pattern making no contribution to the result.  For the fourth combination
    // "Select Per Regular Expression / Omit Per Regular Expression", a value is selected if
    // and only if it is both selected and not omitted.
    //
    // Filter settings for an archive apply to the packages and classes of the archive.  Selection
    // of an archive selects the packages and classes of the archive.  Omission of an archive omits
    // the packages and classes of the archive.
    //
    // Filter settings for a package or class cause the package or class to be completely ignored
    // when the package or class is encountered.  This has the effect of omitting the package or
    // class from the listing of scanned classes, has the effect of omitting the package or class
    // from class inheritance tables, and has the effect of causing all annotations on the package
    // or class to be ignored.
    //
    // Note that when a package or class occurs in more than one location in a module class path,
    // the omission of a package or class from one location of the module class may unmask the
    // class in another location of the class path.  Unmasking occurs when the first archive which
    // contains the class is filtered, while a second archive which also contains the class is
    // not filtered.
    //
    // Filter settings for an annotation cause all occurrences of that annotation to be ignored.
    //
    // All filter settings operate independently.  This is significant mostly for annotation
    // filtering, with the result that the annotation filtering settings cannot be set to filter
    // a particular annotation on a particular class.  Either, the entire class must be omitted,
    // or all occurrences of an annotation must be omitted.
    //
    // "Archive" filtering is a slight misnomer: Archive filtering applies to directory type
    // containers (for example, "WEB-INF/classes") as well as to child archives.
    //
    // Archive filtering typically uses "." (as a separator for file extensions) and "/".
    // "." has a special meaning for regular expressions, and must be escaped when used as a
    // literal search character for archive filtering.  "/" does not have a special meaning
    // and need not be escaped.  (All values provided to archive filtering are converted to
    // use forward slashes ("/").  Windows style slash characters are not used.)
    //
    // For example, to filter "WEB-INF/lib/aLibJar.jar", use the filter expression
    // "WEB-INF/lib/aLibJar\.jar".
    //
    // For matching package and class names, including matching of annotation class names,
    // matching applies to the entire qualified name of the package or class.  The qualified
    // name does not include a ".class" tail.
    //
    // "." and "$" are usual characters in class names.  Since these have special meanings
    // in regular expressions, they must be escaped when intended as literal match characters.
    // For example, to match "myPackage.myClass$myInnerClass", an expression with two escapes
    // must be used: "myPackage\.myClass\$myInnerClass".
    //
    // Filter options are specified at up to three increasingly finer levels: As a process
    // global setting, as an application global setting, as a module global setting.  For
    // filter expressions, the effective expression is the union of the expressions for the
    // several scopes.
    //
    // For example, when filtering classes within a web module, starting with these expressions:
    //
    // global: omitClasses="package1\.*|package2\.internal\.*"
    // application: omitClasses="package3\.*"
    // module1: omitClasses="package\.\m1impl\.\*"
    // module2: omitClasses="package\.\m2impl\.\*"
    //
    // For module 1, the effective expression for the module is:
    //
    // module1: omitClasses="package1\.*|package2\.internal\.*|package3\.*|package\.\m1impl\.\*"
    //
    // And for module 2, the effective expression is:
    //
    // module2: omitClasses="package1\.*|package2\.internal\.*|package3\.*|package\.\m2impl\.\*"
    //
    // Alternate expression format:
    //
    // As an alternative, the following might also be used as the format for filter expressions:
    //
    // Escaping:
    //   "\" Escape character.
    //
    // Wildcards:
    //   "*" Zero or more characters.
    //   "?" Exactly one character.
    //   "+" One or more characters.
    //
    // Delimiters:
    //   "," Delimiter
    //
    // White space:
    //   WHITESPACE Ignored as leading and trailing text.  Otherwise, used as literal characters.
    //
    // Literal characters:
    //   OTHER Other characters taken as literal characters.
    //
    // This better fits the intended usage, which is to select values which are relative
    // path values and java qualified name values.
    //
    // This also is backwards compatible with the expressions used for TWAS filtering.
    //
    // The rules for combining expressions from different scopes apply without
    // modification.  With this expression format, expressions are parsed into a list
    // of sub-expressions; expressions from different scopes are combined by appending
    // the lists of sub-expressions.

    public static final String SELECT_ALL_PATTERN = "*";
    public static final String SELECT_NONE_PATTERN = "";

    public static boolean selectsAll(String regEx) {
        return ( (regEx != null) && regEx.equals(SELECT_ALL_PATTERN) );
    }

    public static boolean selectsNone(String regEx) {
        return ( (regEx != null) && regEx.equals(SELECT_NONE_PATTERN) );
    }

    public static Pattern compile(String regEx) {
        return Pattern.compile(regEx);
    }

    //

    public static Set<String> unintern(Util_InternMap internMap, Set<String> internSet) {
        if ( (internSet == null) || internSet.isEmpty() ) {
            return Collections.emptySet();
        } else {
            return new UtilImpl_NonInternSet(internMap, internSet);
        }
    }

    public static Set<String> createNonInternSet(Util_InternMap internMap, Set<String> internSet) {
        return new UtilImpl_NonInternSet(internMap, internSet);
    }
    
    //
    
    public static Map<String, String> duplicateIdentityMap(Map<String, String> map) {
        Map<String, String> duplicateMap = new IdentityHashMap<String, String>();
        duplicateMap.putAll(map);
        return duplicateMap;
    }

    //

    public static final String SIMPLE_DATE_FORMAT_TEXT = "EEE, dd-MMM-yyyy HH:mm:ss Z";
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(SIMPLE_DATE_FORMAT_TEXT);

    public static String getDateAndTime() {
        Date currentDate = new Date();

        // Per: https://stackoverflow.com/questions/18383251/strange-arrayindexoutofboundsexception-for-java-simpledateformat
        // SimpleDateFormat is not thread safe.

        synchronized ( SIMPLE_DATE_FORMAT ) {
            return SIMPLE_DATE_FORMAT.format(currentDate);
        }
    }
}
