
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.Thread;

public class ChatServer {
    
    List<Socket> socketList = Collections.synchronizedList(new ArrayList<Socket>());
    ServerSocket server_socket;
    ServerSocket server_socketFTP;
    int port = 6001;
    int portFTP = 6002;
    public static void main(String[] args) {

        int port;
        if (args.length < 1 || args.length > 2) {
            System.out.println("Usage:");
            System.out.println("   ChatServer <port number>");
            return;
        }

        ChatServer server = new ChatServer();
        port = Integer.parseInt(args[0]);
        try {
            ServerSocket server_socket = new ServerSocket(port);
            server.server_socket = server_socket;
            boolean running = true;
            while(running){
              Socket client_socket = server_socket.accept();  
              System.out.println("Client connected...");
              server.newClient(client_socket);
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        System.out.println("server terminated");
        server.close();
        System.exit(0);
    }

    public void newClient(Socket clientSocket){
        ChatServer server = this;
        socketList.add(clientSocket);
        Runnable recvTask = () -> {
            Socket socket = clientSocket;
            String username = null;
            try {
                DataInputStream input = new DataInputStream(socket.getInputStream());
                String message;
                while ((message = input.readUTF()) != null) {
                    if(username == null){
                        username = message;
                        System.out.println(username +" has joined!");
                        continue;
                    }
                    server.broadcastMessage(socket, username+": " + message);
                    System.out.println(username+": " + message);
                }
                server.removeClient(socket);
                System.out.println(username + " has left the building.");
            } catch (Exception e) {
                server.removeClient(socket);
                System.out.println(username + " has left the building.");
                //System.out.println("Exception: " + e.getMessage());
            }
        };
        new Thread (recvTask).start();
    }

    public void removeClient(Socket clientSocket) {
        
        socketList.remove(clientSocket);
    }

    public void broadcastMessage(Socket socket, String userMessage) {
        for(Socket s: socketList){
            if(s == socket){
                continue;
            }else{
                try {
                    DataOutputStream output = new DataOutputStream(s.getOutputStream());
                    output.writeUTF(userMessage);
                } catch (Exception e) {
                    System.out.println("Exception: " + e.getMessage());
                }
            }
        }
    }

    public void close() {
        try {
            if (server_socket != null) {
                server_socket.close();
            }
        } catch (Exception e) {

        }
    }

/* 
    public void run() {
        try {
            DataInputStream input = new DataInputStream(socket.getInputStream());
            String message;
            while ((message = input.readUTF()) != null) {
                System.out.println(message);
            }
            close();
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }*/
}