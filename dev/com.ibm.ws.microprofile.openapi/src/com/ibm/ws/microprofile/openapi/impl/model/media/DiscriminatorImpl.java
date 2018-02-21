package com.ibm.ws.microprofile.openapi.impl.model.media;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.media.Discriminator;

import com.ibm.ws.microprofile.openapi.Constants;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

public class DiscriminatorImpl implements Discriminator {
    private String propertyName;
    private Map<String, String> mapping;

    @Override
    public Discriminator propertyName(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public Discriminator addMapping(String name, String value) {
        if (this.mapping == null) {
            this.mapping = new HashMap<>();
        }
        this.mapping.put(name, value);
        return this;
    }

    @Override
    public Discriminator mapping(Map<String, String> mapping) {
        this.mapping = mapping;
        return this;
    }

    @Override
    public Map<String, String> getMapping() {
        return mapping;
    }

    @Override
    public void setMapping(Map<String, String> mapping) {
        this.mapping = mapping;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DiscriminatorImpl)) {
            return false;
        }

        DiscriminatorImpl that = (DiscriminatorImpl) o;

        if (propertyName != null ? !propertyName.equals(that.propertyName) : that.propertyName != null) {
            return false;
        }
        return mapping != null ? mapping.equals(that.mapping) : that.mapping == null;

    }

    @Override
    public int hashCode() {
        int result = propertyName != null ? propertyName.hashCode() : 0;
        result = 31 * result + (mapping != null ? mapping.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Discriminator {\n");
        sb = (!toIndentedString(propertyName).equals(Constants.NULL_VALUE)) ? sb.append("    propertyName: ").append(toIndentedString(propertyName)).append("\n") : sb.append("");
        sb = (!toIndentedString(mapping).equals(Constants.NULL_VALUE)) ? sb.append("    mapping: ").append(OpenAPIUtils.mapToString(mapping)).append("\n") : sb.append("");

        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
