/**
 * class for a request for the elevators, keeping track of floor and direction.
 * @author Axelsson
 *
 */
public class Request{
	public int direction;
	public int floor;
	public Request(int floor, int dir){
		this.direction = dir;
		this.floor = floor;
	}
	/**
	 * equals check if objects are equal; if their direction and floor is the same
	 */
	 public boolean equals(Object o){
		 Request r = (Request) o;
		 if(this.direction == r.direction && this.floor == r.floor)
			 return true;
		 return false;
	 }
}