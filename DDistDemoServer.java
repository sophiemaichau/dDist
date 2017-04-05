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

public class DDistDemoServer {

  /*
  * Your group should use port number 40HGG, where H is your "hold nummer (1,2 or 3)
  * and GG is gruppe nummer 00, 01, 02, ... So, if you are in group 3 on hold 1 you
  * use the port number 40103. This will avoid the unfortunate situation that you
  * connect to each others servers.
  */
  protected int portNumber = 40307;
  protected ServerSocket serverSocket;
  private LinkedBlockingQueue<QASocket> questionQueue = new LinkedBlockingQueue<QASocket>();


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

// Adding new questions to the que
  public void listenForNewQuestion(){
    new Thread(new Runnable() {

      public void run(){

        while(true){
          Socket socket = waitForConnectionFromClient();

          if(socket != null){
        	  System.out.println("Connection from: " + socket);
	          try{
            ObjectInputStream objInput = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream objOutput = new ObjectOutputStream(socket.getOutputStream());
	          QA qa;

	          // checks if there is any new questions to be added to the que
	          while((qa = (QA) objInput.readObject()) != null){
	            QASocket qaSocket = new QASocket(qa, socket, objOutput, objInput);
	            questionQueue.add(qaSocket);
	          }
	        } catch (IOException e){
	            System.err.println(e);
              System.out.println("listenForNewQuestion");
	         } catch(ClassNotFoundException c){
	        	 System.err.println(c);
	         }
          } else{
        	  break;
          }
        }
      }

    }).start();
  }

  public void run() {
    printLocalHostAddress();
    registerOnPort();
    //listenForNewQuestion();

    while (true) {
      Socket socket = waitForConnectionFromClient();
      System.out.println("Connection from: " + socket);
      new Thread(new QuestionHandler(socket, questionQueue)).start();


      /*
      QASocket qaSocket = null;
		try {
			qaSocket = questionQueue.take();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
      QA qa = qaSocket.qa;
      Socket socket = qaSocket.socket;
      String s;
      BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
      System.out.print("Received question: " + qa.getQuestion() +
      "\nType an answer and press ENTER> ");
      try {
        //wait for stdinput. Closes down thread afterwards.
    	ObjectOutputStream objOutput = qaSocket.outputStream;
    	s = stdin.readLine();
        qa.setAnswer(s);
        objOutput.writeObject(qa);
        objOutput.flush();
      } catch (IOException e) {
        System.err.println(e);
      }*/
    }
//    deregisterOnPort();
//    System.out.println("Goodbuy world!");
  }

  public static void main(String[] args) throws IOException {
    DDistDemoServer server = new DDistDemoServer();
    server.run();
  }

}
