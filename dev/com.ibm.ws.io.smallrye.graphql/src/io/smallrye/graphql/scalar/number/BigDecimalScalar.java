// https://github.com/smallrye/smallrye-graphql/blob/4a47a2da6e4f4b4a6aedf2fc6464510737fe4c52/server/implementation/src/main/java/io/smallrye/graphql/scalar/number/BigDecimalScalar.java
// Apache v2.0 licensed - https://github.com/smallrye/smallrye-graphql/blob/1.0.9/LICENSE
package io.smallrye.graphql.scalar.number;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Scalar for BigDecimal.
 * Based on graphql-java's Scalars.GraphQLBigDecimal
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public class BigDecimalScalar extends AbstractNumberScalar {

    public BigDecimalScalar() {

        super("BigDecimal",
                new Converter() {
                    @Override
                    public Object fromBigDecimal(BigDecimal bigDecimal) {
                        return bigDecimal;
                    }

                    @Override
                    public Object fromBigInteger(BigInteger bigInteger) {
                        return new BigDecimal(bigInteger);
                    }
                },
                BigDecimal.class);
    }

}