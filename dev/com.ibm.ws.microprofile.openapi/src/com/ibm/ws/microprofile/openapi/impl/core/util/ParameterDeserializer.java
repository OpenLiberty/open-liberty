package com.ibm.ws.microprofile.openapi.impl.core.util;

import java.io.IOException;

import org.eclipse.microprofile.openapi.models.parameters.CookieParameter;
import org.eclipse.microprofile.openapi.models.parameters.HeaderParameter;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.PathParameter;
import org.eclipse.microprofile.openapi.models.parameters.QueryParameter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.ibm.ws.microprofile.openapi.impl.model.parameters.ParameterImpl;

public class ParameterDeserializer extends JsonDeserializer<Parameter> {
    @Override
    public Parameter deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        Parameter result = null;

        JsonNode node = jp.getCodec().readTree(jp);
        JsonNode sub = node.get("$ref");
        JsonNode inNode = node.get("in");

        if (sub != null) {
            result = new ParameterImpl().ref(sub.asText());
        } else if (inNode != null) {
            String in = inNode.asText();

            ObjectReader reader = null;

            if ("query".equals(in)) {
                reader = Json.mapper().readerFor(QueryParameter.class);
            } else if ("header".equals(in)) {
                reader = Json.mapper().readerFor(HeaderParameter.class);
            } else if ("path".equals(in)) {
                reader = Json.mapper().readerFor(PathParameter.class);
            } else if ("cookie".equals(in)) {
                reader = Json.mapper().readerFor(CookieParameter.class);
            }
            if (reader != null) {
                result = reader.with(DeserializationFeature.READ_ENUMS_USING_TO_STRING).readValue(node);
            }
        }

        return result;
    }
}
