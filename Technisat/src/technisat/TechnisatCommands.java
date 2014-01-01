package technisat;

public class TechnisatCommands {
	public static final byte PT_ACK 			= 0x01;	
	
	public static final byte PT_GETSYSINFO 		= 0x02;	
	
	public static final byte PT_GETDIR 			= 0x03;
	
	public static final byte PT_RECSYSINFO 		= 0x1c;
	
	public static final byte PT_GETFILE_BYNAME	= 0x04;
			
	public static final byte PT_GETFILE_BYRECNO	= 0x05;
	
	public static final byte PT_RMFILE_BYRECNO  = 0x17;
	/* Not sure about that one, just guessing from the usage of that number
	 * in the code. */
}