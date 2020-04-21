package pt.tecnico.sauron.silo;


import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.io.IOException;

public class SiloServerApp {
	
	public static void main(String[] args) {
		System.out.println(SiloServerApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if (args.length < 5) {
			System.out.println("Argument missing!");
			return;
		}
		final String zooHost = args[0];
		final String zooPort = args[1];
		final String path = args[2];
		final String host = args[3];
		final String port = args[4];
		final BindableService impl = new SiloServerImpl();

		ZKNaming zkNaming = null;
		try {
			zkNaming = new ZKNaming(zooHost, zooPort);
			// publish
			zkNaming.bind(path, host, port);
			// Create a new server to listen on port
			Server server = ServerBuilder.forPort(Integer.parseInt(port)).addService(impl).build();
			// Start the server
			server.start();
			// Server threads are running in the background.
			System.out.println("Server started");
			// Do not exit the main thread. Wait until server is terminated.
			server.awaitTermination();
		} catch (IOException ioe) {
			System.out.println("Can't start server.");
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		} catch (ZKNamingException e) {
			System.out.println("Can't start server - Zookeeper error.");
			Thread.currentThread().interrupt();
		} finally  {
			if (zkNaming != null) {
				// remove
				try {
					zkNaming.unbind(path, host, String.valueOf(port));
				} catch (ZKNamingException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}
	
}
