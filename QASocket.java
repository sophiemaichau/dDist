import java.net.Socket;

// wrapper
public class QASocket{
	public Socket socket;
	public QA qa;

	public QASocket(QA qa, Socket socket){
		this.socket = socket;
		this.qa = qa;
	}
}