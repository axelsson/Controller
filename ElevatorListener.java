import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ElevatorListener extends Thread{
	BufferedReader inFromServer;
	public ElevatorListener (Socket reader) { 
		try {
			inFromServer = new BufferedReader(
			               new InputStreamReader(reader.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} 
	public void run () {
		while (true){
			try {
				String input = inFromServer.readLine();
				System.err.println(input);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	//input v velocity
	public void velocityChanged(int velocity){
		
	}
	
	//input b elevator direction 
	public void buttonPressed(int elevator, int floor){
		
	}
	
	//input p elevator destination
	public void panelPressed(int elevator, int floor){
		
	}
	
	//input f elevator position
	public void elevatorMoving(int elevator, double position){
		
	}
}