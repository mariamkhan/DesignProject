package designProject;
import lejos.nxt.*;
import lejos.util.Delay;

/*
 * Commands sent to sensor hardware:
 * ------------------------
 * The command itself is 3 bits long. Rest of the bits contain additional data. x means the bit state doesn't matter.
 * [b7] [b6] [b5] [b4] [b3] [b2] [b1] [b0]
 *  0    0    1    x    x    x    x    x   <- Get current mode in the hardware. Return 1 byte: 0 (off), 1 (reset), 2(ping), 3(continuous)
 *  0    1    0    0    0    0    m1   m0  <- Set current mode in the hardware. off (m2m1=00), 
 *                                            reset (m2m1=01), ping (m2m1=10), continuous (m2m1=11)
 *  1    d6   d5   d4   d3   d2   d1   d0  <- Get the distance value(s). [d6-d0] is the number of distance values to read (1-128) minus 1 
 *  
 *  
 *  In continuous mode, the sensor keeps polling for distance at a certain interval and stores it in memory until getDistance function is
 *  called by I2C master at which point it returns the latest distance.
 */

public class theNextSensor {

	private I2cUart sensor;
	private Mode currentMode;

	public static final int MAX_DISTANCES = 32;
	public static final int xpixels = 1944;

	private final byte CMD_GET_MODE = 1;
	private final byte CMD_SET_MODE = 2;
	private final byte CMD_GET_DIST = 4;

	// Position of the command bits
	private final byte CMD_SHIFT = 5;

	private String errorMessage;

	public static enum Mode{
		MODE_OFF,
		MODE_RESET,
		MODE_PING,
		MODE_CONTINUOUS,
	}

	public theNextSensor(SensorPort port, boolean simulation) {		
		sensor = new I2cUart(port, simulation);
		currentMode = Mode.MODE_PING;
		setMode(currentMode);
		errorMessage = "";
		
		if (!sensor.testConn()) {
			System.out.println("theNextSensor ERROR: Unable to connect to the sensor.");
		}		
	}

	public theNextSensor() {		
		sensor = new I2cUart();
		currentMode = Mode.MODE_CONTINUOUS;
		errorMessage = "";

		if (!sensor.testConn()) {
			System.out.println("theNextSensor ERROR: Unable to connect to the sensor.");
		}

	}

	public int setMode(Mode m) {
		switch (m) {
		case MODE_OFF:
			return off();
		case MODE_RESET:
			return reset();
		case MODE_PING:
			return ping();
		case MODE_CONTINUOUS:
			return continuous();
		default:
			return -1;				
		}
	}

	/*
	 * Convert mode from enum to byte value
	 */
	private byte modeToByte(Mode m) {
		switch (m) {
		case MODE_OFF:
			return 0;
		case MODE_RESET:
			return 1;
		case MODE_PING:
			return 2;
		case MODE_CONTINUOUS:
			return 3;
		default:
			return 0;				
		}
	}

	/*
	 * Combine two bytes into a short signed integer
	 */
	/*private int combineBytes(byte b1, byte b2, byte b3, byte b4) {
		int s4 = b4 >= 0 ? b4: 256 + b4;
		int s3 = b3 >= 0 ? b3: 256 + b3;
		int s2 = b2 >= 0 ? b2: 256 + b2;
		int s1 = b1 >= 0 ? b1: 256 + b1;
		return (int)(s1 | (s2 << 8) | (s3 << 16) | (s4 << 24));			
	}
	*/
	
	/*
	 * Combine two bytes into a short signed integer
	 */
	private short combineBytes(byte b1, byte b2) {
		int high = b2 >= 0 ? b2 : 256 + b2;
		int low = b1 >= 0 ? b1 : 256 + b1;
		return (short)(low | (high << 8));			
	}

	public String getErrorTrace() {
		return (errorMessage + "\n\t..." + sensor.getErrorMessage());
	}

	private void createErrorMessage(String msg) {
		errorMessage = "theNextSensor: " + msg;
	}

	public Mode getMode() {
		return currentMode;
	}

	/*
	 * Turn off the sensor. This call disables the sensor. 
	 * No pings will be issued after this call, until either ping, continuous or reset is called.
	 */
	public int off() {		
		// send a request to the sensor to change the mode to off state
		if(sensor.writeByte((byte)((CMD_SET_MODE << CMD_SHIFT) | modeToByte(Mode.MODE_OFF))) != 0) {
			// some error occurred
			createErrorMessage("Unable to turn off the sensors");
			return -1;			
		}
		currentMode = Mode.MODE_OFF;
		return 0;
	}	

	/*
	 * Reset the device Performs a "soft reset" of the device. 
	 * Restores things to the default state. 
	 * Following this call the sensor will be operating in continuous mode.
	 */
	public int reset() {		
		// send a request to the sensor to change reset and initialize mode to default (continuous)
		if(sensor.writeByte((byte)((CMD_SET_MODE << CMD_SHIFT) | modeToByte(Mode.MODE_RESET))) != 0) {
			// some error occurred
			createErrorMessage("Unable to set reset the sensor");			
			return -1;
		}
		currentMode = Mode.MODE_CONTINUOUS;
		return 0;
	}	

