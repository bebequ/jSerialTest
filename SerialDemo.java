import com.fazecast.jSerialComm.*;
import java.io.OutputStream;
import java.util.Scanner;
import java.util.Queue;
import java.util.LinkedList;

public class SerialDemo
{
	private int recvCount = 0;
	private RecvBuffManager recvBuffManager = new RecvBuffManager();
	public static void main(String[] args)
	{
		System.out.println("SerialDemo");
		SerialDemo sd = new SerialDemo();
		sd.SerialTest();
	}

	private void SerialTest()
	{	
		SerialPort[] comPorts = SerialPort.getCommPorts();
		for(SerialPort com : comPorts)
		{
			System.out.println(com.getDescriptivePortName() + " : " + com.getSystemPortName());
		}
		//SerialPort comPort = SerialPort.getCommPort("bserial-FTCXK07X");	
		SerialPort comPort = SerialPort.getCommPorts()[0];
		/*
		System.out.println("Enter SerialPort index : ");
		Scanner s= new Scanner(System.in);
		int portnumber = s.nextInt();
		s.close();
		if(portnumber < 0)
		{
			System.out.println("quit");
			return;
		}
		final SerialPort comPort = comPorts[portnumber];
		*/
		comPort.setComPortParameters(9600, 7, 1, 2);
		comPort.setFlowControl(0);
		//comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 100);
		if(comPort.openPort())
		{
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
					
					int bytesAvailable = comPort.bytesAvailable();
					byte[] newData = new byte[bytesAvailable];
					comPort.readBytes(newData, newData.length);
					recvBuffManager.addData(newData);
					if(recvBuffManager.completed)
					{
						if(recvBuffManager.eType == E_MSG_TYPE.SHORT_FORMAT)
						{
							System.out.print("Short Format : ");
							byte[] resp = recvBuffManager.getBuff();
							System.out.print(new String(resp, 0, resp.length));
						}
						else if(recvBuffManager.eType == E_MSG_TYPE.LONG_FORMAT)
						{
							System.out.print("Long Format : ");
							byte[] resp = recvBuffManager.getBuff();
							System.out.println(new String(resp, 0, resp.length));
							System.out.println(String.format("%x %x %x %x %x %x", resp[9], resp[10], resp[11], resp[12], resp[13], resp[14]));
							
							double result = 0;
						       	for(int j=10; j<14; j++)
							{

								if(resp[j] == 0x20) continue;
								result += ((resp[j]-'0')*Math.pow(10,13-j)); 
							}
							if(resp[9] == '-')
								result *= -Math.pow(10,(resp[14]-'0')-4);
							else
								result *= Math.pow(10, (resp[14]-'0')-4);
							System.out.println(String.format("result : %.3f lux", result));
						}
						//System.out.print(String.format("%x %x %x %x", resp[0], resp[1], resp[2], resp[3]));
						//System.out.println("\n");
					}
				}
			});
			try 
			{ 
				byte[] buff = new byte[] {0x02,0x30,0x30,0x35,0x34,0x31,0x20,0x20,0x20,0x03,0x31,0x33, 0x0d,0x0a};
				OutputStream out = comPort.getOutputStream();
				out.write(buff);
				Thread.sleep(500);
				
				Scanner sc = new Scanner(System.in);
				while(true)
				{
					System.out.println("Enter if you want to measure light");
					String msg = sc.nextLine();
					if(msg.equals("quit"))
						break;
					buff = new byte[] {0x02,0x30,0x30,0x31,0x30,0x30,0x32,0x30,0x30,0x03,0x30,0x30,0x0d,0x0a};
					out.write(buff);
					Thread.sleep(3000);
				}
				sc.close();
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

	private enum E_MSG_TYPE {SHORT_FORMAT, LONG_FORMAT, NOT_PARSED};
	private class RecvBuffManager
	{
		private E_MSG_TYPE eType = E_MSG_TYPE.NOT_PARSED;
		private int pos = 0;
		private byte[] mBuff = new byte[1024];
		public boolean completed = false;
		public void addData(byte[] inData)
		{
			//System.out.println("len : " + inData.length);
			if(eType == E_MSG_TYPE.NOT_PARSED) 
			{
				System.arraycopy(inData, 0, mBuff, pos, inData.length);
				pos += inData.length;
				if(pos >= 5)
				{
					//System.out.println(String.format("type : %x %x", inData[3], inData[4]));
					if(mBuff[3]== '1' && (mBuff[4] == '1' || mBuff[4] == '0'))
						eType = E_MSG_TYPE.LONG_FORMAT;
					else
						eType = E_MSG_TYPE.SHORT_FORMAT;	
				}
			}
			else if(eType == E_MSG_TYPE.SHORT_FORMAT)
			{
				if(pos + inData.length >= 14)
				{
					completed = true;
				}
				System.arraycopy(inData, 0, mBuff, pos, inData.length);  
				pos += inData.length;
			}
			else if(eType == E_MSG_TYPE.LONG_FORMAT)
			{
				if(pos + inData.length >= 32)
				{
					completed = true;
				}
				System.arraycopy(inData, 0, mBuff, pos, inData.length);
				pos += inData.length;
			}
		}

		public byte[] getBuff()
		{
			if(completed)
			{
				//System.out.println(String.format("completed %x %x ",mBuff[0], mBuff[1] ));
				if(eType == E_MSG_TYPE.SHORT_FORMAT)
				{
					byte[] tmp = new byte[14];
					System.arraycopy(mBuff, 0, tmp, 0, 14);
					eType = E_MSG_TYPE.NOT_PARSED;
					completed = false;
					pos = 0;
					return tmp;
				}
				else if(eType == E_MSG_TYPE.LONG_FORMAT)
				{
					byte[] tmp = new byte[32];
					System.arraycopy(mBuff, 0, tmp, 0, 32);
					eType = E_MSG_TYPE.NOT_PARSED;
					completed = false;
					pos = 0;
					return tmp;
				}
				else
				{
					return null;
				}
			}
			else 
			{
				return null;
			}
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
