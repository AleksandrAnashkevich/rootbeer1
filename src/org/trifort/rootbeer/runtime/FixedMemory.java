package org.trifort.rootbeer.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.omg.CORBA._IDLTypeStub;
import org.trifort.rootbeer.generate.bytecode.Constants;

/**
 * This class is only a wrapper for FixedMemory.c
 * The memory can be fixed, because all member variables have to be known
 * at compile-time
 * It seems to be some kind of managed memory buffer / stream.
 * The use of C here is in order to serialize the data, because Java
 * maybe would save even a byte array non-sequentially to RAM ???
 */
public class FixedMemory implements Memory
{
  /**
   * start address of memroy chunk.
   * This address isn't changed except for malloc and free
   */
  protected final long       m_address        ;
  protected final long       m_size           ; /**< site in bytes of memory chunk */
  protected final MemPointer m_staticPointer  ;
  protected final MemPointer m_instancePointer;
  protected       MemPointer m_currentPointer ;
  protected final List<List<Long>> m_integerList;

  /**
   * Allocates a memory location of fixed width i.e. which is serialized
   *
   * @param[in] size memory size to allocate in bytes
   */
  public FixedMemory( long size )
  {
      m_address = malloc(size);
      if( m_address == 0 /* null pointer */ ) {
          throw new RuntimeException("cannot allocate memory of size: "+size);
      }
      m_size            = size;
      m_instancePointer = new MemPointer("instance_mem");
      m_staticPointer   = new MemPointer("static_mem");
      m_currentPointer  = m_instancePointer;
      m_integerList     = new ArrayList<List<Long>>();
  }

    protected long currPointer(){ return m_currentPointer.m_pointer;}

    /* Why the Bitshift !!!??? */
    @Override public long readRef ()
    {
        long ret = readInt();
        ret = ret << 4;
        return ret;
    }
    @Override public void writeRef (long value)
    {
        value = value >> 4;
        writeInt( (int) value );
    }

    /* I miss the preprocessor. Or maybe use templates to shorten this
     * boiler plate code ?
     * Best to deactivate line-wrap here for undestanding :(
     * I also miss a sizeof operator in Java in order to make those magic
     * constants more understandable
     * @todo the function signatures should be less diverse. E.g. why
     *       does readArray take m_address+currPointer where read does
     *       take them separately?
     */
    @Override public byte    readByte   () { byte    ret = doReadByte   (currPointer(), m_address); incrementAddress(1); return ret; }
    @Override public boolean readBoolean() { boolean ret = doReadBoolean(currPointer(), m_address); incrementAddress(1); return ret; }
    @Override public short   readShort  () { short   ret = doReadShort  (currPointer(), m_address); incrementAddress(2); return ret; }
    @Override public int     readInt    () { int     ret = doReadInt    (currPointer(), m_address); incrementAddress(4); return ret; }
    @Override public float   readFloat  () { float   ret = doReadFloat  (currPointer(), m_address); incrementAddress(4); return ret; }
    @Override public double  readDouble () { double  ret = doReadDouble (currPointer(), m_address); incrementAddress(8); return ret; }
    @Override public long    readLong   () { long    ret = doReadLong   (currPointer(), m_address); incrementAddress(8); return ret; }
    @Override public void writeByte   (byte    value) { doWriteByte   (currPointer(), value, m_address); incrementAddress(1); }
    @Override public void writeBoolean(boolean value) { doWriteBoolean(currPointer(), value, m_address); incrementAddress(1); }
    @Override public void writeShort  (short   value) { doWriteShort  (currPointer(), value, m_address); incrementAddress(2); }
    @Override public void writeInt    (int     value) { doWriteInt    (currPointer(), value, m_address); incrementAddress(4); }
    @Override public void writeFloat  (float   value) { doWriteFloat  (currPointer(), value, m_address); incrementAddress(4); }
    @Override public void writeDouble (double  value) { doWriteDouble (currPointer(), value, m_address); incrementAddress(8); }
    @Override public void writeLong   (long    value) { doWriteLong   (currPointer(), value, m_address); incrementAddress(8); }
    @Override public void readArray (byte   [] array){ doReadByteArray    (array, m_address+currPointer(), 0, array.length); }
    @Override public void readArray (boolean[] array){ doReadBooleanArray (array, m_address+currPointer(), 0, array.length); }
    @Override public void readArray (short  [] array){ doReadShortArray   (array, m_address+currPointer(), 0, array.length); }
    @Override public void readArray (int    [] array){ doReadIntArray     (array, m_address+currPointer(), 0, array.length); }
    @Override public void readArray (float  [] array){ doReadFloatArray   (array, m_address+currPointer(), 0, array.length); }
    @Override public void readArray (double [] array){ doReadDoubleArray  (array, m_address+currPointer(), 0, array.length); }
    @Override public void readArray (long   [] array){ doReadLongArray    (array, m_address+currPointer(), 0, array.length); }
    @Override public void writeArray(byte   [] array){ doWriteByteArray   (array, m_address+currPointer(), 0, array.length); }
    @Override public void writeArray(boolean[] array){ doWriteBooleanArray(array, m_address+currPointer(), 0, array.length); }
    @Override public void writeArray(short  [] array){ doWriteShortArray  (array, m_address+currPointer(), 0, array.length); }
    @Override public void writeArray(int    [] array){ doWriteIntArray    (array, m_address+currPointer(), 0, array.length); }
    @Override public void writeArray(float  [] array){ doWriteFloatArray  (array, m_address+currPointer(), 0, array.length); }
    @Override public void writeArray(double [] array){ doWriteDoubleArray (array, m_address+currPointer(), 0, array.length); }
    @Override public void writeArray(long   [] array){ doWriteLongArray   (array, m_address+currPointer(), 0, array.length); }

