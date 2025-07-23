package io.openliberty.jpa.data.tests.models;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ZipCodeConverter implements AttributeConverter<ZipCode, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ZipCode zipCode) {
        return zipCode != null ? zipCode.getValue() : null;
    }

    @Override
    public ZipCode convertToEntityAttribute(Integer dbData) {
        return dbData != null ? ZipCode.of(dbData) : null;
    }
}
