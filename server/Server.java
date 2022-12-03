package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final String ADDRESS = "127.0.0.1";
    private final int PORT = 23456;

    public Server() {
        System.out.println("Server started!");

        try (ServerSocket server = new ServerSocket(PORT)) {
            try (
                    Socket socket = server.accept();
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    ) {
                String msg = input.readUTF();
                System.out.println("Received: " + msg);

                String outputMsg = "All files were sent!";
                System.out.println("Sent: " + outputMsg);
                output.writeUTF(outputMsg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
