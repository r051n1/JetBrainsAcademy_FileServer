package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends Thread {
    private final int PORT = 23456;
    private boolean serverOnline;
    private HashMap<String, Integer> idMap;

    /**
     * Stops the server from running by closing its communication channels.
     *
     * @param input the input stream used to receive requests from clients
     * @param output the output stream used to send responses to clients
     * @param socket the client socket
     * @throws IOException if an error occurs while closing the channels
     */
    private void stop(ObjectInputStream input, DataOutputStream output, Socket socket) throws IOException {
        saveIdMap(idMap);
        input.close();
        output.close();
        socket.close();
        serverOnline = false;
    }

    /**
     * Sets up the correct path for a file to be saved in the server data directory.
     * If the directory is not already present it creates a new one.
     *
     * @param fileName the name of the file for which the path is referring to
     * @return the correct path to the file
     */
    private String setUpFileStorage(String fileName) {

        String filePath = System.getProperty("user.dir") + "//src//server//data//";
        File fileStorage = new File(filePath);

        if (!fileStorage.exists()) {
            if (fileStorage.mkdir()) {
                System.out.println("Created server storage folder");
            }
        }

        return filePath + fileName;
    }

    /**
     * Saves the id map containing files' id in memory for later use.
     * If the ID map directory does not exist to save the map, this method creates a new one.
     *
     * @param idMap the id map that needs to be saved
     * @throws IOException when an error while saving the map occurs
     */
    private void saveIdMap(HashMap<String, Integer> idMap) throws IOException {
        String mapPath = System.getProperty("user.dir") + "//src//server//ID map//";
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
            oos.close();
            fos.close();
        }
    }

    /**
     * Loads a previous saved map from the ID map directory if present, otherwise it creates a new one for the server.
     *
     * @return the loaded id map or the newly created one
     * @throws IOException when an error occurs while reading the content of the map
     * @throws ClassNotFoundException if tries to read another class type apart from HashMap
     */
    private HashMap<String, Integer> readIdMap() throws IOException, ClassNotFoundException {
        String mapPath = System.getProperty("user.dir") + "//src//server//ID map//map.bin";
        File idMapFile = new File(mapPath);

        if (idMapFile.exists() && !idMapFile.isDirectory()) {
            HashMap<String, Integer> tmpIdMap;

            FileInputStream fis = new FileInputStream(idMapFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);

            tmpIdMap = (HashMap<String, Integer>) ois.readObject();
            ois.close();
            fis.close();

            return  tmpIdMap;
        } else {
            System.out.println("No id map loaded, creating new id map.");
            return new HashMap<>();
        }
    }

    /**
     * It retrieves the file name from the request tokens.
     * If the user did not specify a file name this method forms a new one.
     *
     * @param commandToken the request tokens provided by client
     * @return the file name of the file needed
     */
    private String getFileName(ArrayList<String> commandToken) {

        String fileName = commandToken.get(1);

        if (fileName.isEmpty()) {
            int i = 1;
            fileName = "newFile" + i + ".dat";
            while (idMap.containsKey(fileName)) {
                i++;
                fileName = "newFile" + i + ".dat";
            }
            return fileName;
        } else {
            return fileName;
        }
    }

    /**
     * Saves a file in the server data directory.
     * It also creates a new id for the saved file based on the hash of its name, it later adds this in the id hash map.
     *
     * @param fileContent byte array of the content of the file that needs to be saved
     * @param fileName the name of the file that is being saved to form its new id
     * @throws IOException when an error occurs while saving the file
     */
    private void saveFile(byte[] fileContent, String fileName) throws IOException {

        File putFile = new File(setUpFileStorage(fileName));

        if (!putFile.exists() && !putFile.isDirectory()) {
            try {
                Files.write(putFile.toPath(), fileContent);
                int fileId = Math.abs(fileName.hashCode());
                idMap.put(fileName, fileId);
            } catch (NoSuchFileException e) {
                e.printStackTrace();
                throw new IOException();
            }
        } else {
            throw new IOException();
        }
    }

    /**
     * Loads the content of a file stored in the server data directory.
     * The file can be searched by using its name or its id.
     *
     * @param action this string specifies if the client is searching the file by name or by id
     * @param nameOrId this string contains the name or the id of the file being searched
     * @return the byte array of the content of the retrieved file
     * @throws FileNotFoundException when an error occurs while reading the content of the file
     */
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

                try {
                    Integer.parseInt(nameOrId);
                } catch (NumberFormatException e) {
                    throw new FileNotFoundException();
                }

                if (idMap.containsValue(Integer.parseInt(nameOrId))) {
                    for (var entry : idMap.entrySet()) {
                        if (entry.getValue().equals(Integer.parseInt(nameOrId))) {
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

    /**
     * Delete a file stored in the server data directory.
     * The specified file can be searched by name or id, and it gets deleted if it exists.
     *
     * @param action this string specifies if the client is searching the file by name or id
     * @param nameOrId this string contains the name or the id of the file being searched
     * @return true if the file was deleted successfully, false otherwise
     * @throws FileNotFoundException when an error occurs while searching the file
     */
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
                if (idMap.containsValue(Integer.parseInt(nameOrId))) {
                    for (var entry : idMap.entrySet()) {
                        if (entry.getValue().equals(Integer.parseInt(nameOrId))) {
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

    /**
     * Processes the command provided by the client.
     * It calls different methods based on the command's tokens.
     *
     * @param input the object input stream to receive the request by clients
     * @param output the output stream to send responses to clients or requested data
     * @return true if the response was sent correctly, false otherwise
     * @throws IOException when an error occurs while communicating between client and server
     * @throws ClassNotFoundException if the input stream cannot read the tokens list provided by the client
     */
    public boolean processCommand(ObjectInputStream input, DataOutputStream output)
            throws IOException, ClassNotFoundException {

        ArrayList<String> commandToken;
        commandToken = (ArrayList<String>) input.readObject();

        switch (commandToken.get(0)) {

            case "exit":
                return false;

            case "PUT":
                String fileName = getFileName(commandToken);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                int size = input.readInt();
                byte[] userContent = new byte[size];
                input.readFully(userContent, 0, userContent.length);

                executor.submit(() -> {
                    try {
                        saveFile(userContent, fileName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                executor.shutdown();
                output.writeInt(200);
                output.writeInt(Math.abs(fileName.hashCode()));
                output.flush();
                return true;

            case "GET":
                byte[] fileContent;

                try {
                    fileContent = getFile(commandToken.get(1), commandToken.get(2));
                    output.writeInt(200);
                    output.writeInt(fileContent.length);
                    output.write(fileContent);
                    output.flush();
                    return true;
                } catch (FileNotFoundException e) {
                    output.writeInt(404);
                    return true;
                }

            case "DELETE":
                try {
                    if (deleteFile(commandToken.get(1), commandToken.get(2))) {
                        idMap.remove(commandToken.get(2));
                        output.writeInt(200);
                        return true;
                    } else {
                        output.writeInt(403);
                        return true;
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    output.writeInt(403);
                    return true;
                }

            default:

                System.out.println("System invalid command.");
                output.writeInt(400);
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
                        if (!processCommand(input, output)) {
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
