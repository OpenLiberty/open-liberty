/**
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 * Copyright 2017 SmartBear Software
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.microprofile.openapi.models.parameters;

import java.util.Map;

import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Schema;

/**
 * Parameter
 * <p>
 * Describes a single operation parameter.
 * <p>
 * A unique parameter is defined by a combination of a name and location. There are four possible parameter locations specified by the in field:
 * <ul>
 * <li>path - Used together with Path Templating, where the parameter value is actually part of the operation's URL. This does not include the host or
 * base path of the API. For example, in /items/{itemId}, the path parameter is itemId.</li>
 * <li>query - Parameters that are appended to the URL. For example, in /items?id=###, the query parameter is id.</li>
 * <li>header - Custom headers that are expected as part of the request. Note that RFC7230 states header names are case insensitive.</li>
 * <li>cookie - Used to pass a specific cookie value to the API.</li>
 * </ul>
 * <p>
 * The rules for serialization of the parameter are specified in one of two ways. For simpler scenarios, a schema and style can describe the structure
 * and syntax of the parameter.
 * <p>
 * For more complex scenarios, the content property can define the media type and schema of the parameter. A parameter must contain either a schema
 * property, or a content property, but not both.
 * 
 * @see <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#parameterObject">OpenAPI Specification Parameter
 *      Object</a>
 */
@SuppressWarnings("rawtypes")
public interface Parameter<T extends Parameter<T>> extends Extensible, Reference<T> {

    /**
     * The values allowed for the style field.
     */
    enum StyleEnum {
        MATRIX("matrix"), LABEL("label"), FORM("form"), SIMPLE("simple"), SPACEDELIMITED("spaceDelimited"), PIPEDELIMITED(
                "pipeDelimited"), DEEPOBJECT("deepObject");

        private final String value;

