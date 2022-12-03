package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private final String ADDRESS = "127.0.0.1";
    private final int PORT = 23456;

    public Client() {
        System.out.println("Client started!");

        try (
                Socket socket = new Socket(ADDRESS, PORT);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        ) {
            String msg = "Give me everything you have!";

            System.out.println("Sent: " + msg);
            output.writeUTF(msg);
            String receivedMsg = input.readUTF();

            System.out.println("Received: " + receivedMsg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
