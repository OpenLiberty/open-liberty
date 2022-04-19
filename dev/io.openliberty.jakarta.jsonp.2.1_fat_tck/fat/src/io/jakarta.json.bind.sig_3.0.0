#Signature file v4.1
#Version 3.0

CLSS public abstract interface jakarta.json.bind.Jsonb
intf java.lang.AutoCloseable
meth public abstract <%0 extends java.lang.Object> {%%0} fromJson(java.io.InputStream,java.lang.Class<{%%0}>)
meth public abstract <%0 extends java.lang.Object> {%%0} fromJson(java.io.InputStream,java.lang.reflect.Type)
meth public abstract <%0 extends java.lang.Object> {%%0} fromJson(java.io.Reader,java.lang.Class<{%%0}>)
meth public abstract <%0 extends java.lang.Object> {%%0} fromJson(java.io.Reader,java.lang.reflect.Type)
meth public abstract <%0 extends java.lang.Object> {%%0} fromJson(java.lang.String,java.lang.Class<{%%0}>)
meth public abstract <%0 extends java.lang.Object> {%%0} fromJson(java.lang.String,java.lang.reflect.Type)
meth public abstract java.lang.String toJson(java.lang.Object)
meth public abstract java.lang.String toJson(java.lang.Object,java.lang.reflect.Type)
meth public abstract void toJson(java.lang.Object,java.io.OutputStream)
meth public abstract void toJson(java.lang.Object,java.io.Writer)
meth public abstract void toJson(java.lang.Object,java.lang.reflect.Type,java.io.OutputStream)
meth public abstract void toJson(java.lang.Object,java.lang.reflect.Type,java.io.Writer)

CLSS public abstract interface jakarta.json.bind.JsonbBuilder
meth public abstract jakarta.json.bind.Jsonb build()
meth public abstract jakarta.json.bind.JsonbBuilder withConfig(jakarta.json.bind.JsonbConfig)
meth public abstract jakarta.json.bind.JsonbBuilder withProvider(jakarta.json.spi.JsonProvider)
meth public static jakarta.json.bind.Jsonb create()
meth public static jakarta.json.bind.Jsonb create(jakarta.json.bind.JsonbConfig)
meth public static jakarta.json.bind.JsonbBuilder newBuilder()
meth public static jakarta.json.bind.JsonbBuilder newBuilder(jakarta.json.bind.spi.JsonbProvider)
meth public static jakarta.json.bind.JsonbBuilder newBuilder(java.lang.String)

CLSS public jakarta.json.bind.JsonbConfig
cons public init()
fld public final static java.lang.String ADAPTERS = "jsonb.adapters"
fld public final static java.lang.String BINARY_DATA_STRATEGY = "jsonb.binary-data-strategy"
fld public final static java.lang.String CREATOR_PARAMETERS_REQUIRED = "jsonb.creator-parameters-required"
fld public final static java.lang.String DATE_FORMAT = "jsonb.date-format"
fld public final static java.lang.String DESERIALIZERS = "jsonb.derializers"
fld public final static java.lang.String ENCODING = "jsonb.encoding"
fld public final static java.lang.String FORMATTING = "jsonb.formatting"
fld public final static java.lang.String LOCALE = "jsonb.locale"
fld public final static java.lang.String NULL_VALUES = "jsonb.null-values"
fld public final static java.lang.String PROPERTY_NAMING_STRATEGY = "jsonb.property-naming-strategy"
fld public final static java.lang.String PROPERTY_ORDER_STRATEGY = "jsonb.property-order-strategy"
fld public final static java.lang.String PROPERTY_VISIBILITY_STRATEGY = "jsonb.property-visibility-strategy"
fld public final static java.lang.String SERIALIZERS = "jsonb.serializers"
fld public final static java.lang.String STRICT_IJSON = "jsonb.strict-ijson"
meth public !varargs final jakarta.json.bind.JsonbConfig withAdapters(jakarta.json.bind.adapter.JsonbAdapter[])
meth public !varargs final jakarta.json.bind.JsonbConfig withDeserializers(jakarta.json.bind.serializer.JsonbDeserializer[])
meth public !varargs final jakarta.json.bind.JsonbConfig withSerializers(jakarta.json.bind.serializer.JsonbSerializer[])
meth public final jakarta.json.bind.JsonbConfig setProperty(java.lang.String,java.lang.Object)
meth public final jakarta.json.bind.JsonbConfig withBinaryDataStrategy(java.lang.String)
meth public final jakarta.json.bind.JsonbConfig withCreatorParametersRequired(boolean)
meth public final jakarta.json.bind.JsonbConfig withDateFormat(java.lang.String,java.util.Locale)
meth public final jakarta.json.bind.JsonbConfig withEncoding(java.lang.String)
meth public final jakarta.json.bind.JsonbConfig withFormatting(java.lang.Boolean)
meth public final jakarta.json.bind.JsonbConfig withLocale(java.util.Locale)
meth public final jakarta.json.bind.JsonbConfig withNullValues(java.lang.Boolean)
meth public final jakarta.json.bind.JsonbConfig withPropertyNamingStrategy(jakarta.json.bind.config.PropertyNamingStrategy)
meth public final jakarta.json.bind.JsonbConfig withPropertyNamingStrategy(java.lang.String)
meth public final jakarta.json.bind.JsonbConfig withPropertyOrderStrategy(java.lang.String)
meth public final jakarta.json.bind.JsonbConfig withPropertyVisibilityStrategy(jakarta.json.bind.config.PropertyVisibilityStrategy)
meth public final jakarta.json.bind.JsonbConfig withStrictIJSON(java.lang.Boolean)
meth public final java.util.Map<java.lang.String,java.lang.Object> getAsMap()
meth public final java.util.Optional<java.lang.Object> getProperty(java.lang.String)
supr java.lang.Object
hfds configuration

