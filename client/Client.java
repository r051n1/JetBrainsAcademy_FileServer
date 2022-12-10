package client;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Client extends Thread {
    private final String ADDRESS = "127.0.0.1";
    private final int PORT = 23456;
    private ArrayList<String> commandToken = new ArrayList<>();

    private void clientMenu() {

        Scanner input = new Scanner(System.in);
        String choiceOrId;
        String fileName;
        String fileContent;

        System.out.println("Enter action (1 - get a file, 2 - create a file, 3 - delete a file): ");

        choiceOrId = input.nextLine();

        switch (choiceOrId) {

            case "1":
                choiceOrId = "GET";
                System.out.println("Enter filename: ");
                fileName = input.nextLine();
                commandToken.add(choiceOrId);
                commandToken.add(fileName);
                break;

            case "2":
                choiceOrId = "PUT";
                System.out.println("Enter filename: ");
                fileName = input.nextLine();
                System.out.println("Enter file content: ");
                fileContent = input.nextLine();
                commandToken.add(choiceOrId);
                commandToken.add(fileName);
                commandToken.add(fileContent);
                break;

            case "3":
                choiceOrId = "DELETE";
                System.out.println("Enter filename: ");
                fileName = input.nextLine();
                commandToken.add(choiceOrId);
                commandToken.add(fileName);
                break;

            case "exit":
                choiceOrId = "exit";
                commandToken.add(choiceOrId);
                break;

            default:
                System.out.println("Invalid choice.");
        }
    }

    private void processResponse(String response) {

        String[] responseTokens = response.split(" ");

        switch (responseTokens[0]) {
            case "200":
                if (commandToken.get(0).equals("PUT")) {
                    System.out.println("The response says that the file was created!");
                } else if (commandToken.get(0).equals("GET")) {
                    System.out.println("The content of the file is: " + responseTokens[1]);
                } else if (commandToken.get(0).equals("DELETE")) {
                    System.out.println("The response says that the file was successfully deleted!");
                }
                break;

            case "403":
                if (commandToken.get(0).equals("PUT")) {
                    System.out.println("The response says that creating the file was forbidden!");
                } else if (commandToken.get(0).equals("GET")) {
                    System.out.println("The response says that retrieving the file was forbidden!");
                } else if (commandToken.get(0).equals("DELETE")) {
                    System.out.println("The response says that deleting the file was forbidden!");
                }
                break;

            case "404":
                System.out.println("The response says that the file was not found!");
                break;
        }
    }

    @Override
    public void run() {

        try (
                Socket socket = new Socket(ADDRESS, PORT);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
        ) {

            clientMenu();

            if (!commandToken.isEmpty()) {
                output.writeObject(commandToken);
                String response = input.readUTF();
                processResponse(response);
            }

        } catch (IOException e) {
            System.out.println("Server not responding.");
        }
    }
}
