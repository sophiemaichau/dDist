import java.net.*;
import java.io.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Queue;
/**
*
* A very simple server which will way for a connection from a client and print
* what the client sends. When the client closes the connection, the server is
* ready for the next client.
*/

public class dServer {

  /*
  * Your group should use port number 40HGG, where H is your "hold nummer (1,2 or 3)
  * and GG is gruppe nummer 00, 01, 02, ... So, if you are in group 3 on hold 1 you
  * use the port number 40103. This will avoid the unfortunate situation that you
  * connect to each others servers.
  */
  protected int portNumber = 40499;
  protected ServerSocket serverSocket;
  private LinkedBlockingQueue<QAWrapper> questionQueue = new LinkedBlockingQueue<QAWrapper>();


  /**
  *
  * Will print out the IP address of the local host and the port on which this
  * server is accepting connections.
  */
  protected void printLocalHostAddress() {
    try {
      InetAddress localhost = InetAddress.getLocalHost();
      String localhostAddress = localhost.getHostAddress();
      System.out.println("Contact this server on the IP address " + localhostAddress);
    } catch (UnknownHostException e) {
      System.err.println("Cannot resolve the Internet address of the local host.");
      System.err.println(e);
      System.exit(-1);
    }
  }

  /**
  *
  * Will register this server on the port number portNumber. Will not start waiting
  * for connections. For this you should call waitForConnectionFromClient().
  */
  protected void registerOnPort() {
    try {
      serverSocket = new ServerSocket(portNumber);
    } catch (IOException e) {
      serverSocket = null;
      System.err.println("Cannot open server socket on port number" + portNumber);
      System.err.println(e);
      System.exit(-1);
    }
  }

  public void deregisterOnPort() {
    if (serverSocket != null) {
      try {
        serverSocket.close();
        serverSocket = null;
      } catch (IOException e) {
        System.err.println(e);
      }
    }
  }

  /**
  *
  * Waits for the next client to connect on port number portNumber or takes the
  * next one in line in case a client is already trying to connect. Returns the
  * socket of the connection, null if there were any failures.
  */
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
    printLocalHostAddress();
    registerOnPort();
    while (true) {
      Socket socket = waitForConnectionFromClient();
      System.out.println("Connection from: " + socket);
      new Thread(new QuestionHandler(socket, questionQueue)).start();
    }
  }



  public static void main(String[] args) throws IOException {
    dServer server = new dServer();
    server.run();
  }

}
