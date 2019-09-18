package org.commonjava.indy.filer.def;

import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.indy.metrics.RequestContextHelper;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class TimingOutputStream
        extends FilterOutputStream
{
    private long nanos = -1L;

    public TimingOutputStream( final OutputStream stream )
    {
        super( stream );
    }

    @Override
    public void write( final int b )
            throws IOException
    {
        if ( nanos < 0 )
        {
            nanos = System.nanoTime();
        }

        super.write( b );
    }

    @Override
    public void write( final byte[] b )
            throws IOException
    {
        if ( nanos < 0 )
        {
            nanos = System.nanoTime();
        }

        super.write( b );
    }

    @Override
    public void write( final byte[] b, final int off, final int len )
            throws IOException
    {
        if ( nanos < 0 )
        {
            nanos = System.nanoTime();
        }

        super.write( b, off, len );
    }

    @Override
    public void close()
            throws IOException
    {
        super.close();

        RequestContextHelper.setContext( RequestContextHelper.RAW_NANOS, System.nanoTime() - nanos );
    }
}
