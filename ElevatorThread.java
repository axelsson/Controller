import java.util.Queue;


public class ElevatorThread extends Thread{
	public int id;
	public double position = 0.0;
	public boolean openDoor = false;
	public boolean moving = false;
	public Queue queue;
	private ElevatorWriter writer;
	public ElevatorThread(int id, ElevatorWriter writer){
		this.id = id;
		this.writer = writer;
	}
	//wait for changed position
	public void run(){
		while(!queue.isEmpty()){
			
		}
	}
	public void setPosition(double position){
		this.position = position;
	}
	
	public void openDoor(){
		
	}
}
