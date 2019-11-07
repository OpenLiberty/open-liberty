/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.featureverifier.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 *
 */
public class NewManifestProcessor {

    /**
     * This method parses through Manifest files and creates internal representations of
     * all of the attributes listed within.
     * 
     * @param file A file to be parsed (we use a file so we know where it lives)
     * @return A Manifest representing the file parsed
     */
    public static Manifest parseManifest(File file) {

        // This is the manifest that will eventually be returned
        Manifest returnManifest = new Manifest();

        try {
            InputStream input = new FileInputStream(file);
            // Wrap the input stream with a reader
            BufferedReader lineReader = new BufferedReader(new InputStreamReader(input));

            /*
             * This is the master loop that contains all the flow logic. Basically the pattern is
             * this:
             * 1. Read a new line
             * 2. Skip empty lines
             * 3. Determine if the line is part of a multi-line statement
             * 4. Begin building the total string
             * 5. Loop over all the lines ending with \, building the multi-line statement
             * 6. If the line starts with "-include" process all the included files
             * 7. If the line starts with "-", split on :, remove the - and add to the manifest.
             * 8. If the line includes ":" split on that and save the attribute
             * 9. If the line includes "=" split on that and save the attribute
             */
            while (lineReader.ready()) {
                // Read the next line
                String currentLine = lineReader.readLine();

                // Ignore blank lines
                if (currentLine.isEmpty())
                    continue;

                // Determine if this is a multi-line statement
                boolean isMultiLine = (currentLine.endsWith("\\")) ? true : false;

                // Add the first line to the StringBuilder, removing any \'s or whitespace from the ends
                StringBuilder fullLine = new StringBuilder(customTrim(currentLine, '\\'));

                /*
                 * Multi-line elements are allowed. In this style of manifest we know that every multi-line
                 * element ends with a \ character. This allows us to read-ahead intelligently when the \
                 * is seen at the end of the line.
                 */
                while (isMultiLine && lineReader.ready()) {
                    // Read the next line
                    currentLine = lineReader.readLine();

                    // Determine if there is yet another line to read
                    isMultiLine = (currentLine.endsWith("\\")) ? true : false;

                    // Add this line to the StringBuilder
                    fullLine.append(customTrim(currentLine, '\\'));
                }

                // Now the full attribute is known. Save it in a new string
                String fullAttribute = fullLine.toString();

                /*
                 * We need to determine which separator is being used. The basic logic is:
                 * Find the index of both the first : and the first =
                 * If the fullAttribute didn't either character indexOf() returns -1, set it to MAX_VALUE so we can do a comparison
                 * Check which separator we saw first in the string and set a boolean to remember that
                 */
                int colonIndex = fullAttribute.indexOf(':');
                int equalsIndex = fullAttribute.indexOf('=');
                if (-1 == colonIndex)
                    colonIndex = Integer.MAX_VALUE;
                if (-1 == equalsIndex)
                    equalsIndex = Integer.MAX_VALUE;
                boolean isColonSeparator = (colonIndex < equalsIndex) ? true : false;
                boolean isEqualsSeparator = (equalsIndex < colonIndex) ? true : false;

                /*
                 * With the full attribute in a single string it can now be parsed and saved into the Manifest.
                 * The three scenarios currently are: attributes that start with -, attributes that use : as the split,
                 * and attributes that use = as the split. The one special case is the -include case, as each of the files
                 * listed needs to be processed again and all the attributes added to this Manifest.
                 */
                if (fullAttribute.startsWith("-include=") || fullAttribute.startsWith("-include =")) {
                    /*
                     * The basic idea here is to use recursion to parse through the list of
                     * manifest files that need to be included. This returns a Manifest object
                     * from which the attributes can be taken and added to the returnManifest.
                     */

                    // Split the line on =
                    String[] splitAttribute = fullAttribute.split("=", 2);

                    // Loop over every file in the list
                    for (String includedManifest : splitAttribute[1].trim().split(",")) {
                        // Strip out the ~ at the beginning of the path
                        String includedManifestNoTilde = includedManifest.substring(1);
                        File includedFile = new File(file.getParent(), includedManifestNoTilde.trim());
                        if (file.exists()) {
                            // Parse the manifest
                            Manifest temporaryManifest = NewManifestProcessor.parseManifest(includedFile);

                            // For each attribute in the temporaryManifest, add it to the returnManifest
                            for (Entry<Object, Object> temporaryAttributes : temporaryManifest.getMainAttributes().entrySet()) {
                                returnManifest.getMainAttributes().put(temporaryAttributes.getKey(), temporaryAttributes.getValue());
                            }
                        } else
                        {
                            System.out.println("Could not process " + includedManifest + " because the file could not be found.");
                        }
                    }
                } else if (isColonSeparator && !isEqualsSeparator) {
                    // Split the line on the :
                    String[] splitAttribute = fullAttribute.split(":", 2);

                    // Remove the potential prefixed -
                    if (splitAttribute[0].startsWith("-"))
                        splitAttribute[0] = splitAttribute[0].substring(1);

                    // Modify any potential .'s, they're not allowed in Attribute names
                    if (-1 != splitAttribute[0].indexOf('.'))
                        splitAttribute[0] = splitAttribute[0].replace('.', '_');

                    // Add the attribute to the manifest
                    returnManifest.getMainAttributes().put(new Attributes.Name(splitAttribute[0].trim()), splitAttribute[1].trim());
                } else if (isEqualsSeparator && !isColonSeparator) {
                    // Split the line on the =
                    String[] splitAttribute = fullAttribute.split("=", 2);

                    // Remove the potential prefixed -
                    if (splitAttribute[0].startsWith("-"))
                        splitAttribute[0] = splitAttribute[0].substring(1);

                    // Modify any potential .'s, they're not allowed in Attribute names
                    if (-1 != splitAttribute[0].indexOf('.'))
                        splitAttribute[0] = splitAttribute[0].replace('.', '_');

                    // Add the attribute to the manifest
                    returnManifest.getMainAttributes().put(new Attributes.Name(splitAttribute[0].trim()), splitAttribute[1].trim());
                }
            }

            // Close the BufferedReader
            lineReader.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e.printStackTrace();
        }

        // Return the final version of the manifest
        return returnManifest;
    }

