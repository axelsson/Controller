import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import elevator.rmi.GetAll;
/*TODO
 * om en hiss snart är klar med sin order och nära, låt den ta request
 * 
 * */
public class Controller implements Serializable, Remote{
	private static final long serialVersionUID = 1L;
	public static final int UP = 1;
	public static final int DOWN = -1;
	public static final int STOP = 0;
	private static final int OPEN = 1;
	private static final int CLOSE = -1;
	private double velocity = 1.5686274509803922E-4;
	public int numElevators;
	public Socket socket;
	public double topFloor;

	ElevatorWriter writer;
	ElevatorThread[] elevators;
	double[] positions;

	public Controller (int numElevators){
		this.numElevators = numElevators;
		elevators = new ElevatorThread[numElevators];
		positions = new double[numElevators];
		topFloor = (double) numElevators - 0.04;
		try {
			socket = new Socket("localhost", 4711);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		for (int i = 0; i < numElevators; i++) {
			elevators[i] = new ElevatorThread(i, socket);
		}
	}

	public void connect() throws UnknownHostException, IOException{
		
		BufferedReader inFromServer = new BufferedReader(
									  new InputStreamReader(socket.getInputStream()));
		writer = new ElevatorWriter(socket);
		writer.start();
		for (ElevatorThread elevator : elevators) {
			elevator.start();
		}
		while (true){
			try {
				String input = inFromServer.readLine();
				System.err.println(input);
				handleInput(input.split(" "));
			} catch (IOException e) {
				System.out.println("Bye bye!");
				break;
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
				elevatorPosition(Integer.parseInt(input[1]), Double.parseDouble(input[2])); break;
		}
	}
	
	//decide which elevator should take the request
	public int closestElevator(Request request){
		int floor = request.floor;
		int direction = request.direction;
		int bestSoFar = 0;
		double tmpDistance = numElevators;
		double distance = numElevators;
		for (ElevatorThread elevator : elevators) {
			tmpDistance = Math.abs(elevator.position-floor);
			if(elevator.direction == STOP && tmpDistance <= distance){
				bestSoFar = elevator.id;
				distance = tmpDistance;
				continue;
			}
			boolean differentDirections = (elevator.direction != direction);
			boolean bothUp = (elevator.direction == direction && direction == UP);
			boolean bothDown = (elevator.direction == direction && direction == DOWN);
			if(differentDirections || (bothUp && (elevator.position > floor)) || (bothDown && (elevator.position < floor))){
				continue;
			}
			if(tmpDistance <= distance){
				distance = tmpDistance;
				bestSoFar = elevator.id;
			}
		}
		return bestSoFar;
	}
	
	//input v velocity
	public void velocityChanged(double velocity){
		this.velocity = velocity;
		System.err.println("changed velocity!");
	}

	//input b elevator direction 
	public void buttonPressed(int floor, int direction){
		Request r = new Request(floor, direction);
		int id = closestElevator(r);
		elevators[id].addRequest(floor);
		System.err.println("Button pressed at floor "+floor+ " with dir "+direction+ "\n elevator assigned:"+id);
		/*if(direction == UP)
			writer.moveElevatorUp(id);
		else
			writer.moveElevatorDown(id);*/
	}

	//input p elevator destination
	public void panelPressed(int elevator, int destination){
		elevators[elevator-1].addRequest(destination);
	}

	//input f elevator position
	public void elevatorPosition(int elevator, double position){
		position = cutDecimals(position);
		elevators[elevator-1].position = position;
		positions[elevator-1] = position;
		//gör varje position till en monitor?
		/*if (position == 2.00){
			writer.stopElevator(elevator);
		}*/
	}

	public static void main(String[] args) throws UnknownHostException, IOException{
		int numberOfElevators = 5;
		Controller c = new Controller(numberOfElevators);
		//RMI rmi = new RMI();
		c.connect();
	}

	
	public double cutDecimals(double n){
		BigDecimal fd = new BigDecimal(n);
		BigDecimal cutted = fd.setScale(2, RoundingMode.DOWN);
		return cutted.doubleValue();
	}
	


}