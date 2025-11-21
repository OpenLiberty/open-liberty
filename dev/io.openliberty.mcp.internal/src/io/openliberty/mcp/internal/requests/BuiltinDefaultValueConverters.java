package io.openliberty.mcp.internal.requests;

public class BuiltinDefaultValueConverters {
    public static class BooleanConverter implements DefaultValueConverter<Boolean> {

        @Override
        public Boolean convert(String defaultValue) {
            return Boolean.parseBoolean(defaultValue);
        }

    }

    public static class ByteConverter implements DefaultValueConverter<Byte> {

        @Override
        public Byte convert(String defaultValue) {
            return Byte.parseByte(defaultValue);
        }

    }

    public static class ShortConverter implements DefaultValueConverter<Short> {

        @Override
        public Short convert(String defaultValue) {
            return Short.parseShort(defaultValue);
        }

    }

    public static class IntegerConverter implements DefaultValueConverter<Integer> {

        @Override
        public Integer convert(String defaultValue) {
            return Integer.parseInt(defaultValue);
        }

    }

    public static class LongConverter implements DefaultValueConverter<Long> {

        @Override
        public Long convert(String defaultValue) {
            return Long.parseLong(defaultValue);
        }

    }

    public static class FloatConverter implements DefaultValueConverter<Float> {

        @Override
        public Float convert(String defaultValue) {
            return Float.parseFloat(defaultValue);
        }

    }

    public static class DoubleConverter implements DefaultValueConverter<Double> {

        @Override
        public Double convert(String defaultValue) {
            return Double.parseDouble(defaultValue);
        }

    }

    public static class CharacterConverter implements DefaultValueConverter<Character> {

        @Override
        public Character convert(String defaultValue) {
            if (defaultValue.length() == 1) {
                return defaultValue.charAt(0);
            }
            throw new IllegalArgumentException("Not a char: " + defaultValue);
        }

    }

}