    /**
     * This is a method to modify the Manifests that are being returned.
     * It replaces all instances of variables with the form $(var_name) with the
     * value of the attribute that has the name "var_name"
     * 
     * @param returnManifest
     */
    public static void replaceVariables(Manifest returnManifest) {
        // Get all the attributes
        Attributes mainAttributes = returnManifest.getMainAttributes();

        // Loop through all of the attributes
        for (Object attribute : mainAttributes.keySet()) {
            // Pull out the value
            String attributeValue = (String) mainAttributes.get(attribute);

            // As long as there is still a variable in the value, remove it
            while (attributeValue.contains("${")) {
                // Pull out the start and end of the variable, '$' and '}'
                int start = attributeValue.indexOf('$');
                int end = attributeValue.indexOf('}');

                // Save the variable name
                String variableName = attributeValue.substring(start + 2, end);

                // In the Attributes all .'s are _'s. Make the same change now to help lookup
                variableName = variableName.replace('.', '_');

                // Get the variables value
                String variableValue = mainAttributes.getValue(variableName);;

                // If we have a variable value, replace the attribute value
                if (null != variableValue) {
                    String newAttributeValue;
                    String firstPart = attributeValue.substring(0, start);

                    if (attributeValue.length() == (end + 1)) {
                        // There is no more attributeValue past the variable
                        newAttributeValue = firstPart + variableValue;
                        attributeValue = newAttributeValue;
                    } else {
                        // The variable is in the middle of the string, save the end of the attributeValue
                        String lastPart = attributeValue.substring(end + 1, attributeValue.length());
                        newAttributeValue = firstPart + variableValue + lastPart;
                        attributeValue = newAttributeValue;
                    }

                    // Done! Put the new attribute value back into the main attributes
                    mainAttributes.put(attribute, attributeValue);
                } else {
                    break;
                }
            }
        }
    }

    /**
     * A utility method used to list out all of the attributes stored in a Manifest
     * 
     * @param inputManifest The Manifest object to parse
     * @return A Map containing all of the attributes
     */
    public static Map<String, String> readManifestIntoMap(Manifest inputManifest) {
        Map<String, String> returnMap = new HashMap<String, String>();

        Attributes mainAttributes = inputManifest.getMainAttributes();

        for (Entry<Object, Object> individualAttribute : mainAttributes.entrySet()) {
            returnMap.put(individualAttribute.getKey().toString(), individualAttribute.getValue().toString());
        }

        return returnMap;
    }

    /*
     * This is a helper method to do two trims at once. The first thing done is a
     * check to see if "character" appears at the end of the input. If it does it's removed.
     * Then a normal trim() is done and the result returned.
     */
    private static String customTrim(String input, char character) {
        int lastIndex = input.length() - 1;
        if (input.charAt(lastIndex) == character) {
            input = input.substring(0, lastIndex);
        }

        return input.trim();
    }
}
