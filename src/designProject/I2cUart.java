package designProject;

import lejos.nxt.*;
import lejos.nxt.remote.NXTProtocol;
import lejos.util.Delay;

public class I2cUart {

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


	final byte EFR_ENABLE_ENHANCED_FUNCTIONS = 1 << 4;

	final byte UART_CONFIG_DIVL = 0x60; // Bauderate of 9600 with xtal=14MHz
	final byte UART_CONFIG_DIVH = 0x00;
	final byte UART_CONFIG_DATA_FORMAT = 0x3;

	// This depends on how A0 and A1 are connected on the chip.
	// Current address is valid if A0 and A1 and connected to Vgg (Ground).
	final int ADDRESS = 0x9A;

	private I2CSensor mySensor;
	private boolean simulation;
	private int timeout;
	private byte myByte[];
	private String errorMessage;

	private piSimulator piSim;

	public I2cUart(SensorPort port, boolean simulation) {

		mySensor = new I2CSensor(port, ADDRESS, I2CPort.STANDARD_MODE, SensorConstants.TYPE_LOWSPEED);
		this.simulation = simulation;

		if (simulation) {
			piSim = new piSimulator();
		} else {
			init();
		}
		myByte = new byte [1]; 
		errorMessage = "";
	}

	/*
	 * Constructor to run this class in the simulation mode
	 */
	public I2cUart() {
		this.simulation = true;
		piSim = new piSimulator();

		myByte = new byte [1]; 
		errorMessage = "";
	}
	
	public I2cUart(SensorPort port) {
		mySensor = new I2CSensor(port, ADDRESS, NXTProtocol.RAWMODE, SensorConstants.TYPE_LOWSPEED);
	}

	private int sendDataRoot(int register, byte value) {
		if (simulation) {
			return piSim.sendData(register, value);
		} else {
			return mySensor.sendData(register, value);
		}
	}

	private int getDataRoot(int register, byte buf[], int length) {
		if (simulation) {
			return piSim.getData(register, buf, length);
		} else {
			return mySensor.getData(register, buf, length);
		}
	}

	private void createErrorMessage(String msg) {
		errorMessage = "I2cUart: " + msg;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void init() {
		// Initialize SC16IS750 settings related to UART configuration
		sendDataRoot(LCR, (byte)0x80); // 0x80 to program baudrate
		sendDataRoot(DLL, UART_CONFIG_DIVL);
		sendDataRoot(DLM, UART_CONFIG_DIVH); 

		sendDataRoot(LCR, (byte)0xBF); // access EFR register
		sendDataRoot(EFR, EFR_ENABLE_ENHANCED_FUNCTIONS); // enable enhanced registers
		sendDataRoot(LCR, UART_CONFIG_DATA_FORMAT); // 8 data bit, 1 stop bit, no parity
		sendDataRoot(FCR, (byte)0x06); // reset TXFIFO, reset RXFIFO, non FIFO mode
		sendDataRoot(FCR, (byte)0x01); // enable FIFO mode   
	}

	public int availableData() {
		/*
		 * Get the number of bytes (characters) available for reading.
		 * This is data that's already arrived and stored in the receive
		 * buffer (which holds 64 bytes).
		 */
		// This alternative just checks if there's data but doesn't
		// return how many characters are in the buffer:
		//    readRegister(LSR) & 0x01
		if(getDataRoot(RXLVL, myByte, 1) != 0) {
			return -1; 
		} else {
			// Return the number of available bytes
			return myByte[0];
		}
	}

	/*
	 * Return 1 if success, 0 otherwise.
	 */
	public int readData(byte[] buf, int len) {
		// Wait 20 ms reading the data so the sensor has enough time to gather the data
		Delay.msDelay(20);
		if (this.availableData() < len) {
			// Wait a bit longer
			Delay.msDelay(timeout-20);
			int availDataLen = this.availableData();
			if (availDataLen < len) {
				// For some reason there is no data available
				// Timeout
				createErrorMessage("The data available to be read (" + availDataLen + ") is less than required length of " + len);				
				return -1;
			}
		}

		if(getDataRoot(RHR, buf, len) == 0) {
			// success
			return 0;
		} else {
			createErrorMessage("Unable to read data from RHR register over I2C");
			return -1;
		}
	}

	public int writeByte(byte value) {
		/*
		 * Write byte to UART.
		 */

		// Read the TX buffer space availability 
		if (getDataRoot(TXLVL, myByte, 1) != 0) {
			// failure to read
			createErrorMessage("Unable to read register TXLVL over I2C");
			return -1;
		}

		if (myByte[0] == 0) {
			// Wait for timeout period and try again
			Delay.msDelay(timeout);
			if (getDataRoot(TXLVL, myByte, 1) != 0) {
				// failure to read
				createErrorMessage("Unable to read register TXLVL over I2C");
				return -1;
			}
			if (myByte[0] == 0) {
				// The buffer is not empty so return an error
				createErrorMessage("Unable to write byte. TX buffer is not empty");				
				return -1;
			}
		}

		// Send the data		
		if(sendDataRoot(THR, value) == 0) {
			// success
			return 0;
		} else {
			// failure
			createErrorMessage("Unable to write byte to THR register over I2C");
			return -1;
		}
	}
	
	
	/*
	 * Check that UART is connected and operational 
	 */
	public boolean testConn() {
		if(simulation) {
			return true;
		} 

		// Perform read/write test to check if UART is working
		byte TEST_CHARACTER = 'H';

		if(sendDataRoot(SPR, TEST_CHARACTER) == 0) {
			// Add some delay
			Delay.msDelay(20);

			if (getDataRoot(SPR, myByte, 1) == 0) {
				if (myByte[0] == TEST_CHARACTER) {
					return true;
				}
			} else {
				createErrorMessage("Unable to get data from SPR register over I2C");
				return false;
			}
		} else {
			createErrorMessage("Unable to send data to SPR register over I2C");
			return false;
		}

		// Failure: byte received didn't match byte sent
		createErrorMessage("Test failure. Sent byte '" + TEST_CHARACTER + "'does not match the received byte '" + myByte[0] + "'");
		return false;
	}
	
}
