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

    private void stop(ObjectInputStream input, DataOutputStream output, Socket socket) throws IOException {
        saveIdMap(idMap);
        input.close();
        output.close();
        socket.close();
        serverOnline = false;
    }

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

    private String getFileName(ArrayList<String> commandToken) {

        String fileName = commandToken.get(2);

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

    private void saveFile(ObjectInputStream input, ArrayList<String> commandToken, String fileName) throws IOException {

        File putFile = new File(setUpFileStorage(fileName));


        if (!putFile.exists() && !putFile.isDirectory()) {

            byte[] fileContent = new byte[Integer.parseInt(commandToken.get(1))];
            input.readFully(fileContent, 0, fileContent.length);
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
                executor.submit(() -> {
                    try {
                        saveFile(input, commandToken, fileName);
                        output.writeInt(200);
                        output.writeInt(Math.abs(fileName.hashCode()));
                        output.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                executor.shutdown();
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
                    e.printStackTrace();
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