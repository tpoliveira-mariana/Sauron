package pt.tecnico.sauron.silo.client;


import pt.tecnico.sauron.exceptions.SauronException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class SiloClientApp {
	
	public static void main(String[] args) {
		System.out.println(SiloClientApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if (args.length > 4 || args.length < 2) {
			System.out.println("Invalid Number of arguments!");
			System.out.printf("Usage: java %s host port [replicaNum] [instance] [message]%n", SiloClientApp.class.getSimpleName());
			return;
		}

		try {
			String msg = "SiloClientApp";
			int replicaNum = args.length >= 3 && args[2] != null ? Integer.parseInt(args[2]) : 1;
			int instance = (args.length == 4 && args[3] != null ? Integer.parseInt(args[3]) : -1);
			SiloFrontend frontend = new SiloFrontend(args[0], args[1], instance, replicaNum);

			String response = frontend.ctrlPing(msg);
			System.out.println(response);
		} catch (SauronException e) {
			System.out.println("Invalid message provided.");
		} catch (NumberFormatException e) {
			System.out.println("Invalid number provided.");
		} catch (ZKNamingException e) {
			System.out.println("Zookeeper error.");
		}

	}
	
}
