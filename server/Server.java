package server;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Server extends Thread {
    private final String ADDRESS = "127.0.0.1";
    private final int PORT = 23456;
    private boolean serverOnline;
    private HashMap<String, Integer> idMap;

    private void stop(ObjectInputStream input, DataOutputStream output, Socket socket) throws IOException {
        saveIdMap(idMap);
        input.close();
        output.close();
        socket.close();
        serverOnline = false;
    }

    private String setUpFileStorage(String fileName) {

        String filePath = System.getProperty("user.dir") + "//File Server//task//src//server//data//";
        File fileStorage = new File(filePath);

        if (!fileStorage.exists()) {
            if (fileStorage.mkdir()) {
                System.out.println("Created server storage folder");
            }
        }
        return filePath + fileName;
    }

    private void saveIdMap(HashMap<String, Integer> idMap) throws IOException {
        String mapPath = System.getProperty("user.dir") + "//File Server//task//src//server//ID map//";
        File mapStorage = new File(mapPath);

        if (!mapStorage.exists()) {
            if (mapStorage.mkdir()) {
                System.out.println("Created map storage");
            }
        } else {
            mapPath += "map.bin";
            File idMapFile = new File(mapPath);

            FileOutputStream fos = new FileOutputStream(idMapFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(idMap);
        }
    }

    private HashMap<String, Integer> readIdMap() throws IOException, ClassNotFoundException {
        String mapPath = System.getProperty("user.dir") + "//File Server//task//src//server//ID map//map.bin";
        File idMapFile = new File(mapPath);

        if (idMapFile.exists() && !idMapFile.isDirectory()) {
            HashMap<String, Integer> idMap;

            FileInputStream fis = new FileInputStream(idMapFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);

            idMap = (HashMap<String, Integer>) ois.readObject();
            return idMap;
        } else {
            System.out.println("No id map loaded, creating new id map.");
            return new HashMap<>();
        }
    }
    private boolean saveFile(ObjectInputStream input, ArrayList<String> commandToken) throws IOException {

        File putFile = new File(setUpFileStorage(commandToken.get(2)));

        if (!putFile.exists() && !putFile.isDirectory()) {
            byte[] fileContent = new byte[Integer.parseInt(commandToken.get(1))];
            input.readFully(fileContent, 0, Integer.parseInt(commandToken.get(1)));

            try {
                Files.write(putFile.toPath(), fileContent);
                int fileId = putFile.hashCode();
                idMap.put(commandToken.get(2), fileId);
                return true;

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    private byte[] getFile(String action, String nameOrId) throws FileNotFoundException {

        byte[] fileContent;
        File userFile;

        switch (action) {

            case "BY_NAME":

                userFile = new File(setUpFileStorage(nameOrId));

                if (userFile.exists() && !userFile.isDirectory()) {
                    try {
                        fileContent = Files.readAllBytes(userFile.toPath());
                        return fileContent;
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new FileNotFoundException();
                    }
                } else {
                    throw new FileNotFoundException();
                }

            case "BY_ID":

                if (idMap.containsKey(nameOrId)) {
                    for (var entry : idMap.entrySet()) {
                        if (entry.getValue().equals(nameOrId)) {
                            String fileName = entry.getKey();
                            userFile = new File(setUpFileStorage(fileName));
                            if (userFile.exists() && !userFile.isDirectory()) {
                                try {
                                    fileContent = Files.readAllBytes(userFile.toPath());
                                    return fileContent;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    throw new FileNotFoundException();
                                }
                            } else {
                                throw new FileNotFoundException();
                            }
                        }
                    }
                } else {
                    throw new FileNotFoundException();
                }

            default:
                throw new FileNotFoundException();
        }
    }

    private boolean deleteFile(String action, String nameOrId) throws FileNotFoundException {
        File userFile;

        switch (action) {

            case "BY_NAME":
                userFile = new File(setUpFileStorage(nameOrId));
                try {
                    return Files.deleteIfExists(userFile.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new FileNotFoundException();
                }

            case "BY_ID":
                if (idMap.containsKey(nameOrId)) {
                    for (var entry: idMap.entrySet()) {
                        if (entry.getValue().equals(nameOrId)) {
                            String fileName = entry.getKey();
                            userFile = new File(setUpFileStorage(fileName));
                            try {
                                return Files.deleteIfExists(userFile.toPath());
                            } catch (IOException e) {
                                e.printStackTrace();
                                throw new FileNotFoundException();
                            }
                        }
                    }
                } else {
                    throw new FileNotFoundException();
                }

            default:
                throw new FileNotFoundException();
        }
    }

    public boolean processCommand(ObjectInputStream input, DataOutputStream output)
            throws IOException, ClassNotFoundException {

        ArrayList<String> commandToken;
        commandToken = (ArrayList<String>) input.readObject();

        switch (commandToken.get(0)) {

            case "exit":
                return false;

            case "PUT":
                try {
                    if (saveFile(input, commandToken)) {
                        int id = idMap.get(commandToken.get(2));
                        output.writeUTF("200 ");
                        output.writeInt(id);
                        output.flush();
                        return true;
                    } else {
                        output.writeUTF("403");
                        return true;
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    output.writeUTF("403");
                    return true;
                }

            case "GET":
                byte[] fileContent;

                try {
                    fileContent = getFile(commandToken.get(1), commandToken.get(2));
                    output.writeUTF("200");
                    output.writeInt(fileContent.length);
                    output.write(fileContent);
                    output.flush();
                    return true;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    output.writeUTF("404");
                    return true;
                }

            case "DELETE":
                try {
                    if (deleteFile(commandToken.get(1), commandToken.get(2))) {
                        output.writeUTF("200");
                        return true;
                    } else {
                        output.writeUTF("403");
                        return true;
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    output.writeUTF("403");
                    return true;
                }

            default:

                System.out.println("System invalid command.");
                output.writeUTF("400");
                return true;
        }
    }

    @Override
    public void run() {

        try (ServerSocket server = new ServerSocket(PORT)) {

            serverOnline = true;

            try {
                idMap = readIdMap();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            System.out.println("Server started!");

            while (serverOnline) {

                try (
                        Socket socket = server.accept();
                        ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                ) {

                    try {
                        if (processCommand(input, output)) {
                            System.out.println("Response sent");
                        } else {
                            stop(input, output, socket);
                        }

                    } catch (EOFException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}