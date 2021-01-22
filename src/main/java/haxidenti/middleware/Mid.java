package haxidenti.middleware;

import java.net.ServerSocket;
import java.net.Socket;

public class Mid {

    public static void main(String[] args) throws Exception {
        Args a = parseArgs(args);

        System.out.println("Servicing:");
        System.out.println(a.host + ":" + a.forwardPort + " --> " + a.serverPort);

        ServerSocket ss = new ServerSocket(a.serverPort);
        while (true) {
            Thread.sleep(1);
            try {
                Socket clientSocket = ss.accept();
                Socket serverSocket = new Socket(a.host, a.forwardPort);

                System.out.println("Client connected!");

                new Thread(() -> {
                    try {
                        Exception e = serveSocket(a.delay, clientSocket, serverSocket);
                        if (e != null) System.out.println("Disconnected with message: " + e.getMessage());
                    } catch (Exception e) {
                    }
                }).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static Exception serveSocket(int minDelay, Socket clientSocket, Socket serverSocket) throws Exception {
        var clientIn = clientSocket.getInputStream();
        var clientOut = clientSocket.getOutputStream();

        var serverIn = serverSocket.getInputStream();
        var serverOut = serverSocket.getOutputStream();

        int clientByte = 0;

        boolean[] stopBool = new boolean[]{false};

        new Thread(() -> {
            int serverByte = 0;
            try {
                while (!stopBool[0]) {
                    Thread.sleep(minDelay);
                    serverByte = serverIn.read();
                    if (serverByte > -1) clientOut.write(serverByte);
                }
            } catch (Exception e) {
                stopBool[0] = true;
            }
        }).start();

        try {
            while (true) {
                Thread.sleep(minDelay);
                clientByte = clientIn.read();
                if (clientByte > -1) serverOut.write(clientByte);
                if (stopBool[0]) throw new RuntimeException("Server seems to be stopped!");
            }
        } catch (Exception e) {
            return e;
        }
    }

    private static Args parseArgs(String[] args) {
        int state = 0;
        // 0 - nothing
        // 1 - setting f-port
        // 2 - setting s-port
        // 3 - setting host
        Args a = new Args();
        a.host = "localhost";
        a.serverPort = 2020;
        a.forwardPort = 8080;
        a.delay = 0;
        for (String arg : args) {
            if ("-f".equals(arg)) {
                state = 1;
            } else if ("-s".equals(arg)) {
                state = 2;
            } else if ("-h".equals(arg)) {
                state = 3;
            } else {
                if (state == 0) continue;
                if (state == 1) {
                    a.forwardPort = Integer.parseInt(arg);
                } else if (state == 2) {
                    a.serverPort = Integer.parseInt(arg);
                } else if (state == 3) {
                    a.host = arg;
                }
            }
        }
        return a;
    }
}
