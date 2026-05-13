package org.example.server.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketServer {
    private static final int PORT = 8888;
    private Selector bossSelector;
    private ServerSocketChannel serverChannel;
    private NioWorker[] workers;
    private final AtomicInteger roundRobin = new AtomicInteger(0);
    private volatile boolean running = true;

    public void run(String... args) {
        try {
//            int workerCount = Runtime.getRuntime().availableProcessors();
            int workerCount = 3;
            workers = new NioWorker[workerCount];
            for (int i = 0; i < workerCount; i++) {
                workers[i] = new NioWorker(i);
                new Thread(workers[i], "NioWorker-" + i).start();
            }

            bossSelector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(PORT));
            serverChannel.configureBlocking(false);
            serverChannel.register(bossSelector, SelectionKey.OP_ACCEPT);

            System.out.println(">>> Boss Thread (Acceptor) is LIVE on port: " + PORT);
            System.out.println(">>> Workers: " + workerCount);

            while (running) {
                if (bossSelector.select() == 0) continue;

                Set<SelectionKey> selectedKeys = bossSelector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println(">>> Boss Thread Error: " + e.getMessage());
            }
        } finally {
            stop();
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        client.configureBlocking(false);

        // Assign to worker using Round Robin
        NioWorker worker = workers[roundRobin.getAndIncrement() % workers.length];
        worker.register(client);
        
        System.out.println(">>> Accepted and assigned to worker: " + client.getRemoteAddress());
    }

    public void stop() {
        running = false;
        try {
            if (bossSelector != null) bossSelector.close();
            if (serverChannel != null) serverChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
