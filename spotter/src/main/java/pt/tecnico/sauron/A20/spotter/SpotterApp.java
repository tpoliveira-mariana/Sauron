package pt.tecnico.sauron.A20.spotter;


import pt.tecnico.sauron.A20.silo.client.SiloFrontend;

import java.util.Scanner;

public class SpotterApp {
	private static String PERSON = "person";
	private static String CAR = "car";
	
	public static void main(String[] args) {
		System.out.println(SpotterApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if (args.length != 2) {
			System.out.println("Invalid Number of arguments!");
			System.out.printf("Usage: java %s host port%n", SpotterApp.class.getSimpleName());
			return;
		}

		final String host = args[0];
		final int port = Integer.parseInt(args[1]);
		final String target = host + ":" + port;

		waitInput(target);

	}

	private static void waitInput(String target) {
		SiloFrontend frontend = new SiloFrontend();
		System.out.println("Type <help> for usage");
		try (Scanner scanner = new Scanner(System.in)) {
			while(scanner.hasNextLine()){
				boolean status;
				String command = scanner.nextLine();
				String arguments[] = command.split(" ");
				switch(arguments[0]){
					case "spot":
						status = spotCommand(frontend,target, arguments);
						break;
					case "trail":
						status = trailCommand(frontend,target, arguments);
						break;
					case "ping":
						status = pingCommand(frontend,target, arguments);
						break;
					case "clear":
						status = clearCommand(frontend,target, arguments);
						break;
					case "init":
						status = initCommand(frontend,target, arguments);
						break;
					case "exit":
						if (arguments.length != 1) {
							status = false;
							break;
						}
						System.out.println("Exiting!");
						return;
					case "help":
					default:
						System.out.println("Invalid Usage!");
						status = displayHelp();
						break;
				}
				if (!status)
					System.out.println("Invalid Usage!");
			}
		}
	}

	private static boolean spotCommand(SiloFrontend frontend, String target, String[] arguments) {
		//TODO- catch exceptions
		if (arguments.length != 3)
			return false;
		if (!checkObjectArguments(arguments[1], arguments[2], true))
			return false;
		if (!arguments[2].contains("*"))
			frontend.track(target, arguments[0], arguments[1]);
		else
			frontend.trackMatch(target, arguments[0], arguments[1]);
		return true;
	}

	private static boolean trailCommand(SiloFrontend frontend, String target, String[] arguments) {
		if (arguments.length != 3 || !checkObjectArguments(arguments[1], arguments[2], false))
			return false;
		frontend.trace(target, arguments[0], arguments[1]);
		return true;
	}

	private static boolean pingCommand(SiloFrontend frontend, String target, String[] arguments) {
		//TODO- catch exceptions and create ping
		if (arguments.length != 1)
			return false;
		//frontend.ping(target);
		return true;
	}

	private static boolean clearCommand(SiloFrontend frontend, String target, String[] arguments) {
		//TODO- catch exceptions and create clear
		if (arguments.length != 1)
			return false;
		//frontend.clear(target);
		return true;
	}

	private static boolean initCommand(SiloFrontend frontend, String target, String[] arguments) {
		//TODO-Call frontend and create init
		//frontend.init(target);
		return true;
	}

	private static boolean displayHelp() {
		//TODO-print help messages
		System.out.println("Usage!");
		return true;
	}

	private static boolean checkObjectArguments(String type, String id, boolean regex) {
		if (!type.equals(CAR)  && !type.equals(PERSON)) {
			System.out.println("Invalid object type provided.");
			return false;
		}
		else if ((type.equals(CAR) && !checkCarId(id, regex)) || (type.equals(PERSON) && !checkPersonId(id, regex))) {
			System.out.println(String.format("Invalid %s identifier provided.", type));
			return false;
		}

		return true;
	}

	private static boolean checkCarId(String id, boolean regex) {
		int numFields = 0;
		if (!id.contains("*")) { //check if string contains *
			if (id.length() != 6)
				return false;

			for (int i = 0; i < 3; i++) {
				char firstChar = id.charAt(2 * i);
				char secChar = id.charAt(2 * i + 1);

				if (Character.isDigit(firstChar) && Character.isDigit(secChar))
					numFields++;

				else if (!(Character.isUpperCase(firstChar) && Character.isUpperCase(secChar)))
					return false;
			}
			return numFields != 3 && numFields != 0;
		}
		else{
			return regex && id.length() <= 6 && !id.matches(".*[*][*]+.*");
		}

	}

	private static boolean checkPersonId(String id, boolean regex) {
		try{
			if (!id.matches("[0-9*]+") || id.matches(".*[*][*]+.*") || (id.contains("*") && !regex))
				return false; //check if id doesn't match a number or if it doesn't have sequenced * or if it has * and regex is not possible
			if (!id.contains("*")) { //check if string doesn't contain *
				Long.parseLong(id);
				return Long.parseLong(id) > 0;
			}
		}
		catch(NumberFormatException e){
			return false;
		}
		return true;
	}

}