    /* why does readChar wrap readInt Oo ???
     * https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html
     * char is 16 bit, while int should be 32 bit.
     * Used by BclMemory.java and VisitorReadGen.java
     * Well in the end it shouldn't matter as long as read and write do it
     * in the same way, it only means 16-bit are wasted for no reason, because
     * readShort doesn't use int, meaning there seems to be no reason for
     * this to not call readShort instead of readInt. */
    @Override public char readChar() { int value = readInt(); char ret = (char) value; return ret; }
    @Override public void writeChar(char value){ writeInt(value); }

    public native void doReadByteArray    (byte   [] array, long addr, int start, int len);
    public native void doReadBooleanArray (boolean[] array, long addr, int start, int len);
    public native void doReadShortArray   (short  [] array, long addr, int start, int len);
    public native void doReadIntArray     (int    [] array, long addr, int start, int len);
    public native void doReadFloatArray   (float  [] array, long addr, int start, int len);
    public native void doReadDoubleArray  (double [] array, long addr, int start, int len);
    public native void doReadLongArray    (long   [] array, long addr, int start, int len);
    public native void doWriteByteArray   (byte   [] array, long addr, int start, int len);
    public native void doWriteBooleanArray(boolean[] array, long addr, int start, int len);
    public native void doWriteShortArray  (short  [] array, long addr, int start, int len);
    public native void doWriteIntArray    (int    [] array, long addr, int start, int len);
    public native void doWriteFloatArray  (float  [] array, long addr, int start, int len);
    public native void doWriteDoubleArray (double [] array, long addr, int start, int len);
    public native void doWriteLongArray   (long   [] array, long addr, int start, int len);

    public native byte    doReadByte      (long ptr, long cpu_base);
    public native boolean doReadBoolean   (long ptr, long cpu_base);
    public native short   doReadShort     (long ptr, long cpu_base);
    public native int     doReadInt       (long ptr, long cpu_base);
    public native float   doReadFloat     (long ptr, long cpu_base);
    public native double  doReadDouble    (long ptr, long cpu_base);
    public native long    doReadLong      (long ptr, long cpu_base);
    public native void    doWriteByte     (long ptr, byte    value, long cpu_base);
    public native void    doWriteBoolean  (long ptr, boolean value, long cpu_base);
    public native void    doWriteShort    (long ptr, short   value, long cpu_base);
    public native void    doWriteInt      (long ptr, int     value, long cpu_base);
    public native void    doWriteFloat    (long ptr, float   value, long cpu_base);
    public native void    doWriteDouble   (long ptr, double  value, long cpu_base);
    public native void    doWriteLong     (long ptr, long    value, long cpu_base);

    private native long malloc(long size);
    private native void free(long address);
    @Override public long getHeapEndPtr  (){ return m_currentPointer.m_heapEnd ; }
    @Override public long getSize        (){ return m_size                     ; }
    @Override public long getAddress     (){ return m_address                  ; }
    @Override public long getPointer     (){ return m_currentPointer.m_pointer ; }
    @Override public void clearHeapEndPtr(){ m_currentPointer.clearHeapEndPtr(); }
    @Override public void pushAddress    (){ m_currentPointer.pushAddress    (); }
    @Override public void popAddress     (){ m_currentPointer.popAddress     (); }
    @Override public long mallocWithSize  (int  size   ){ return m_currentPointer.mallocWithSize(size); }
    @Override public void setHeapEndPtr   (long value  ){ m_currentPointer.m_heapEnd = value       ; }
    @Override public void setAddress      (long address){ m_currentPointer.setAddress(address)     ; }
    @Override public void incrementAddress(int  offset ){ m_currentPointer.incrementAddress(offset); }
    @Override public void setPointer      (long ptr    ){ setAddress(ptr); }
    @Override public void incPointer      (long value  ){ incrementAddress((int) value); }
    @Override public List<byte[]> getBuffer() { throw new UnsupportedOperationException("Not supported yet."); }
    @Override public void finishCopy(long size) {}
    @Override public void finishRead() {}
    @Override public void useInstancePointer() { m_currentPointer = m_instancePointer; }
    @Override public void useStaticPointer  () { m_currentPointer = m_staticPointer  ; }
    @Override public void align(){ m_currentPointer.align(); }
    @Override public void close() { free(m_address); }

