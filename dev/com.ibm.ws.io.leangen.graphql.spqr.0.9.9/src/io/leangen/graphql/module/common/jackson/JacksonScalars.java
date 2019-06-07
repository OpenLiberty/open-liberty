package io.leangen.graphql.module.common.jackson;

import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ShortNode;
import com.fasterxml.jackson.databind.node.TextNode;
import graphql.language.BooleanValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLScalarType;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.leangen.graphql.util.Scalars.errorMessage;
import static io.leangen.graphql.util.Scalars.literalOrException;
import static io.leangen.graphql.util.Scalars.serializationException;
import static io.leangen.graphql.util.Scalars.valueParsingException;

@SuppressWarnings("WeakerAccess")
public class JacksonScalars {

    public static final GraphQLScalarType JsonTextNode = new GraphQLScalarType("JsonText", "Text JSON node", new Coercing() {
        @Override
        public Object serialize(Object dataFetcherResult) {
            if (dataFetcherResult instanceof String) {
                return dataFetcherResult;
            } if (dataFetcherResult instanceof TextNode) {
                return ((TextNode) dataFetcherResult).textValue();
            } else {
                throw serializationException(dataFetcherResult, String.class, TextNode.class);
            }
        }

        @Override
        public Object parseValue(Object input) {
            if (input instanceof String) {
                return new TextNode((String) input);
            }
            if (input instanceof TextNode) {
                return input;
            }
            throw valueParsingException(input, String.class, TextNode.class);
        }

        @Override
        public Object parseLiteral(Object input) {
            return new TextNode(literalOrException(input, StringValue.class).getValue());
        }
    });

    public static final GraphQLScalarType JsonBinaryNode = new GraphQLScalarType("JsonBase64Binary", "Base64-encoded binary JSON node", new Coercing() {
        private final Base64.Encoder encoder = Base64.getEncoder();
        private final Base64.Decoder decoder = Base64.getDecoder();

        @Override
        public Object serialize(Object dataFetcherResult) {
            if (dataFetcherResult instanceof String) {
                return dataFetcherResult;
            } if (dataFetcherResult instanceof BinaryNode) {
                return encoder.encodeToString(((BinaryNode) dataFetcherResult).binaryValue());
            } else {
                throw serializationException(dataFetcherResult, String.class, BinaryNode.class);
            }
        }

        @Override
        public Object parseValue(Object input) {
            if (input instanceof String) {
                return new BinaryNode(decoder.decode(input.toString()));
            }
            if (input instanceof BinaryNode) {
                return input;
            }
            throw valueParsingException(input, String.class, BinaryNode.class);
        }

        @Override
        public Object parseLiteral(Object input) {
            return new BinaryNode(decoder.decode(literalOrException(input, StringValue.class).getValue()));
        }
    });

    public static final GraphQLScalarType JsonBooleanNode = new GraphQLScalarType("JsonBoolean", "Boolean JSON node", new Coercing() {

        @Override
        public Object serialize(Object dataFetcherResult) {
            if (dataFetcherResult instanceof Boolean) {
                return dataFetcherResult;
            } if (dataFetcherResult instanceof BooleanNode) {
                return ((BooleanNode) dataFetcherResult).booleanValue();
            } else {
                throw serializationException(dataFetcherResult, Boolean.class, BooleanNode.class);
            }
        }

        @Override
        public Object parseValue(Object input) {
            if (input instanceof Boolean) {
                return (Boolean) input ? BooleanNode.TRUE : Boolean.FALSE;
            }
            if (input instanceof BooleanNode) {
                return input;
            }
            throw valueParsingException(input, Boolean.class, BooleanNode.class);
        }

        @Override
        public Object parseLiteral(Object input) {
            return literalOrException(input, BooleanValue.class).isValue();
        }
    });

