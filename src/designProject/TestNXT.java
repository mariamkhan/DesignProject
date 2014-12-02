package designProject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import lejos.nxt.*;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;

//import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Graphics;

import lejos.util.Delay;

public class TestNXT {
	

	/*
	  Find Largest and Smallest Number in an Array Example
	  This Java Example shows how to find largest and smallest number in an
	  array.
	*/

	        public  static int  getMax(int[] numbers, int size) {
	               
	                //assign first element of an array to largest and smallest
	                int smallest = numbers[0];
	                int largetst = numbers[0];
	               
	                for(int i=1; i< size; i++)
	                {
	                        if(numbers[i] > largetst)
	                                largetst = numbers[i];
	                        else if (numbers[i] < smallest)
	                                smallest = numbers[i];
	                       
	                }
	               
	                return largetst;
	        }
	        
	        public static int  getMin(int[] numbers, int size) {
	               
                //assign first element of an array to largest and smallest
                int smallest = numbers[0];
                int largetst = numbers[0];
               
                for(int i=1; i< size; i++)
                {
                        if(numbers[i] > largetst)
                                largetst = numbers[i];
                        else if (numbers[i] < smallest)
                                smallest = numbers[i];
                       
                }
               
                return smallest;
        }
	
	
	public static void drawGraph(int[][] dist, int numDistances) {

		Graphics g = new Graphics();
		
		int lcdHeight = 63; //64;
		int lcdWidth = 99; //100;
		int lcdStartX = 0;
		int lcdStartY = 0;


		double x_int = ((double)(lcdWidth-lcdStartX))/theNextSensor.xpixels;
		double dist_int = ((double)(lcdHeight-lcdStartY))/(getMax(dist[1], numDistances)); //-getMin(dist[1]));

		int x_width;
		if (numDistances <= 1) {
			x_width = 5;
		} else {
			x_width = (int)(Math.round(x_int*(dist[0][1] - dist[0][0])/2));
		}

		// Go through the x and distances values
		for (int i=0; i<numDistances; i++) {
//			System.out.println("XINT: " + dist_int + "YINT: " + dist_int);
//			System.out.println("D: " + 2.5 + " DIST_INT: " + (lcdHeight-lcdStartY)/(getMax(dist[1])-getMin(dist[1])));
//			System.out.println("MX: " + getMax(dist[1]) + "MIN: " + getMin(dist[1]));
//			System.out.println("X: " + (int)(Math.round(x_int*dist[0][i])) + "Y: " + (int)(Math.round(dist_int*(dist[1][i]-getMin(dist[1])))));
//			System.out.println("XW: " + x_width);
			int distance = (int)(Math.round(dist_int*(dist[1][i])));
//			g.drawRect((int)(Math.round(x_int*dist[0][i])), lcdStartY, x_width, distance);
			g.drawRect((int)(Math.round(x_int*dist[0][i])), (lcdHeight-lcdStartY)-distance, x_width, distance);
		}
	}
	public static void main(String[] args) {

		int[][] dist = new int[2][theNextSensor.MAX_DISTANCES];

	
    Delay.msDelay(2000);
    int distToRead = 2;
	theNextSensor mySensor = new theNextSensor(SensorPort.S1, false);
	
	LCD.clear();
	System.out.println("Distances to Get: " + distToRead);
    while(true) {
        int myButton = Button.waitForAnyPress();
        
    	if (myButton == Button.ENTER.getId()) {
    		// Enter button was pressed so get the distances and plot the graph
    		if (mySensor.getDistance(dist, distToRead) == -1) {
    			System.out.println(mySensor.getErrorTrace());
    			continue;
    		}	
    		LCD.clear();
    		drawGraph(dist, distToRead);	
    	} else if (myButton == Button.ESCAPE.getId()) {
    		// Reset the distance numbers and graph
    		//distToRead = 16;
    		//LCD.clear();
    		break;
    		//System.out.println("Distances to Get: " + distToRead);
    	} else if (myButton == Button.LEFT.getId()) {
    		// Decrease the distance numbers
    		distToRead -= 2;
    		if (distToRead < 1) {
    			distToRead = 1;
    		}
    		LCD.clear();
    		System.out.println("Distances to Get: " + distToRead);
    	} else if (myButton == Button.RIGHT.getId()) {
    		// Increase the distance numbers
    		distToRead += 2;
    		if (distToRead > theNextSensor.MAX_DISTANCES) {
    			distToRead = theNextSensor.MAX_DISTANCES;
    		}
    		LCD.clear();
    		System.out.println("Distances to Get: " + distToRead);
    	}
    	Delay.msDelay(30);
    }

	/*theNextSensor myUart = new theNextSensor(SensorPort.S1, false);
	
	
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
	
	
	
	
	
	
		int myDistance;
		int[] dist = new int[theNextSensor.MAX_DISTANCES];
		
		System.out.println("Hello World!");
		
		//theNextSensor myUart = new theNextSensor();

		// Set the mode to off
		//myUart.getMode();

		// Get the distance but it should return an error because the sensor is off
//		while(true){
//		myDistance = myUart.getDistance();
//		
//		System.out.println("Read: " + myDistance);
//		Button.waitForAnyPress();
//	}
		
		
		// Set the mode to continuous
		myUart.setMode(theNextSensor.Mode.MODE_CONTINUOUS);

//		// Get the distance
//		myDistance = myUart.getDistance();
//		System.out.println("Distance is: " + myDistance);
		
		if (myDistance == -1) {
			// Either an error occurred or there is no object detected
			System.out.println(myUart.getErrorTrace());
		}
		
		
		
		// Set the mode to Ping
//		myUart.setMode(theNextSensor.Mode.MODE_PING);
//		
//		// Get multiple distance values
		//myUart.getDistance(dist,8);
		
		if(myUart.getDistance(dist, 8) == -1) {
			// An error occurred while reading the distances
			System.out.println(myUart.getErrorTrace());
		}

		System.out.println("Multiple distance values: ");
		for (int i=0; i<8; i++) {
			System.out.print(dist[i] + ", ");
		}
		System.out.println("\n");
		
		while(true){
			
		
		System.out.println("Printing x and corresponding z values: ");
		int[][] mdist = new int[2][32];
		if (myUart.getDistance(mdist, 32) == -1) {
			System.out.println(myUart.getErrorTrace());
			Button.waitForAnyPress();
		}
		for (int i=0; i<10; i++) {
			//System.out.print(mdist[0][i] + ", " + mdist[1][i] + " || ");
			//Button.waitForAnyPress();
			
			
		}
		Button.waitForAnyPress();
		Button.ENTER.isDown();
		}*/
		//Button.waitForAnyPress();
	}

}