    @Override
    public void readIntArray(int[] array, int size) {
      for(int i = 0; i < size; ++i){
        array[i] = readInt();
      }
    }

  public void startIntegerList(){
    m_integerList.add(new ArrayList<Long>());
    pushAddress();
  }

  public void addIntegerToList(long value){
    List<Long> top = m_integerList.get(m_integerList.size()-1);
    top.add(value);
  }

  public void endIntegerList(){
    popAddress();
    List<Long> top = m_integerList.get(m_integerList.size()-1);
    for(Long curr : top){
      writeRef(curr);
    }
    m_integerList.remove(m_integerList.size()-1);
  }

  public void finishReading(){
    long ptr = m_currentPointer.m_pointer;

    int mod = (int) (ptr % Constants.MallocAlignBytes);
    if(mod != 0){
      ptr += (Constants.MallocAlignBytes - mod);
    }
    setPointer(ptr);
  }

    @Override
    public void align16()
    {
        m_instancePointer.align16();
        m_staticPointer.align16();
    }

    /**
     * Manages an automatically growing memory address.
     */
    private class MemPointer
    {
        private Stack<Long> m_stack;
        /**
         * as per java language spec initialized to 0
         * @see http://docs.oracle.com/javase/specs/jls/se7/html/jls-4.html#jls-4.12.5
         * The current offset i.e. pointer.
         * This value is equivalent to the used heap size in bytes for all
         * elements except the last
         */
        private long m_pointer;
        /**
         * Initialized to 0. The last byte in the heap which holds date
         * (m_heapEnd - m_pointer) is therefore at minimum the size of the
         * last allocated chunk. It can be larger when using alignment.
         */
        private long m_heapEnd;
        private String name;

        public MemPointer(String name){
            this.name = name;
            m_stack = new Stack<Long>();
        }
        public String getName  () { return name; }
        public void popAddress () { m_pointer = m_stack.pop(); }
        public void pushAddress() { m_stack.push(m_pointer); }

        /**
         * Allocates aligned memory in managed memory
         * @return the start address of the new allocation. This also is the
         *         aligned end of heap i.e. roughly the used memory in bytes
         *         before the allocation
         */
        public long mallocWithSize( int size )
        {
            /* pad to align 'size' */
            final int mod = size % Constants.MallocAlignBytes;
            if ( mod != 0 ) {
                size += (Constants.MallocAlignBytes - mod);
            }
            /* pad to align 'm_heapEnd' for beginning of new data */
            final long mod2 = m_heapEnd % Constants.MallocAlignBytes;
            if( mod2 != 0 ) {
                m_heapEnd += (Constants.MallocAlignBytes - mod2);
            }

            String debugMsg =
                "[FixedMemory.java : mallocWithSize]\n" +
                "    currentHeapEnd / bytes currently in use: " + m_heapEnd + " B\n" +
                "    Bytes requested to allocate            : " + size      + " B\n" +
                "    total size available in FixedMemory    : " + m_size    + " B\n" ;
            //System.out.print( debugMsg );

            assert( size      % Constants.MallocAlignBytes == 0 );
            assert( m_heapEnd % Constants.MallocAlignBytes == 0 );

            if ( m_heapEnd + size > m_size )
            {
                throw new OutOfMemoryError( debugMsg +
                    "(This happens if createContext(size) was called with "     +
                    "an insufficient size, or CUDAContext.java:findMemorySize " +
                    "failed to determine the correct size automatically)"       );
            }

            m_pointer  = m_heapEnd;
            m_heapEnd += size;

            return m_pointer;
        }

        /**
         * Only called by writeBlocksList and writeBlocksTemplate
         */
        private void clearHeapEndPtr()
        {
            /**
             * shouldn't this also clear m_stack ???
             * Note that no error may happen if the caller behaves correctly,
             * meaning he doesn't call pop more often than push!
             * Then even if he calls pop less than push no bug may happen,
             * but it would be a memory leak.
             */
            m_heapEnd = 0;
            m_pointer = 0;
        }

        /**
         * Sets current position of heap and increases heap size if necessary
         */
        private void setAddress( long address )
        {
            m_pointer = address;
            if ( address > m_heapEnd )
                m_heapEnd = address;
        }
        private void incrementAddress( int offset ) { setAddress( m_pointer + offset ); }

        /**
         * Aligns current address in heap to 8 bytes.
         * @todo Why align to 8 and 16 byte version, not only one ?
         */
        private void align()
        {
            if ( m_pointer % 8 != 0 ) {
                setAddress( m_pointer + ( 8 - m_pointer % 8 ) );
            }
        }
        /**
         * @todo Why does align16 align the heapEnd while in contrast align
         *       aligns the current position ? Wanted? If so, then it's
         *       confusing to call them both 'align'
         */
        public void align16()
        {
            final long mod = m_heapEnd % Constants.MallocAlignBytes;
            if ( mod != 0 ) {
                m_heapEnd += (Constants.MallocAlignBytes - mod);
            }
        }
    }
}
