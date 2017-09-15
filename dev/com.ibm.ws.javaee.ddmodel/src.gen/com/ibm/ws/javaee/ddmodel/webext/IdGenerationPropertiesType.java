/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.webext;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class IdGenerationPropertiesType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.webext.IdGenerationProperties {
    com.ibm.ws.javaee.ddmodel.BooleanType use_uri;
    com.ibm.ws.javaee.ddmodel.StringType alternate_name;
    com.ibm.ws.javaee.ddmodel.BooleanType use_path_infos;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.CacheVariableType, com.ibm.ws.javaee.dd.webext.CacheVariable> cache_variable;

    @Override
    public boolean isSetUseURI() {
        return use_uri != null;
    }

    @Override
    public boolean isUseURI() {
        return use_uri != null ? use_uri.getBooleanValue() : false;
    }

    @Override
    public boolean isSetAlternateName() {
        return alternate_name != null;
    }

    @Override
    public java.lang.String getAlternateName() {
        return alternate_name != null ? alternate_name.getValue() : null;
    }

    @Override
    public boolean isSetUsePathInfos() {
        return use_path_infos != null;
    }

    @Override
    public boolean isUsePathInfos() {
        return use_path_infos != null ? use_path_infos.getBooleanValue() : false;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.webext.CacheVariable> getCacheVariables() {
        if (cache_variable != null) {
            return cache_variable.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if ("use-uri".equals(localName)) {
                this.use_uri = parser.parseBooleanAttributeValue(index);
                return true;
            }
            if ("alternate-name".equals(localName)) {
                this.alternate_name = parser.parseStringAttributeValue(index);
                return true;
            }
            if ("use-path-infos".equals(localName)) {
                this.use_path_infos = parser.parseBooleanAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if ("cache-variable".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.webext.CacheVariableType cache_variable = new com.ibm.ws.javaee.ddmodel.webext.CacheVariableType();
            parser.parse(cache_variable);
            this.addCacheVariable(cache_variable);
            return true;
        }
        return false;
    }

    void addCacheVariable(com.ibm.ws.javaee.ddmodel.webext.CacheVariableType cache_variable) {
        if (this.cache_variable == null) {
            this.cache_variable = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.CacheVariableType, com.ibm.ws.javaee.dd.webext.CacheVariable>();
        }
        this.cache_variable.add(cache_variable);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet("use-uri", use_uri);
        diag.describeIfSet("alternate-name", alternate_name);
        diag.describeIfSet("use-path-infos", use_path_infos);
        diag.describeIfSet("cache-variable", cache_variable);
    }
}
