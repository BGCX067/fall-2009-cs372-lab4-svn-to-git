/**
 * class Dentry
 *
 * Directory entry
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */
import java.nio.ByteBuffer;

public class Dentry implements Comparable {
	public static final int DENTRY_SIZE = 36;
	public static final int MAX_FILENAME = 32;
	private int ino;
	private String fileName;

	public Dentry() {
		this.fileName = null;
		this.ino = 0;
	}
   // Dentries sort by file name
    public int compareTo(Object d) {
       return this.fileName.compareTo(((Dentry)d).getFileName());
    }

	public Dentry(String fileName, int ino) {
		this.fileName = new String(fileName);
		this.ino = ino;
	}

	/**
	 * store Dentry into byte array
	 */
	public byte[] getBytes() {
		ByteBuffer buffer = ByteBuffer.allocate(DENTRY_SIZE);
		buffer.putInt(ino);
		if (fileName == null) {
			for (int i = 0; i < MAX_FILENAME; i++)
				buffer.put((byte)0);
		}
		else {
			buffer.put(fileName.getBytes());
         if (fileName.getBytes().length < MAX_FILENAME)
            buffer.put((byte)0);
		}		
		return buffer.array();
	}

	/**
	 * load byte[] into Dentry
	 */
	public void setBytes(byte[] buf) {
		ByteBuffer buffer = ByteBuffer.wrap(buf);
		this.ino = buffer.getInt();
		this.fileName = (new String(buffer.array(), sizeOfIno(), MAX_FILENAME)).trim();
	}

	public void setBytes(byte[] buf, int offset) {
		ByteBuffer buffer = ByteBuffer.wrap(buf, offset, DENTRY_SIZE);
		this.ino = buffer.getInt();
		byte[] tmp = new byte[DENTRY_SIZE-sizeOfIno()];
		System.arraycopy(buffer.array(), buffer.position(), tmp, 0, tmp.length);
		this.fileName = (new String(tmp)).trim();
	}

	public String getFileName() { return this.fileName; }
	public int getIno() { return this.ino; }
	public int sizeOfIno() { return 4; }
}
