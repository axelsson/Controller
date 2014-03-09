import java.io.IOException;
import java.net.Socket;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

public class ElevatorThread extends Thread{
	//TODO måste fortsätta i samma direction efter button pressed up/ner
	//glapp: hissen fortsätter upp 
	//problem: tryck 2 sen 1 när den precis har passerat 1. samma direction ger fel request först i kön. 
	//om en hiss tvingas ta request i fel riktning ex upp 3, ner 2, och den är på väg upp, kommer två prioriteras högst ändå.
	public int id;
	public boolean willChangeDirection = false;
	private int actualId;
	public double position = 0.0;
	public boolean openDoor = false;
	public int direction = Controller.STOP;
	public boolean moving = false;
	public Integer currentRequest = null;
	private ElevatorWriter writer;
	PriorityQueue<Request> queue =  new PriorityQueue<Request>(10, new Comparator<Request>() {	
			//compare sorts the queue in different orders depending on the direction of the elevator
			public int compare(Request o1, Request o2) {

				switch (direction){
				case 1: 
					//up sorts values in default order
					if (o1.floor > o2.floor){
						return 1;
					}
					//else if(o1.direction != o2.direction)
					//	return 1;
					else{return -1;}
				case 2: 
					//down sorts the values backwards
					if (o1.floor < o2.floor){
						return 1;
					}
					//else if(o1.direction != o2.direction)
						//return -1;
					else{return -1;}
				}
				return 1; }
		});
	
	public ElevatorThread(int id, Socket socket){
		this.id = id;
		this.actualId = id+1;
		try {
			this.writer = new ElevatorWriter(socket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//wait for changed position
	public void run(){
		while(true){
			try {
				synchronized (this){
					this.wait (10);
				}
				if(!queue.isEmpty() && !moving){
					currentRequest = queue.peek().floor;
					System.err.println("Request taken, elevator "+id);
					if(isCloseToFloor())
						openDoor();
					else if(position < currentRequest)
						moveUp();
					else
						moveDown();
				}
				else if(moving && isCloseToFloor()){
					stopMove();
					openDoor();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void setPosition(double position){
		this.position = position;
	}
	public double getPosition(){
		return this.position;
	}
	
	//will remove a queued floor if the elevator is positioned there
	public boolean isCloseToFloor(){
		//an interval is needed to be able to stop at the top floor
		int floor = queue.peek().floor;
		if(position > ((double) floor)-0.05 && position < ((double) floor)+0.05){
			queue.poll();
			return true;
		}
		
		return false;
	}

	public void addRequest(Request r){
		if(!queue.isEmpty()){
			if(queue.peek().floor == r.floor && queue.peek().direction == r.direction){
				System.err.println("Request already exists.");
				return;
			}
		}
		System.err.println("Direction is: "+direction);
		System.err.println("Request for floor "+r.floor+ " added for elevator "+id+ "\n Queue:");
		queue.add(r);
		for (Request request : queue) {
			System.err.println(request.floor);
		}
		currentRequest = queue.peek().floor;
	}

	public void openDoor(){
		writer.openDoor(actualId);
		direction = Controller.STOP;
		openDoor = true;
		try {
			Thread.sleep(2000);

			writer.closeDoor(actualId);
			openDoor = false;
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		moving = false;
	}
	
	public void emergencyStop(){
		moving = false;
		direction = Controller.STOP;
		queue.clear();
		writer.stopElevator(actualId);
	}
	public void stopMove(){
		moving = false;
		direction = Controller.STOP;
		writer.stopElevator(actualId);
	}
	public void moveUp(){
		System.err.println("Elevator "+id+ " moving up");
		moving = true;
		direction = Controller.UP;
		writer.moveElevatorUp(actualId);
	}
	public void moveDown(){
		System.err.println("Elevator "+id+ " moving down");
		moving = true;
		direction = Controller.DOWN;
		writer.moveElevatorDown(actualId);
	}
}
