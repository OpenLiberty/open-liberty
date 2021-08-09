package concurrent.mp.fat.cdi.web;

public abstract class AbstractBean {

    public static String UNINITIALIZED = "UNINITIALIZED";

    private String currentState = UNINITIALIZED;

    public void setState(String state) {
        this.currentState = state;
    }

    public String getState() {
        return this.currentState;
    }

}
