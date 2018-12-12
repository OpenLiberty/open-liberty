package concurrent.mp.fat.cdi.web;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.NamedInstance;

@ApplicationScoped
public class ConcurrencyBean {

    @Inject
    ManagedExecutor noAnno;

    @Inject
    @NamedInstance("defaultAnno")
    ManagedExecutor defaultAnno;

    @Inject
    @NamedInstance("maxAsync5")
    ManagedExecutor maxAsync5;

    @Inject
    @NamedInstance("producerDefined")
    ManagedExecutor producerDefined;

    public ManagedExecutor getNoAnno() {
        return noAnno;
    }

    public ManagedExecutor getDefaultAnno() {
        return defaultAnno;
    }

    public ManagedExecutor getMaxAsync5() {
        return maxAsync5;
    }

    public ManagedExecutor getProducerDefined() {
        return producerDefined;
    }

    @Produces
    @ApplicationScoped
    @NamedInstance("producerDefined")
    public ManagedExecutor createExec() {
        ManagedExecutor exec = ManagedExecutor.builder().maxAsync(5).build();
        System.out.println("Application produced ManagedExecutor: " + exec);
        return exec;
    }

}
