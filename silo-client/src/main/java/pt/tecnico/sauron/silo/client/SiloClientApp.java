package pt.tecnico.sauron.silo.client;


import pt.tecnico.sauron.exceptions.SauronException;

public class SiloClientApp {
	
	public static void main(String[] args) {
		System.out.println(SiloClientApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if (args.length != 2) {
			System.out.println("Invalid Number of arguments!");
			System.out.printf("Usage: java %s host port [message]%n", SiloClientApp.class.getSimpleName());
			return;
		}

		try {
			String msg = "SiloClientApp";
			SiloFrontend frontend = new SiloFrontend(args[0], args[1]);

			String response = frontend.ctrlPing(msg);
			System.out.println(response);
		} catch (SauronException e) {
			System.out.println("Invalid message provided.");
		} catch (NumberFormatException e) {
			System.out.println("Invalid number provided.");
		}

	}
	
}
