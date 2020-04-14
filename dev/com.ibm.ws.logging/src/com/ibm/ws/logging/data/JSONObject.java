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
package com.ibm.ws.logging.data;

/**
 * Class for immutable JSONObjects
 */
public class JSONObject {

    private StringBuilder jsonBuilder = new StringBuilder();

    private JSONObject(JSONObjectBuilder builder) {
        this.jsonBuilder = builder.jsonBuilder;
    }

    /**
     * Return String value of JSONObject StringBuilder
     */
    @Override
    public String toString() {
        return jsonBuilder.toString();
    }

    /**
     * Class to build JSONObjects
     */
    public static class JSONObjectBuilder {

        private StringBuilder jsonBuilder;
        private final String OMIT_FIELDS_STRING = "@@@OMIT@@@";
        private boolean hasAnyFields = false;

        public JSONObjectBuilder() {
            jsonBuilder = new StringBuilder();
        }

        /**
         * Return JSONObject built with surrounding curly braces
         */
        public JSONObject build() {
            jsonBuilder = new StringBuilder().append("{").append(jsonBuilder.toString()).append("}");
            return new JSONObject(this);
        }

        /**
         * Return String representation of current JSONObjectBuilder
         */
        @Override
        public String toString() {
            return jsonBuilder.toString();
        }

        /**
         * Add formatted JSON, primarily used for adding multiple fields
         */
        public JSONObjectBuilder addPreformatted(String s) {
            if (s.isEmpty()) {
                return this;
            }
            prepForNewField();

            jsonBuilder.append(s);
            return this;
        }

        /**
         * Add preformatted field that is omittable, primarily used for adding "ibm_tags" and "tags" fields
         */
        public JSONObjectBuilder addPreformattedField(String name, String preformattedValue) {
            if (name.isEmpty() || preformattedValue.isEmpty())
                return this;

            if (name.equals(OMIT_FIELDS_STRING))
                return this;

            prepForNewField();

            jsonBuilder.append("\"" + name + "\":" + preformattedValue);
            return this;
        }

        /**
         * Add String field value
         */
        public JSONObjectBuilder addField(String name, String value, boolean jsonEscapeName, boolean jsonEscapeValue) {
            appendNameValue(name, value, jsonEscapeName, jsonEscapeValue, false);
            return this;
        }

        /**
         * Add Integer field value
         */
        public JSONObjectBuilder addField(String name, int value, boolean jsonEscapeName) {
            appendNameValue(name, Integer.toString(value), jsonEscapeName, false, true);
            return this;
        }

        /**
         * Add Boolean field value
         */
        public JSONObjectBuilder addField(String name, boolean value, boolean jsonEscapeName) {
            appendNameValue(name, Boolean.toString(value), jsonEscapeName, false, true);
            return this;
        }

        /**
         * Add Long field value
         */
        public JSONObjectBuilder addField(String name, long value, boolean jsonEscapeName) {
            appendNameValue(name, Long.toString(value), jsonEscapeName, false, true);
            return this;
        }

        /**
         * Add Float field value
         */
        public JSONObjectBuilder addField(String name, float value, boolean jsonEscapeName) {
            appendNameValue(name, Float.toString(value), jsonEscapeName, false, true);
            return this;
        }

        /**
         * Prepares JSONObjectBuilder StringBuilder for new fields by appending a comma
         */
        private void prepForNewField() {
            if (hasAnyFields) {
                jsonBuilder.append(",");
                hasAnyFields = true;
            } else {
                hasAnyFields = true;
            }
        }

        /**
         * Appends name value pairs based on whether the fields are to be omitted, JSON escaped, and surrounded by quotes
         *
         * @param name
         *            field name
         * @param value
         *            field value
         * @param jsonEscapeName
         *            if name needs to be JSON escaped
         * @param jsonEscapevalue
         *            if value needs to be JSON escaped
         * @param isQuoteless
         *            if value needs to be surrounded by quotes
         */
        private void appendNameValue(String name, String value, boolean jsonEscapeName, boolean jsonEscapeValue, boolean isQuoteless) {

            if (name == null || value == null)
                return;

            // if field to be omitted
            if (name.equals(OMIT_FIELDS_STRING))
                return;

            prepForNewField();

            // append name
            jsonBuilder.append("\"");

            if (jsonEscapeName)
                jsonEscape3(name);
            else
                jsonBuilder.append(name);

            //append value
            if (isQuoteless) {
                jsonBuilder.append("\":" + value);
            } else {
                jsonBuilder.append("\":\"");

                if (jsonEscapeValue)
                    jsonEscape3(value);
                else
                    jsonBuilder.append(value);

                jsonBuilder.append("\"");
            }

        }

        /**
         * Escape \b, \f, \n, \r, \t, ", \, / characters, appends to JSONObjectBuilder StringBuilder jsonBuilder
         *
         * @param s
         *            String to escape
         */
        private void jsonEscape3(String s) {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '\b':
                        jsonBuilder.append("\\b");
                        break;
                    case '\f':
                        jsonBuilder.append("\\f");
                        break;
                    case '\n':
                        jsonBuilder.append("\\n");
                        break;
                    case '\r':
                        jsonBuilder.append("\\r");
                        break;
                    case '\t':
                        jsonBuilder.append("\\t");
                        break;

                    // Fall through because we just need to add \ (escaped) before the character
                    case '\\':
                    case '\"':
                    case '/':
                        jsonBuilder.append("\\");
                        jsonBuilder.append(c);
                        break;
                    default:
                        jsonBuilder.append(c);
                }
            }
        }
    }
}