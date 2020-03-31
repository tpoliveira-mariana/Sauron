package pt.tecnico.sauron.A20.eye;


import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import pt.tecnico.sauron.A20.exceptions.SauronException;
import pt.tecnico.sauron.A20.silo.client.SiloFrontend;

import static pt.tecnico.sauron.A20.exceptions.ErrorMessage.DUPLICATE_CAMERA;

public class EyeApp {
	private static final String PERSON = "person";
	private static final String CAR = "car";

	private static SiloFrontend _frontend;

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
		_frontend = new SiloFrontend(host, Integer.toString(port));

		String cameraName = args[2];
		double lat, lon;

		try {
			lat = Double.parseDouble(args[3]);
			lon = Double.parseDouble(args[4]);
		}
		catch (Exception e) {
			shutDownMessage("Invalid double provided.");
			return ;
		}

		processCameraObservations(cameraName, lat, lon);
	}

	private static void processCameraObservations(String cameraName, double lat, double lon) {
		if (registerCamera(cameraName, lat, lon)) {
			int[] info = {0, 0};
			try (Scanner scanner = new Scanner(System.in)) {
				List<List<String>> observations = new ArrayList<>();

				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					if (!handledSpecialLine(cameraName, line, observations, info)) {
						StringTokenizer st = new StringTokenizer(line, ",");
						if (st.countTokens() != 2) {
							info[1]++;
							skipMessage("Invalid command.");
							continue;
						}
						processObservation(observations, st, info);
					}
				}

				int count_obs = observations.size();
				submitObservations(cameraName, observations);
				info[0] += count_obs;

				printReport(info);

			} catch (Exception e) {
				shutDownMessage("An unexpected error occurred.");
			}
		}
	}

	private static void processObservation(List<List<String>> observations, StringTokenizer st, int[] info) {
		String objectType = st.nextToken();
		String objectId = st.nextToken();

		if (checkObsArguments(objectType, objectId, info)) {
			addObservation(observations, objectType, objectId);
		}
	}

	private static void addObservation(List<List<String>> observations, String objectType, String objectId) {
		List<String> obs = new ArrayList<>();
		obs.add(objectType);
		obs.add(objectId);
		observations.add(obs);
	}

	private static boolean registerCamera(String name, double lat, double lon) {
		if (!name.matches("[A-Za-z0-9]+") || name.length() < 3 || name.length() > 15) {
			shutDownMessage("Invalid camera name provided.");
			return false;
		}
		else if (lat > 90 || lat < -90 || lon > 180 || lon < -180) {
			shutDownMessage("Invalid coordinates provided.");
			return false;
		}

		try {
			_frontend.camJoin(name, lat, lon);
			return true;
		}
		catch (SauronException e) {
			if (e.getErrorMessage() == DUPLICATE_CAMERA) {
				System.out.println("Camera already registered. Processing observations...");
				return true;
			}
			else {
				shutDownMessage(e.getErrorMessageLabel());
				return false;
			}
		}
	}

	private static boolean handledSpecialLine(String camName, String line, List<List<String>> observations, int[] info) {
		// blank line
		if (line.isBlank()) {
			if (!observations.isEmpty()) {
				int count_obs = observations.size();
				submitObservations(camName, observations);
				info[0] += count_obs;
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
				info[1]++;
				skipMessage("Invalid command.");
				return true;
			}
			st.nextToken();	// remove zzz
			try {
				int timeout = Integer.parseInt(st.nextToken());
				TimeUnit.MILLISECONDS.sleep(timeout);
				return true;

			}
			catch (NumberFormatException ne) {
				info[1]++;
				skipMessage("Invalid command.");
			}
			catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
			}
		}
		// not a special line
		return false;
	}

	private static void submitObservations(String camName, List<List<String>> observations) {
		try {
			_frontend.report(camName, observations);
			observations.clear();
		}
		catch (SauronException e) {
			System.out.println(e.getErrorMessageLabel());
			shutDownMessage("Observation not submitted.");
		}

	}

	private static boolean checkObsArguments(String type, String id, int[] info) {
		if (!type.equals(CAR)  && !type.equals(PERSON)) {
			info[1]++;
			skipMessage("Invalid object type provided.");
			return false;
		}
		else if ((type.equals(CAR) && !checkCarId(id)) || (type.equals(PERSON) && !checkPersonId(id))) {
			info[1]++;
			skipMessage(String.format("Invalid %s identifier provided.", type));
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

	private static void skipMessage(String msg) {
		System.out.println(msg);
		System.out.println("Skipping...");
	}

	private static void shutDownMessage(String msg) {
		System.out.println(msg);
		System.out.println("Shutting down...");
	}

	private static void printReport(int[] info) {
		System.out.println("\n----------REPORT----------");
		System.out.printf("Sent:      %d\n", info[0]);
		System.out.printf("Skipped:   %d\n", info[1]);

	}



}