    public static final GraphQLScalarType JsonDecimalNode = new GraphQLScalarType("JsonNumber", "Decimal JSON node", new Coercing() {

        @Override
        public Object serialize(Object dataFetcherResult) {
            if (dataFetcherResult instanceof DecimalNode) {
                return ((DecimalNode) dataFetcherResult).numberValue();
            } else {
                throw serializationException(dataFetcherResult, IntNode.class);
            }
        }

        @Override
        public Object parseValue(Object input) {
            if (input instanceof Number || input instanceof String) {
                return JsonNodeFactory.instance.numberNode(new BigDecimal(input.toString()));
            }
            if (input instanceof DecimalNode) {
                return input;
            }
            throw valueParsingException(input, Number.class, String.class, DecimalNode.class);
        }

        @Override
        public Object parseLiteral(Object input) {
            if (input instanceof IntValue) {
                return JsonNodeFactory.instance.numberNode(((IntValue) input).getValue());
            } else if (input instanceof FloatValue) {
                return JsonNodeFactory.instance.numberNode(((FloatValue) input).getValue());
            } else {
                throw new CoercingParseLiteralException(errorMessage(input, IntValue.class, FloatValue.class));
            }
        }
    });

    public static final GraphQLScalarType JsonIntegerNode = new GraphQLScalarType("JsonInteger", "Integer JSON node", new Coercing() {

        @Override
        public Object serialize(Object dataFetcherResult) {
            if (dataFetcherResult instanceof IntNode) {
                return ((IntNode) dataFetcherResult).numberValue();
            } else {
                throw serializationException(dataFetcherResult, IntNode.class);
            }
        }

        @Override
        public Object parseValue(Object input) {
            if (input instanceof Number || input instanceof String) {
                try {
                    return new IntNode(new BigInteger(input.toString()).intValueExact());
                } catch (ArithmeticException e) {
                    throw new CoercingParseValueException(input + " does not fit into an int without a loss of precision");
                }
            }
            if (input instanceof IntNode) {
                return input;
            }
            throw valueParsingException(input, Number.class, String.class, IntNode.class);
        }

        @Override
        public Object parseLiteral(Object input) {
            if (input instanceof IntValue) {
                try {
                    return new IntNode(((IntValue) input).getValue().intValueExact());
                } catch (ArithmeticException e) {
                    throw new CoercingParseLiteralException(input + " does not fit into an int without a loss of precision");
                }
            } else {
                throw new CoercingParseLiteralException(errorMessage(input, IntValue.class));
            }
        }
    });

    public static final GraphQLScalarType JsonBigIntegerNode = new GraphQLScalarType("JsonBigInteger", "BigInteger JSON node", new Coercing() {

        @Override
        public Object serialize(Object dataFetcherResult) {
            if (dataFetcherResult instanceof BigIntegerNode) {
                return ((BigIntegerNode) dataFetcherResult).numberValue();
            } else {
                throw serializationException(dataFetcherResult, BigIntegerNode.class);
            }
        }

        @Override
        public Object parseValue(Object input) {
            if (input instanceof Number || input instanceof String) {
                return new BigIntegerNode(new BigInteger(input.toString()));
            }
            if (input instanceof BigIntegerNode) {
                return input;
            }
            throw valueParsingException(input, Number.class, String.class, BigIntegerNode.class);
        }

        @Override
        public Object parseLiteral(Object input) {
            if (input instanceof IntValue) {
                return new BigIntegerNode(((IntValue) input).getValue());
            } else {
                throw new CoercingParseLiteralException(errorMessage(input, IntValue.class));
            }
        }
    });

    public static final GraphQLScalarType JsonShortNode = new GraphQLScalarType("JsonShort", "Short JSON node", new Coercing() {

        @Override
        public Object serialize(Object dataFetcherResult) {
            if (dataFetcherResult instanceof ShortNode) {
                return ((ShortNode) dataFetcherResult).numberValue();
            } else {
                throw serializationException(dataFetcherResult, ShortNode.class);
            }
        }

        @Override
        public Object parseValue(Object input) {
            if (input instanceof Number || input instanceof String) {
                try {
                    return new ShortNode(new BigInteger(input.toString()).shortValueExact());
                } catch (ArithmeticException e) {
                    throw new CoercingParseValueException(input + " does not fit into a short without a loss of precision");
                }
            }
            if (input instanceof ShortNode) {
                return input;
            }
            throw valueParsingException(input, Number.class, String.class, ShortNode.class);
        }

        @Override
        public Object parseLiteral(Object input) {
            if (input instanceof IntValue) {
                try {
                    return new ShortNode(((IntValue) input).getValue().shortValueExact());
                } catch (ArithmeticException e) {
                    throw new CoercingParseLiteralException(input + " does not fit into a short without a loss of precision");
                }
            } else {
                throw new CoercingParseLiteralException(errorMessage(input, IntValue.class));
            }
        }
    });