CLSS public jakarta.json.bind.JsonbException
cons public init(java.lang.String)
cons public init(java.lang.String,java.lang.Throwable)
supr java.lang.RuntimeException
hfds serialVersionUID

CLSS public abstract interface jakarta.json.bind.adapter.JsonbAdapter<%0 extends java.lang.Object, %1 extends java.lang.Object>
meth public abstract {jakarta.json.bind.adapter.JsonbAdapter%0} adaptFromJson({jakarta.json.bind.adapter.JsonbAdapter%1}) throws java.lang.Exception
meth public abstract {jakarta.json.bind.adapter.JsonbAdapter%1} adaptToJson({jakarta.json.bind.adapter.JsonbAdapter%0}) throws java.lang.Exception

CLSS public abstract interface !annotation jakarta.json.bind.annotation.JsonbAnnotation
 anno 0 java.lang.annotation.Documented()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
intf java.lang.annotation.Annotation

CLSS public abstract interface !annotation jakarta.json.bind.annotation.JsonbCreator
 anno 0 jakarta.json.bind.annotation.JsonbAnnotation()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE, METHOD, CONSTRUCTOR])
intf java.lang.annotation.Annotation

CLSS public abstract interface !annotation jakarta.json.bind.annotation.JsonbDateFormat
 anno 0 jakarta.json.bind.annotation.JsonbAnnotation()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE, FIELD, METHOD, TYPE, PARAMETER, PACKAGE])
fld public final static java.lang.String DEFAULT_FORMAT = "##default"
fld public final static java.lang.String DEFAULT_LOCALE = "##default"
fld public final static java.lang.String TIME_IN_MILLIS = "##time-in-millis"
intf java.lang.annotation.Annotation
meth public abstract !hasdefault java.lang.String locale()
meth public abstract !hasdefault java.lang.String value()

CLSS public abstract interface !annotation jakarta.json.bind.annotation.JsonbNillable
 anno 0 jakarta.json.bind.annotation.JsonbAnnotation()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE, FIELD, METHOD, TYPE, PACKAGE])
intf java.lang.annotation.Annotation
meth public abstract !hasdefault boolean value()

CLSS public abstract interface !annotation jakarta.json.bind.annotation.JsonbNumberFormat
 anno 0 jakarta.json.bind.annotation.JsonbAnnotation()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE, FIELD, METHOD, TYPE, PARAMETER, PACKAGE])
fld public final static java.lang.String DEFAULT_LOCALE = "##default"
intf java.lang.annotation.Annotation
meth public abstract !hasdefault java.lang.String locale()
meth public abstract !hasdefault java.lang.String value()

CLSS public abstract interface !annotation jakarta.json.bind.annotation.JsonbProperty
 anno 0 jakarta.json.bind.annotation.JsonbAnnotation()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE, METHOD, FIELD, PARAMETER])
intf java.lang.annotation.Annotation
meth public abstract !hasdefault boolean nillable()
 anno 0 java.lang.Deprecated(boolean forRemoval=false, java.lang.String since="2.1")
meth public abstract !hasdefault java.lang.String value()

CLSS public abstract interface !annotation jakarta.json.bind.annotation.JsonbPropertyOrder
 anno 0 jakarta.json.bind.annotation.JsonbAnnotation()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE, TYPE])
intf java.lang.annotation.Annotation
meth public abstract java.lang.String[] value()

