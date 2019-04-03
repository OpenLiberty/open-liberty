/**
 * Copyright (c) 2018-2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.microprofile.reactive.messaging;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Configure the acknowledgement policy for the given {@code @Incoming}.
 *
 * The set of supported acknowledgment policy depends on the method signature. The given list indicates the supported
 * policies for each type of signature:
 *
 * <ul>
 *     <li><code>@Incoming Subscriber&lt;Message&lt;I&gt;&gt; method()</code>: None, Pre, Manual</li>
 *     <li><code>@Incoming Subscriber&lt;I&gt; method()</code>: None, Pre, Post</li>
 *     <li><code>@Incoming void method(I msg)</code>: None, Pre, Post</li>
 *     <li><code>@Incoming CompletionStage&lt;?&gt; method(Message&lt;I&gt; msg)</code>: None, Pre, Post, Manual</li>
 *     <li><code>@Incoming CompletionStage&lt;?&gt; method(I msg)</code>: None, Pre, Post</li>
 *
 *     <li><code>@Outgoing @Incoming Processor&lt;Message&lt;I&gt;, Message&lt;O&gt;&gt; method()</code>: None, Pre,
 *     Manual, Post with the assumption that each incoming message produces a single outgoing message</li>
 *     <li><code>@Outgoing @Incoming Processor&lt;I, O&gt; method()</code>: None, Pre, Post with the assumption
 *     that each incoming payload produces a single outgoing payload</li>
 *     <li><code>@Outgoing @Incoming ProcessorBuilder&lt;Message&lt;I&gt;, Message&lt;O&gt;&gt; method()</code>: None,
 *     Manual, Pre, Post with the assumption that each incoming message produces a single outgoing message</li>
 *     <li><code>@Outgoing @Incoming ProcessorBuilder&lt;I, O&gt; method()</code>: None, Manual, Pre, Post with the
 *     assumption that each incoming payload produces a single outgoing payload</li>
 *
 *     <li><code>@Outgoing @Incoming Publisher&lt;Message&lt;O&gt;&gt; method(Message&lt;I&gt; msg)</code>: None,
 *     Manual,Pre</li>
 *     <li><code>@Outgoing @Incoming Publisher&lt;O&gt; method(I payload)</code>: None, Pre</li>
 *     <li><code>@Outgoing @Incoming PublisherBuilder&lt;Message&lt;O&gt;&gt; method(Message&lt;I&gt; msg)</code>: None,
 *     Pre, Manual</li>
 *     <li><code>@Outgoing @Incoming PublisherBuilder&lt;O&gt; method(I payload)</code>: None, Pre</li>
 *
 *     <li><code>@Outgoing @Incoming Message&lt;O&gt; method(Message&lt;I&gt; msg)</code>: None, Manual, Pre, Post</li>
 *     <li><code>@Outgoing @Incoming O method(I payload)</code>: None, Pre, Post</li>
 *     <li><code>@Outgoing @Incoming CompletionStage&lt;Message&lt;O&gt;&gt; method(Message&lt;I&gt; msg)</code>: None,
 *     Manual, Pre, Post</li>
 *     <li><code>@Outgoing @Incoming CompletionStage&lt;O&gt; method(I msg)</code>: None, Pre, Post</li>
 *
 *     <li><code>@Outgoing @Incoming Publisher&lt;Message&lt;O&gt;&gt; method(Publisher&lt;Message&lt;I&gt;&gt; pub)
 *     </code>: None, Manual, Pre</li>
 *     <li><code>@Outgoing @Incoming Publisher&lt;O&gt; method(Publisher&lt;I&gt; pub)</code>: None, Pre</li>
 *     <li><code>@Outgoing @Incoming PublisherBuilder&lt;Message&lt;O&gt;&gt; method(PublisherBuilder&lt;Message&lt;I&gt;&gt; pub)
 *     </code>: None, Manual, Pre</li>
 *     <li><code>@Outgoing @Incoming PublisherBuilder&lt;O&gt; method(PublisherBuilder&lt;I&gt; pub)</code>: None, Pre
 *     </li>
 * </ul>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Acknowledgment {

  enum Strategy {
    /**
     * No acknowledgment performed.
     */
    NONE,

    /**
     * Acknowledgment managed by the user code. No automatic acknowledgment is performed. This strategy is only
     * supported by methods consuming {@link Message} instances.
     */
    MANUAL,

    /**
     * Acknowledgment performed automatically before the processing of the message by the user code.
     */
    PRE_PROCESSING,

    /**
     * Acknowledgment performed automatically after the user processing of the message.
     *
     * Notice that this mode is not supported for all signatures. check the list above.
     * When supported it's the default policy.
     *
     */
    POST_PROCESSING
  }

    /**
     * @return the acknowledgement policy.
     */
  Strategy value();

}
