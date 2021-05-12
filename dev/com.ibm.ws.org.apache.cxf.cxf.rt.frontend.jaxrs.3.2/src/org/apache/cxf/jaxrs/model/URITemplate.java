/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.jaxrs.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;

import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public final class URITemplate {

    public static final String TEMPLATE_PARAMETERS = "jaxrs.template.parameters";
    public static final String URI_TEMPLATE = "jaxrs.template.uri";
    public static final String LIMITED_REGEX_SUFFIX = "(/.*)?";
    public static final String FINAL_MATCH_GROUP = "FINAL_MATCH_GROUP";
    private static final String DEFAULT_PATH_VARIABLE_REGEX = "([^/]+?)";
    private static final Pattern INTEGER_PATH_VARIABLE_REGEX_PATTERN = Pattern.compile("\\-?[0-9]+"); // Liberty change
    private static final Pattern DECIMAL_PATH_VARIABLE_REGEX_PATTERN = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?"); // Liberty change
    private static final String CHARACTERS_TO_ESCAPE = ".*+$()";
    private static final String SLASH = "/";
    private static final String SLASH_QUOTE = "/;";
    private static final int MAX_URI_TEMPLATE_CACHE_SIZE = SystemPropertyAction.getInteger("org.apache.cxf.jaxrs.max_uri_template_cache_size", 2000);
    private static final Map<String, URITemplate> URI_TEMPLATE_CACHE = new ConcurrentHashMap<String, URITemplate>();

    private final String template;
    private final List<String> variables = new ArrayList<>();
    private final List<String> customVariables = new ArrayList<>();
    private final Pattern templateRegexPattern;
    private final String literals;
    private final List<UriChunk> uriChunks;

    public URITemplate(String theTemplate) {
        this(theTemplate, Collections.<Parameter> emptyList());
    }

    public URITemplate(String theTemplate, List<Parameter> params) {
        template = theTemplate;
        StringBuilder literalChars = new StringBuilder();
        StringBuilder patternBuilder = new StringBuilder();
        CurlyBraceTokenizer tok = new CurlyBraceTokenizer(template);
        uriChunks = new ArrayList<>();
        while (tok.hasNext()) {
            String templatePart = tok.next();
            UriChunk chunk = UriChunk.createUriChunk(templatePart, params);
            uriChunks.add(chunk);
            if (chunk instanceof Literal) {
                String encodedValue = HttpUtils.encodePartiallyEncoded(chunk.getValue(), false);
                String substr = escapeCharacters(encodedValue);
                literalChars.append(substr);
                patternBuilder.append(substr);
            } else if (chunk instanceof Variable) {
                Variable var = (Variable)chunk;
                variables.add(var.getName());
                String pattern = var.getPattern();
                if (pattern != null) {
                    customVariables.add(var.getName());
                    // Add parenthesis to the pattern to identify a regex in the pattern, 
                    // however do not add them if they already exist since that will cause the Matcher
                    // to create extraneous values.  Parens identify a group so multiple parens would
                    // indicate multiple groups.
                    if (pattern.startsWith("(") && pattern.endsWith(")")) {
                        patternBuilder.append(pattern);
                    } else {
                        patternBuilder.append('(');
                        patternBuilder.append(pattern);
                        patternBuilder.append(')');
                    }
                } else {
                    patternBuilder.append(DEFAULT_PATH_VARIABLE_REGEX);
                }
            }
        }
        literals = literalChars.toString();

        int endPos = patternBuilder.length() - 1;
        boolean endsWithSlash = (endPos >= 0) && patternBuilder.charAt(endPos) == '/';
        if (endsWithSlash) {
            patternBuilder.deleteCharAt(endPos);
        }
        patternBuilder.append(LIMITED_REGEX_SUFFIX);

        templateRegexPattern = Pattern.compile(patternBuilder.toString());
    }

    public String getLiteralChars() {
        return literals;
    }

    public String getValue() {
        return template;
    }

    public String getPatternValue() {
        return templateRegexPattern.toString();
    }

    /**
     * List of all variables in order of appearance in template.
     *
     * @return unmodifiable list of variable names w/o patterns, e.g. for "/foo/{v1:\\d}/{v2}" returned list
     *         is ["v1","v2"].
     */
    public List<String> getVariables() {
        return Collections.unmodifiableList(variables);
    }

    /**
     * List of variables with patterns (regexps). List is subset of elements from {@link #getVariables()}.
     *
     * @return unmodifiable list of variables names w/o patterns.
     */
    public List<String> getCustomVariables() {
        return Collections.unmodifiableList(customVariables);
    }

    private static String escapeCharacters(String expression) {

        int length = expression.length();
        int i = 0;
        char ch = ' ';
        for (; i < length; ++i) {
            ch = expression.charAt(i);
            if (isReservedCharacter(ch)) {
                break;
            }
        }

        if (i == length) {
            return expression;
        }

        // Allows for up to 8 escaped characters before we start creating more
        // StringBuilders. 8 is an arbitrary limit, but it seems to be
        // sufficient in most cases.
        StringBuilder sb = new StringBuilder(length + 8);
        sb.append(expression, 0, i);
        sb.append('\\');
        sb.append(ch);
        ++i;
        for (; i < length; ++i) {
            ch = expression.charAt(i);
            if (isReservedCharacter(ch)) {
                sb.append('\\');
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    private static boolean isReservedCharacter(char ch) {
        return CHARACTERS_TO_ESCAPE.indexOf(ch) != -1;
    }

    public boolean match(String uri, MultivaluedMap<String, String> templateVariableToValue) {

        if (uri == null) {
            return (templateRegexPattern == null) ? true : false;
        }

        if (templateRegexPattern == null) {
            return false;
        }

        Matcher m = templateRegexPattern.matcher(uri);
        if (!m.matches() || template.equals(SLASH) && uri.startsWith(SLASH_QUOTE)) {
            if (uri.contains(";")) {
                // we might be trying to match one or few path segments
                // containing matrix
                // parameters against a clear path segment as in @Path("base").
                List<PathSegment> pList = JAXRSUtils.getPathSegments(template, false);
                List<PathSegment> uList = JAXRSUtils.getPathSegments(uri, false);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < uList.size(); i++) {
                    String segment = null;
                    if (pList.size() > i && pList.get(i).getPath().indexOf('{') == -1) {
                        segment = uList.get(i).getPath();
                    } else {
                        segment = HttpUtils.fromPathSegment(uList.get(i));
                    }
                    if (segment.length() > 0) {
                        sb.append(SLASH);
                    }
                    sb.append(segment);
                }
                uri = sb.toString();
                if (uri.length() == 0) {
                    uri = SLASH;
                }
                m = templateRegexPattern.matcher(uri);
                if (!m.matches()) {
                    return false;
                }
            } else {
                return false;
            }
        }

        // Assign the matched template values to template variables
        int groupCount = m.groupCount();

        int i = 1;
        for (String name : variables) {
            while (i <= groupCount) {
                String value = m.group(i++);
                if ((value == null || value.length() == 0 && i < groupCount)
                    && variables.size() + 1 < groupCount) {
                    continue;
                }
                templateVariableToValue.add(name, value);
                break;
            }
        }
        // The right hand side value, might be used to further resolve
        // sub-resources.

        String finalGroup = i > groupCount ? SLASH : m.group(groupCount);
        if (finalGroup == null || finalGroup.startsWith(SLASH_QUOTE)) {
            finalGroup = SLASH;
        }

        templateVariableToValue.putSingle(FINAL_MATCH_GROUP, finalGroup);

        return true;
    }

    /**
     * Substitutes template variables with listed values. List of values is counterpart for
     * {@link #getVariables() list of variables}. When list of value is shorter than variables substitution
     * is partial. When variable has pattern, value must fit to pattern, otherwise
     * {@link IllegalArgumentException} is thrown.
     * <p>
     * Example1: for template "/{a}/{b}/{a}" {@link #getVariables()} returns "[a, b, a]"; providing here list
     * of value "[foo, bar, baz]" results with "/foo/bar/baz".
     * <p>
     * Example2: for template "/{a}/{b}/{a}" providing list of values "[foo]" results with "/foo/{b}/{a}".
     *
     * @param values values for variables
     * @return template with bound variables.
     * @throws IllegalArgumentException when values is null, any value does not match pattern etc.
     */
    public String substitute(List<String> values) throws IllegalArgumentException {
        if (values == null) {
            throw new IllegalArgumentException("values is null");
        }
        Iterator<String> iter = values.iterator();
        StringBuilder sb = new StringBuilder();
        for (UriChunk chunk : uriChunks) {
            if (chunk instanceof Variable) {
                Variable var = (Variable)chunk;
                if (iter.hasNext()) {
                    String value = iter.next();
                    if (!var.matches(value)) {
                        throw new IllegalArgumentException("Value '" + value + "' does not match variable "
                                                           + var.getName() + " with pattern "
                                                           + var.getPattern());
                    }
                    sb.append(value);
                } else {
                    sb.append(var);
                }
            } else {
                sb.append(chunk);
            }
        }
        return sb.toString();
    }

    String substitute(Map<String, ? extends Object> valuesMap) throws IllegalArgumentException {
        return this.substitute(valuesMap, Collections.<String>emptySet(), false);
    }

    /**
     * Substitutes template variables with mapped values. Variables are mapped to values; if not all variables
     * are bound result will still contain variables. Note that all variables with the same name are replaced
     * by one value.
     * <p>
     * Example: for template "/{a}/{b}/{a}" {@link #getVariables()} returns "[a, b, a]"; providing here
     * mapping "[a: foo, b: bar]" results with "/foo/bar/foo" (full substitution) and for mapping "[b: baz]"
     * result is "{a}/baz/{a}" (partial substitution).
     *
     * @param valuesMap map variables to their values; on each value Object.toString() is called.
     * @return template with bound variables.
     */
    public String substitute(Map<String, ? extends Object> valuesMap,
                             Set<String> encodePathSlashVars,
                             boolean allowUnresolved) throws IllegalArgumentException {
        if (valuesMap == null) {
            throw new IllegalArgumentException("valuesMap is null");
        }
        StringBuilder sb = new StringBuilder();
        for (UriChunk chunk : uriChunks) {
            if (chunk instanceof Variable) {
                Variable var = (Variable)chunk;
                Object value = valuesMap.get(var.getName());
                if (value != null) {
                    String sval = value.toString();
                    if (!var.matches(sval)) {
                        throw new IllegalArgumentException("Value '" + sval + "' does not match variable "
                                                           + var.getName() + " with pattern "
                                                           + var.getPattern());
                    }
                    if (encodePathSlashVars.contains(var.getName())) {
                        sval = sval.replaceAll("/", "%2F");
                    }
                    sb.append(sval);
                } else if (allowUnresolved) {
                    sb.append(chunk);
                } else {
                    throw new IllegalArgumentException("Template variable " + var.getName()
                                                       + " has no matching value");
                }
            } else {
                sb.append(chunk);
            }
        }
        return sb.toString();
    }

    /**
     * Encoded literal characters surrounding template variables,
     * ex. "a {id} b" will be encoded to "a%20{id}%20b"
     *
     * @return encoded value
     */
    public String encodeLiteralCharacters(boolean isQuery) {
        final float encodedRatio = 1.5f;
        StringBuilder sb = new StringBuilder((int)(encodedRatio * template.length()));
        for (UriChunk chunk : uriChunks) {
            String val = chunk.getValue();
            if (chunk instanceof Literal) {
                sb.append(HttpUtils.encodePartiallyEncoded(val, isQuery));
            } else {
                sb.append(val);
            }
        }
        return sb.toString();
    }

    // Liberty Change start
    public static URITemplate createTemplate(Path path, List<Parameter> params, String classNameandPath) {

        return createTemplate(path == null ? null : path.value(), params, classNameandPath);
    }
    
    public static URITemplate createTemplate(Path path, List<Parameter> params) {

        return createTemplate(path == null ? null : path.value(), params);
    }

    public static URITemplate createTemplate(Path path) {

        return createTemplate(path == null ? null : path.value(), Collections.<Parameter> emptyList());
    }
    
    public static URITemplate createTemplate(Path path, String classNameandPath) {

        return createTemplate(path == null ? null : path.value(), Collections.<Parameter> emptyList(), classNameandPath);
    }

    public static URITemplate createTemplate(String pathValue) {
        return createTemplate(pathValue, Collections.<Parameter> emptyList(), pathValue);
    }
    
    public static URITemplate createTemplate(String pathValue, String classNameandPath) {
        return createTemplate(pathValue, Collections.<Parameter> emptyList(), classNameandPath);
    }

    public static URITemplate createTemplate(String pathValue, List<Parameter> params) {
        return createExactTemplate(pathValue, params, pathValue);
    }
    
    public static URITemplate createTemplate(String pathValue, List<Parameter> params, String classNameandPath) {
        if (pathValue == null) {
            pathValue = "/";
        } else if (!pathValue.startsWith("/")) {
            pathValue = "/" + pathValue;
        }
        return createExactTemplate(pathValue, params, classNameandPath);
    }

    public static URITemplate createExactTemplate(String pathValue) {
        return createExactTemplate(pathValue, Collections.<Parameter> emptyList());
    }

    public static URITemplate createExactTemplate(String pathValue, List<Parameter> params) {        
         return createExactTemplate(pathValue, params, pathValue);
    }
    
    public static URITemplate createExactTemplate(String pathValue, List<Parameter> params, String classNameandPath) {        
        URITemplate template = URI_TEMPLATE_CACHE.get(classNameandPath);
        if (template == null) {
            template = new URITemplate(pathValue, params);
            if (URI_TEMPLATE_CACHE.size() >= MAX_URI_TEMPLATE_CACHE_SIZE) {
                URI_TEMPLATE_CACHE.clear();
            }
            URI_TEMPLATE_CACHE.put(classNameandPath, template);            
        }
 
        return template;
    }
    // Liberty Change end

    public static int compareTemplates(URITemplate t1, URITemplate t2) {
        int l1 = t1.getLiteralChars().length();
        int l2 = t2.getLiteralChars().length();
        // descending order
        int result = l1 < l2 ? 1 : l1 > l2 ? -1 : 0;
        if (result == 0) {
            int g1 = t1.getVariables().size();
            int g2 = t2.getVariables().size();
            // descending order
            result = g1 < g2 ? 1 : g1 > g2 ? -1 : 0;
            if (result == 0) {
                int gCustom1 = t1.getCustomVariables().size();
                int gCustom2 = t2.getCustomVariables().size();
                result = gCustom1 < gCustom2 ? 1 : gCustom1 > gCustom2 ? -1 : 0;
                if (result == 0) {
                    result = t1.getPatternValue().compareTo(t2.getPatternValue());
                }
            }
        }

        return result;
    }

    /**
     * Stringified part of URI. Chunk is not URI segment; chunk can span over multiple URI segments or one URI
     * segments can have multiple chunks. Chunk is used to decompose URI of {@link URITemplate} into literals
     * and variables. Example: "foo/bar/{baz}{blah}" is decomposed into chunks: "foo/bar", "{baz}" and
     * "{blah}".
     */
    private abstract static class UriChunk {
        /**
         * Creates object form string.
         *
         * @param uriChunk stringified uri chunk
         * @return If param has variable form then {@link Variable} instance is created, otherwise chunk is
         *         treated as {@link Literal}.
         */
        public static UriChunk createUriChunk(String uriChunk, List<Parameter> params) {
            if (uriChunk == null || "".equals(uriChunk)) {
                throw new IllegalArgumentException("uriChunk is empty");
            }
            UriChunk uriChunkRepresentation = Variable.create(uriChunk, params); // Liberty change
            if (uriChunkRepresentation == null) {
                uriChunkRepresentation = Literal.create(uriChunk);
            }
            return uriChunkRepresentation;
        }

        public abstract String getValue();

        @Override
        public String toString() {
            return getValue();
        }
    }

    private static final class Literal extends UriChunk {
        private String value;

        private Literal() {
            // empty constructor
        }

        public static Literal create(String uriChunk) {
            if (uriChunk == null || "".equals(uriChunk)) {
                throw new IllegalArgumentException("uriChunk is empty");
            }
            Literal literal = new Literal();
            literal.value = uriChunk;
            return literal;
        }

        @Override
        public String getValue() {
            return value;
        }

    }

    /**
     * Variable of URITemplate. Variable has either "{varname:pattern}" syntax or "{varname}".
     */
    private static final class Variable extends UriChunk {
        private static final Pattern VARIABLE_PATTERN = Pattern.compile("(\\w[-\\w\\.]*[ ]*)(\\:(.+))?");
        private String name;
        private Pattern pattern;

        private Variable() {
            // empty constructor
        }

        /**
         * Creates variable from stringified part of URI.
         *
         * @param uriChunk uriChunk chunk that depicts variable
         * @return Variable if variable was successfully created; null if uriChunk was not a variable
         */
        public static Variable create(String uriChunk, List<Parameter> params) { //Liberty change
            // Liberty change start
            //Variable newVariable = new Variable();
            // Liberty change end
            if (uriChunk == null || "".equals(uriChunk)) {
                return null;
            }
            if (CurlyBraceTokenizer.insideBraces(uriChunk)) {
                uriChunk = CurlyBraceTokenizer.stripBraces(uriChunk).trim();
                Matcher matcher = VARIABLE_PATTERN.matcher(uriChunk);
                if (matcher.matches()) {
                    // Liberty change start
                    Variable newVariable = new Variable();
                    // Liberty change end
                    newVariable.name = matcher.group(1).trim();
                    if (matcher.group(2) != null && matcher.group(3) != null) {
                        String patternExpression = matcher.group(3).trim();
                        newVariable.pattern = Pattern.compile(patternExpression);
                        // Liberty change start
                    } else {
                        //check parameter types
                        for (Parameter p : params) {
                            if (p.getName() != null && p.getName().equals(newVariable.name)) {
                                Class<?> paramType = p.getJavaType();
                                if (paramType.isPrimitive()) {
                                    if (int.class.equals(paramType) || short.class.equals(paramType) || long.class.equals(paramType)) {
                                        newVariable.pattern = INTEGER_PATH_VARIABLE_REGEX_PATTERN;
                                    } else if (double.class.equals(paramType) || float.class.equals(paramType)) {
                                        newVariable.pattern = DECIMAL_PATH_VARIABLE_REGEX_PATTERN;
                                    }
                                }
                                break;
                            }
                        }
                    }
                    // Liberty change end
                    return newVariable;
                }
            }
            return null;
        }

        public String getName() {
            return name;
        }

        public String getPattern() {
            return pattern != null ? pattern.pattern() : null;
        }

        /**
         * Checks whether value matches variable. If variable has pattern its checked against, otherwise true
         * is returned.
         *
         * @param value value of variable
         * @return true if value is valid for variable, false otherwise.
         */
        public boolean matches(String value) {
            if (pattern == null) {
                return true;
            }
            return pattern.matcher(value).matches();
        }

        @Override
        public String getValue() {
            if (pattern != null) {
                return "{" + name + ":" + pattern + "}";
            }
            return "{" + name + "}";
        }
    }

    /**
     * Splits string into parts inside and outside curly braces. Nested curly braces are ignored and treated
     * as part inside top-level curly braces. Example: string "foo{bar{baz}}blah" is split into three tokens,
     * "foo","{bar{baz}}" and "blah". When closed bracket is missing, whole unclosed part is returned as one
     * token, e.g.: "foo{bar" is split into "foo" and "{bar". When opening bracket is missing, closing
     * bracket is ignored and taken as part of current token e.g.: "foo{bar}baz}blah" is split into "foo",
     * "{bar}" and "baz}blah".
     * <p>
     * This is helper class for {@link URITemplate} that enables recurring literals appearing next to regular
     * expressions e.g. "/foo/{zipcode:[0-9]{5}}/". Nested expressions with closed sections, like open-closed
     * brackets causes expression to be out of regular grammar (is context-free grammar) which are not
     * supported by Java regexp version.
     */
    static class CurlyBraceTokenizer {

        private final List<String> tokens = new ArrayList<>();
        private int tokenIdx;

        CurlyBraceTokenizer(String string) {
            boolean outside = true;
            int level = 0;
            int lastIdx = 0;
            int idx;
            // Liberty change start
            int length = string.length();
            for (idx = 0; idx < length; idx++) {
                char c = string.charAt(idx);
                if (c == '{') {
                    // Liberty change end
                    if (outside) {
                        if (lastIdx < idx) {
                            tokens.add(string.substring(lastIdx, idx));
                        }
                        lastIdx = idx;
                        outside = false;
                    } else {
                        level++;
                    }
                // Liberty change start
                } else if (c == '}' && !outside) {
                // Liberty change end
                    if (level > 0) {
                        level--;
                    } else {
                        if (lastIdx < idx) {
                            // Liberty change start
                            tokens.add(lastIdx == 0 && idx + 1 == length ? string : string.substring(lastIdx, idx + 1));
                            // Liberty change end
                        }
                        lastIdx = idx + 1;
                        outside = true;
                    }
                }
            }
            if (lastIdx < idx) {
                // Liberty change start
                tokens.add(lastIdx == 0 ? string : string.substring(lastIdx, idx));
                // Liberty change end
            }
        }

        /**
         * Token is enclosed by curly braces.
         *
         * @param token
         *            text to verify
         * @return true if enclosed, false otherwise.
         */
        public static boolean insideBraces(String token) {
            return token.charAt(0) == '{' && token.charAt(token.length() - 1) == '}';
        }

        /**
         * Strips token from enclosed curly braces. If token is not enclosed method
         * has no side effect.
         *
         * @param token
         *            text to verify
         * @return text stripped from curly brace begin-end pair.
         */
        public static String stripBraces(String token) {
            if (insideBraces(token)) {
                return token.substring(1, token.length() - 1);
            }
            return token;
        }

        public boolean hasNext() {
            return tokens.size() > tokenIdx;
        }

        public String next() {
            if (hasNext()) {
                return tokens.get(tokenIdx++);
            }
            throw new IllegalStateException("no more elements");
        }
    }
}