CLSS public abstract interface !annotation jakarta.json.bind.annotation.JsonbSubtype
 anno 0 jakarta.json.bind.annotation.JsonbAnnotation()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[])
intf java.lang.annotation.Annotation
meth public abstract java.lang.Class<?> type()
meth public abstract java.lang.String alias()

CLSS public abstract interface !annotation jakarta.json.bind.annotation.JsonbTransient
 anno 0 jakarta.json.bind.annotation.JsonbAnnotation()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE, FIELD, METHOD])
intf java.lang.annotation.Annotation

CLSS public abstract interface !annotation jakarta.json.bind.annotation.JsonbTypeAdapter
 anno 0 jakarta.json.bind.annotation.JsonbAnnotation()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE, TYPE, FIELD, METHOD, PARAMETER])
intf java.lang.annotation.Annotation
meth public abstract java.lang.Class<? extends jakarta.json.bind.adapter.JsonbAdapter> value()

CLSS public abstract interface !annotation jakarta.json.bind.annotation.JsonbTypeDeserializer
 anno 0 jakarta.json.bind.annotation.JsonbAnnotation()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE, TYPE, FIELD, METHOD, PARAMETER])
intf java.lang.annotation.Annotation
meth public abstract java.lang.Class<? extends jakarta.json.bind.serializer.JsonbDeserializer> value()

CLSS public abstract interface !annotation jakarta.json.bind.annotation.JsonbTypeInfo
 anno 0 jakarta.json.bind.annotation.JsonbAnnotation()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE, TYPE])
fld public final static java.lang.String DEFAULT_KEY_NAME = "@type"
intf java.lang.annotation.Annotation
meth public abstract !hasdefault jakarta.json.bind.annotation.JsonbSubtype[] value()
meth public abstract !hasdefault java.lang.String key()

CLSS public abstract interface !annotation jakarta.json.bind.annotation.JsonbTypeSerializer
 anno 0 jakarta.json.bind.annotation.JsonbAnnotation()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE, TYPE, FIELD, METHOD])
intf java.lang.annotation.Annotation
meth public abstract java.lang.Class<? extends jakarta.json.bind.serializer.JsonbSerializer> value()

CLSS public abstract interface !annotation jakarta.json.bind.annotation.JsonbVisibility
 anno 0 jakarta.json.bind.annotation.JsonbAnnotation()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE, TYPE, PACKAGE])
intf java.lang.annotation.Annotation
meth public abstract java.lang.Class<? extends jakarta.json.bind.config.PropertyVisibilityStrategy> value()

CLSS public final jakarta.json.bind.config.BinaryDataStrategy
fld public final static java.lang.String BASE_64 = "BASE_64"
fld public final static java.lang.String BASE_64_URL = "BASE_64_URL"
fld public final static java.lang.String BYTE = "BYTE"
supr java.lang.Object

CLSS public abstract interface jakarta.json.bind.config.PropertyNamingStrategy
fld public final static java.lang.String CASE_INSENSITIVE = "CASE_INSENSITIVE"
fld public final static java.lang.String IDENTITY = "IDENTITY"
fld public final static java.lang.String LOWER_CASE_WITH_DASHES = "LOWER_CASE_WITH_DASHES"
fld public final static java.lang.String LOWER_CASE_WITH_UNDERSCORES = "LOWER_CASE_WITH_UNDERSCORES"
fld public final static java.lang.String UPPER_CAMEL_CASE = "UPPER_CAMEL_CASE"
fld public final static java.lang.String UPPER_CAMEL_CASE_WITH_SPACES = "UPPER_CAMEL_CASE_WITH_SPACES"
meth public abstract java.lang.String translateName(java.lang.String)

CLSS public final jakarta.json.bind.config.PropertyOrderStrategy
fld public final static java.lang.String ANY = "ANY"
fld public final static java.lang.String LEXICOGRAPHICAL = "LEXICOGRAPHICAL"
fld public final static java.lang.String REVERSE = "REVERSE"
supr java.lang.Object

CLSS public abstract interface jakarta.json.bind.config.PropertyVisibilityStrategy
meth public abstract boolean isVisible(java.lang.reflect.Field)
meth public abstract boolean isVisible(java.lang.reflect.Method)

CLSS public abstract interface jakarta.json.bind.serializer.DeserializationContext
meth public abstract <%0 extends java.lang.Object> {%%0} deserialize(java.lang.Class<{%%0}>,jakarta.json.stream.JsonParser)
meth public abstract <%0 extends java.lang.Object> {%%0} deserialize(java.lang.reflect.Type,jakarta.json.stream.JsonParser)

