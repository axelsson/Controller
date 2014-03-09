import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Remote;
/*TODO
 * om en hiss snart är klar med sin order och nära, låt den ta request
 * måste räkna ut kostnad för halvklar hiss, och ha koll på vilket håll den ska åt från button req
 * lägg på kostnad för dörröppning också
 * problem: om hissen får req från knapptryckning på samma våning fast ett upp och ett ner. Går bra när första är i samma riktning som hissen
 * 
 * */

//en hiss, flera request åt olika håll. 
public class Controller implements Serializable, Remote{
	private static final long serialVersionUID = 1L;
	public static final int UP = 1;
	public static final int DOWN = -1;
	public static final int STOP = 0;
	private double velocity = 1.5686274509803922E-4;
	public int numElevators;
	public Socket socket;

	ElevatorWriter writer;
	ElevatorThread[] elevators;
	double[] positions;

	public Controller (int numElevators){
		this.numElevators = numElevators;
		elevators = new ElevatorThread[numElevators];
		positions = new double[numElevators];
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
		//knapp i motsatt direction ändrar inte direction när den åker till platsen
		//båda uppåt, befinner sig ovanför men tidigare request ska nedåt
		int floor = request.floor;
		int direction = request.direction;
		int bestSoFar = 0;
		double tmpDistance = numElevators;
		double distance = numElevators+1;
		for (ElevatorThread elevator : elevators) {
			tmpDistance = Math.abs(elevator.position-floor);
			System.out.println("elev "+elevator.id+ " " +"tmpdist:"+tmpDistance + " vs dist:"+distance);
			if(elevator.direction == STOP){
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
	
	//input v velocity
	public void velocityChanged(double velocity){
		this.velocity = velocity;
		System.err.println("changed velocity!");
	}

	//input b elevator direction 
	public void buttonPressed(int floor, int direction){
		Request r = new Request(floor, direction);
		int id = closestElevator(r);
		elevators[id].addRequest(r);
		//System.err.println("Button r at floor "+floor+ " with dir "+direction+ "\n elevator assigned:"+id);
	}

	//input p elevator destination
	public void panelPressed(int elevator, int destination){
		int direction = getDirection(elevator, destination);
		Request r = new Request(destination, direction);
		if(destination == 32000)
			elevators[elevator-1].emergencyStop();
		else
			elevators[elevator-1].addRequest(r);
	}

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

	//input f elevator position
	public void elevatorPosition(int elevator, double position){
		position = cutDecimals(position);
		elevators[elevator-1].setPosition(position);
	}

	public static void main(String[] args) throws UnknownHostException, IOException{
		int numberOfElevators = 5;
		Controller c = new Controller(numberOfElevators);
		c.connect();
	}

	public double cutDecimals(double n){
		BigDecimal fd = new BigDecimal(n);
		BigDecimal cutted = fd.setScale(2, RoundingMode.DOWN);
		return cutted.doubleValue();
	}
}