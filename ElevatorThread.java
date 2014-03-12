import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * An elevator thread simulates an elevator. It will wait for being notified with new information from the 
controller, and then check for some conditions to decide what to do. It sends commands to a writer.
 * @author Axelsson
 *
 */
public class ElevatorThread extends Thread{
	public int id;
	private int actualId;
	public double position = 0.0;
	public int direction = STOP;
	public boolean moving = false;
	public Integer currentRequest = null;
	public static final int UP = 1;
	public static final int DOWN = -1;
	public static final int STOP = 0;
	private ElevatorWriter writer;
	//a queue for requests that will be handled later on
	private Queue<Request> nextRoundRequests = new LinkedList<Request>();
	//the main queue, containing requests sorted depending on direction
	PriorityQueue<Request> queue =  new PriorityQueue<Request>(10, new Comparator<Request>() {	
			//compare sorts the queue in different orders depending on the direction of the elevator
			public int compare(Request o1, Request o2) {
				switch (direction){
				case 1: 
					//up sorts values in default order
					if (o1.floor > o2.floor){
						return 1;
					}
					else{return -1;}
				case -1: 
					//down sorts the values backwards
					if (o1.floor < o2.floor){
						return 1;
					}
					else{return -1;}
				}
				return 1; }
		});
	
	/**
	 * ElevatorThread simply sets some values needed.
	 * @param id
	 * @param writer
	 */
	public ElevatorThread(int id, ElevatorWriter writer){
		this.id = id;
		this.actualId = id+1;
		this.writer = writer;
	}
	
	/**
	 * Run action while the controller is running
	 */
	public void run(){
		while(Controller.isRunning){
			action();
		}
	}
	/**
	 * action will wait on notify, then the current conditions to see if there is a queue to fill, 
	 * take a new request or let someone on/off the elevator.
	 */
	public void action(){
		try {
			synchronized (this){
				this.wait (1000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//if the queue is empty, add remaining requests
		if(queue.isEmpty()){
			queue.addAll(nextRoundRequests);
			nextRoundRequests.clear();
		}
		//is there are requests left but the elevator is standing still, take the next request.
		if(!queue.isEmpty() && !moving){
			currentRequest = queue.peek().floor;
			if(direction == STOP)
				direction = queue.peek().direction;
			System.err.println("directioN:"+direction);
			System.err.println("current request:"+currentRequest);
			//if the request is on the same floor, open up and serve that request, else move in the right way
			if(isCloseToFloor(currentRequest))
				openDoor();
			else if(position < currentRequest)
				moveUp();
			else
				moveDown();
		}
		//if the elevator is moving and is in position to let someone off/on, do it. 
		else if(moving && isCloseToFloor(currentRequest)){
			stopMove();
			openDoor();
			if(queue.isEmpty())
				direction = STOP;
		}
	}
	
	/**
	 * setPosition will update the position and notify the thread.
	 * @param position
	 */
	public void setPosition(double position){
		this.position = position;
		for (int i = 0; i < Controller.numFloors+1; i++) {
			if(isCloseToFloor(i))
				writer.setScale(this.actualId, i);
		}
		synchronized (this){
			this.notify ();
		}
	}
	
	public double getPosition(){
		return this.position;
	}
	
	/**
	 * isCloseToFloor will remove a queued request if the elevator is positioned at the request's destination.
	 * @return true if a request was removed, false otherwise
	 */
	public boolean isCloseToFloor(int floor){
		//an interval is needed to be able to stop at the top floor
		if(position > ((double) floor)-0.05 && position < ((double) floor)+0.05){
			return true;
		}
		return false;
	}

	/**
	 * addRequest will add a new request to one of the queues if it doesn't exist already.
	 * @param request
	 */
	public void addRequest(Request request){
		if(!queue.isEmpty()){
			if(queue.contains(request)){
				return;
			}
		}
		//if it is headed in the wrong direction or badly situated, place it in the next-queue
		if ((direction != request.direction && (direction != STOP))||(direction == UP && request.floor < position) || (direction == DOWN && request.floor > position)){
			System.err.println("added request for floor: "+request.floor+" to next queue");
			nextRoundRequests.add(request);
			return;
		}
		//else just add it and update the current request (which might be the new one)
		queue.add(request);
		currentRequest = queue.peek().floor;
		System.err.println("directioN;"+direction);
		System.err.println("added req:"+request.floor+" to queue, current req is: "+currentRequest);
		//to get going, notify thread, else it wont move.
		if(queue.size() == 1){
			synchronized (this){
				this.notify ();
			}
		}
	}

	/**
	 * openDoor will order the writer to open the door, sleep for a while, then close again.
	 */
	public void openDoor(){
		queue.poll();
		writer.openDoor(actualId);
		//direction = Controller.STOP;
		try {
			Thread.sleep((int)(2000*(Controller.initialVelocity/Controller.velocity)));

			writer.closeDoor(actualId);
			Thread.sleep((int) (1000*(Controller.initialVelocity/Controller.velocity)));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		moving = false;
	}
	
	/**
	 * emergencyStop will stop everything and empty the queues.
	 */
	public void emergencyStop(){
		moving = false;
		direction = STOP;
		queue.clear();
		nextRoundRequests.clear();
		writer.stopElevator(actualId);
	}
	
	/**
	 * stopMove will order the writer to send stop moving message.
	 */
	public void stopMove(){
		moving = false;
		//direction = Controller.STOP;
		writer.stopElevator(actualId);
	}
	
	/**
	 * moveUp will order the writer to send move message and will also update direction. 
	 */
	public void moveUp(){
		System.err.println("Elevator "+id+ " moving up");
		moving = true;
		direction = Controller.UP;
		writer.moveElevatorUp(actualId);
	}
	/**
	 * moveDown will order the writer to send move message and will also update direction. 
	 */
	public void moveDown(){
		System.err.println("Elevator "+id+ " moving down");
		moving = true;
		direction = Controller.DOWN;
		writer.moveElevatorDown(actualId);
	}
}
