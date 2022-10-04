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
 * Test cases for OnErrorResume stages. This includes the
 * {@link org.eclipse.microprofile.reactive.streams.operators.spi.Stage.OnErrorResume} and
 * {@link org.eclipse.microprofile.reactive.streams.operators.spi.Stage.OnErrorResumeWith} stages.
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
        
        @Override
        @Test
        public void required_spec109_mustIssueOnSubscribeForNonNullSubscriber() {
            System.out.println("The test case that is failing is called required_spec109_mustIssueOnSubscribeForNonNullSubscriber");
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