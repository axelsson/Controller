import java.io.IOException;
import java.net.Socket;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

public class ElevatorThread extends Thread{
	public int id;
	private int actualId;
	public double position = 0.0;
	public boolean openDoor = false;
	public int direction = Controller.STOP;
	public boolean moving = false;
	public Integer currentRequest = null;
	private ElevatorWriter writer;
	PriorityQueue<Integer> queue =  new PriorityQueue<Integer>(10, new Comparator<Integer>() {	
			//compare sorts the queue in different orders depending on the direction of the elevator
			public int compare(Integer o1, Integer o2) {
				switch (direction){
				case 1: 
					//up sorts values in default order
					if (o1 > o2){
						return 1;
					}
					else{return -1;}
				case 2: 
					//down sorts the values backwards
					if (o1 < o2){
						return 1;
					}
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
					currentRequest = queue.peek();
					System.err.println("Request taken, elevator "+id);
					if(isCloseToFloor(position, currentRequest))
						openDoor();
					else if(position < currentRequest)
						moveUp();
					else
						moveDown();
				}
				else if(moving && isCloseToFloor(position, currentRequest)){//position == (double) currentRequest.floor){
					stopMove();
					openDoor();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public void stopOrder(){
		this.position = position;
	}
	public boolean isCloseToFloor(double position, int floor){
		if(position > ((double) floor)-0.05 && position < ((double) floor)+0.05)
			return true;
		return false;
	}

	public void addRequest(int destination){
		if(queue.contains(destination)){
			System.err.println("Request already exists.");
			return;
		}
		queue.add(destination);
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
		queue.poll();
	}

	public void stopMove(){
		moving = false;
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
