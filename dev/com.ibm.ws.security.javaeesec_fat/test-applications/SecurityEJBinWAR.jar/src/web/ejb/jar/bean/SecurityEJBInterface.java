package web.ejb.jar.bean;

/**
 * Interface for Enterprise Bean
 */
public interface SecurityEJBInterface {

    public abstract String denyAll();

    public abstract String denyAll(String input);

    public abstract String permitAll();

    public abstract String permitAll(String input);

    public abstract String checkAuthenticated();

    public abstract String permitAuthenticated();

    public abstract String manager();

    public abstract String manager(String input);

    public abstract String employee();

    public abstract String employee(String input);

    public abstract String employeeAndManager();

    public abstract String employeeAndManager(String input);

    public abstract String employeeAndManager(int i);

    public abstract String employeeAndManager(String i1, String i2);

    public abstract String declareRoles01();

    public abstract String runAsClient();

    public abstract String runAsSpecified();

}