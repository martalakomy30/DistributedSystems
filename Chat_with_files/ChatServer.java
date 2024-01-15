import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Thread;


public class ChatServer {

    class Client 
    {
        String username = null;
        int portFTP;
        Socket socket;
        DataInputStream input;
        DataOutputStream output;
    }
    
    List<Client> clientList = Collections.synchronizedList(new ArrayList<Client>());
    //List<Socket> socketList = Collections.synchronizedList(new ArrayList<Socket>());
    //ConcurrentHashMap<String, Socket> userSockets= new ConcurrentHashMap<String, Socket>();
    ConcurrentHashMap<String, Client> userSockets= new ConcurrentHashMap<String, Client>();
    ServerSocket server_socket;
    ServerSocket server_socketFTP;
    public static String client_name = "xx";
    int port = 6001;
    int portFTP = 6007;

    public static void main(String[] args) {

        int port;
        if (args.length < 1 || args.length > 2) {
            System.out.println("Usage:");
            System.out.println("   ChatServer <port number>");
            return;
        }
        ChatServer server = new ChatServer();
        port = Integer.parseInt(args[0]);
        client_name = "server";
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
            e.printStackTrace();
            System.exit(0);
        }
        System.out.println("server terminated");
        server.close();
        System.exit(0);
    }


    public void newClient(Socket clientSocket){
        ChatServer server = this;
        Client c = new Client();
        c.socket = clientSocket;
        clientList.add(c);
        Runnable recvTask = () -> {
            
            try {
                DataInputStream input = new DataInputStream(c.socket.getInputStream());
                c.input = input;
                c.output = new DataOutputStream(c.socket.getOutputStream());
                String message;

                if((message = input.readUTF()) != null){
                    c.username = message;
                    userSockets.put(c.username, c);
                    c.portFTP = c.input.readInt();
                    System.out.println(c.username + " @" + c.portFTP + " has joined!");
                }

                while ((message = input.readUTF()) != null) {
                    if(message.equals("m")){
                        if ((message = input.readUTF()) != null) {
                            server.broadcastMessage(c, c.username+": " + message);
                            System.out.println(c.username+": " + message);
                        }
                    }
                    else if(message.equals("f")){
                        String user = input.readUTF();
                        String file_name = input.readUTF();
                        startFTPRelay(c, user, file_name);
                    }
                    else if(message.equals("x")){
                        server.removeClient(c);
                        userSockets.remove(c.username);
                        System.out.println(c.username + " has left the building.");
                    }
                    
                }
                server.removeClient(c);
                userSockets.remove(c.username);
                System.out.println(c.username + " has left the building.");
            } catch (Exception e) {
                System.err.println(e);
                server.removeClient(c);
                System.out.println(c.username + " has left the building.");
                e.printStackTrace();
                //System.out.println("Exception: " + e.getMessage());
            }
        };
        new Thread (recvTask).start();
    }


    public void removeClient(Client c) {
        clientList.remove(c);
    }


    public void broadcastMessage(Client c, String userMessage) {
        for(Client s: clientList){
            if(s == c){
                continue;
            }else{
                try {
                    s.output.writeUTF("m");
                    s.output.writeUTF(userMessage);
                } catch (Exception e) {
                    System.out.println("Exception: " + e.getMessage());
                    e.printStackTrace();
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
            System.err.println(e);
            e.printStackTrace();
        }
    }


    public void startFTPRelay(Client c, String user, String file_name){
        Runnable server_task = () -> {
            try {
                System.err.println("StartFTPRelay: " + user + ", " + file_name);
                server_socketFTP = new ServerSocket(portFTP);

                Client c2 = userSockets.get(user);
                c2.output.writeUTF("f");
                c2.output.writeUTF(file_name);
                int port2 = c2.portFTP;
                System.err.println("Connecting to port " + port2);
                Thread.sleep(500);
                Socket FTP2_socket = new Socket("localhost", port2);
                DataInputStream inputFTP2 = new DataInputStream(FTP2_socket.getInputStream());

                // c.output.writeInt(portFTP);
                Socket socketFTP = server_socketFTP.accept();
                DataOutputStream outputFTP1 = new DataOutputStream(socketFTP.getOutputStream());

                System.err.println("Starting file transfer.");
                byte[] file_buffer= new byte[1500];
		        int number_read;
		        while( (number_read = inputFTP2.read( file_buffer )) != -1 ){
			        outputFTP1.write( file_buffer, 0, number_read );
                }
                System.err.println("done with xfer.");
		        inputFTP2.close();
                outputFTP1.close();

                //socketFTP.shutdownOutput();
                socketFTP.close();
                //FTP2_socket.shutdownOutput();
                FTP2_socket.close();
                server_socketFTP.close();

                System.err.println("FTP server done.");
                System.err.println("FTP server closing socket....");
                server_socketFTP.close();
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
                //System.exit(0);
            }
            System.err.println("FTP server really done.");
        };
        new Thread(server_task).start();
    }

}