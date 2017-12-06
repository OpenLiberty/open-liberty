package com.ibm.ws.microprofile.openapi.impl.model.media;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.media.Discriminator;

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
        return "Discriminator{" +
               "propertyName='" + propertyName + '\'' +
               ", mapping=" + mapping +
               '}';
    }
}
