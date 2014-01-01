package technisat;

public class TechnisatResponses {
	public static final byte	DIR					= 0;
	public static final byte	BIN					= 1;
	public static final byte	TS_RADIO			= 3;
	public static final byte	TS_RECORD_SD		= 4;
	public static final byte	TS_RECORD_HD		= 7;
	public static final byte	USB_STICK			= 9;

	public static final byte	OK					= 1;
	public static final byte	DISK_BUSY			= -4;
	public static final byte	DISK_STARTING_UP	= -7;

	public static final byte	DONE				= (byte) 0xff;
}
