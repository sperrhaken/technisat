import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class TechnisatWorker {
	private Socket m_connection;
	private InputStream m_input;
	private OutputStream m_output;
	private byte[] m_rewriteBuffer;

	
	public TechnisatWorker(Socket connection) throws IOException {
		m_connection = connection;
		m_input = connection.getInputStream();
		m_output = connection.getOutputStream();
		/*
		 * Thread for Idle Communication
		 */
	}
   
	/**
	 * 
	 * @param buffer to read into
	 * @param offset the start offset in array b at which the data is written
	 * @param len the maximum number of bytes to read
	 * @return number of bytes read
	 * @throws IOException
	 */
    private int read(
    	byte[] buffer, int offset, int len
    	) throws IOException {

      final int MAXRETRIES = 60;
      int bytesReadSofar = 0, bytesRead = 0;
      int retries= 0;
      do{
    	  try {
    		  bytesRead = m_input.read(buffer, offset+bytesReadSofar, len-bytesReadSofar );
        	  if(bytesRead>=0) {
        		  bytesReadSofar+=bytesRead;
        	  }
        	  else
        		  throw new IOException("Socket IO Exception " +
        				  bytesRead + "(" + bytesReadSofar + " of " +
        				  len + " bytes read, No Data)");    		  
    	  }
    	  catch(IOException e) {
    		  retries++;
    		  if(retries >= MAXRETRIES)
    			  throw e;
    	  }
      } while(len > bytesReadSofar);

      Logfile.Data("RxD", buffer, bytesReadSofar);
      return bytesReadSofar;
    }

      
    private int read(byte[] paData) throws IOException {    	
    	return read(paData, 0, paData.length);
    }

	
    private byte readbyte() throws IOException {
    	byte[] laBuffer = new byte[1];
    	if(read(laBuffer, 0, 1) > 0)
    		return laBuffer[0];
    	else
    		throw new IOException("No Data");
    }


    private boolean readack() throws IOException {    	
    	byte[] buffer = new byte[1];
    	while (read(buffer, 0, 1) > 0) {
    		switch(buffer[0]) {
    		case 1: // OK
    			return true;
    		case -4: // DISK_BUSY
    			Logfile.Write("Disk is Busy (Record/Replay in Progress)");
				rewrite();
    			break;
    		case -7: // DISK_STARTING_UP
    			Logfile.Write("Disk is starting up...");
    			break;
    		}
    		try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
		return false;
    }
    

    private short readshort() throws IOException {
    	byte[] laShort = new byte[2];
    	int lnBytes = read(laShort, 0, 2);
    	if(lnBytes == 2) {
    		return (new DataInputStream((new ByteArrayInputStream(laShort)))).readShort();
    	}
    	throw new IOException("No Short Value");
    }

    private int readint() throws IOException {
    	byte[] laShort = new byte[4];
    	int lnBytes = read(laShort, 0, 4);
    	if(lnBytes==4) {
    		return (new DataInputStream((new ByteArrayInputStream(laShort)))).readInt();
    	}
    	throw new IOException("No Short Value");
    }     
    
    private long readlong() throws IOException {
    	byte[] laShort = new byte[8];
    	int lnBytes = read(laShort, 0, 8);
    	if(lnBytes==8) {
    		return (new DataInputStream((new ByteArrayInputStream(laShort)))).readLong();
    	}
    	throw new IOException("No Short Value");
    }    
    
    private void write(byte[] paData) throws IOException {
    	Logfile.Data("TxD", paData, paData.length);
    	m_rewriteBuffer = paData;
   		m_output.write(paData);
	}
	
	private void rewrite() throws IOException {
    	Logfile.Data("TxD", m_rewriteBuffer, m_rewriteBuffer.length);
   		write(m_rewriteBuffer);
	}
	
	private void write(byte b) throws IOException {
		write(new byte[]{b});
	}
	
	private void write(String s) throws IOException {
		write(s.getBytes());
	}

	public String get_receiver_name() {
		String lcName = "";
		try {
			write(Header.PT_GETSYSINFO);
			read(new byte[5]); // flags
			read(new byte[3]); // lang
			lcName = readstring();
			ack();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lcName;
	}
	
	private boolean ping() throws IOException {
		ack();
		return readack();
	}
	
	private void ack() throws IOException {
		write(new byte[] { Header.PT_ACK });
	}
	
	private String readstring() throws IOException {
		byte lnFieldLen = readbyte();
		if(lnFieldLen>0) {
			byte[] laField = new byte[lnFieldLen & 0xff];
			int lnStartPos = 0;
			read(laField);		
			switch(laField[0]) {
			case 0x05:
			case 0x0b:
				lnStartPos=1;
				break;
			}
			return new String(laField, lnStartPos, laField.length-lnStartPos, "CP1252");

		} else
			return "";
	}
	
	private void readskip(int i) throws IOException {
		byte[] laSkip = new byte[i];
		read(laSkip);
	}	
	
	public void OpenDir(DvrDirectory poDir) {
		if(poDir.m_bIsOpen)
			return;

		try {		
			Calendar loCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));			
			byte[] laGetDir = new byte[] //Command
				{
					Header.PT_GETDIR,
					0,
					(byte) (poDir.m_oParent==null ? 0 : 1)
				};
			write(laGetDir); //Send
			readack();
			
			if(poDir.m_oParent!=null) {
				write(poDir.m_cRemoteName);		
				readbyte();
				ping();
			}
		
			short lnAnzElements = readshort();
			short lnIndex = 0;
			String lcFileName = "";
			long lnSize = 0;
			int lnTimeStamp = 0;
			while(lnAnzElements>0) {
				loCalendar.set(1999, 12, 01, 00, 00, 00);
				byte lbType = readbyte();
				byte lbIsDir = 0;
				switch(lbType) {
				case 0: //Directory
					lbIsDir = readbyte();
					lcFileName = readstring();
					poDir.m_oDirectorys.add(new DvrDirectory(poDir, lcFileName, lcFileName, null));
					break;
				case 1: //Binary
					lcFileName = readstring();
					lnSize = readlong();
					lnTimeStamp = readint();
					loCalendar.add(Calendar.SECOND, lnTimeStamp);
					poDir.m_oFiles.add( new DvrFile(poDir, lcFileName, lnSize, (short)-1, lbType, loCalendar.getTime()));
					break;
				case 3: //TS Radio
				case 4: //TS File Record SD Quality
				case 7: //TS File Record HD Quality
					lbIsDir = readbyte();
					lnIndex = readbyte();
					lcFileName = readstring();
					lnSize = readlong();
					lnTimeStamp = readint();
					loCalendar.add(Calendar.SECOND, lnTimeStamp);
					poDir.m_oFiles.add( new DvrFile(poDir, lcFileName, lnSize, lnIndex, lbType, loCalendar.getTime()));
					break;
				case 9: //USB Memory Stick
					lbIsDir = readbyte();
					String lcDescription = readstring();
					String lcName = readstring();
					poDir.m_oDirectorys.add(new DvrDirectory(poDir, lcName, lcName.substring(1), lcDescription));
					break;
				default:
					throw new IOException("Unknown RecordType " + lbType);
				}
				lnAnzElements--;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		poDir.m_bIsOpen=true;
	}
	
	// TODO
	public void download_by_recno(DvrFile poFile, String dst) throws IOException {
		ByteArrayOutputStream loSocketWriteLow = new ByteArrayOutputStream();
		DataOutputStream loSocketWrite = new DataOutputStream(loSocketWriteLow);
		String laDstFiles[];
		
		loSocketWrite.writeByte(Header.PT_GETFILE_BYRECNO); //Download Command;
		loSocketWrite.writeShort(poFile.getIndex()); //File Index		
		loSocketWrite.writeLong(0); //Start Position (maybe!!)
		write(loSocketWriteLow.toByteArray()); // Send Message to DVR

		readbyte(); // response
		readlong(); // file size
		byte lbFileCount = readbyte();								
		BufferedOutputStream[] laWrite = new BufferedOutputStream[lbFileCount];
		laDstFiles = new String[lbFileCount];
		for(int i=0; i<laWrite.length; i++) {
			byte lbFileNo = readbyte();
			laDstFiles[i] = dst + "."+readstring().toLowerCase();
			laWrite[lbFileNo] = getDstBufferedFileStream(laDstFiles[i]);
		}				
		write(Header.PT_ACK);
		readstream_multipart(laWrite);
	}
	
	public void download_by_name(DvrFile poFile, String dst)throws IOException {
		write(new byte[] {Header.PT_GETFILE_BYNAME,0,1,0,0,0,0,0,0,0,0} );
		readbyte();
		write(poFile.m_oParent.m_cRemoteName.getBytes("CP1252"));
		readbyte();
		ping();
		write(poFile.getFileName().getBytes("CP1252"));
		readbyte();
		readstream_singlepart(getDstBufferedFileStream(dst));
	}
	
	/**
	 * Copys `poFile' to the destination pcDstFile. If pcDstFile is a
	 * directory files are copied into it.
	 * 
	 * @param poFile DvrFile to copy
	 * @param pcDstFile Destination to copy to
	 * @throws IOException
	 */
	public void download(DvrFile poFile, String pcDstFile)
			throws IOException {
		// TODO handle windows directory separator
		if(pcDstFile.endsWith("/")) {
			pcDstFile = pcDstFile + poFile.getUniqueFileName();
		}

		try {
			Logfile.Write("Copy File " + poFile.getFileName() + " to "+pcDstFile);
			
			if(poFile.m_nType==1) {
				download_by_name(poFile, pcDstFile);
			} else {			
				download_by_recno(poFile, pcDstFile);
			}
			Logfile.Write("Transfer Complete");
		} catch (IOException e) {
			Logfile.Write("Transfer failed");
			throw e;
		}
	}
	
	/**
	 * @param DvrFile poFile
	 * @param OutputStream poWrite
	 * Lese Datenströme im Single File Streaming
	 * Format mit statischer Chunk größe
	 * @throws InterruptedException 
	 */	
	private void readstream_singlepart(
			BufferedOutputStream poWrite
			) throws IOException {
		int lnChunkSize = 0, lnBytes = 0;
		byte[] laBuffer = null;
		byte lbRead = 0;		
		int lnUnknown = readint();
		int lnFileSize = readint();
		int lnReadSize = 0;
		lnChunkSize = readint();
		laBuffer = new byte[lnChunkSize];
		do{
			lbRead = readbyte();
			if(lbRead>=0) {
				readskip(3);			
				lnReadSize = lnFileSize - lnBytes > lnChunkSize ? lnChunkSize : lnFileSize - lnBytes;			
				read(laBuffer,0,lnReadSize);
				poWrite.write(laBuffer,0,lnReadSize);
				lnBytes+=lnChunkSize;
			} else
				resumeread(lbRead);
		} while(lnBytes<lnFileSize);		
		read(laBuffer,0,lnChunkSize-lnReadSize);
		poWrite.close();
	}
	/**
	 * @param DvrFile poFile
	 * @param OutputStream[] paWrite
	 * Lese Datenströme im Multi Part Streaming
	 * Format vom mit dynamischer Chunk größe.
	 * @throws InterruptedException 
	 */
	private void readstream_multipart(
			BufferedOutputStream[] paWrite
			) throws IOException
	{
		byte lbFileNo = 0;
		int lnChunkSize = 0;
		byte[] laBuffer = new byte[65536];
		int lnRead = 0;
		do{		
			lbFileNo = readbyte();
			if(lbFileNo>=0) {
				lnChunkSize = readint();
				readskip(3);
				lnRead = read(laBuffer, 0, lnChunkSize);
				paWrite[lbFileNo].write(laBuffer,0,lnRead);
			}
		} while(resumeread(lbFileNo));
		ack();
		for(int i=0; i<paWrite.length; i++)
			paWrite[i].close();
	}
	
	private BufferedOutputStream getDstBufferedFileStream(String path) throws IOException {
		File dstFile = new File(path);
		if(dstFile.exists()) {
			if(Props.Get("SAFEITY").equals("1")) {
				Logfile.Write("Error File " + path + " already exists!");
				throw new IOException("Error File " + path + " already exists!");
			}
		}
		return new BufferedOutputStream(new FileOutputStream(path));
	}
	
	private boolean resumeread(byte pbFlag) throws IOException {
		if(pbFlag>=0)
			return true;
		switch(pbFlag) {
		case -4:
		case -7:
			Logfile.Write("Device is Busy!");
			break;
		case (byte) 0xff:
			return false;
		default:
			throw new IOException("Unknown Protocol Flag " + pbFlag);
		}	
		return true;
	}

	/**
	 * Remove a File from the Receiver FS
	 */
	public void rm(DvrFile poFile) 
			throws TechnisatException, IOException {
		byte response;
		
		if(!poFile.isRecNo()) {
			throw new TechnisatException("File has no unique Record Number (Not implemented)");
		}

		Logfile.Write("Removing File " + poFile);
		ByteArrayOutputStream command = new ByteArrayOutputStream();
		DataOutputStream commandDataStream = new DataOutputStream(command);

		commandDataStream.writeByte(Header.PT_RMFILE_BYRECNO);
		commandDataStream.writeShort(poFile.getRecNo());
		write(command.toByteArray());
		response = readbyte();
		if(response == 1) {
			poFile.m_oParent.m_oFiles.remove(poFile);
		}
		else {
			throw new TechnisatException("Error in Receiver Response (RM Command) " + response);
		}
	}

	/**
	 * Closes the used socket.
	*/
	public void close() {
		try {
			m_connection.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}		
}
