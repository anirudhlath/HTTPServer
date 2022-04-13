import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class WebSocketConnection {
    Socket webSocket;
    handleClient currentClient;
    DataInputStream inputStream;
    String message;
    String JSONFormat = "";
    static ArrayList<clientRoom> rooms = new ArrayList<>();

    public WebSocketConnection(handleClient client) {
        webSocket = client.s;
        currentClient = client;
        inputStream = new DataInputStream(client.input);
    }

    public void decodeData() throws IOException {
        byte[] resolverBytes = inputStream.readNBytes(2);
        int opcode = resolverBytes[0] & 0x0F;
        boolean isMasked = (resolverBytes[1] & 0x80) != 0;
        int lenGuess = resolverBytes[1] & 0x7F;
        if (lenGuess > 125) {
            if (lenGuess == 126) {
                byte[] guessLengthBytes = inputStream.readNBytes(2);
                lenGuess = inputStream.readUnsignedShort();
            } else if (lenGuess == 127) {
                byte[] guessLengthBytes = inputStream.readNBytes(8);
                lenGuess = (int) inputStream.readLong();
            }
        }

        String DECODED = "";
        if (isMasked) {
            byte[] MASK = inputStream.readNBytes(4);
            byte[] ENCODED = inputStream.readNBytes(lenGuess);
            DECODED = "";

            for (int i = 0; i < ENCODED.length; i++) {
                DECODED += (char) (ENCODED[i] ^ MASK[i % 4]);
            }
            message = DECODED;
        }
        System.out.println("OPCODE: " + opcode + " | Is Masked? " + isMasked + " | Guess Length? " + lenGuess);
        System.out.println("DECODED: ");
        System.out.println(DECODED);

    }

    public void handleResponse() throws IOException {
        String[] wordArray = message.split(" ");
        String firstWord = wordArray[0];
        char firstLetter = firstWord.charAt(0);

        if (wordArray[0].equals("join")) {
            boolean roomExists = false;
            for (clientRoom room : rooms) {
                if (room.roomName.equals(wordArray[1])) {
                    roomExists = true;
                }
            }

            if (!roomExists) {
                System.out.println("Room does not exist, adding room...");
                clientRoom newRoom = new clientRoom(wordArray[1]);
                newRoom.addClient(currentClient);
                rooms.add(newRoom);
                currentClient.clientRoom = wordArray[1];
                System.out.println(rooms);
            } else {
                System.out.println("Room exists, trying to add client...");
                for (clientRoom room : rooms) {
                    if (room.roomName.equals(wordArray[1])) {
                        room.addClient(currentClient);
                        currentClient.clientRoom = wordArray[1];
                    }
                }
            }

        } else if (Character.isLetter(firstLetter)){
            System.out.println("Message Algorithm triggered...");
            String user = wordArray[0];
            String userMessage = "";
            for (int i = 1; i < wordArray.length; i++) {
                userMessage += wordArray[i] + " ";
            }
            System.out.println("User: " + user + " | Message: " + userMessage);

            JSONFormat = "{ " + '"' + "user" + '"' + ": " + '"' + user + '"' + ", " + '"' + "message" + '"' + ": " + '"' + userMessage + '"' + " }";
            System.out.println(JSONFormat);


            for (clientRoom room : rooms) {

                if (room.roomName.equals(currentClient.clientRoom)) {
                    System.out.println("Found room");
                    System.out.println(room.roomClients_);
                    for (handleClient client : room.roomClients_) {
                        System.out.println("sending message to " + client);
                        sendMessage(client);
                    }
                }
            }
        }
    }

    public void sendMessage(handleClient client) throws IOException {
        BufferedOutputStream outputStream = new BufferedOutputStream(client.output);
        byte[] byteArray = JSONFormat.getBytes();
        int length = JSONFormat.length();
        int reservedByteLength = 2;
        if (length > 125) {
            if (length == 126) {
                reservedByteLength = 4;
            } else if (length == 127) {
                reservedByteLength = 10;
            }
        }
        byte[] reservedBytes = new byte[reservedByteLength];
        reservedBytes[0] = (byte) 0x81;
        reservedBytes[1] = (byte) length;
        for (byte responseByteDatum : reservedBytes) {
            outputStream.write(responseByteDatum);
        }
        for (byte responseByteDatum : byteArray) {
            outputStream.write(responseByteDatum);
        }

        outputStream.flush();
    }
}
