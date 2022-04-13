import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class clientRoom {
    String roomName = "";
    ArrayList<handleClient> roomClients_ = new ArrayList<>();

    public clientRoom(String room) {
        roomName = room;
    }

    public void addClient(handleClient client) {
        boolean clientExists = false;

        for (handleClient element : roomClients_) {
            if (element == client) {
                clientExists = true;
            }
        }
        if (!clientExists) {
            roomClients_.add(client);
            client.clientRoom = roomName;
        } else {
            System.out.println("This client already exists in room " + roomName);
        }
    }
}
