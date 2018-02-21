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
package com.ibm.ws.microprofile.openapi.impl.model.media;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;

import com.ibm.ws.microprofile.openapi.Constants;

/**
 * MediaType
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#mediaTypeObject"
 */

public class MediaTypeImpl implements MediaType {
    private Schema schema = null;
    private Map<String, Example> examples = null;
    private Object example = null;
    private Map<String, Encoding> encoding = null;
    private java.util.Map<String, Object> extensions = null;

    @Override
    public Schema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    @Override
    public MediaType schema(Schema schema) {
        this.schema = schema;
        return this;
    }

    @Override
    public Map<String, Example> getExamples() {
        return examples;
    }

    @Override
    public void setExamples(Map<String, Example> examples) {
        this.examples = examples;
    }

    @Override
    public MediaType examples(Map<String, Example> examples) {
        this.examples = examples;
        return this;
    }

    @Override
    public MediaType addExample(String key, Example example) {
        if (this.examples == null) {
            this.examples = new HashMap<String, Example>();
        }
        this.examples.put(key, example);
        return this;
    }

    @Override
    public Object getExample() {
        return example;
    }

    @Override
    public void setExample(Object example) {
        this.example = example;
    }

    @Override
    public MediaType example(Object example) {
        this.example = example;
        return this;
    }

    @Override
    public Map<String, Encoding> getEncoding() {
        return encoding;
    }

    @Override
    public void setEncoding(Map<String, Encoding> encoding) {
        this.encoding = encoding;
    }

    @Override
    public MediaType encoding(Map<String, Encoding> encoding) {
        this.encoding = encoding;
        return this;
    }

    @Override
    public MediaType addEncoding(String key, Encoding encodingItem) {
        if (this.encoding == null) {
            this.encoding = new HashMap<String, Encoding>();
        }
        this.encoding.put(key, encodingItem);
        return this;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MediaTypeImpl mediaType = (MediaTypeImpl) o;
        return Objects.equals(this.schema, mediaType.schema) &&
               Objects.equals(this.examples, mediaType.examples) &&
               Objects.equals(this.example, mediaType.example) &&
               Objects.equals(this.encoding, mediaType.encoding) &&
               Objects.equals(this.extensions, mediaType.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, examples, example, encoding, extensions);
    }

    @Override
    public java.util.Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    public void addExtension(String name, Object value) {
        if (this.extensions == null) {
            this.extensions = new java.util.HashMap<>();
        }
        this.extensions.put(name, value);
    }

    @Override
    public void setExtensions(java.util.Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MediaType {\n");
        sb = (!toIndentedString(schema).equals(Constants.NULL_VALUE)) ? sb.append("    schema: ").append(toIndentedString(schema)).append("\n") : sb.append("");
        sb = (!toIndentedString(examples).equals(Constants.NULL_VALUE)) ? sb.append("    examples: ").append(toIndentedString(examples)).append("\n") : sb.append("");
        sb = (!toIndentedString(example).equals(Constants.NULL_VALUE)) ? sb.append("    example: ").append(toIndentedString(example)).append("\n") : sb.append("");
        sb = (!toIndentedString(encoding).equals(Constants.NULL_VALUE)) ? sb.append("    encoding: ").append(toIndentedString(encoding)).append("\n") : sb.append("");

        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     * This method adds formatting to the general toString() method.
     *
     * @param o Java object to be represented as String
     * @return Formatted String representation of the object
     */

    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

}
