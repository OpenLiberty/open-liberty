/*******************************************************************************
 * Copyright (c) 2018, 2022 Contributors to the Eclipse Foundation
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
 ******************************************************************************/

package org.eclipse.microprofile.reactive.streams.operators.tck.spi;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.LongStream;

import static org.testng.Assert.assertEquals;

/**
 * 
 * This class is a copy of the same class name OnErrorResumeStageVerification and package name 
 * org.eclipse.microprofile.reactive.streams.operators.tck.spi of eclipse project 
 * microprofile-reactive-streams-operators. The class has however been slightly modified to 
 * override the required_spec109_mustIssueOnSubscribeForNonNullSubscriber() testcase. This test
 * case is implemented in the classes PublisherVerification and IdentityProcessorVerification
 * 
 */
public class OnErrorResumeStageVerification extends AbstractStageVerification {

  OnErrorResumeStageVerification(ReactiveStreamsSpiVerification.VerificationDeps deps) {
    super(deps);
  }

  @Test
  public void onErrorResumeShouldCatchErrorFromSource() {
    AtomicReference<Throwable> exception = new AtomicReference<>();
    assertEquals(await(rs.failed(new QuietRuntimeException("failed"))
        .onErrorResume(err -> {
          exception.set(err);
          return "foo";
        })
        .toList()
        .run(getEngine())), Collections.singletonList("foo"));
    assertEquals(exception.get().getMessage(), "failed");
  }

  @Test
  public void onErrorResumeWithShouldCatchErrorFromSource() {
    AtomicReference<Throwable> exception = new AtomicReference<>();
    assertEquals(await(rs.failed(new QuietRuntimeException("failed"))
      .onErrorResumeWith(err -> {
        exception.set(err);
        return rs.of("foo", "bar");
      })
      .toList()
      .run(getEngine())), Arrays.asList("foo", "bar"));
    assertEquals(exception.get().getMessage(), "failed");
  }

  @Test
  public void onErrorResumeWithRsPublisherShouldCatchErrorFromSource() {
    AtomicReference<Throwable> exception = new AtomicReference<>();
    assertEquals(await(rs.failed(new QuietRuntimeException("failed"))
      .onErrorResumeWithRsPublisher(err -> {
        exception.set(err);
        return rs.of("foo", "bar").buildRs(getEngine());
      })
      .toList()
      .run(getEngine())), Arrays.asList("foo", "bar"));
    assertEquals(exception.get().getMessage(), "failed");
  }

  @Test
  public void onErrorResumeShouldCatchErrorFromStage() {
    AtomicReference<Throwable> exception = new AtomicReference<>();
    assertEquals(await(rs.of("a", "b", "c")
      .map(word -> {
        if (word.equals("b")) {
          throw new QuietRuntimeException("failed");
        }
        return word.toUpperCase();
      })
      .onErrorResume(err -> {
        exception.set(err);
        return "foo";
      })
      .toList()
      .run(getEngine())), Arrays.asList("A", "foo"));
    assertEquals(exception.get().getMessage(), "failed");
  }

  @Test
  public void onErrorResumeWithShouldCatchErrorFromStage() {
    AtomicReference<Throwable> exception = new AtomicReference<>();
    assertEquals(await(rs.of("a", "b", "c")
      .map(word -> {
        if (word.equals("b")) {
          throw new QuietRuntimeException("failed");
        }
        return word.toUpperCase();
      })
      .onErrorResumeWith(err -> {
        exception.set(err);
        return rs.of("foo", "bar");
      })
      .toList()
      .run(getEngine())), Arrays.asList("A", "foo", "bar"));
    assertEquals(exception.get().getMessage(), "failed");
  }

  @Test
  public void onErrorResumeWithRsPublisherShouldCatchErrorFromStage() {
    AtomicReference<Throwable> exception = new AtomicReference<>();
    assertEquals(await(rs.of("a", "b", "c")
      .map(word -> {
        if (word.equals("b")) {
          throw new QuietRuntimeException("failed");
        }
        return word.toUpperCase();
      })
      .onErrorResumeWithRsPublisher(err -> {
        exception.set(err);
        return rs.of("foo", "bar").buildRs(getEngine());
      })
      .toList()
      .run(getEngine())), Arrays.asList("A", "foo", "bar"));
    assertEquals(exception.get().getMessage(), "failed");
  }


