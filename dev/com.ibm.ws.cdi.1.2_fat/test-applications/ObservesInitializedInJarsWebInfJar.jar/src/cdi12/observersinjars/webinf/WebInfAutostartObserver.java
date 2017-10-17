package cdi12.observersinjars.webinf;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class WebInfAutostartObserver {

    private boolean onInitCalled = false;

    public boolean methodCalled() {
        return onInitCalled;
    }

    public void onInitialize(@Observes @Initialized(ApplicationScoped.class) Object o) {
        onInitCalled = true;
    }

}