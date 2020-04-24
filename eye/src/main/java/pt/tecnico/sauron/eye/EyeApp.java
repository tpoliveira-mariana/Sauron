package pt.tecnico.sauron.eye;


import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import pt.tecnico.sauron.exceptions.SauronException;
import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.tecnico.sauron.exceptions.ErrorMessage;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class EyeApp {

	private static SiloFrontend _frontend;

	public static void main(String[] args) {
		System.out.println(EyeApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// check arguments
		if (args.length < 6) {
			System.out.println("Argument(s) missing!");
			System.out.printf("Usage: java eye host port cameraName latitude longitude%n");
			return ;
		}

		try {
			_frontend = new SiloFrontend(args[0], args[1]);

			String cameraName = args[2];
			double lat = Double.parseDouble(args[3]);
			double lon = Double.parseDouble(args[4]);

			processCameraObservations(cameraName, lat, lon);
		}
		catch (NumberFormatException e) {
			shutDownMessage("Invalid number provided.");
		}
		catch (ZKNamingException e) {
			shutDownMessage("Zookeeper error.");
		}
	}

	private static void processCameraObservations(String cameraName,  double lat, double lon){
		try {
			_frontend.camJoin(cameraName, lat, lon);
		} catch (SauronException e) {
			if (e.getErrorMessage() == ErrorMessage.DUPLICATE_CAMERA)
				System.out.println("Camera already registered. Processing observations...");

			else {
				shutDownMessage(e.getErrorMessageLabel());
				return ;
			}
		}

		try {
			Scanner scanner = new Scanner(System.in);
			List<List<String>> observations = new ArrayList<>();

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (!handledSpecialLine(cameraName, line, observations)) {
					StringTokenizer st = new StringTokenizer(line, ",");
					if (st.countTokens() != 2) {
						System.out.println("Invalid command.");
						continue;
					}
					String objectType = st.nextToken();
					String objectId = st.nextToken();

					addObservation(observations, objectType, objectId);
				}
			}

			if (!observations.isEmpty()) {
				submitObservations(cameraName, observations);
			}

		} catch (Exception e) {
			shutDownMessage("An unexpected error occurred.");
		}
	}

	private static void addObservation(List<List<String>> observations, String objectType, String objectId) {
		List<String> obs = new ArrayList<>();
		obs.add(objectType);
		obs.add(objectId);
		observations.add(obs);
	}

	private static boolean handledSpecialLine(String camName, String line, List<List<String>> observations) {
		// blank line
		if (line.isBlank()) {
			if (!observations.isEmpty()) {
				submitObservations(camName, observations);
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
				return true;
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
			System.out.println("Some observations were not submitted - " + e.getErrorMessageLabel());
			observations.clear();
		}
	}

	private static void shutDownMessage(String msg) {
		System.out.println(msg);
		System.out.println("Shutting down...");
	}

}
