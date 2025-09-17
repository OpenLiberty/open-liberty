/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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
package com.ibm.ws.logging.data;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.logging.data.JSONObject.JSONObjectBuilder;

public class AccessLogDataFormatter {

    // list of actions to populate JSONObjectBuilder
    private ArrayList<JsonFieldAdder> jsonFieldAdders = new ArrayList<JsonFieldAdder>();
    private ArrayList<ListFieldAdder> ListFieldAdders = new ArrayList<ListFieldAdder>();

    public static class AccessLogDataFormatterBuilder {
        private final ArrayList<JsonFieldAdder> jsonFieldAdders = new ArrayList<JsonFieldAdder>();
        private final ArrayList<ListFieldAdder> ListFieldAdders = new ArrayList<ListFieldAdder>();

        public AccessLogDataFormatterBuilder() {
        }

        public AccessLogDataFormatterBuilder add(JsonFieldAdder jsonFieldAdder) {
            this.jsonFieldAdders.add(jsonFieldAdder);
            return this;
        }

        public AccessLogDataFormatterBuilder add(ListFieldAdder ListFieldAdder) {
            this.ListFieldAdders.add(ListFieldAdder);
            return this;
        }

        public AccessLogDataFormatter build() {
            AccessLogDataFormatter formatter = new AccessLogDataFormatter(this);
            return formatter;
        }
    }

    private AccessLogDataFormatter(AccessLogDataFormatterBuilder builder) {
        this.jsonFieldAdders = builder.jsonFieldAdders;
        this.ListFieldAdders = builder.ListFieldAdders;
    }

    // adds fields to JSONObjectBuilder by running all jsonFieldAdders
    public JSONObjectBuilder populate(JSONObjectBuilder jsonObject, AccessLogData a) {
        for (JsonFieldAdder jfa : jsonFieldAdders) {
            jfa.populate(jsonObject, a);
        }
        return jsonObject;
    }

    public List<KeyValuePair> populate(List<KeyValuePair> kvpList, AccessLogData a) {
        for (ListFieldAdder lfa : ListFieldAdders) {
            lfa.populate(kvpList, a);
        }
        return kvpList;
    }
}
