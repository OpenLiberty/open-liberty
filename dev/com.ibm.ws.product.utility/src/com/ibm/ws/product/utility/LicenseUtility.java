/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.ibm.wsspi.kernel.service.utils.FileUtils;

/**
 *
 */
public class LicenseUtility {

    private static final int LINE_WRAP_COLUMNS = 72;

    /**
     * Display the license file. If an error occurs reading or writing the
     * license file, exit with a message
     * 
     * @param licenseFile
     *            The license file to display
     */
    public Object displayLicenseFile(InputStream licenseFile, CommandConsole commandConsole) {
        Object e = LicenseUtility.class;
        if (licenseFile != null) {
            e = showLicenseFile(licenseFile, commandConsole);
        }
        if (e != null) {
            commandConsole.printErrorMessage(CommandUtils.getMessage("LICENSE_NOT_FOUND"));
        }
        return e;
    }

    /**
     * Gets the license information and agreement from wlp/lafiles directory depending on Locale
     * 
     * @param licenseFile location of directory containing the licenses
     * @param prefix LA/LI prefix
     * @return licensefile
     * @throws FileNotFoundException
     */
    public final File getLicenseFile(File installLicenseDir, String prefix) throws FileNotFoundException {
        if (!!!prefix.endsWith("_")) {
            prefix = prefix + "_";
        }

        Locale locale = Locale.getDefault();
        String lang = locale.getLanguage();
        String country = locale.getCountry();

        String[] suffixes = new String[] { lang + '_' + country, lang, "en" };
        File[] listOfFiles = installLicenseDir.listFiles();

        File licenseFile = null;

        outer: for (File file : listOfFiles) {
            for (String suffix : suffixes) {
                if (file.getName().startsWith(prefix) && file.getName().endsWith(suffix)) {
                    licenseFile = new File(file.getAbsolutePath());
                    break outer;
                }
            }
        }

        return licenseFile;
    }

    /**
     * @param licenseFile License file to display: paginate/word wrap
     */
    protected Exception showLicenseFile(InputStream in, CommandConsole commandConsole) {
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in, "UTF-16"));
            for (String line; (line = reader.readLine()) != null;) {
                wordWrap(line, lines);
            }

            for (int i = 0; i < lines.size(); i++) {
                commandConsole.printlnInfoMessage(lines.get(i));
            }
            commandConsole.printlnInfoMessage("");
            return null;
        } catch (Exception e) {
            return e;
        } finally {
            FileUtils.tryToClose(reader);
        }
    }

    static List<String> wordWrap(String line, List<String> lines) {
        if (lines == null)
            lines = new ArrayList<String>();

        // If lines is empty, add empty string and return;
        if (line.length() == 0) {
            lines.add("");
            return lines;
        }

        // Split a more complicated line... 
        for (int begin = 0; begin < line.length();) {
            // ??? Java has no wcwidth (Unicode TR#11), so we assume
            // all code points have a console width of 1.
            // ??? This code assumes all characters are BMP.

            // Does the rest of the string fit in a single line?
            if (begin + LINE_WRAP_COLUMNS >= line.length()) {
                lines.add(line.substring(begin));
                break;
            }

            // Choose a split point.
            int tryEnd = Math.min(line.length(), begin + LINE_WRAP_COLUMNS);

            // If we're in the middle of a word, find the beginning.
            int end = tryEnd;
            while (end > begin && !Character.isWhitespace(line.charAt(end - 1))) {
                end--;
            }

            // Skip preceding whitespace.
            while (end > begin && Character.isWhitespace(line.charAt(end - 1))) {
                end--;
            }

            // If we couldn't find a preceding split point, then this
            // is a really long word (e.g., a URL).  Find the end of
            // the word and add it without splitting.
            if (end == begin) {
                end = tryEnd;
                while (end < line.length() && !Character.isWhitespace(line.charAt(end))) {
                    end++;
                }
            }

            lines.add(line.substring(begin, end));

            // Skip whitespace and find the beginning of the next word.
            begin = end;
            while (begin < line.length() && Character.isWhitespace(line.charAt(begin))) {
                begin++;
            }
        }

        return lines;
    }

}
