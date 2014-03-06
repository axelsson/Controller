import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ElevatorWriter extends Thread {
	private static final int UP = 1;
	private static final int DOWN = -1;
	private static final int STOP = 0;
	private static final int OPEN = 1;
	private static final int CLOSE = -1;
	DataOutputStream outToServer;
	
	public ElevatorWriter(Socket clientSocket) throws IOException {
		outToServer = new DataOutputStream(clientSocket.getOutputStream()); 
	}

	public void run(){
		BufferedReader inFromUser = new BufferedReader( new InputStreamReader(System.in));
		/*while(true){
			String sentence;
			try {
				sentence = inFromUser.readLine();
				outToServer.writeBytes(sentence + '\n'); 
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}*/
	}
	
	public void sendMessage(String msg){
		try {
			outToServer.writeBytes(msg+'\n');
			System.out.println(msg+'\n');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void moveElevatorUp(int id){
		sendMessage("m "+id+" "+UP);
	}
	
	public void moveElevatorDown(int id){
		sendMessage("m "+id+" "+DOWN);
	}
	public void stopElevator(int id){
		sendMessage("m "+id+" "+STOP);
	}
	
	public void setScale(int id, int floor){
		sendMessage("s "+id+" "+floor);
	}
	
	public void inspectElevator(int id){
		sendMessage("w "+id);
	}
	
	public void openDoor(int id){
		sendMessage("d "+id+" "+OPEN);
	}
	
	public void closeDoor(int id){
		sendMessage("d "+id+" "+CLOSE);
	}
}
