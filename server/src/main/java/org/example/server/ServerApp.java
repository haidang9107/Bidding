package org.example.server;


import org.example.server.network.SocketServer;

/**
 * Main entry point for the server application.
 */
public class ServerApp {
    /**
     * Starts the socket server.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
	    SocketServer socketServer = new SocketServer();
		socketServer.run();
    }
}
