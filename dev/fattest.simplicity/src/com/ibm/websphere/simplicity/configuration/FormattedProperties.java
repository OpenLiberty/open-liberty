/*******************************************************************************
 * Copyright (c) 2011, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.simplicity.configuration;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * This class extends the <code>java.util.Properties</code> Object overwriting the store method to
 * store the properties in a format suitable for the project
 */
public class FormattedProperties extends Properties implements Comparator<String> {

    private static final long serialVersionUID = 6615492615448326924L;

    Map<String, String> userProps;
    Map<String, String> generatedProps;

    /**
     * No argument constructor
     */
    public FormattedProperties() {
        super();
    }

    /**
     * Properties Constructor
     *
     * @param defaults The default properties to initialize the Object
     */
    public FormattedProperties(Properties defaults) {
        super(defaults);
    }

    /**
     * Write the properties to the specified <code>OutputStream</code>. User specified properties
     * (those which do not contain {@link BootStrappingProperty#DATA}) are written first followed
     * by generated data.
     *
     * @param out      The <code>OutputStream</code> to write to
     * @param comments Comments to write to the stream
     * @see            java.util.Properties
     */
    @Override
    public void store(OutputStream out, String comments) throws IOException {
        // write comments
        if (comments != null) {
            out.write('#');
            out.write(comments.getBytes());
            out.write(System.getProperty("line.separator").getBytes());
        }
        // write date
        out.write('#');
        out.write(new Date().toString().getBytes());
        out.write(System.getProperty("line.separator").getBytes());

        userProps = new HashMap<String, String>();
        generatedProps = new HashMap<String, String>();

        Set<Object> props = super.keySet();
        String property = null;
        for (Object prop : props) {
            property = (String) prop;
            if (getProperty(property) != null) {
                if (property.indexOf("data") == -1) {
                    userProps.put(property, getProperty(property));
                } else {
                    generatedProps.put(property, getProperty(property));
                }
            }
        }

        String[] userPropsArray = userProps.keySet().toArray(new String[0]);
        Arrays.sort(userPropsArray, this);
        String[] generatedPropsArray = generatedProps.keySet().toArray(new String[0]);
        Arrays.sort(generatedPropsArray, this);

        // first write the user properties
        String next;
        out.write(("# User Data" + System.getProperty("line.separator")).getBytes());
        for (int i = 0; i < userPropsArray.length; ++i) {
            next = userPropsArray[i];
            write(out, next, true);
            out.write('=');
            write(out, userProps.get(next), false);
            out.write(System.getProperty("line.separator").getBytes());
        }

        // write a couple of newlines
        out.write(System.getProperty("line.separator").getBytes());
        out.write(System.getProperty("line.separator").getBytes());

        // next write the generated data
        out.write(("# Generated Data" + System.getProperty("line.separator")).getBytes());
        for (int i = 0; i < generatedPropsArray.length; ++i) {
            next = generatedPropsArray[i];
            write(out, next, true);
            out.write('=');
            write(out, generatedProps.get(next), false);
            out.write(System.getProperty("line.separator").getBytes());
        }
    }

    /**
     * Write a value to the <code>OutputStream</code>
     *
     * @param  out         The <code>OutputStream</code> to write to
     * @param  next        The next value to write
     * @param  isKey       true if the next value is a property; false if it is a property value
     * @throws IOException
     */
    private void write(OutputStream out, String next, boolean isKey) throws IOException {
        for (int j = 0; j < next.length(); ++j) {
            char ch = next.charAt(j);
            byte[] bytes;
            switch (ch) {
                case '\\':
                    bytes = "\\\\".getBytes();
                    break;
                case '\t':
                    bytes = "\\t".getBytes();
                    break;
                case '\f':
                    bytes = "\\f".getBytes();
                    break;
                case '\n':
                    bytes = "\\n".getBytes();
                    break;
                case '\r':
                    bytes = "\\r".getBytes();
                    break;
                case '#':
                case '!':
                case '=':
                case ':':
                case ' ':
                    bytes = ("\\" + ch).getBytes();
                    break;
                default:
                    bytes = ("" + ch).getBytes();
                    break;
            }
            out.write(bytes);
        }
    }

    /**
     * Compare two Strings
     *
     * @param object1 The first String to compare
     * @param object2 The second String to compare
     */
    @Override
    public int compare(String object1, String object2) {
        if (object1.equals(object2)) {
            return 0;
        }
        if (object1.length() > object2.length()) {
            for (int i = 0; i < object2.length(); ++i) {
                if (object1.charAt(i) < object2.charAt(i)) {
                    return -1;
                } else if (object1.charAt(i) > object2.charAt(i)) {
                    return 1;
                }
            }
        } else {
            for (int i = 0; i < object1.length(); ++i) {
                if (object1.charAt(i) < object2.charAt(i)) {
                    return -1;
                } else if (object1.charAt(i) > object2.charAt(i)) {
                    return 1;
                }
            }
        }
        return 0;
    }

}
