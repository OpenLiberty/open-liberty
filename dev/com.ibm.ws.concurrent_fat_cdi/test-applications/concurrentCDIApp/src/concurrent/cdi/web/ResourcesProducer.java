package concurrent.cdi.web;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class ResourcesProducer {

    @PostConstruct
    public void init() {
        System.out.println("Initialized bean: " + this);
    }

    @Resource(lookup = "concurrent/sampleExecutor")
    @Produces
    private ManagedExecutorService exec;

}
