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
package componenttest.rules.repeater;


import java.io.FileInputStream;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Assists with renaming FFDC exceptions within @AllowedFFDC
 * and @ExpectedFFDC.
 *
 * replacePackages, containsWildcard, and stripWildcard methods
 * were pulled from https://github.com/tbitonti/jakartaee-prototype
 * -- org.eclipse.transformer.action.impl.SignatureRuleImpl
 * 
 * @param  text           String embedding zero, one, or more package names.
 * @param  packageRenames map of names and replacement values
 * @return                The text with all embedded package names replaced. Original text if no
 *                        replacements were performed.
 */
public class EE9PackageReplacementHelper {

    private Map<String, String> packageRenameRules;

    private static final Logger Log = Logger.getLogger(EE9PackageReplacementHelper.class.getName());

    public EE9PackageReplacementHelper(String rulesPath) {
        Properties appProps = new Properties();
        try {
            appProps.load(new FileInputStream(rulesPath));
            packageRenameRules = (Map) appProps;
        } catch (Exception e) {
            Log.warning("Error occured when reading in " + rulesPath);
            e.printStackTrace();
        }
    }

    public EE9PackageReplacementHelper() {
            this(System.getProperty("user.dir") + "/autoFVT-templates/" + "jakarta-renames.properties");
    }
    
    public String replacePackages(String text) {
        return replacePackages(text, this.packageRenameRules);
    }

    /**
     * Replace all embedded packages of specified text with replacement
     * packages.
     *
     * @param  text           String embedding zero, one, or more package names.
     * @param  packageRenames map of names and replacement values
     * @return                The text with all embedded package names replaced. Null if no
     *                        replacements were performed.
     */
    private String replacePackages(String text, Map<String, String> packageRenames) {

        Log.info("Initial text [ " + text + " ]");

		String initialText = text;

		for (Map.Entry<String, String> renameEntry : packageRenames.entrySet() ) {
			String key = renameEntry.getKey();
			int keyLen = key.length();
			
            boolean matchSubpackages = containsWildcard(key);
            if (matchSubpackages) {
                key = stripWildcard(key);
            }

            //Log.info("Next target [ " + key + " ]");

            int textLimit = text.length() - keyLen;

            int lastMatchEnd = 0;
            while (lastMatchEnd <= textLimit) {
                int matchStart = text.indexOf(key, lastMatchEnd);
                if (matchStart == -1) {
                    break;
                }

                if (!isTruePackageMatch(text, matchStart, keyLen, matchSubpackages)) {
                    lastMatchEnd = matchStart + keyLen;
                    continue;
                }

                String value = renameEntry.getValue();
                int valueLen = value.length();

                String head = text.substring(0, matchStart);
                String tail = text.substring(matchStart + keyLen);
                text = head + value + tail;

                lastMatchEnd = matchStart + valueLen;
                textLimit += (valueLen - keyLen);

                //Log.info("Next text [ " + text + " ]");
            }
        }

        if (initialText == text) {
            Log.info("RETURN Final text is unchanged ");
            return initialText;
        } else {
            Log.info("RETURN Final text [ " + text + " ]");
            return text;
        }
    }

    /**
     * Checks the character before and after a match to verify that the match
     * is NOT a subset of a larger package, and thus not really a match.
     */
    public boolean isTruePackageMatch(String text, int matchStart, int keyLen, boolean matchSubpackages) {

        //       Log.info("isTruePackageMatch:" 
        //                           + " text[" + text + "]"
        //                           + " key[" + text.substring(matchStart, matchStart + keyLen) + "]"
        //                           + " tail[" + text.substring(matchStart + keyLen)
        //                           + " *************");

        int textLength = text.length();

        if (matchStart > 0) {
            char charBeforeMatch = text.charAt(matchStart - 1);
            if (Character.isJavaIdentifierPart(charBeforeMatch) || (charBeforeMatch == '.')) {
                return false;
            }
        }

        int matchEnd = matchStart + keyLen;
        if (textLength > matchEnd) {

            char charAfterMatch = text.charAt(matchEnd);

            // Check the next character can also be part of a package name then 
            // we are looking at a larger package name, and thus not a match.
            if (Character.isJavaIdentifierPart(charAfterMatch)) {
                return false;
            }

            // If the next char is dot, check the character after the dot.  Assume an upper case letter indicates the start of a 
            // class name and thus the end of the package name which indicates a match. ( This means this doesn't work 
            // for package names that do not follow the convention of using lower case characters ).            
            // If lower case, then it indicates we are looking at a larger package name, and thus not a match.
            // If the character after the dot is a number, also assume the number is a continuation of the package name.
            if (!matchSubpackages) {
                if (charAfterMatch == '.') {
                    if (textLength > (matchEnd + 1)) {
                        char charAfterDot = text.charAt(matchEnd + 1);
                        if (Character.isLowerCase(charAfterDot) || Character.isDigit(charAfterDot)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Determines if the key contains a wildcard suffix which indicates
     * that sub-package names are to be matched.
     * 
     * Packages names and their replacements are specified in properties files
     * in key=value pairs or more specifically oldPackageName=newPackageName
     * 
     * The key can contain a ".*" suffix which indicates that sub-packages are a
     * match.
     * 
     * @param  key package name
     * @return     true if sub-packages are to be matched
     */
    public boolean containsWildcard(String key) {

        if (key.endsWith(".*")) {
            return true;
        }
        return false;
    }

    public String stripWildcard(String key) {
        if (key.endsWith(".*")) {
            key = key.substring(0, key.length() - 2 );
        }
        return key;
    }
}