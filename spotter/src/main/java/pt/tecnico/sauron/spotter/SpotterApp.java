package pt.tecnico.sauron.spotter;


import com.google.protobuf.util.Timestamps;
import pt.tecnico.sauron.exceptions.SauronException;
import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.tecnico.sauron.silo.grpc.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class SpotterApp {

	private static SiloFrontend _frontend;

	public static void main(String[] args) {
		System.out.println(SpotterApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if (args.length > 3 || args.length < 2) {
			System.out.println("Invalid Number of arguments!");
			System.out.printf("Usage: java %s host port%n", SpotterApp.class.getSimpleName());
			return;
		}
		try {
			int instance = (args.length == 3 && args[2] != null ? Integer.parseInt(args[2]) : -1);
			_frontend = new SiloFrontend(args[0], args[1], instance);
			waitInput();
		} catch (NumberFormatException e) {
			System.out.println("Invalid number provided.");
		} catch (ZKNamingException e){
			System.out.println("Zookeper could not start.");
		} catch (SauronException e) {
			System.out.println("No Silo replicas found.");
		}
	}

	private static void waitInput() {
		System.out.println("Type <help> for usage");
		try (Scanner scanner = new Scanner(System.in)) {
			System.out.print("Insert command ->");
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
				System.out.print("Insert command ->");
			}
		}
	}

	private static void spotCommand(String[] arguments) {
		if (arguments.length != 3) {
			System.out.println("Invalid usage of spot - Too many arguments!");
			displayCommandUsage("spot");
			return;
		}
		try {
			List<String> output = new ArrayList<>();
			if (!arguments[2].contains("*")) {
				TrackResponse response = _frontend.track(arguments[1], arguments[2]);
				output.add(printObservation(response.getObservation()));
			}else {
				TrackMatchResponse response = _frontend.trackMatch(arguments[1], arguments[2]);
				output = response.getObservationsList()
						.stream()
						.sorted(getComparator(response))
						.map(SpotterApp::printObservation)
						.collect(Collectors.toList());
			}
			printResult(output);
			System.out.println("Success!");
		} catch(SauronException e) {
			System.out.println("Invalid usage of spot - " + reactToException(e));
		}
	}


	private static void trailCommand(String[] arguments) {
		if (arguments.length != 3) {
			System.out.println("Invalid usage of trail - Too many arguments!");
			displayCommandUsage("trail");
			return;
		}
		try {
			TraceResponse response =  _frontend.trace(arguments[1], arguments[2]);
			printResult(response.getObservationsList().stream()
					.map(SpotterApp::printObservation)
					.collect(Collectors.toList()));
			System.out.println("Success!");
		} catch(SauronException e){
			System.out.println("Invalid usage of trail - " + reactToException(e));
		}
	}

	private static void pingCommand(String[] arguments) {
		if (arguments.length != 1) {
			System.out.println("Invalid usage of ping - Too many arguments!");
			displayCommandUsage("ping");
			return;
		}
		try {
			String response = _frontend.ctrlPing("spotter");
			System.out.println(response);
		} catch(SauronException e){
			System.out.println("Invalid usage of ping - " + reactToException(e));
		}
	}

	private static void clearCommand(String[] arguments) {
		if (arguments.length != 1) {
			System.out.println("Invalid usage of clear - Too many arguments!");
			displayCommandUsage("clear");
			return;
		}
		try {
			_frontend.ctrlClear();
			System.out.println("Success!");
		} catch(SauronException e){
			System.out.println("Invalid usage of clear - " + reactToException(e));
		}
	}

	private static void initCommand(String[] arguments) {
		if (arguments.length != 2) {
			System.out.println("Invalid usage of init - Too many arguments!");
			displayCommandUsage("init");
			return;
		}
		String filePath =  System.getProperty("user.dir") + "/" + arguments[1];
		try{
			_frontend.ctrlInit(filePath);
			System.out.println("Success!");
		} catch(SauronException e){
			System.out.println("Invalid usage of init - " + reactToException(e));
		}
	}

	private static void displayHelp() {
		System.out.println("\t\t-=+=-");
		System.out.println("spot  - gets the last observation of the object of type [type] and id [id]");
		System.out.println("      - id can contain * meaning any match, for example 1*2 means any number starting with 1 and ending with 2");
		System.out.println("\tUsage: spot [ObjectType] [ObjectId]");
		System.out.println("trail - gets all the observation of the object of type [type] and exact id [id]");
		System.out.println("\tUsage: trail [ObjectType] [ObjectId]");
		System.out.println("ping  - checks status of the server");
		System.out.println("\tUsage: ping");
		System.out.println("clear - clears the server");
		System.out.println("\tUsage: clear");
		System.out.println("init  - import parameters to start the server from a file");
		System.out.println("\tUsage: init [file]");
		System.out.println("exit  - leaves the program");
		System.out.println("\tUsage: exit");
		System.out.println("help  - show program usage");
		System.out.println("\tUsage: help");
		System.out.println("\t\t-=+=-");
	}

	private static void printResult(List<String> output) {
		output.forEach(System.out::println);
	}

	private static Comparator<Observation> getComparator(TrackMatchResponse response) {
		ObjectType type = response.getObservationsCount() == 0 ?
				ObjectType.CAR : response.getObservations(0).getObject().getType();
		switch (type) {
			case PERSON:
				return Comparator.comparingLong(obs -> Long.parseLong(obs.getObject().getId()));
			case CAR:
				return Comparator.comparing(obs -> obs.getObject().getId());
			default:
				return Comparator.comparing(Observation::toString);
		}
	}

	private static String printObservation(Observation obs) {
		String ts = Timestamps.toString(obs.getTimestamp());
		return typeToString(obs.getObject().getType()) + ","
				+ obs.getObject().getId() + ","
				+ ts.substring(0, ts.lastIndexOf('.')) + ","
				+ obs.getCam().getName() + ","
				+ obs.getCam().getCoordinates().getLatitude() + ","
				+ obs.getCam().getCoordinates().getLongitude();
	}

	private static String typeToString(ObjectType type) {
		switch (type){
			case PERSON:
				return "person";
			case CAR:
				return "car";
			default:
				return "<UNRECOGNIZED>";
		}
	}

	private static String reactToException(SauronException e) {
		switch(e.getErrorMessage()) {
			case OBJECT_NOT_FOUND:
				return "No ID matches the one given!";
			case INVALID_PERSON_ID:
				return "Invalid person ID given!";
			case INVALID_CAR_ID:
				return "Invalid car ID given!";
			case INVALID_ID:
				return "Invalid ID given!";
			case TYPE_DOES_NOT_EXIST:
				return "Invalid type given!";
			case ERROR_PROCESSING_FILE:
				return "Error processing the file!";
			case INVALID_ARGUMENT:
				return "Invalid argument!";
			default:
				return "An error occurred!";
		}
	}

	private static void displayCommandUsage(String command) {
		switch (command){
			case "spot":
				System.out.println("Usage: spot [ObjectType] [ObjectId]");
				break;
			case "trail":
				System.out.println("Usage: trail [ObjectType] [ObjectId]");
				break;
			case "ping":
				System.out.println("Usage: ping");
				break;
			case "clear":
				System.out.println("Usage: clear");
				break;
			case "init":
				System.out.println("Usage: init [file]");
				break;
			case "exit":
				System.out.println("Usage: exit");
				break;
			case "help":
				System.out.println("Usage: help");
		}
	}

}
