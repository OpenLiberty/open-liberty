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

import java.util.ArrayList;

import com.ibm.ws.logging.data.JSONObject.JSONObjectBuilder;

public class AccessLogDataFormatter {

    // list of actions to populate JSONObjectBuilder
    private ArrayList<JsonFieldAdder> jsonFieldAdders = new ArrayList<JsonFieldAdder>();

    public static class AccessLogDataFormatterBuilder {
        private final ArrayList<JsonFieldAdder> jsonFieldAdders = new ArrayList<JsonFieldAdder>();

        public AccessLogDataFormatterBuilder() {
        }

        public AccessLogDataFormatterBuilder add(JsonFieldAdder jsonFieldAdder) {
            this.jsonFieldAdders.add(jsonFieldAdder);
            return this;
        }

        public AccessLogDataFormatter build() {
            AccessLogDataFormatter formatter = new AccessLogDataFormatter(this);
            return formatter;
        }
    }

    private AccessLogDataFormatter(AccessLogDataFormatterBuilder builder) {
        this.jsonFieldAdders = builder.jsonFieldAdders;
    }

    // adds fields to JSONObjectBuilder by running all jsonFieldAdders
    public JSONObjectBuilder populate(JSONObjectBuilder jsonObject, AccessLogData a) {
        for (JsonFieldAdder jfa : jsonFieldAdders) {
            jfa.populate(jsonObject, a);
        }
        return jsonObject;
    }

}
