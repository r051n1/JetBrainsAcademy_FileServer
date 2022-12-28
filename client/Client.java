package client;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
    private final String ADDRESS = "127.0.0.1";
    private final int PORT = 23456;
    private String lastRequestType = "";

    private void setUpClientStorage() {
        String storagePath = System.getProperty("user.dir") + "//src//client//data//";
        File storage = new File(storagePath);

        if (!storage.exists()) {
            if (storage.mkdir()) {
                System.out.println("Created client storage folder");
            }
        }
    }

    private byte[] getFileContent(String fileName) throws FileNotFoundException {

        File userFile = new File(System.getProperty("user.dir") + "//src//client//data//"
                + fileName);
        byte[] fileContent;

        if (userFile.exists() && !userFile.isDirectory()) {
            try {
                fileContent = Files.readAllBytes(userFile.toPath());
                return fileContent;
            } catch (IOException e) {
                System.out.println("Failed finding file to be saved on server");
                e.printStackTrace();
                throw new FileNotFoundException();
            }
        } else {
            throw new FileNotFoundException();
        }
    }

    private void saveFile(DataInputStream input)  throws IOException {
        String filePath = System.getProperty("user.dir")
                + "//src//client//data//";

        int size = input.readInt();
        byte[] fileContent = new byte[size];
        input.readFully(fileContent, 0, fileContent.length);

        System.out.println("The file was downloaded! Specify a name for it: ");
        Scanner scanner = new Scanner(System.in);
        String fileName = scanner.nextLine();

        filePath += fileName;
        Files.write(Paths.get(filePath), fileContent);
        System.out.println("File saved on the hard drive!");
    }

    private boolean sendRequest(ObjectOutputStream output) throws IOException {

        ArrayList<String> commandToken = new ArrayList<>();
        Scanner input = new Scanner(System.in);
        String choiceOrId;

        System.out.println("Enter action (1 - get a file, 2 - save a file, 3 - delete a file): ");

        choiceOrId = input.nextLine();

        switch (choiceOrId) {

            case "1":

                choiceOrId = "GET";
                lastRequestType = choiceOrId;
                commandToken.add(choiceOrId);
                System.out.println("Do you want to get the file by name or by id (1 - name, 2 - id): ");
                choiceOrId = input.nextLine();

                if (choiceOrId.equals("1")) {
                    choiceOrId = "BY_NAME";
                    commandToken.add(choiceOrId);
                    System.out.println("Enter filename: ");
                    choiceOrId = input.nextLine();
                    commandToken.add(choiceOrId);
                    output.writeObject(commandToken);
                    return true;

                } else if (choiceOrId.equals("2")) {
                    choiceOrId = "BY_ID";
                    commandToken.add(choiceOrId);
                    System.out.println("Enter file id: ");
                    choiceOrId = input.nextLine();
                    commandToken.add(choiceOrId);
                    output.writeObject(commandToken);
                    return true;

                } else {
                    System.out.println("Invalid choice.");
                    return false;
                }

            case "2":

                choiceOrId = "PUT";
                lastRequestType = choiceOrId;
                commandToken.add(choiceOrId);
                System.out.println("Enter filename you want to save on server: ");
                choiceOrId = input.nextLine();

                try {
                    byte[] fileContent = getFileContent(choiceOrId);
                    commandToken.add(String.valueOf(fileContent.length));
                    System.out.println("Enter filename to be saved on server: ");
                    choiceOrId = input.nextLine();
                    commandToken.add(choiceOrId);
                    output.writeObject(commandToken);
                    output.write(fileContent);
                    return true;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return false;
                }

            case "3":

                choiceOrId = "DELETE";
                lastRequestType = choiceOrId;
                commandToken.add(choiceOrId);
                System.out.println("Do you want to delete the file by name or by id (1 - name, 2 - id): ");
                choiceOrId = input.nextLine();

                if (choiceOrId.equals("1")) {
                    choiceOrId = "BY_NAME";
                    commandToken.add(choiceOrId);
                    System.out.println("Enter filename: ");
                    choiceOrId = input.nextLine();
                    commandToken.add(choiceOrId);
                    output.writeObject(commandToken);
                    return true;

                } else if (choiceOrId.equals("2")) {
                    choiceOrId = "BY_ID";
                    commandToken.add(choiceOrId);
                    System.out.println("Enter file id: ");
                    choiceOrId = input.nextLine();
                    commandToken.add(choiceOrId);
                    output.writeObject(commandToken);
                    return true;

                } else {
                    System.out.println("Invalid choice");
                    return false;
                }

            case "exit":

                choiceOrId = "exit";
                commandToken.add(choiceOrId);
                output.writeObject(commandToken);
                return true;

            default:

                System.out.println("Invalid choice.");
                return false;
        }
    }

    private void processResponse(DataInputStream input) throws IOException {

        int response;
        response = input.readInt();

        switch (response) {

            case 200:

                if (lastRequestType.equals("PUT")) {
                    int id = input.readInt();
                    System.out.println("Response says that file is saved! ID = " + id);

                } else if (lastRequestType.equals("GET")) {
                    try {
                        saveFile(input);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else if (lastRequestType.equals("DELETE")) {
                    System.out.println("The response says that this file was deleted successfully!");
                }
                break;

            case 403:

                if (lastRequestType.equals("PUT")) {
                    System.out.println("The response says that creating the file was forbidden!");
                } else if (lastRequestType.equals("GET")) {
                    System.out.println("The response says that retrieving the file was forbidden!");
                } else if (lastRequestType.equals("DELETE")) {
                    System.out.println("The response says that this file is not found!");
                }
                break;

            case 404:

                System.out.println("The response says that the file was not found!");
                break;
        }
    }

    public static void main(String[] args) {
        Client client = new Client();

        client.setUpClientStorage();

        try (
                Socket socket = new Socket(client.ADDRESS, client.PORT);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())
        ) {

            if (client.sendRequest(output)) {
                System.out.println("The request was sent.");
                client.processResponse(input);
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to send a request to the server or the server is offline.");
        }
    }
}