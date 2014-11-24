package designProject;

import lejos.nxt.*;

public class TestNXT {

	public static void main(String[] args) {
		
			// TODO Auto-generated method stub
	System.out.println("Hello");
	Button.waitForAnyPress();
	
	theNextSensor myUart = new theNextSensor(SensorPort.S1, false);

	myUart.setMode(theNextSensor.Mode.MODE_CONTINUOUS);
	
	int myDistance = myUart.getDistance();
	if (myDistance == -1) {
		System.out.println(myUart.getErrorTrace());
	}
	int[] dist = new int[theNextSensor.MAX_DISTANCES];	
	// Get multiple distance values
	if(myUart.getDistance(dist, 8) == -1) {
		// An error occurred while reading the distances
		System.out.println(myUart.getErrorTrace());
		return;
	}
	
	Button.waitForAnyPress();
	
	
		/*int myDistance;
		int[] dist = new int[theNextSensor.MAX_DISTANCES];
		
		System.out.println("Hello World!");
		theNextSensor myUart = new theNextSensor();

		// Set the mode to off
		myUart.setMode(theNextSensor.Mode.MODE_OFF);

		// Get the distance but it should return an error because the sensor is off
		myDistance = myUart.getDistance();
		if (myDistance == -1) {
			System.out.println(myUart.getErrorTrace());
		}
		// Set the mode to continuous
		myUart.setMode(theNextSensor.Mode.MODE_CONTINUOUS);

		// Get the distance
		myDistance = myUart.getDistance();
		System.out.println("Distance is: " + myDistance);
		
		if (myDistance == -1) {
			// Either an error occurred or there is no object detected
			System.out.println(myUart.getErrorTrace());
		}
		
		// Set the mode to Ping
		myUart.setMode(theNextSensor.Mode.MODE_PING);
		
		// Get multiple distance values
		if(myUart.getDistance(dist, 8) == -1) {
			// An error occurred while reading the distances
			System.out.println(myUart.getErrorTrace());
			return;
		}
		
		System.out.println("Multiple distance values: ");
		for (int i=0; i<7; i++) {
			System.out.print(dist[i] + ", ");
		}
		System.out.println("\n");
		*/
	}

}
