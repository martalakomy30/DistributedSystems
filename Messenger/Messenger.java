import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.Thread;

public class Messenger implements Runnable {

    public Socket socket = null;
    ServerSocket server_socket;

    public static void main(String[] args) {

        int port;
        if (args.length < 1 || args.length > 2) {
            System.out.println("Usage:");
            System.out.println("   MessengerServer -l <port number>");
            System.out.println("   MessengerClient <port number>");
            return;
        }

        Messenger M = new Messenger();
        if (args.length == 2 && args[0].startsWith("-l")) {
            port = Integer.parseInt(args[1]);
            try {
                ServerSocket server_socket = new ServerSocket(port);
                Socket client_socket = server_socket.accept();
                M.socket = client_socket;
                M.server_socket = server_socket;
            } catch (Exception e) {
                //System.out.println("Exception: " + e.getMessage());
            }
        } else {
            port = Integer.parseInt(args[0]);
            try {
                Socket client_socket = new Socket("localhost", port);
                M.socket = client_socket;
            } catch (Exception e) {
               // System.out.println("Exception: " + e.getMessage());
            }
        }

        Thread t1 = new Thread(M, "Send");
        t1.start();
        M.runRecv();
        M.close();
        System.exit(0);

    }

    public void close(){
        try{
            if(socket != null){            
                socket.shutdownOutput();
                socket.close();
            } 
            if(server_socket != null){            
                server_socket.close();
            } 
        }
        catch(Exception e){}
    }

    public void runRecv() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String message;
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            while ((message = reader.readLine()) != null) {
                output.writeUTF(message);
            }
            close();

        } catch (EOFException eofx) {
           // System.out.println("EOF encountered; other side shut down");
        } catch (IOException e1) {e1.printStackTrace();}

    }

    public void run() {
        try {
            DataInputStream input = new DataInputStream(socket.getInputStream());
            String message;
            while ((message = input.readUTF()) != null) {
                System.out.println(message);
            }
            close();
        } catch (Exception e) {//  System.out.println("Exception: " + e.getMessage());
        }
    }
}
