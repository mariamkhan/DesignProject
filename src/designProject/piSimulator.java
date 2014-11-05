package designProject;

/*
 * This class simulations what the Rasberry PI distance sensor does when it receives commands from NXT
 * over its UART interface through SC16IS750 I2C-UART chip.
 */
public class piSimulator {

	// SC16IS750 Register definitions. Values are shifted according to Table 33 in the datasheet
	final int THR        = 0x00 << 3;
	final int RHR        = 0x00 << 3;
	final int IER        = 0x01 << 3;
	final int FCR        = 0x02 << 3;
	final int IIR        = 0x02 << 3;
	final int LCR        = 0x03 << 3;
	final int MCR        = 0x04 << 3;
	final int LSR        = 0x05 << 3;
	final int MSR        = 0x06 << 3;
	final int SPR        = 0x07 << 3;
	final int TXLVL      = 0x08 << 3;
	final int RXLVL      = 0x09 << 3;
	final int DLAB       = 0x80 << 3;
	final int IODIR      = 0x0A << 3;
	final int IOSTATE    = 0x0B << 3;
	final int IOINTMSK   = 0x0C << 3;
	final int IOCTRL     = 0x0E << 3;
	final int EFCR       = 0x0F << 3;

	final int DLL        = 0x00 << 3;
	final int DLM        = 0x01 << 3;
	final int EFR        = 0x02 << 3;
	final int XON1       = 0x04 << 3; 
	final int XON2       = 0x05 << 3;
	final int XOFF1      = 0x06 << 3;
	final int XOFF2      = 0x07 << 3;

	final boolean verbose = true;

	private Mode currentMode;

	private final byte MAX_DISTANCES = 8;

	private final byte CMD_GET_MODE = 1;
	private final byte CMD_SET_MODE = 2;
	private final byte CMD_GET_DIST = 3;

	// Value to represent how many bits are used for the command
	private final byte CMD_MASK = 0x7; // 3 bits

	// Position of the command bits
	private final byte CMD_SHIFT = 5;

	private final byte MODE_MASK = 0x3; // 2 bits
	private final byte DIST_LEN_MASK = 0x7; // 3 bits

	private byte[] bytesToSend; // array of bytes to send
	private int sendLen; // number of bytes to send when getData function is called

	private int[] distances;
	
	public static enum Mode{
		MODE_OFF,
		MODE_RESET,
		MODE_PING,
		MODE_CONTINUOUS,
	}

	public piSimulator() {
		// Each distance is represented by two bytes
		bytesToSend = new byte[MAX_DISTANCES*2];
		sendLen = 0;

		// Initialize bytes to send
		for (int i=0; i<bytesToSend.length; i++) {
			bytesToSend[i] = 0;
		}
		
		// Set the default mode
		currentMode = Mode.MODE_CONTINUOUS;
		
		// populate some distance values to send when requested
		distances = new int[MAX_DISTANCES];
		for (int i=0; i<distances.length; i++) {
			distances[i] = 500+i;
		}
		distances[4] = -1; // set it to denote that no object was found
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

	private Mode byteToMode(byte m) {
		switch (m) {
		case 0:
			return Mode.MODE_OFF;
		case 1:				
			return Mode.MODE_RESET;
		case 2:				
			return Mode.MODE_PING;
		case 3:
			return Mode.MODE_CONTINUOUS;
		default:
			return Mode.MODE_OFF;				
		}
	}

	public int sendData(int register, byte value) {
		switch (register) {	
		case THR:
			// user is sending a command to the sensor
			int command =  (value >> CMD_SHIFT) & CMD_MASK;
			switch ((byte)command) {
			case CMD_GET_MODE:
				// send the mode when next RHR read command is sent
				bytesToSend[0] = modeToByte(currentMode);
				sendLen = 1;
				if (verbose) {
					System.out.println("piSimulator: Received command CMD_GET_MODE. Current mode: " + modeToByte(currentMode));
				}
				break;
			case CMD_SET_MODE:
				// set the current mode
				int data = (value & MODE_MASK);
				if (verbose) {
					System.out.println("piSimulator: Received command CMD_SET_MODE. Old mode: " 
							+ currentMode + ". New mode: " + byteToMode((byte)data));
				}								
				currentMode = byteToMode((byte)data);
				if (currentMode == Mode.MODE_RESET) {
					// Reset the device and then change the mode to continuous
					currentMode = Mode.MODE_CONTINUOUS;
				}
				sendLen = 0;
				break;
			case CMD_GET_DIST:
				// send the distance value (s) when next RHR read command is sent
				int num = (value & DIST_LEN_MASK) + 1; // get the number of distances to read				
				if (verbose) {
					System.out.println("piSimulator: Received command CMD_GET_DIST of length " + num); 
				}
				if ((currentMode != Mode.MODE_CONTINUOUS) && (currentMode != Mode.MODE_PING)) {
					// Don't send any data because the sensor is off
					if (verbose) {
						System.out.println("piSimulator: Distance requested but the sensor is off"); 
					}					
					sendLen = 0;
					break;
				}
				if (num <= MAX_DISTANCES) {
					for (int i=0; i<num; i++) {
						// Convert integer distance into two bytes to send over I2C (little-endian)
						bytesToSend[i*2] = (byte)(distances[i] & 0xff);								
						bytesToSend[(i*2)+1] = (byte)((distances[i] >> 8) & 0xff);
					}
					sendLen = num*2;
				} else {
					System.out.println("piSimulator: Number of distances requested is more than maximum"); 					
				}
				break;
			}
		default:
			break;
		}

		return 0;
	}

	public int getData(int register, byte[] buf, int length) {
		switch (register) {
		case RXLVL:
			// user is trying to read available data length so return maximum
			buf[0] = 100;
			break;
		case TXLVL:
			// user is trying to check whether TX buffer is empty before sending real data so send "empty" status
			buf[0] = 1;
			break;			
		case RHR:
			if (buf.length < sendLen) {
				System.out.println("Sent");
				if (verbose) {
					System.out.println("piSimulator: Could not send data because buffer length " 
							+ buf.length + " is less than sendLen " + sendLen);
				}																						
				return -1;
			}
			if (sendLen == 0) {
				if (verbose) {
					System.out.println("piSimulator: Received request to send data but nothing to send"); 
				}																						
				return -1;
			}
			// user is trying to get some data (mode or distance)
			if (verbose) {
				System.out.println("piSimulator: Sending data of byte length " + sendLen);
			}																		
			for (int i=0; i<sendLen; i++) {
				buf[i] = bytesToSend[i];
			}
			// Reset the send buffer length
			sendLen = 0;
			break;
		default:
			buf[0] = 17;
			break;
		}
		return 0;
	}
}
