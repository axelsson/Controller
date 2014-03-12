import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Socket;
import java.net.UnknownHostException;
/**
 * 
 * This program is meant to be the controller for the green elevator application. 
 * 
 * Controller is the main class that will listen for input and decide what to do with it. 
 * It will decide which elevator should take a request, and notify a thread when an update of 
 * position or a new request is available.
 * @author Axelsson
 *
 */
public class Controller{
	public static final int UP = 1;
	public static final int DOWN = -1;
	public static final int STOP = 0;
	public static double velocity = 1.5686274509803922E-4;
	public static double initialVelocity = 1.5686274509803922E-4;
	public static int numElevators;
	public static int numFloors;
	public Socket socket;
	public static boolean isRunning = true;

	ElevatorWriter writer;
	ElevatorThread[] elevators;

	/**
	 * The controller will have a number of elevator threads, given in input, and a socket for reading input
	 * @param numElevators
	 */
	public Controller (int numberOfElevators, int numberOfFloors){
		numElevators = numberOfElevators;
		numFloors = numberOfFloors;
		
		elevators = new ElevatorThread[numElevators];
		try {
			socket = new Socket("localhost", 4711);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 

	}

	/**
	 * Connect starts the threads and begins to read input.
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void connect() throws UnknownHostException, IOException{
		
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer = new ElevatorWriter(socket);
		writer.start();
		for (int i = 0; i < numElevators; i++) {
			elevators[i] = new ElevatorThread(i, writer);
		}
		for (ElevatorThread elevator : elevators) {
			elevator.start();
		}
		while (true){
			try {
				String input = inFromServer.readLine();
				//System.err.println(input);
				handleInput(input.split(" "));
			} catch (IOException e) {
				System.out.println("Bye bye!");
				isRunning = false;
				socket.close();
				for (ElevatorThread elevator : elevators) {
					try {
						elevator.join();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				break;
			}
		}
	}	
	
	/**
	 * handleInput parses input and decides what method to run.
	 * @param input
	 */
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
	
	/**
	 * closestElevator will decide which elevator should take the request given in input
	 * @param request
	 * @return the chosen elevators id
	 */
	public int closestElevator(Request request){
		int floor = request.floor;
		int direction = request.direction;
		int bestSoFar = 0;	//keep track on which elevator is best fit for taking the request
		double tmpDistance = numElevators;
		double distance = numFloors;
		for (ElevatorThread elevator : elevators) {
			tmpDistance = Math.abs(elevator.position-floor);
			//if the elevator stands still, check the distance and update if better
			if(elevator.queue.isEmpty()){
				if(tmpDistance <= distance){
					bestSoFar = elevator.id;
					distance = tmpDistance;
				}
				continue;
			}
			boolean differentDirections = (elevator.direction != direction);
			boolean bothUp = (elevator.direction == direction && direction == UP);
			boolean bothDown = (elevator.direction == direction && direction == DOWN);
			boolean wrongDirection = (direction != elevator.queue.peek().direction);
			//check several conditions for when it is not good to queue the request
			if(differentDirections || (bothUp && (elevator.position > floor)) || (bothDown && (elevator.position < floor) || wrongDirection)){
				continue;
			}
			if(tmpDistance <= distance){
				distance = tmpDistance;
				bestSoFar = elevator.id;
			}
		}
		return bestSoFar;
	}
	
	/**
	 * velocityChanged will take a new velocity as input.
	 * @param newVelocity
	 */
	public void velocityChanged(double newVelocity){
		velocity = newVelocity;
	}

	/**
	 * buttonPressed takes a floor and a direction, creates a new request and sends it to the best elevator. 
	 * @param floor
	 * @param direction
	 */
	public void buttonPressed(int floor, int direction){
		Request r = new Request(floor, direction);
		int id = closestElevator(r);
		elevators[id].addRequest(r);
	}

	/**
	 * panelPressed takes an elevator id and a destination, creates a new request and sends it to the given elevator. 
	 * Calls emergencyStop if the request destination is 3200.
	 * @param floor
	 * @param direction
	 */
	public void panelPressed(int elevator, int destination){
		int direction = getDirection(elevator, destination);
		Request r = new Request(destination, direction);
		if(destination == 32000)
			elevators[elevator-1].emergencyStop();
		else
			elevators[elevator-1].addRequest(r);
	}

	/**
	 * getDirection calculates the direction of the request relative to the elevator position.
	 * @param elevator
	 * @param destination
	 * @return direction
	 */
	private int getDirection(int elevator, int destination) {
		double elevatorPosition = elevators[elevator-1].getPosition();
		if(destination > elevatorPosition){
			return UP;
		}
		else if(destination < elevatorPosition){
			return DOWN;
		}
		return STOP;
	}

	/**
	 * elevatorPosition takes an elevator id and a position as input and calls setPosition on the elevator object.
	 * @param elevator
	 * @param position
	 */
	public void elevatorPosition(int elevator, double position){
		position = cutDecimals(position);
		elevators[elevator-1].setPosition(position);
	}

	/**
	 * Main method, arguments should be a number of elevators
	 * @param args
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public static void main(String[] args) throws UnknownHostException, IOException{
		if(args.length != 2){
			System.err.println("Incorrect arguments. Need to give a number of elevators");
			return;
		}
		int numberOfElevators = Integer.parseInt(args[0]);
		int numberOfFloors = Integer.parseInt(args[1]);
		Controller c = new Controller(numberOfElevators, numberOfFloors);
		c.connect();
	}

	/**
	 * cutDecimals is used to cut the double values of a position into something more useful.
	 * @param n
	 * @return double with only two decimals
	 */
	public double cutDecimals(double n){
		BigDecimal fd = new BigDecimal(n);
		BigDecimal cutted = fd.setScale(2, RoundingMode.DOWN); 
		return cutted.doubleValue();
	}
}