CLSS public abstract interface jakarta.json.bind.serializer.JsonbDeserializer<%0 extends java.lang.Object>
meth public abstract {jakarta.json.bind.serializer.JsonbDeserializer%0} deserialize(jakarta.json.stream.JsonParser,jakarta.json.bind.serializer.DeserializationContext,java.lang.reflect.Type)

CLSS public abstract interface jakarta.json.bind.serializer.JsonbSerializer<%0 extends java.lang.Object>
meth public abstract void serialize({jakarta.json.bind.serializer.JsonbSerializer%0},jakarta.json.stream.JsonGenerator,jakarta.json.bind.serializer.SerializationContext)

CLSS public abstract interface jakarta.json.bind.serializer.SerializationContext
meth public abstract <%0 extends java.lang.Object> void serialize(java.lang.String,{%%0},jakarta.json.stream.JsonGenerator)
meth public abstract <%0 extends java.lang.Object> void serialize({%%0},jakarta.json.stream.JsonGenerator)

CLSS public abstract jakarta.json.bind.spi.JsonbProvider
cons protected init()
meth public abstract jakarta.json.bind.JsonbBuilder create()
meth public static jakarta.json.bind.spi.JsonbProvider provider()
meth public static jakarta.json.bind.spi.JsonbProvider provider(java.lang.String)
supr java.lang.Object
hfds DEFAULT_PROVIDER

CLSS public abstract interface java.io.Serializable

CLSS public abstract interface java.lang.AutoCloseable
meth public abstract void close() throws java.lang.Exception

CLSS public java.lang.Exception
cons protected init(java.lang.String,java.lang.Throwable,boolean,boolean)
cons public init()
cons public init(java.lang.String)
cons public init(java.lang.String,java.lang.Throwable)
cons public init(java.lang.Throwable)
supr java.lang.Throwable

CLSS public java.lang.Object
cons public init()
meth protected java.lang.Object clone() throws java.lang.CloneNotSupportedException
meth protected void finalize() throws java.lang.Throwable
 anno 0 java.lang.Deprecated(boolean forRemoval=false, java.lang.String since="9")
meth public boolean equals(java.lang.Object)
meth public final java.lang.Class<?> getClass()
meth public final void notify()
meth public final void notifyAll()
meth public final void wait() throws java.lang.InterruptedException
meth public final void wait(long) throws java.lang.InterruptedException
meth public final void wait(long,int) throws java.lang.InterruptedException
meth public int hashCode()
meth public java.lang.String toString()

CLSS public java.lang.RuntimeException
cons protected init(java.lang.String,java.lang.Throwable,boolean,boolean)
cons public init()
cons public init(java.lang.String)
cons public init(java.lang.String,java.lang.Throwable)
cons public init(java.lang.Throwable)
supr java.lang.Exception

CLSS public java.lang.Throwable
cons protected init(java.lang.String,java.lang.Throwable,boolean,boolean)
cons public init()
cons public init(java.lang.String)
cons public init(java.lang.String,java.lang.Throwable)
cons public init(java.lang.Throwable)
intf java.io.Serializable
meth public final java.lang.Throwable[] getSuppressed()
meth public final void addSuppressed(java.lang.Throwable)
meth public java.lang.StackTraceElement[] getStackTrace()
meth public java.lang.String getLocalizedMessage()
meth public java.lang.String getMessage()
meth public java.lang.String toString()
meth public java.lang.Throwable fillInStackTrace()
meth public java.lang.Throwable getCause()
meth public java.lang.Throwable initCause(java.lang.Throwable)
meth public void printStackTrace()
meth public void printStackTrace(java.io.PrintStream)
meth public void printStackTrace(java.io.PrintWriter)
meth public void setStackTrace(java.lang.StackTraceElement[])
supr java.lang.Object

CLSS public abstract interface java.lang.annotation.Annotation
meth public abstract boolean equals(java.lang.Object)
meth public abstract int hashCode()
meth public abstract java.lang.Class<? extends java.lang.annotation.Annotation> annotationType()
meth public abstract java.lang.String toString()

CLSS public abstract interface !annotation java.lang.annotation.Documented
 anno 0 java.lang.annotation.Documented()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE])
intf java.lang.annotation.Annotation

CLSS public abstract interface !annotation java.lang.annotation.Retention
 anno 0 java.lang.annotation.Documented()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE])
intf java.lang.annotation.Annotation
meth public abstract java.lang.annotation.RetentionPolicy value()

CLSS public abstract interface !annotation java.lang.annotation.Target
 anno 0 java.lang.annotation.Documented()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE])
intf java.lang.annotation.Annotation
meth public abstract java.lang.annotation.ElementType[] value()

