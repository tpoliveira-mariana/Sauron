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
		display(SpotterApp.class.getSimpleName());
		
		// receive and print arguments
		display("Received " + args.length + " arguments");
		for (int i = 0; i < args.length; i++) {
			display("arg[" + i + "] = " + args[i]);
		}

		if (args.length > 4 || args.length < 2) {
			display("Invalid Number of arguments!");
			display("Usage: java " + SpotterApp.class.getSimpleName() + " zkhost zkport [replicaNum] [replicaInstance]");
			return;
		}
		try {
			int replicaNum = args.length >= 3  && args[2] != null ? Integer.parseInt(args[2]) : 1;
			int instance = (args.length == 4 && args[3] != null ? Integer.parseInt(args[3]) : -1);
			_frontend = new SiloFrontend(args[0], args[1], instance, replicaNum);
			waitInput();
		} catch (NumberFormatException e) {
			display("Invalid number provided.");
		} catch (ZKNamingException e){
			display("ZooKeeper could not start.");
		} catch (SauronException e) {
			display("No Silo replicas found.");
		}
	}

	private static void waitInput() {
		display("Type <help> for usage");
		try (Scanner scanner = new Scanner(System.in)) {
			System.out.print("Insert command -> ");
			System.out.flush();
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
					case "info":
						infoCommand(arguments);
						break;
					case "exit":
						if (arguments.length != 1) {
							displayCommandUsage("exit");
							break;
						}
						display("Exiting!");
						return;
					case "help":
						if (arguments.length != 1) {
							displayCommandUsage("help");
							break;
						}
						displayHelp();
						break;
					default:
						display("Invalid Command! Type <help> to display available commands");
						break;
				}
				System.out.print("Insert command -> ");
				System.out.flush();
			}
		}
	}

	private static void infoCommand(String[] arguments) {
		if (arguments.length != 2) {
			display("Invalid usage of info - Invalid number of arguments!");
			displayCommandUsage("info");
			return;
		}
		try {
			CamInfoResponse response = _frontend.camInfo(arguments[1]);
			display(arguments[1] + "," + response.getCoordinates().getLatitude() + "," + response.getCoordinates().getLongitude());
		} catch(SauronException e) {
			display("Invalid usage of info - " + reactToException(e));
		}

	}

	private static void spotCommand(String[] arguments) {
		if (arguments.length != 3) {
			display("Invalid usage of spot - Too many arguments!");
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
			display("Success!");
		} catch(SauronException e) {
			display("Invalid usage of spot - " + reactToException(e));
		}
	}


	private static void trailCommand(String[] arguments) {
		if (arguments.length != 3) {
			display("Invalid usage of trail - Too many arguments!");
			displayCommandUsage("trail");
			return;
		}
		try {
			TraceResponse response =  _frontend.trace(arguments[1], arguments[2]);
			printResult(response.getObservationsList().stream()
					.map(SpotterApp::printObservation)
					.collect(Collectors.toList()));
			display("Success!");
		} catch(SauronException e){
			display("Invalid usage of trail - " + reactToException(e));
		}
	}

	private static void pingCommand(String[] arguments) {
		if (arguments.length != 1) {
			display("Invalid usage of ping - Too many arguments!");
			displayCommandUsage("ping");
			return;
		}
		try {
			String response = _frontend.ctrlPing("spotter");
			display(response);
		} catch(SauronException e){
			display("Invalid usage of ping - " + reactToException(e));
		}
	}

	private static void clearCommand(String[] arguments) {
		if (arguments.length != 1) {
			display("Invalid usage of clear - Too many arguments!");
			displayCommandUsage("clear");
			return;
		}
		try {
			_frontend.ctrlClear();
			display("Success!");
		} catch(SauronException e){
			display("Invalid usage of clear - " + reactToException(e));
		}
	}

	private static void initCommand(String[] arguments) {
		if (arguments.length != 2) {
			display("Invalid usage of init - Too many arguments!");
			displayCommandUsage("init");
			return;
		}
		String filePath =  System.getProperty("user.dir") + "/" + arguments[1];
		try{
			_frontend.ctrlInit(filePath);
			display("Success!");
		} catch(SauronException e){
			display("Invalid usage of init - " + reactToException(e));
		}
	}

	private static void displayHelp() {
		display("\t\t-=+=-");
		display("info  - gets the info for the camera with name [camName]");
		display("\tUsage: info [camName]");
		display("spot  - gets the last observation of the object of type [type] and id [id]");
		display("      - id can contain * meaning any match, for example 1*2 means any number starting with 1 and ending with 2");
		display("\tUsage: spot [ObjectType] [ObjectId]");
		display("trail - gets all the observation of the object of type [type] and exact id [id]");
		display("\tUsage: trail [ObjectType] [ObjectId]");
		display("ping  - checks status of the server");
		display("\tUsage: ping");
		display("clear - clears the server");
		display("\tUsage: clear");
		display("init  - import parameters to start the server from a file");
		display("\tUsage: init [file]");
		display("exit  - leaves the program");
		display("\tUsage: exit");
		display("help  - show program usage");
		display("\tUsage: help");
		display("\t\t-=+=-");
	}

	private static void printResult(List<String> output) {
		output.forEach(SpotterApp::display);
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
			case INVALID_CAM_NAME:
				return "Invalid camera name";
			case CAMERA_NOT_FOUND:
				return "Non existing camera";
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
			case "info":
				display("Usage: info [camName]");
				break;
			case "spot":
				display("Usage: spot [ObjectType] [ObjectId]");
				break;
			case "trail":
				display("Usage: trail [ObjectType] [ObjectId]");
				break;
			case "ping":
				display("Usage: ping");
				break;
			case "clear":
				display("Usage: clear");
				break;
			case "init":
				display("Usage: init [file]");
				break;
			case "exit":
				display("Usage: exit");
				break;
			case "help":
				display("Usage: help");
		}
	}

	private static void display(String msg) {
		System.out.println(msg);
	}

}