        StyleEnum(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
    /**
     * The values allowed for the in field.
     */
    enum In {
        PATH("path"), QUERY("query"), HEADER("header"), COOKIE("cookie");

        private final String value;

        In(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    /**
     * Returns the name property from a Parameter instance.
     *
     * @return the name of the parameter
     **/
    String getName();

    /**
     * Sets the name property of a Parameter instance to the given string.
     *
     * @param name the name of the parameter
     */
    void setName(String name);

    /**
     * Sets the name property of a Parameter instance to the given string.
     *
     * @param name the name of the parameter
     * @return the current Parameter instance
     */
    T name(String name);

    /**
     * Returns the in property from a Parameter instance.
     *
     * @return the location of the parameter
     **/
    In getIn();

    /**
     * Returns the description property from a Parameter instance.
     *
     * @return a brief description of the parameter
     **/
    String getDescription();

    /**
     * Sets the description property of a Parameter instance to the given string.
     *
     * @param description a brief description of the parameter
     */
    void setDescription(String description);

    /**
     * Sets the description property of a Parameter instance to the given string.
     *
     * @param description a brief description of the parameter
     * @return the current Parameter instance
     */
    T description(String description);

    /**
     * Returns the required property from a Parameter instance.
     *
     * @return indicates whether this parameter is mandatory
     **/
    Boolean getRequired();

    /**
     * Sets the required property of a Parameter instance to the given value.
     *
     * @param required indicates whether this parameter is mandatory
     */
    void setRequired(Boolean required);

    /**
     * Sets the required property of a Parameter instance to the given value.
     *
     * @param required indicates whether this parameter is mandatory
     * @return the current Parameter instance
     */
    T required(Boolean required);

    /**
     * Returns the deprecated property from a Parameter instance.
     *
     * @return specifies that a parameter is deprecated
     **/
    Boolean getDeprecated();

    /**
     * Sets the deprecated property of a Parameter instance to the given value.
     *
     * @param deprecated specifies that a parameter is deprecated
     */
    void setDeprecated(Boolean deprecated);

    /**
     * Sets the deprecated property of a Parameter instance to the given value.
     *
     * @param deprecated specifies that a parameter is deprecated
     * @return the current Parameter instance
     */
    T deprecated(Boolean deprecated);

    /**
     * Returns the allowEmptyValue property from a Parameter instance.
     *
     * @return specifies the ability to pass empty-valued parameters
     **/
    Boolean getAllowEmptyValue();

    /**
     * Sets the allowEmptyValue property of a Parameter instance to the given value.
     *
     * @param allowEmptyValue specify the ability to pass empty-valued parameters
     */
    void setAllowEmptyValue(Boolean allowEmptyValue);

    /**
     * Sets the allowEmptyValue property of a Parameter instance to the given value.
     *
     * @param allowEmptyValue specify the ability to pass empty-valued parameters
     * @return the current Parameter instance
     */
    T allowEmptyValue(Boolean allowEmptyValue);

    /**
     * Returns the style property from a Parameter instance.
     *
     * @return describes how the parameter value will be serialized
     **/
    Parameter.StyleEnum getStyle();

    /**
     * Sets the style property of a Parameter instance to the given value.
     *
     * @param style describes how the parameter value will be serialized
     */
    void setStyle(Parameter.StyleEnum style);

    /**
     * Sets the style property of a Parameter instance to the given value.
     *
     * @param style describes how the parameter value will be serialized
     * @return the current Parameter instance
     */
    T style(Parameter.StyleEnum style);

    /**
     * Returns the explode property from a Parameter instance.
     *
     * @return whether parameter values of type "array" or "object" generate separate parameters for each value
     **/
    Boolean getExplode();

    /**
     * Sets the explode property of a Parameter instance to the given value.
     *
     * @param explode whether parameter values of type "array" or "object" generate separate parameters for each value
     */
    void setExplode(Boolean explode);

    /**
     * Sets the explode property of a Parameter instance to the given value.
     *
     * @param explode whether parameter values of type "array" or "object" generate separate parameters for each value
     * @return the current Parameter instance
     */
    T explode(Boolean explode);

    /**
     * Returns the allowReserved property from a Parameter instance.
     *
     * @return specifies whether the parameter value should allow reserved characters
     **/
    Boolean getAllowReserved();

    /**
     * Sets the allowReserved property of a Parameter instance to the given value.
     *
     * @param allowReserved specifies whether the parameter value should allow reserved characters
     */
    void setAllowReserved(Boolean allowReserved);

    /**
     * Sets the allowReserved property of a Parameter instance to the given value.
     *
     * @param allowReserved specifies whether the parameter value should allow reserved characters
     * @return the current Parameter instance
     */
    T allowReserved(Boolean allowReserved);

    /**
     * Returns the schema property from a Parameter instance.
     *
     * @return schema defining the type used for the parameter
     **/
    Schema getSchema();

    /**
     * Sets the schema property of a Parameter instance to the given value.
     *
     * @param schema schema defining the type used for the parameter
     */
    void setSchema(Schema schema);

    /**
     * Sets the schema property of a Parameter instance to the given value.
     *
     * @param schema schema defining the type used for the parameter
     * @return the current Parameter instance
     */
    T schema(Schema schema);

    /**
     * Returns the examples property from a Parameter instance.
     *
     * @return examples of the media type
     **/
    Map<String, Example> getExamples();

    /**
     * Sets the examples property of a Parameter instance to the given value. Each example should contain a value in the correct format as specified
     * in the parameter encoding. The examples object is mutually exclusive of the example object.
     *
     * @param examples examples of the media type
     */
    void setExamples(Map<String, Example> examples);

    /**
     * Sets the examples property of a Parameter instance to the given value. Each example should contain a value in the correct format as specified
     * in the parameter encoding. The examples object is mutually exclusive of the example object.
     *
     * @param examples examples of the media type
     * @return the current Parameter instance
     */
    T examples(Map<String, Example> examples);

    /**
     * Adds an example of the media type using the specified key. The example should contain a value in the correct format as specified in the
     * parameter encoding.
     *
     * @param key string to represent the example
     * @param example example of the media type
     * @return the current Parameter instance
     */
    T addExample(String key, Example example);

    /**
     * Returns the example property from a Parameter instance.
     *
     * @return example of the media type
     **/
    Object getExample();

    /**
     * Sets the example property of a Parameter instance to the given object. The example should match the specified schema and encoding properties if
     * present. The examples object is mutually exclusive of the example object.
     *
     * @param example example of the media type
     */
    void setExample(Object example);

    /**
     * Sets the example property of a Parameter instance to the given object. The example should match the specified schema and encoding properties if
     * present. The examples object is mutually exclusive of the example object.
     *
     * @param example example of the media type
     * @return the current Parameter instance
     */
    T example(Object example);

    /**
     * Returns the content property from a Parameter instance.
     *
     * @return a map containing the media representations for the parameter
     **/
    Content getContent();

    /**
     * Sets the content property of a Parameter instance to the given object.
     *
     * @param content a map containing the media representations for the parameter
     */
    void setContent(Content content);

    /**
     * Sets the content property of a Parameter instance to the given object.
     *
     * @param content a map containing the media representations for the parameter
     * @return the current Parameter instance
     */
    T content(Content content);

}