package nioSample.nio.asyn;

public class AddProcessor {
    public void process(String input) {
        if (input == null) { System.out.println("no input"); return; }
        String[] elements = input.split(",", -1);
        if (elements.length != 2) { System.out.println("Expected two numbers: A,B"); return; }
        try {
            double a = Double.parseDouble(elements[0].trim());
            double b = Double.parseDouble(elements[1].trim());
            if (!Double.isFinite(a) || !Double.isFinite(b)) throw new NumberFormatException("Non-finite input");
            System.out.println(a + "+" + b + "=" + (a + b));
        } catch (NumberFormatException e) { System.out.println("Expected two numbers: A,B"); }
    }
}