	/*
	 * Send a single ping. The sensor operates in two modes, continuous and ping. 
	 * When in continuous mode the sensor sends out pings as often as it can and the most 
	 * recently obtained result is available via a call to getDistance. When in ping mode a 
	 * ping is only transmitted when a call is made to ping. This sends a single ping and up 
	 * to 8 echoes are captured. These may be read by making a call to getDistance and 
	 * passing a suitable array. A delay of approximately 20ms is required between the call 
	 * to ping and getDistance. This delay is not included in the method. Calls to getDistance 
	 * before this period may result in an error or no data being returned. The normal 
	 * getDistance call may also be used with ping, returning information for the first echo. 
	 * Calling this method will disable thh default continuous mode, to switch back to continuous 
	 * mode call continuous.
	 *
	 */
	public int ping() {
		// send a request to the sensor to change the mode to ping state
		if (sensor.writeByte((byte)((CMD_SET_MODE << CMD_SHIFT) | modeToByte(Mode.MODE_PING))) != 0) {
			// some error occurred
			createErrorMessage("Unable to set the mode to Ping");						
			return -1;
		}
		currentMode = Mode.MODE_PING;		
		return 0;
	}	

	/*
	 * Switch to continuous ping mode. This method enables continuous ping and capture mode. 
	 * This is the default operating mode of the sensor. Please the notes for ping for more details.
	 */
	public int continuous() {
		// send a request to the sensor to change the mode to continuous
		if (sensor.writeByte((byte)((CMD_SET_MODE << CMD_SHIFT) | modeToByte(Mode.MODE_CONTINUOUS))) != 0) {
			// some error occurred
			createErrorMessage("Unable to set the mode to Continuous");									
			return -1;
		}
		currentMode = Mode.MODE_CONTINUOUS;		
		return 0;
	}	

	/*
	 * Return distance to an object. 
	 * Returns: distance or -1 if no object in range
	 */
	public int getDistance() {
		int[] dist = new int [1];
		dist[0] = -1;
		getDistance(dist, 1);
		return dist[0];
	}

	/*
	 * Return an array of 8 echo distances. These are generated when using ping mode. 
	 * A value of -1 indicates that no echo was obtained. The array must contain at least 8 elements, 
	 * if not -1 is returned. If the distance data is not yet available the method will wait until it is.
	 */
	public int getDistance(int[] dist) {
		int num;
		if (currentMode == Mode.MODE_CONTINUOUS) {
			// In continuous mode, only retrieve one distance values
			num  = 1;
		} else {
			// In Ping mode, get maximum number of distance values
			num = MAX_DISTANCES;
		}
		return getDistance(dist, num);
	}	

	public int getDistance(int[] dist, int num) {
		byte[] buf = new byte [MAX_DISTANCES*2];

		if (num > MAX_DISTANCES) {
			// Currently maximum of 8 distances from 8 different objects are returned
			createErrorMessage("getDistance: Invalid number of distances requested (" + num + "). Max is " + MAX_DISTANCES);			
			return -1;
		} else if (num < 1) {
			// invalid distance number provided
			createErrorMessage("getDistance: Invalid number of distances requested (" + num + ")");			
			return -1;
		}

		if (dist.length < num) {
			// array is not big enough to hold the returned values
			createErrorMessage("getDistance: array (length: " + dist.length + ") is not big enough to hold the requested distances " 
					+ num);			
			return -1;
		}

		// send request to get the given number of distances
		if(sensor.writeByte((byte)((CMD_GET_DIST << CMD_SHIFT) | num-1)) < 0) {
			// error writing the request
			createErrorMessage("getDistance: Error sending 'get distance' command");
			return -1;
		};
//		Delay.msDelay(1000);
//		if(sensor.readData(buf, num*4) < 0) {
//			// error reading the data
//			createErrorMessage("getDistance: Error reading distance values from the sensor");			
//			return -1;
//		}
		
		
		
		byte[] tmpbuf = new byte[2];
//		int i = 0;
		int i = 0;
		// read the distances (each distance value is 2 bytes long)
		while(sensor.availableData() < num*2) ;
		//System.out.println("Initial Count: " + sensor.availableData());
		//Button.waitForAnyPress();
		//if(sensor.readData(buf, num*4) < 0) {
		while(sensor.availableData() > 0){
			// error reading the data
			sensor.readData(tmpbuf, 2);
			//System.out.println("Bytes available: " + sensor.availableData());
			dist[i] = combineBytes(tmpbuf[0], tmpbuf[1]);
			//int firstInterval = xpixels/(num+1);
			i++;
			}
			//createErrorMessage("getDistance: Error reading distance values from the sensor");			
			//return -1;
		return 0;
			 }
		
		
//		System.out.println(sensor.availableData());

		
		/*
		
		// Initialize all the distance values to default (-1)
		for (int i=0; i<dist.length; i++) {
			dist[i] = -1;
		}

		for (int i=0; i<num; i++) {		
			dist[i] = combineBytes(buf[i*4], buf[(i*4)+1], buf[(i*4)+2], buf[(i*4)+3]);
			//int firstInterval = xpixels/(num+1);
			
		}
		return 0;
	}
	*/
	
	public int getDistance(int[][] dist, int num) {
		if (dist.length < 2) {
			createErrorMessage("2D distance array length has to be 2");
			return -11;
		}
		if (dist[0].length < num) {
			createErrorMessage("2D distance array size must be equal to greater than required distances");
			return -12;
		}
		int firstInterval = Math.round(xpixels/(num+1));
		for (int i=0; i<num; i++) {
			dist[0][i] = (i+1)*firstInterval;
		}
		return getDistance(dist[1], num);
	}

}
