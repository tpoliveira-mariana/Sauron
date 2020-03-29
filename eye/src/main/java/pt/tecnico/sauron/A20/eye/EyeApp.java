package pt.tecnico.sauron.A20.eye;


import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import pt.tecnico.sauron.A20.exceptions.ErrorMessage;
import pt.tecnico.sauron.A20.exceptions.SauronException;
import pt.tecnico.sauron.A20.silo.client.SiloFrontend;

import static pt.tecnico.sauron.A20.exceptions.ErrorMessage.DUPLICATE_CAMERA;

public class EyeApp {
	private static String PERSON = "person";
	private static String CAR = "car";

	public static void main(String[] args) {
		System.out.println(EyeApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// check arguments
		if (args.length < 5) {
			System.out.println("Argument(s) missing!");
			System.out.printf("Usage: java eye host port cameraName latitude longitude%n");
			return ;
		}

		final String host = args[0];
		final int port = Integer.parseInt(args[1]);
		final String target = host + ":" + port;
		String cameraName = args[2];
		double lat, lon;

		try {
			lat = Double.parseDouble(args[3]);
			lon = Double.parseDouble(args[4]);
		}
		catch (Exception e) {
			System.out.println("Invalid double provided.");
			System.out.println("Shutting down...");
			return ;
		}

		SiloFrontend frontend = new SiloFrontend();

		if (!registerCamera(frontend, target, cameraName, lat, lon)) {
			System.out.println("Shutting down...");
			return;
		}


		try (Scanner scanner = new Scanner(System.in)) {
			List<List<String>> observations = new ArrayList<>();

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (handledSpecialLines(frontend, target, cameraName, line, observations)) {
					continue;
				}

				StringTokenizer st = new StringTokenizer(line, ",");
				if (st.countTokens() != 2) {
					System.out.println("Invalid command.");
					System.out.println("Skipping...");
					continue;
				}

				String objectType = st.nextToken();
				String objectId = st.nextToken();


				if (!checkObsArguments(objectType, objectId)) {
					System.out.println("Skipping...");
					continue;
				}

				List<String> obs = new ArrayList<>();
				obs.add(objectType);
				obs.add(objectId);
				observations.add(obs);
			}

			submitObservations(frontend, target, cameraName, observations);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

	private static boolean registerCamera(SiloFrontend frontend, String target, String name, double lat, double lon) {
		if (!name.matches("[A-Za-z0-9]+") || name.length() < 3 || name.length() > 15) {
			System.out.println("Invalid camera name provided.");
			return false;
		}
		else if (lat > 90 || lat < -90 || lon > 180 || lon < -180) {
			System.out.println("Invalid coordinates provided.");
			return false;
		}

		try {
			frontend.camJoin(target, name, lat, lon);
			return true;
		}
		catch (SauronException e) {
			if (e.getErrorMessage() == DUPLICATE_CAMERA) {
				System.out.println("Camera already registered. Processing observations...");
				return true;
			}
			else {
				System.out.println(e.getErrorMessageLabel());
				return false;
			}
		}
	}

	private static boolean handledSpecialLines(SiloFrontend frontend, String target, String camName, String line, List<List<String>> observations) {
		// blank line
		if (line.isBlank()) {
			if (!observations.isEmpty()) {
				submitObservations(frontend, target, camName, observations);
			}
			return true;
		}
		// comment
		else if (line.startsWith("#")) {
			return true;
		}
		// sleep
		else if (line.startsWith("zzz")) {
			StringTokenizer st = new StringTokenizer(line, ",");
			if (st.countTokens() != 2) {
				System.out.println("Invalid command.");
				System.out.println("Skipping...");
				return true;
			}
			st.nextToken();	// remove zzz
			try {
				int timeout = Integer.parseInt(st.nextToken());
				TimeUnit.MILLISECONDS.sleep(timeout);
				return true;

			}
			catch (NumberFormatException ne) {
				System.out.println("Invalid command.");
				System.out.println("Skipping...");
			}
			catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
			}
		}
		// not a special line
		return false;
	}

	private static void submitObservations(SiloFrontend frontend, String target, String camName, List<List<String>> observations) {
		try {
			frontend.report(target, camName, observations);
			observations.clear();
		}
		catch (SauronException e) {
			System.out.println(e.getErrorMessageLabel());
			System.out.println("Observations not submitted.");
			System.out.println("Shutting down...");
		}

	}

	private static boolean checkObsArguments(String type, String id) {
		if (!type.equals(CAR)  && !type.equals(PERSON)) {
			System.out.println("Invalid object type provided.");
			return false;
		}
		else if ((type.equals(CAR) && !checkCarId(id)) || (type.equals(PERSON) && !checkPersonId(id))) {
			System.out.println(String.format("Invalid %s identifier provided.", type));
			return false;
		}

		return true;
	}

	private static boolean checkCarId(String id) {
		int numFields = 0;

		if (id.length() != 6)
			return false;

		for (int i = 0; i <3; i++){
			char firstChar = id.charAt(2 * i);
			char secChar = id.charAt(2 * i + 1);

			if (Character.isDigit(firstChar) && Character.isDigit(secChar))
				numFields++;

			else if (!(Character.isUpperCase(firstChar) && Character.isUpperCase(secChar)))
				return false;
		}
		if (numFields == 3 || numFields == 0){
			return false;
		}

		return true;
	}

	private static boolean checkPersonId(String id) {
		try{
			Long.parseLong(id);
			return Long.parseLong(id) > 0;
		}
		catch(NumberFormatException e){
			return false;
		}
	}



}
