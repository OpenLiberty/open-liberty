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

package org.apache.cxf.common.jaxb;

import java.util.Map;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;


public final class NamespaceMapper extends NamespacePrefixMapper {
    private static final String[] EMPTY_STRING = new String[0];

    private final Map<String, String> nspref;
    private String[] nsctxt = EMPTY_STRING;

    public NamespaceMapper(Map<String, String> nspref) {
        this.nspref = nspref;
    }

    public String getPreferredPrefix(String namespaceUri,
                                     String suggestion,
                                     boolean requirePrefix) {
        String prefix = nspref.get(namespaceUri);
        if (prefix != null) {
            return prefix;
        }
        return suggestion;
    }

    public void setContextualNamespace(String[] contextualNamespaceDecls) {
        this.nsctxt = contextualNamespaceDecls;
    }

    public String[] getContextualNamespaceDecls() {
        return nsctxt;
    }


}