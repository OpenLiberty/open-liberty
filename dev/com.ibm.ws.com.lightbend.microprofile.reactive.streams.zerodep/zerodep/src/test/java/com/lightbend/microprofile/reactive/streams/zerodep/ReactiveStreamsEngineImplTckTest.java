/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import org.eclipse.microprofile.reactive.streams.tck.*;
import org.reactivestreams.tck.TestEnvironment;

public class ReactiveStreamsEngineImplTckTest extends ReactiveStreamsTck<ReactiveStreamsEngineImpl> {

  public ReactiveStreamsEngineImplTckTest() {
    super(new TestEnvironment(100));
  }

  @Override
  protected ReactiveStreamsEngineImpl createEngine() {
    return new ReactiveStreamsEngineImpl();
  }
}
