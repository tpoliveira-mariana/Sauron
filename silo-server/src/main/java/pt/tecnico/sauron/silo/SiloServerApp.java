package pt.tecnico.sauron.silo;


import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class SiloServerApp {
	
	public static void main(String[] args) {
		System.out.println(SiloServerApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if (args.length < 1) {
			System.out.println("Argument missing!");
			return;
		}

		final int port = Integer.parseInt(args[0]);
		final BindableService impl = new SiloServerImpl();

		// Create a new server to listen on port
		Server server = ServerBuilder.forPort(port).addService(impl).build();

		try {
			// Start the server
			server.start();

			// Server threads are running in the background.
			System.out.println("Server started");

			// Do not exit the main thread. Wait until server is terminated.
			server.awaitTermination();
		}
		catch (IOException ioe) {
			System.out.println("Can't start server.");
			return;
		}
		catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}
	
}
