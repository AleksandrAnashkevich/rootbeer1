package org.trifort.rootbeer.runtime;

/**
 * 100% boiler plate. In C you would use a simple struct to hold all
 * these properties. This is ridiculous.
 */
public final class GpuDevice
{
    public static final int DEVICE_TYPE_CUDA   = 1;
    public static final int DEVICE_TYPE_OPENCL = 2;
    public static final int DEVICE_TYPE_NEMU   = 3;
    public static final int DEVICE_TYPE_JAVA   = 4;

    /* Constructor which saves whether to use CUDA, OpenCL, ... */
    private GpuDevice( final int device_type )
    {
        m_deviceType = device_type;
    }

    /* this is used by CUDARuntime.c only */
    private static GpuDevice newCudaDevice
    (
        final int     device_id                     ,
        final int     major_version                 ,
        final int     minor_version                 ,
        final String  device_name                   ,
        final long    free_global_mem_size          ,
        final long    total_global_mem_size         ,
        final int     max_registers_per_block       ,
        final int     warp_size                     ,
        final int     max_pitch                     ,
        final int     max_threads_per_block         ,
        final int     max_shared_memory_per_block   ,
        final int     clock_rate_kHz                ,
        final int     memory_clock_rate_kHz         ,
        final int     constant_mem_size             ,
        final boolean integrated                    ,
        final int     max_threads_per_multiprocessor,
        final int     multiprocessor_count          ,
        final int     max_block_dim_x               ,
        final int     max_block_dim_y               ,
        final int     max_block_dim_z               ,
        final int     max_grid_dim_x                ,
        final int     max_grid_dim_y                ,
        final int     max_grid_dim_z
    )
    {
        GpuDevice ret = new GpuDevice(DEVICE_TYPE_CUDA);
        ret.setDeviceId                   ( device_id                      );
        ret.setDeviceName                 ( device_name                    );
        ret.setWarpSize                   ( warp_size                      );
        ret.setMaxPitch                   ( max_pitch                      );
        ret.setIntegrated                 ( integrated                     );
        ret.setMaxBlockDimX               ( max_block_dim_x                );
        ret.setMaxBlockDimY               ( max_block_dim_y                );
        ret.setMaxBlockDimZ               ( max_block_dim_z                );
        ret.setMaxGridDimX                ( max_grid_dim_x                 );
        ret.setMaxGridDimY                ( max_grid_dim_y                 );
        ret.setMaxGridDimZ                ( max_grid_dim_z                 );
        ret.setVersion                    ( major_version, minor_version, 0);
        ret.setClockRateHz                ( clock_rate_kHz * 1000.f        ); // clock_rate as returned by cuDeviceGetAttribute is in kHz
        ret.setMemoryClockRateHz          ( memory_clock_rate_kHz * 1000.f ); // same as clock_rate
        ret.setFreeGlobalMemoryBytes      ( free_global_mem_size           );
        ret.setTotalGlobalMemoryBytes     ( total_global_mem_size          );
        ret.setMaxRegistersPerBlock       ( max_registers_per_block        );
        ret.setMaxThreadsPerBlock         ( max_threads_per_block          );
        ret.setMaxSharedMemoryPerBlock    ( max_shared_memory_per_block    );
        ret.setTotalConstantMemoryBytes   ( constant_mem_size              );
        ret.setMaxThreadsPerMultiprocessor( max_threads_per_multiprocessor );
        ret.setMultiProcessorCount        ( multiprocessor_count           );
        return ret;
    }

    public static GpuDevice newOpenCLDevice( final String device_name )
    {
        GpuDevice ret = new GpuDevice( DEVICE_TYPE_OPENCL );
        ret.setDeviceName( device_name );
        return ret;
    }

    private final int m_deviceType               ;
    private int     m_deviceId                   ;
    private int     m_majorVersion               ;
    private int     m_minorVersion               ;
    private int     m_patchVersion               ;
    private String  m_name                       ;
    private long    m_freeGlobalMemoryBytes      ;
    private long    m_totalGlobalMemoryBytes     ;
    private int     m_maxRegistersPerBlock       ;
    private int     m_warpSize                   ;
    private int     m_maxPitch                   ;
    private int     m_maxThreadsPerBlock         ;
    private int     m_maxSharedMemoryPerBlock    ;
    private float   m_clockRateHz                ;
    private float   m_memoryClockRateHz          ;
    private int     m_totalConstantMemoryBytes   ;
    private boolean m_integrated                 ;
    private int     m_maxThreadsPerMultiprocessor;
    private int     m_multiProcessorCount        ;
    private int     m_maxBlockDimX               ;
    private int     m_maxBlockDimY               ;
    private int     m_maxBlockDimZ               ;
    private int     m_maxGridDimX                ;
    private int     m_maxGridDimY                ;
    private int     m_maxGridDimZ                ;

