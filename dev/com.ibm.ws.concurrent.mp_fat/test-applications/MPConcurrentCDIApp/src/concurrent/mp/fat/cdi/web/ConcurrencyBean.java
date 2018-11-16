package concurrent.mp.fat.cdi.web;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ManagedExecutorConfig;

@ApplicationScoped
public class ConcurrencyBean {

    @Inject
    ManagedExecutor noAnno;

    @Inject
    @ManagedExecutorConfig
    ManagedExecutor defaultAnno;

    @Inject
    @ManagedExecutorConfig(maxAsync = 5)
    ManagedExecutor maxAsync5;

    public ManagedExecutor getNoAnno() {
        return noAnno;
    }

    public ManagedExecutor getDefaultAnno() {
        return defaultAnno;
    }

    public ManagedExecutor getMaxAsync5() {
        return maxAsync5;
    }

}
