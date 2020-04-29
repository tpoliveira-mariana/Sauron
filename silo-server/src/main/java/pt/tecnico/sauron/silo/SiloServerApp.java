package pt.tecnico.sauron.silo;


import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.io.IOException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class SiloServerApp {
	private static final String PATH = "/grpc/sauron/silo";
	private static long gossipTimer = 30;
	private static SiloServerImpl impl;
	private static String zooHost;
	private static String zooPort;

	static class ScheduledTimer extends TimerTask {
		@Override
		public void run() {
			impl.performGossip(PATH);
		}
	}

	public static void main(String[] args) {
		System.out.println(SiloServerApp.class.getSimpleName());

		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if (args.length < 6 || args.length > 7) {
			System.out.println("Invalid number of arguments!");
			return;
		}
		zooHost = args[0];
		zooPort = args[1];
		final int instance;
		final String host = args[3];
		final String port = args[4];
		final int replicaNum;
		if (args.length == 7){
			gossipTimer = Long.parseLong(args[6]);
		}

		ZKNaming zkNaming = null;
		try {
			instance = Integer.parseInt(args[2]);
			replicaNum = Integer.parseInt(args[5]);
			impl = new SiloServerImpl(zooHost, zooPort, replicaNum, instance);
			zkNaming = new ZKNaming(zooHost, zooPort);
			// publish
			zkNaming.rebind(PATH + "/" + args[2], host, port);
			// Create a new server to listen on port
			Server server = ServerBuilder.forPort(Integer.parseInt(port)).addService(impl).build();
			// Start the server
			server.start();
			// Server threads are running in the background.
			System.out.println("Replica " + instance + " started");

			Timer timer = new Timer();
			timer.schedule(new ScheduledTimer(), gossipTimer*1000, gossipTimer*1000);

			// Create new thread where we wait for the user input to terminate the server.
			new Thread(() -> {
				System.out.println("<Press enter to shutdown>");
				new Scanner(System.in).nextLine();

				server.shutdown();
			}).start();

			// Do not exit the main thread. Wait until server is terminated.
			server.awaitTermination();
			timer.cancel();
		} catch (IOException ioe) {
			System.out.println("Can't start replica server.");
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		} catch (ZKNamingException e) {
			System.out.println(e.getMessage());
			Thread.currentThread().interrupt();
		} finally  {
			if (zkNaming != null) {
				// remove
				try {
					zkNaming.unbind(PATH + "/" + args[2], host, port);
				} catch (ZKNamingException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}
}
