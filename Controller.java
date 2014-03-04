import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Remote;
/*
 * Listener en tråd
 * Printer en tråd
 * Listener läser in vad som händer -> skickar input till controller -> avgör vad som ska hända -> skickar output till printer
 * */
public class Controller implements Serializable, Remote{
	private static final long serialVersionUID = 1L;
	private static final int UP = 1;
	private static final int DOWN = -1;
	private static final int STOP = 0;
	private static final int OPEN = 1;
	private static final int CLOSE = -1;
	private double velocity = 1.5686274509803922E-4;
	public int numElevators;

	ElevatorListener listener;
	ElevatorWriter writer;
	ElevatorThread[] elevators;

	public Controller (int numElevators){
		this.numElevators = numElevators;
		elevators = new ElevatorThread[numElevators];
	}

	public void connect() throws UnknownHostException, IOException{
		Socket socket = new Socket("localhost", 4711); 
		BufferedReader inFromServer = 	new BufferedReader(
				new InputStreamReader(socket.getInputStream()));
		//listener = new ElevatorListener(clientSocket);
		//listener.start();
		writer = new ElevatorWriter(socket);
		writer.start();
		while (true){
			try {
				String input = inFromServer.readLine();
				System.err.println(input);
				handleInput(input.split(" "));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}	
	public void handleInput(String[] input){
		String letter = input[0];
		switch (letter) {
			case "v":
				velocityChanged(Double.parseDouble(input[1])); break;
			case "b":
				buttonPressed(Integer.parseInt(input[1]), Integer.parseInt(input[2])); break;
			case "p":
				panelPressed(Integer.parseInt(input[1]), Integer.parseInt(input[2])); break;
			case "f":
				elevatorMoving(Integer.parseInt(input[1]), Double.parseDouble(input[2])); break;
		}
	}
	//input v velocity
	public void velocityChanged(double velocity){
		this.velocity = velocity;
		System.err.println("changed velocity!");
	}

	//input b elevator direction 
	public void buttonPressed(int elevator, int floor){
		writer.moveElevatorUp(1);
	}

	//input p elevator destination
	public void panelPressed(int elevator, int floor){

	}

	//input f elevator position
	public void elevatorMoving(int elevator, double position){
		position = cutDecimals(position);
		if (position == 2.0){
			writer.stopElevator(1);
		}
	}

	public static void main(String[] args) throws UnknownHostException, IOException{
		int numberOfElevators = 5;
		Controller c = new Controller(numberOfElevators);
		c.connect();
	}
	
	public double cutDecimals(double n){
		BigDecimal fd = new BigDecimal(n);
		BigDecimal cutted = fd.setScale(1, RoundingMode.DOWN);
		return cutted.doubleValue();
	}


}