  @Test(expectedExceptions = RuntimeException.class)
  public void onErrorResumeStageShouldPropagateRuntimeExceptions() {
    await(rs.failed(new Exception("source-failure"))
        .onErrorResume(t -> {
          throw new QuietRuntimeException("failed");
        })
        .toList()
        .run(getEngine()));
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void onErrorResumeWithStageShouldPropagateRuntimeExceptions() {
    await(rs.failed(new Exception("source-failure"))
      .onErrorResumeWith(t -> {
        throw new QuietRuntimeException("failed");
      })
      .toList()
      .run(getEngine()));
  }

  @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*failed.*")
  public void onErrorResumeWithRsPublisherStageShouldPropagateRuntimeExceptions() {
    await(rs.failed(new QuietRuntimeException("source-failure"))
      .onErrorResumeWithRsPublisher(t -> {
        throw new QuietRuntimeException("failed");
      })
      .toList()
      .run(getEngine()));
  }

  @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = ".*boom.*")
  public void onErrorResumeWithShouldBeAbleToInjectAFailure() {
    await(rs.failed(new QuietRuntimeException("failed"))
      .onErrorResumeWith(err -> rs.failed(new QuietRuntimeException("boom")))
      .toList()
      .run(getEngine()));
  }

  @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*boom.*")
  public void onErrorResumeWithRsPublisherShouldBeAbleToInjectAFailure() {
    await(rs.failed(new QuietRuntimeException("failed"))
      .onErrorResumeWithRsPublisher(err -> rs.failed(new QuietRuntimeException("boom")).buildRs(getEngine()))
      .toList()
      .run(getEngine()));
  }

    @Override
    List<Object> reactiveStreamsTckVerifiers() {
        return Arrays.asList(
            new OnErrorResumeWithVerification(),
            new OnErrorResumeVerification(),
            new OnErrorResumeWithPublisherVerification()
        );
    }

    public class OnErrorResumeWithVerification extends StageProcessorVerification<Integer> {

        @Override
        public Processor<Integer, Integer> createIdentityProcessor(int bufferSize) {
            return rs.<Integer>builder()
                .onErrorResumeWith(rs::failed)
                .map(Function.identity()).buildRs(getEngine());
        }

        @Override
        public Publisher<Integer> createFailedPublisher() {
            return rs.<Integer>failed(new RuntimeException("failed"))
                .onErrorResumeWith(t -> {
                    // Re-throw the exception.
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    }
                    // Wrap if required.
                    throw new RuntimeException(t);
                })
                .buildRs(getEngine());
        }

        @Override
        public Integer createElement(int element) {
            return element;
        }
        
        /**
         * 
         * Liberty Change:
         * Disable this test due to very infrequent intermittent failures
         * See defect 286531
         * 
         */
        @Override
        @Test
        public void required_spec109_mustIssueOnSubscribeForNonNullSubscriber() {
            System.out.println("Not running required_spec109_mustIssueOnSubscribeForNonNullSubscriber");
        }
    }

    public class OnErrorResumeVerification extends StageProcessorVerification<Integer> {

        @Override
        public Processor<Integer, Integer> createIdentityProcessor(int bufferSize) {
            return rs.<Integer>builder()
                .onErrorResume(t -> {
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    }
                    throw new RuntimeException(t);
                })
                .map(Function.identity()).buildRs(getEngine());
        }

        @Override
        public Publisher<Integer> createFailedPublisher() {
            return rs.<Integer>failed(new RuntimeException("failed"))
                .onErrorResume(t -> {
                    // Re-throw the exception.
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    }
                    // Wrap if required.
                    throw new RuntimeException(t);
                })
                .buildRs(getEngine());
        }

        @Override
        public Integer createElement(int element) {
            return element;
        }

    }

    public class OnErrorResumeWithPublisherVerification extends StagePublisherVerification<Long> {
        @Override
        public Publisher<Long> createPublisher(long elements) {
            return
                rs.<Long>failed(new Exception("BOOM"))
                    .onErrorResumeWith(
                        t -> rs.fromIterable(() -> LongStream.rangeClosed(1, elements).boxed().iterator())
                    )
                    .buildRs(getEngine());
        }
    }


}