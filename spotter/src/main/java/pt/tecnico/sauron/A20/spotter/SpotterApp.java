package pt.tecnico.sauron.A20.spotter;


import pt.tecnico.sauron.A20.exceptions.SauronException;
import pt.tecnico.sauron.A20.silo.client.SiloFrontend;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SpotterApp {

	private static SiloFrontend _frontend;

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
		try {
			_frontend = new SiloFrontend(args[0], args[1]);
		} catch (NumberFormatException e) {
			System.out.println("Invalid number provided.");
		}

		waitInput();

	}

	private static void waitInput() {
		System.out.println("Type <help> for usage");
		try (Scanner scanner = new Scanner(System.in)) {
			while(scanner.hasNextLine()){
				String command = scanner.nextLine();
				String[] arguments = command.split(" ");
				switch(arguments[0]){
					case "spot":
						spotCommand(arguments);
						break;
					case "trail":
						trailCommand(arguments);
						break;
					case "ping":
						pingCommand(arguments);
						break;
					case "clear":
						clearCommand(arguments);
						break;
					case "init":
						initCommand(arguments);
						break;
					case "exit":
						if (arguments.length != 1) {
							displayCommandUsage("exit");
							break;
						}
						System.out.println("Exiting!");
						return;
					case "help":
						if (arguments.length != 1) {
							displayCommandUsage("help");
							break;
						}
						displayHelp();
						break;
					default:
						System.out.println("Invalid Command! Type <help> to display available commands");
						break;
				}
			}
		}
	}

	private static void spotCommand(String[] arguments) {
		if (arguments.length != 3 || !checkObjectArguments(arguments[1], arguments[2], true)) {
			displayCommandUsage("spot");
			return;
		}
		try {
			List<String> output = new ArrayList<>();
			if (!arguments[2].contains("*"))
				output.add(_frontend.track(arguments[1], arguments[2]));
			else
				output  = _frontend.trackMatch(arguments[1], arguments[2]);
			printResult(output);
			System.out.println("Success!");
		} catch(SauronException e) {
			System.out.println("Invalid usage of spot - " + reactToException(e));
			displayCommandUsage("spot");
		}
	}


	private static void trailCommand(String[] arguments) {
		if (arguments.length != 3 || !checkObjectArguments(arguments[1], arguments[2], false)) {
			displayCommandUsage("trail");
			return;
		}
		try {
			List<String> output  = _frontend.trace(arguments[1], arguments[2]);
			printResult(output);
			System.out.println("Success!");
		} catch(SauronException e){
			System.out.println("Invalid usage of trail - " + reactToException(e));
			displayCommandUsage("trail");
		}
	}

	private static void pingCommand(String[] arguments) {
		if (arguments.length != 1) {
			displayCommandUsage("ping");
			return;
		}
		try {
			String response = _frontend.ctrlPing("spotter!");
			System.out.println(response);
		} catch(SauronException e){
			System.out.println("Invalid usage of ping - " + reactToException(e));
			displayCommandUsage("ping");
		}
	}

	private static void clearCommand(String[] arguments) {
		if (arguments.length != 1) {
			displayCommandUsage("clear");
			return;
		}
		try {
			_frontend.ctrlClear();
			System.out.println("Success!");
		} catch(SauronException e){
			System.out.println("Invalid usage of clear - " + reactToException(e));
			displayCommandUsage("clear");
		}
	}

	private static void initCommand(String[] arguments) {
		if (arguments.length != 2) {
			displayCommandUsage("init");
			return;
		}
		String filePath =  System.getProperty("user.dir") + "/" + arguments[1];
		try{
			_frontend.ctrlInit(filePath);
			System.out.println("Success!");
		} catch(SauronException e){
			System.out.println("Invalid usage of init - " + reactToException(e));
			displayCommandUsage("init");
		}
	}

	private static void displayHelp() {
		System.out.println("\t\t-=+=-");
		System.out.println("spot  - gets the last observation of the object of type <type> and id <id>");
		System.out.println("      - id can contain * meaning any match, for example 1*2 means any number starting with 1 and ending with 2");
		System.out.println("\tUsage: spot <ObjectType> <ObjectId>");
		System.out.println("trail - gets all the observation of the object of type <type> and exact id <id>");
		System.out.println("\tUsage: trail <ObjectType> <ObjectId>");
		System.out.println("ping  - checks status of the server");
		System.out.println("\tUsage: ping");
		System.out.println("clear - clears the server");
		System.out.println("\tUsage: clear");
		System.out.println("init  - import parameters to start the server from a file");
		System.out.println("\tUsage: init <filename>");
		System.out.println("exit  - leaves the program");
		System.out.println("\tUsage: exit");
		System.out.println("help  - show program usage");
		System.out.println("\tUsage: help");
		System.out.println("\t\t-=+=-");
	}

	private static boolean checkObjectArguments(String type, String id, boolean regex) {
		String PERSON = "person";
		String CAR = "car";
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

	private static void printResult(List<String> output) {
		output.forEach(System.out::println);
	}

	private static String reactToException(SauronException e) {
		switch(e.getErrorMessage()) {
			case OBJECT_NOT_FOUND:
				return "Object does not exists";
			case INVALID_PERSON_IDENTIFIER:
				return "Invalid person ID given";
			case INVALID_CAR_ID:
				return "Invalid car ID given";
			case INVALID_ID:
				return "Invalid ID given";
			case TYPE_DOES_NOT_EXIST:
				return "Invalid type given";
			case ERROR_PROCESSING_FILE:
				return "Error processing the file";
			case INVALID_ARGUMENT:
				return "Invalid argument";
			default:
				return "An error occurred";
		}
	}

	private static void displayCommandUsage(String command) {
		switch (command){
			case "spot":
				System.out.println("Usage: spot <ObjectType> <ObjectId>");
				break;
			case "trail":
				System.out.println("Usage: trail <ObjectType> <ObjectId>");
				break;
			case "ping":
				System.out.println("Usage: ping");
				break;
			case "clear":
				System.out.println("Usage: clear");
				break;
			case "init":
				System.out.println("Usage: init <filename>");
				break;
			case "exit":
				System.out.println("Usage: exit");
				break;
			case "help":
				System.out.println("Usage: help");
		}
	}

}
