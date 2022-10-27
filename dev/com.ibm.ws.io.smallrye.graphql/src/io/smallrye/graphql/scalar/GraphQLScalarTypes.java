// https://github.com/smallrye/smallrye-graphql/blob/4a47a2da6e4f4b4a6aedf2fc6464510737fe4c52/server/implementation/src/main/java/io/smallrye/graphql/scalar/GraphQLScalarTypes.java
// Apache v2.0 licensed - https://github.com/smallrye/smallrye-graphql/blob/1.0.9/LICENSE
package io.smallrye.graphql.scalar;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import graphql.Scalars;
import graphql.schema.GraphQLScalarType;
import io.smallrye.graphql.scalar.number.BigDecimalScalar;
import io.smallrye.graphql.scalar.number.BigIntegerScalar;
import io.smallrye.graphql.scalar.number.FloatScalar;
import io.smallrye.graphql.scalar.number.IntegerScalar;
import io.smallrye.graphql.scalar.time.DateScalar;
import io.smallrye.graphql.scalar.time.DateTimeScalar;
import io.smallrye.graphql.scalar.time.DurationScalar;
import io.smallrye.graphql.scalar.time.PeriodScalar;
import io.smallrye.graphql.scalar.time.TimeScalar;

/**
 * Here we keep all the graphql-java scalars
 * mapped by classname
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public class GraphQLScalarTypes {

    private GraphQLScalarTypes() {
    }

    public static Map<String, GraphQLScalarType> getScalarMap() {
        return SCALAR_MAP;
    }

    public static GraphQLScalarType getScalarByName(String name) {
        return SCALARS_BY_NAME.get(name);
    }

    public static GraphQLScalarType getScalarByClassName(String className) {
        return SCALAR_MAP.get(className);
    }

    public static boolean isGraphQLScalarType(String className) {
        return SCALAR_MAP.containsKey(className);
    }

    // Scalar map we can just create now.
    private static final Map<String, GraphQLScalarType> SCALAR_MAP = new HashMap<>();

    /**
     * Maps scalar-name to scalar-type.
     */
    private static final Map<String, GraphQLScalarType> SCALARS_BY_NAME = new HashMap<>();
    private static final String ID = "ID";

    static {
        SCALAR_MAP.put(ID, Scalars.GraphQLID);

        SCALAR_MAP.put(Boolean.class.getName(), Scalars.GraphQLBoolean);
        SCALAR_MAP.put(boolean.class.getName(), Scalars.GraphQLBoolean);

        SCALAR_MAP.put(char.class.getName(), Scalars.GraphQLString);
        SCALAR_MAP.put(Character.class.getName(), Scalars.GraphQLString);

        SCALAR_MAP.put(String.class.getName(), Scalars.GraphQLString);
        SCALAR_MAP.put(UUID.class.getName(), Scalars.GraphQLString);
        SCALAR_MAP.put(URL.class.getName(), Scalars.GraphQLString);
        SCALAR_MAP.put(URI.class.getName(), Scalars.GraphQLString);

        mapType(new IntegerScalar()); // AtomicInteger, OptionalInt, Integer, int, Short, short, Byte, byte
        mapType(new FloatScalar()); // OptionalDouble, Float, float, Double, double
        mapType(new BigIntegerScalar()); // AtomicLong, OptionalLong, BigInteger, Long, long
        mapType(new BigDecimalScalar()); // BigDecimal
        mapType(new DateScalar()); // LocalDate, java.sql.Date
        mapType(new TimeScalar()); // LocalTime, java.sql.Time, OffsetTime
        mapType(new DateTimeScalar()); // LocalDateTime, Date, java.sql.Timestamp, ZonedDateTime, OffsetDateTime

        mapType(new PeriodScalar());
        mapType(new DurationScalar());

        for (final GraphQLScalarType value : SCALAR_MAP.values()) {
            SCALARS_BY_NAME.put(value.getName(), value);
        }
    }

    private static void mapType(AbstractScalar abstractScalar) {
        for (Class c : abstractScalar.getSupportedClasses()) {
            SCALAR_MAP.put(c.getName(), abstractScalar.getScalarType());
        }
    }
}