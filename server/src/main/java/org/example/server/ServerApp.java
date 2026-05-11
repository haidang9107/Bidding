package org.example.server;


import org.example.server.network.SocketServer;

public class ServerApp {
    public static void main(String[] args) {
	    SocketServer  socketServer = new SocketServer();
		socketServer.run();
    }
}
