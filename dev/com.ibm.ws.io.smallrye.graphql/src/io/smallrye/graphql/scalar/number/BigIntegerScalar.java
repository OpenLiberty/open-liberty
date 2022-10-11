// https://github.com/smallrye/smallrye-graphql/blob/4a47a2da6e4f4b4a6aedf2fc6464510737fe4c52/server/implementation/src/main/java/io/smallrye/graphql/scalar/number/BigIntegerScalar.java
// Apache v2.0 licensed - https://github.com/smallrye/smallrye-graphql/blob/1.0.9/LICENSE
package io.smallrye.graphql.scalar.number;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Scalar for BigInteger.
 * Based on graphql-java's Scalars.GraphQLBigInteger
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public class BigIntegerScalar extends AbstractNumberScalar {

    public BigIntegerScalar() {

        super("BigInteger",
                new Converter() {
                    @Override
                    public Object fromBigDecimal(BigDecimal bigDecimal) {
                        return bigDecimal.toBigIntegerExact();
                    }

                    @Override
                    public Object fromBigInteger(BigInteger bigInteger) {
                        return bigInteger;
                    }

                },
                AtomicLong.class, OptionalLong.class, BigInteger.class, Long.class, long.class);
    }
}