package joist.web;

public class CurrentContext {

    private static final ThreadLocal<ClickContext> CURRENT = new ThreadLocal<ClickContext>();

    public static ClickContext get() {
        return CurrentContext.CURRENT.get();
    }

    public static void set(ClickContext context) {
        CurrentContext.CURRENT.set(context);
    }

    public static void addControl(Control control) {
        if (CurrentContext.get() == null) {
            return;
        }
        CurrentContext.get().addControl(control);
    }

}