import com.fazecast.jSerialComm.*;
import java.io.OutputStream;
import java.util.Scanner;

public class SerialDemo
{
	private int recvCount = 0;
	private byte[] recvBuff = new byte[1024];
	public static void main(String[] args)
	{
		System.out.println("SerialDemo");
		
		SerialPort[] comPorts = SerialPort.getCommPorts();
		for(SerialPort com : comPorts)
		{
			System.out.println(com.getDescriptivePortName() + " : " + com.getSystemPortName());
		}
		//SerialPort comPort = SerialPort.getCommPort("bserial-FTCXK07X");	
		//SerialPort comPort = SerialPort.getCommPorts()[0];
		System.out.println("Enter SerialPort index : ");
		Scanner s= new Scanner(System.in);
		int portnumber = s.nextInt();
		s.close();
		final SerialPort comPort = comPorts[portnumber];

		comPort.setComPortParameters(9600, 7, 1, 2);
		comPort.setFlowControl(0);
		//comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 100);
		if(comPort.openPort())
		{
			System.out.println("Opened");
			//PacketListener listener = new PacketListener();
			//comPort.addDataListener(listener);
			comPort.addDataListener( new SerialPortDataListener() 
			{
				@Override
				public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }
				@Override
				public void serialEvent(SerialPortEvent event)
				{
					if(event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
						return;
					int len = comPort.bytesAvailable();
					if(len >= 13)
					{	
						byte[] newData = new byte[14];
						int numRead = comPort.readBytes(newData, 14);
						System.out.println("numRead : " + numRead);
						//System.out.println(new String(newData, 0, 14));
					}	
					System.out.println("new data : " + len + " bytes.");
				}
			});
			try 
			{ 
				byte[] buff = new byte[] {0x02,0x30,0x30,0x35,0x34,0x31,0x20,0x20,0x20,0x03,0x31,0x33, 0x0d,0x0a};
				OutputStream out = comPort.getOutputStream();
				out.write(buff);
				Thread.sleep(500);
				buff = new byte[] {0x02,0x30,0x30,0x31,0x30,0x30,0x32,0x30,0x30,0x03,0x30,0x30,0x0d,0x0a};
				out.write(buff);
				
				//Scanner s = new Scanner(Sysetm.in);
			
				Thread.sleep(5000); 
			} 
			catch (Exception e) 
			{ 
				e.printStackTrace(); 
			}
			comPort.removeDataListener();
			comPort.closePort();
		}
		else 
		{
			System.out.println("Open() failed");
		}
	}	
}

/*
final class PacketListener implements SerialPortPacketListener
{
	@Override
	public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_RECEIVED; }
	
	@Override
	public int getPacketSize() { return 1; }

	@Override
	public void serialEvent(SerialPortEvent event)
	{
		byte[] newData = event.getReceivedData();
		System.out.println("Received data of size: " + newData.length);
		for(int i=0; i < newData.length; i++)
			System.out.println((char)newData[i]);
		System.out.println("\n");
	}
}
*/
