import java.text.SimpleDateFormat;
import java.util.Date;

import static technisat.TechnisatResponses.*;

public class DvrFile {	
	public DvrFile(DvrDirectory poParent, String pcFileName, long pnFileSize, short pnIndex, byte pbType, Date pdDate) {
		m_cFileName = pcFileName;
		m_nFileSize = pnFileSize;
		m_nIndex = pnIndex;
		m_nType = pbType;
		m_dDate = pdDate;
		m_oParent = poParent;
	}
	
	public String toString() {
		SimpleDateFormat loForm = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
		return
					(m_nIndex>=0 ? String.format("%4d", m_nIndex) : "  --") + " " + 
					getTypeString() + " "+ 
					String.format("%5d",m_nFileSize/1024/1024)+"MB " + 
					loForm.format(m_dDate) + " " + 
					m_cFileName;
	}
	
	public String getTypeString() {
		switch(m_nType) {
		case BIN:
			return "BIN  ";
		case TS_RECORD_SD:
			return "TS/SD";
		case TS_RECORD_HD:
			return "TS/HD";
		}
		return "     ";
	}
	
	public short getIndex() {
		return m_nIndex;
	}
	
	public long getFileSize() {
		return m_nFileSize;
	}
	
	public String getFileName() {
		return m_cFileName;
	}
	
	public String getUniqueFileName() {
		final String replaceRegex =	"\\\\"	+ "|" +
									"/"		+ "|" +
									":"		+ "|" +
									"\\*"	+ "|" +
									"\\?"	+ "|" +
									"\""	+ "|" +
									"<"		+ "|" +
									">"		+ "|" +
									"\\|";
		String date = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(m_dDate);

		return date + " " + m_cFileName.replaceAll(replaceRegex, "");
	}

	public short getRecNo() throws TechnisatException {
		if(m_nIndex<0)
			throw new TechnisatException("File has no Record Number");
		return m_nIndex;
	}

	public boolean hasRecNo() {
		/* FIXME
		 *
		 * getRecNo used m_nIndex<0 to test for the non-existent of
		 * rec-number, here m_Index>0 is used to test for one.  Does
		 * m_nIndex==0 constitude a valid rec-number?
		 */
		return m_nIndex>0;
	}

		
	DvrDirectory m_oParent;
	String m_cFileName;
	long m_nFileSize;
	short m_nIndex;
	byte m_nType;
	Date m_dDate;
}
