package com.ibm.ws.cdi12.test.ejb.timer;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;

@SessionScoped
public class SessionScopedCounter implements Serializable {
    private static final long serialVersionUID = 1L;
    private final AtomicInteger counter = new AtomicInteger();
    private static final AtomicReference<String> stackRef = new AtomicReference<String>();

    public int get() {
        return counter.get();
    }

    public void increment() {
        counter.incrementAndGet();
    }

    public String getStack() {
        return stackRef.get();
    }

    @PostConstruct
    public void postConstruct() {
        System.out.println(String.format("%s@%08x created", this.getClass().getSimpleName(), System.identityHashCode(this)));
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println(String.format("%s@%08x destroyed", this.getClass().getSimpleName(), System.identityHashCode(this)));
        if (stackRef.get() == null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            (new Exception("Capturing stack")).printStackTrace(pw);
            pw.flush();
            stackRef.compareAndSet(null, sw.toString());
        }
    }
}