    public static final GraphQLScalarType JsonFloatNode = new GraphQLScalarType("JsonFloat", "Float JSON node", new Coercing() {

        @Override
        public Object serialize(Object dataFetcherResult) {
            if (dataFetcherResult instanceof FloatNode) {
                return ((FloatNode) dataFetcherResult).numberValue();
            } else {
                throw serializationException(dataFetcherResult, FloatNode.class);
            }
        }

        @Override
        public Object parseValue(Object input) {
            if (input instanceof Number || input instanceof String) {
                return new FloatNode(new BigDecimal(input.toString()).floatValue());
            }
            if (input instanceof FloatNode) {
                return input;
            }
            throw valueParsingException(input, Number.class, String.class, FloatNode.class);
        }

        @Override
        public Object parseLiteral(Object input) {
            if (input instanceof IntValue) {
                return new FloatNode(((IntValue) input).getValue().floatValue());
            } if (input instanceof FloatValue) {
                return new FloatNode(((FloatValue) input).getValue().floatValue());
            } else {
                throw new CoercingParseLiteralException(errorMessage(input, IntValue.class, FloatValue.class));
            }
        }
    });

    public static final GraphQLScalarType JsonDoubleNode = new GraphQLScalarType("JsonDouble", "Double JSON node", new Coercing() {

        @Override
        public Object serialize(Object dataFetcherResult) {
            if (dataFetcherResult instanceof DoubleNode) {
                return ((DoubleNode) dataFetcherResult).numberValue();
            } else {
                throw serializationException(dataFetcherResult, DoubleNode.class);
            }
        }

        @Override
        public Object parseValue(Object input) {
            if (input instanceof Number || input instanceof String) {
                return new DoubleNode(new BigDecimal(input.toString()).doubleValue());
            }
            if (input instanceof DoubleNode) {
                return input;
            }
            throw valueParsingException(input, Number.class, String.class, DoubleNode.class);
        }

        @Override
        public Object parseLiteral(Object input) {
            if (input instanceof IntValue) {
                return new DoubleNode(((IntValue) input).getValue().doubleValue());
            } if (input instanceof FloatValue) {
                return new DoubleNode(((FloatValue) input).getValue().doubleValue());
            } else {
                throw new CoercingParseLiteralException(errorMessage(input, IntValue.class, FloatValue.class));
            }
        }
    });

    private static final Map<Type, GraphQLScalarType> SCALAR_MAPPING = getScalarMapping();

    public static boolean isScalar(Type javaType) {
        return SCALAR_MAPPING.containsKey(javaType);
    }

    public static GraphQLScalarType toGraphQLScalarType(Type javaType) {
        return SCALAR_MAPPING.get(javaType);
    }

    private static Map<Type, GraphQLScalarType> getScalarMapping() {
        Map<Type, GraphQLScalarType> scalarMapping = new HashMap<>();
        scalarMapping.put(TextNode.class, JsonTextNode);
        scalarMapping.put(BooleanNode.class, JsonBooleanNode);
        scalarMapping.put(BinaryNode.class, JsonBinaryNode);
        scalarMapping.put(BigIntegerNode.class, JsonBigIntegerNode);
        scalarMapping.put(IntNode.class, JsonIntegerNode);
        scalarMapping.put(ShortNode.class, JsonShortNode);
        scalarMapping.put(DecimalNode.class, JsonDecimalNode);
        scalarMapping.put(FloatNode.class, JsonFloatNode);
        scalarMapping.put(DoubleNode.class, JsonDoubleNode);
        scalarMapping.put(NumericNode.class, JsonDecimalNode);
        return Collections.unmodifiableMap(scalarMapping);
    }
}
