import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
/**
 * class for writing messages to the elevator program. 
 * @author Axelsson
 */
public class ElevatorWriter extends Thread {
	private static final int UP = 1;
	private static final int DOWN = -1;
	private static final int STOP = 0;
	private static final int OPEN = 1;
	private static final int CLOSE = -1;
	DataOutputStream outToServer;
	
	/**
	 * The constructor will set up an outputstream to the green elevator application.
	 * @param clientSocket
	 * @throws IOException
	 */
	public ElevatorWriter(Socket clientSocket) throws IOException {
		outToServer = new DataOutputStream(clientSocket.getOutputStream()); 
	}
	
	/**
	 * sendMessage is a synchronized method since object is used by several threads. It will write to the green elevator.
	 */
	public synchronized void sendMessage(String msg){
		try {
			outToServer.writeBytes(msg+'\n');
			//System.out.println(msg+'\n');
		} catch (IOException e) {
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
