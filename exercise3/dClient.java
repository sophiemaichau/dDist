import java.net.*;
import java.io.*;
import java.io.ObjectInputStream;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
*
* A very simple client which will connect to a server, read from a prompt and
* send the text to the server.
*/

public class dClient {

  /*
  * Your group should use port number 40HGG, where H is your "hold nummer (1,2 or 3)
  * and GG is gruppe nummer 00, 01, 02, ... So, if you are in group 3 on hold 1 you
  * use the port number 40103. This will avoid the unfortunate situation that you
  * connect to each others servers.
  */
  protected int portNumber = 40499;

  /**
  *
  * Will print out the IP address of the local host on which this client runs.
  */
  protected void printLocalHostAddress() {
    try {
      InetAddress localhost = InetAddress.getLocalHost();
      String localhostAddress = localhost.getHostAddress();
      System.out.println("I'm a client running with IP address " + localhostAddress);
    } catch (UnknownHostException e) {
      System.err.println(e);
    }
  }

  /**
  *
  * Connects to the server on IP address serverName and port number portNumber.
  */
  protected Socket connectToServer(String serverName) {
    Socket res = null;
    try {
      res = new Socket(serverName,portNumber);
    } catch (IOException e) {
      // We return null on IOExceptions
    }
    return res;
  }

  public void run(String serverName) {
    System.out.println("Starting client. Type CTRL-D to shut down.");
    printLocalHostAddress();
    Socket socket = connectToServer(serverName);
    if (socket != null) {
      System.out.println("Connected to " + socket);
      try {
        // For reading from standard input - exercise 2
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        //For sending objects to the server
        ObjectOutputStream objOutStream = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream objInputStream = new ObjectInputStream(socket.getInputStream());
        objOutStream.flush();
        listenOnServerAnswer(socket, objInputStream);
        listenOnQuestionInput(socket, stdin, objOutStream);
        socket.close();
      } catch (IOException e) {
        System.err.println(e);
      }
    }
  }

  public void listenOnQuestionInput(Socket socket, BufferedReader stdin,
                  ObjectOutputStream objOutStream) throws IOException {
    String s;
    // Read from standard input and send question object to server
    System.out.print("Type a question and then RETURN> ");
    while ((s = stdin.readLine()) != null) {
      System.out.print("Type a question and then RETURN> ");
      QA qa = new QA();
      qa.setQuestion(s);
      objOutStream.writeObject(qa);
      objOutStream.flush();
    }
  }

  public void listenOnServerAnswer(Socket socket, ObjectInputStream objInputStream) {
    new Thread(new Runnable() {
      public void run() {
        QA qa;
        try {
          // Read and print what the client is sending
          while ((qa = (QA) objInputStream.readObject()) != null) {
            System.out.println("\nThe answer to the question: " + qa.getQuestion() +
            " is: " + qa.getAnswer());
            System.out.print("Type a question and then RETURN> ");
          }
        } catch (IOException e) {
          System.err.println(e);
        } catch (Exception e) {
          System.err.println(e);
        }
      }
    }).start();
  }

  public static void main(String[] args) throws IOException {
    dClient client = new dClient();
    client.run(args[0]);
  }

}
