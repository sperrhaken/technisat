import java.io.PrintStream;
import java.util.ListIterator;
import java.util.Vector;

public class DvrDirectory {
	public Vector<DvrDirectory> m_oDirectorys = new Vector<DvrDirectory>();	
	public Vector<DvrFile> m_oFiles = new Vector<DvrFile>();
	public String m_cDisplayName = null ;
	public String m_cRemoteName = null;
	public String m_cDescription = null;
	public DvrDirectory m_oParent;
	public boolean m_bIsOpen = false;
	
	public DvrDirectory(DvrDirectory poParent, String pcRemoteName,
			String pcDisplayName, String pcDescription) {
		m_oParent = poParent;
		m_cDisplayName = pcDisplayName;
		m_cRemoteName = pcRemoteName;
		m_cDescription = pcDescription;
		
		if(poParent!=null)
			m_oDirectorys.add( new DvrDirectory(null, "..", "..", null));
	}
	
	public DvrDirectory(DvrDirectory poSourceDir) {
		m_cDisplayName = poSourceDir.m_cDisplayName;
		m_cRemoteName = poSourceDir.m_cRemoteName;
		m_cDescription = poSourceDir.m_cDescription;		
	}
	
	public String toString() {
		if(m_cDescription!=null)
			return m_cDisplayName + " ("+m_cDescription+")";
		else
			return m_cDisplayName;
	}
	
	public DvrFile GetFileByRecNo(int pnRecNo) {
		for (DvrFile f : m_oFiles) {
			if (f.getIndex() == pnRecNo)
				return f;
		}
		return null;
	}

	public boolean is_dir(String pcDir) {
		if (GetSubDirectory(pcDir) != null)
			return true;
		else
			return false;
	}

	public DvrDirectory GetSubDirectory(String pcDir) {
		for (DvrDirectory d : m_oDirectorys) {
			if (d.m_cDisplayName.toUpperCase().equals(pcDir.toUpperCase())) {
				return d;
			}
		}
		return null;
	}

	public String GetFullPath() {
		return ( m_oParent==null ? "$/" : m_oParent.GetFullPath() + m_cDisplayName + "/" );
	}
}
