package io.leangen.graphql.metadata.strategy.value;

import java.lang.reflect.Type;

public class InputParsingException extends IllegalArgumentException {

    public InputParsingException(Object input, Type targetType, Throwable cause) {
        super("Value: " + input + " could not be parsed into an instance of " + targetType.getTypeName(), cause);
    }
}
