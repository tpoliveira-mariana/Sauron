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
		display(EyeApp.class.getSimpleName());
		
		// receive and print arguments
		display("Received " + args.length + " arguments");
		for (int i = 0; i < args.length; i++) {
			display("arg[" + i + "] = " + args[i]);
		}

		// check arguments
		if (args.length > 6 || args.length < 5) {
			display("Argument(s) missing!");
			display("Usage: java eye zkhost zkport cameraName latitude longitude [replicaInstance]");
			return ;
		}

		try {
			int instance = args.length == 6 && args[5] != null ? Integer.parseInt(args[5]) : -1;
			_frontend = new SiloFrontend(args[0], args[1], instance);

			String cameraName = args[2];
			double lat = Double.parseDouble(args[3]);
			double lon = Double.parseDouble(args[4]);

			processCameraObservations(cameraName, lat, lon);
		} catch (NumberFormatException e) {
			shutDownMessage("Invalid number provided.");
		} catch (ZKNamingException e) {
			shutDownMessage("Zookeeper error.");
		} catch (SauronException e) {
			shutDownMessage("No Silo replicas found.");
		}
	}

	private static void processCameraObservations(String cameraName,  double lat, double lon){
		try {
			_frontend.camJoin(cameraName, lat, lon);
		} catch (SauronException e) {
			if (e.getErrorMessage() == ErrorMessage.DUPLICATE_CAMERA)
				display("Camera already registered. Processing observations...");

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
						display("Invalid command.");
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
				display("Invalid command.");
				return true;
			}
			st.nextToken();	// remove zzz
			try {
				int timeout = Integer.parseInt(st.nextToken());
				TimeUnit.MILLISECONDS.sleep(timeout);
				return true;

			}
			catch (NumberFormatException ne) {
				display("Invalid command.");
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
			display("Some observations were not submitted - " + e.getErrorMessageLabel());
			observations.clear();
		}
	}

	private static void shutDownMessage(String msg) {
		display(msg);
		display("Shutting down...");
	}

	private static void display(String msg) {
		System.out.println(msg);
	}
}