    public Context createContext()
    {
        if ( m_deviceType == DEVICE_TYPE_CUDA ) {
            return new CUDAContext(this);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public Context createContext( int memorySize )
    {
        if ( m_deviceType == DEVICE_TYPE_CUDA )
        {
            CUDAContext ret = new CUDAContext(this);
            ret.setMemorySize(memorySize);
            return ret;
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    public void setVersion(int major, int minor, int patch)
    {
       m_majorVersion = major;
       m_minorVersion = minor;
       m_patchVersion = patch;
    }

    /**
     * Returns the number of arithmetic CUDA cores per streaming multiprocessor
     * Note that there are also extra special function units.
     * Note that for 2.0 the two warp schedulers can only issue 16 instructions
     * per cycle each. Meaning the 32 CUDA cores can't be used in parallel with
     * the 4 special function units. For 2.1 up this is a different matter
     * http://docs.nvidia.com/cuda/cuda-c-programming-guide/index.html#compute-capabilities
     **/
    public int getCudaCoresPerMultiprocessor()
    {
        if ( m_majorVersion == 2 && m_minorVersion == 0 ) /* Fermi */
            return 32;
        if ( m_majorVersion == 2 && m_minorVersion == 1 ) /* Fermi */
            return 48;
        if ( m_majorVersion == 3 )  /* Kepler */
            return 192;
        if ( m_majorVersion == 5 )  /* Maxwell */
            return 128;
        if ( m_majorVersion == 6 )  /* Pascal */
            return 64;
        return 0;
    }

    public float getPeakFlops()
    {
        return (float) m_multiProcessorCount * getCudaCoresPerMultiprocessor() * m_clockRateHz;
    }

    // public long getFreeGlobalMemoryBytes()
    // {
    //     if ( m_freeGlobalMemoryBytes == -1 )
    //         m_freeGlobalMemoryBytes = /* some JNI function which needs to be written ... */
    //     return m_freeGlobalMemoryBytes;
    // }

    /* why is there no define. Why even bother making accessor methods ... */
    public int     getDeviceType                 (){ return m_deviceType  ; }
    public int     getDeviceId                   (){ return m_deviceId    ; }
    public int     getMajorVersion               (){ return m_majorVersion; }
    public int     getMinorVersion               (){ return m_minorVersion; }
    public int     getPatchVersion               (){ return m_patchVersion; }
    public String  getDeviceName                 (){ return m_name        ; }
    public int     getWarpSize                   (){ return m_warpSize    ; }
    public int     getMaxPitch                   (){ return m_maxPitch    ; }
    public float   getClockRateHz                (){ return m_clockRateHz ; }
    public boolean getIntegrated                 (){ return m_integrated  ; }
    public int     getMaxBlockDimX               (){ return m_maxBlockDimX; }
    public int     getMaxBlockDimY               (){ return m_maxBlockDimY; }
    public int     getMaxBlockDimZ               (){ return m_maxBlockDimZ; }
    public int     getMaxGridDimX                (){ return m_maxGridDimX ; }
    public int     getMaxGridDimY                (){ return m_maxGridDimY ; }
    public int     getMaxGridDimZ                (){ return m_maxGridDimZ ; }
    public long    getTotalGlobalMemoryBytes     (){ return m_totalGlobalMemoryBytes     ; }
    public int     getMaxRegistersPerBlock       (){ return m_maxRegistersPerBlock       ; }
    public int     getMaxThreadsPerBlock         (){ return m_maxThreadsPerBlock         ; }
    public int     getMaxSharedMemoryPerBlock    (){ return m_maxSharedMemoryPerBlock    ; }
    public float   getMemoryClockRateHz          (){ return m_memoryClockRateHz          ; }
    public int     getTotalConstantMemoryBytes   (){ return m_totalConstantMemoryBytes   ; }
    public int     getMultiProcessorCount        (){ return m_multiProcessorCount        ; }
    public int     getMaxThreadsPerMultiprocessor(){ return m_maxThreadsPerMultiprocessor; }

    public void setDeviceId                (int value){ m_deviceId     = value; }
    public void setDeviceName              (String sr){ m_name         = sr   ; }
    public void setWarpSize                (int value){ m_warpSize     = value; }
    public void setMaxPitch                (int value){ m_maxPitch     = value; }
    public void setClockRateHz             (float l  ){ m_clockRateHz  = l    ; }
    public void setIntegrated              (boolean b){ m_integrated   = b    ; }
    public void setMaxBlockDimX            (int value){ m_maxBlockDimX = value; }
    public void setMaxBlockDimY            (int value){ m_maxBlockDimY = value; }
    public void setMaxBlockDimZ            (int value){ m_maxBlockDimZ = value; }
    public void setMaxGridDimX             (int value){ m_maxGridDimX  = value; }
    public void setMaxGridDimY             (int value){ m_maxGridDimY  = value; }
    public void setMaxGridDimZ             (int value){ m_maxGridDimZ  = value; }
    public void setFreeGlobalMemoryBytes   (long size){ m_freeGlobalMemoryBytes    = size ; }
    public void setTotalGlobalMemoryBytes  (long size){ m_totalGlobalMemoryBytes   = size ; }
    public void setMaxRegistersPerBlock    (int value){ m_maxRegistersPerBlock     = value; }
    public void setMaxThreadsPerBlock      (int value){ m_maxThreadsPerBlock       = value; }
    public void setMaxSharedMemoryPerBlock (int value){ m_maxSharedMemoryPerBlock  = value; }
    public void setMemoryClockRateHz       (float l  ){ m_memoryClockRateHz        = l    ; }
    public void setTotalConstantMemoryBytes(int value){ m_totalConstantMemoryBytes = value; }
    public void setMultiProcessorCount     (int value){ m_multiProcessorCount      = value; }
    public void setMaxThreadsPerMultiprocessor(int value){ m_maxThreadsPerMultiprocessor = value; }
}
