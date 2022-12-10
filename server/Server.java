package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;

public class Server extends Thread {
    private final String ADDRESS = "127.0.0.1";
    private final int PORT = 23456;
    private boolean serverOnline;

    private void stop(ObjectInputStream input, DataOutputStream output, Socket socket) throws IOException {
        input.close();
        output.close();
        socket.close();
        serverOnline = false;
    }

    private static String setUpFileStorage(String fileName) {
        String filePath = System.getProperty("user.dir") + "/server/data"  + fileName;
        return filePath;
    }

    public String processCommand(ArrayList<String> commandToken) {

        switch (commandToken.get(0)) {

            case "PUT":

                File putFile = new File(setUpFileStorage(commandToken.get(1)));

                try (FileWriter writer = new FileWriter(putFile)) {

                    if (!putFile.exists()) {
                        writer.write(commandToken.get(2));
                        writer.close();
                        return String.valueOf(200);
                    } else {
                        return String.valueOf(403);
                    }
                } catch (IOException e) {
                    return String.valueOf(403);
                }

            case "GET":

                File getFile = new File(commandToken.get(0));
                String fileContent;

                try (Scanner reader = new Scanner(getFile)) {
                    if (getFile.exists() && !getFile.isDirectory()) {
                        fileContent = reader.nextLine();
                        reader.close();
                        return 200 + fileContent;
                    } else {
                        return String.valueOf(403);
                    }
                } catch (FileNotFoundException e) {
                    return String.valueOf(404);
                }

            case "DELETE":

                File deleteFile = new File(commandToken.get(0));

                try {
                    if (Files.deleteIfExists(deleteFile.toPath())) {
                        return String.valueOf(200);
                    } else {
                        return String.valueOf(404);
                    }
                } catch (IOException e) {
                    return String.valueOf(403);
                }

            default:

                System.out.println("System invalid command.");
                return String.valueOf(400);
        }
    }

    @Override
    public void run() {

        ArrayList<String> commandToken;

        try (ServerSocket server = new ServerSocket(PORT)) {

            serverOnline = true;
            System.out.println("Server started!");

            while (serverOnline) {

                try (
                        Socket socket = server.accept();
                        ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                ) {

                    try {
                        commandToken = (ArrayList<String>) input.readObject();
                        if (!commandToken.get(0).equals("exit")) {
                            String response = processCommand(commandToken);
                            output.writeUTF(response);
                        } else {
                            stop(input, output, socket);
                        }
                    } catch (EOFException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}