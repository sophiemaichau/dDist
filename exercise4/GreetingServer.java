// File Name GreetingServer.java
import java.net.*;
import java.io.*;

public class GreetingServer extends Thread {
   private ServerSocket serverSocket;
   private int port = 40307;
   private ConnectionHandler handler;
   private EventReplayer er;

   public GreetingServer(EventReplayer er) throws IOException {
     this.er = er;
     serverSocket = new ServerSocket(port);
   }

   protected Socket waitForConnectionFromClient() {
     Socket res = null;
     try {
       res = serverSocket.accept();
     } catch (IOException e) {
       System.err.println(e);
     }
     return res;
   }

   public void run() {
      while(true) {
         try {
            System.out.println("Waiting for client on port " +
               serverSocket.getLocalPort() + "...");
            Socket socket = waitForConnectionFromClient();

            System.out.println("Connection from " + socket.getRemoteSocketAddress());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            ObjectOutputStream objOutStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objInputStream = new ObjectInputStream(socket.getInputStream());
            handler = new ConnectionHandler(socket, objInputStream, objOutStream);
            er.setConnectionHandler(handler);
         }catch(SocketTimeoutException s) {
            System.out.println("Socket timed out!");
            break;
         }catch(IOException e) {
            System.err.println(e);
            break;
         }
      }
   }
   public ConnectionHandler getConnectionHandler() {
     return handler;
   }
}
