package nio.asyn;

public class AddProcessor {

	public void process(String input) {
		if (input != null) {
			String[] elements = input.split(",");
			if (elements.length != 2) {
				System.out
						.println("sorry, input expression error! right format:A+B");
				return;
			}
			double A = Double.parseDouble(elements[0]);
			double B = Double.parseDouble(elements[1]);

			System.out.println(A+"+"+B+"="+(A+B));
		} else {
			System.out.println("no input");
		}

	}
}
