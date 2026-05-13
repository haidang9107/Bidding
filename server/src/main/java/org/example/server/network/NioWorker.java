package org.example.server.network;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NioWorker implements Runnable {
    private final Selector selector;
    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private final String workerName;

    public NioWorker(int id) throws IOException {
        this.selector = Selector.open();
        this.workerName = "Worker-" + id;
    }

    public void register(SocketChannel clientChannel) {
        taskQueue.add(() -> {
            try {
                SelectionKey key = clientChannel.register(selector, SelectionKey.OP_READ);
                ClientHandler handler = new ClientHandler(clientChannel, this);
                key.attach(handler);
                Broadcaster.addClient(handler);
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        });
        selector.wakeup();
    }

    @Override
    public void run() {
        System.out.println(">>> " + workerName + " started.");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                selector.select();

                // Process registration tasks
                Runnable task;
                while ((task = taskQueue.poll()) != null) {
                    task.run();
                }

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (key.isValid() && key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println(">>> " + workerName + " error: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void handleRead(SelectionKey key) {
        ClientHandler handler = (ClientHandler) key.attachment();
        try {
            handler.read();
        } catch (IOException e) {
            System.out.println(">>> Client disconnected: " + workerName);
            key.cancel();
            Broadcaster.removeClient(handler);
            handler.close();
        }
    }

    public void wakeup() {
        selector.wakeup();
    }

    private void close() {
        try {
            selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
