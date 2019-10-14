/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.filer.def;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.CountingOutputStream;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.AbstractTransferDecorator;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;

public class IOLatencyDecorator
        extends AbstractTransferDecorator
{
    private Function<String, Timer.Context> timerProvider;

    private Function<String, Meter> meterProvider;

    public IOLatencyDecorator( Function<String, Timer.Context> timerProvider, Function<String, Meter> meterProvider )
    {
        this.timerProvider = timerProvider;
        this.meterProvider = meterProvider;
    }

    @Override
    public InputStream decorateRead( final InputStream stream, final Transfer transfer, final EventMetadata metadata )
            throws IOException
    {
        return new TimingInputStream( new CountingInputStream( stream ), timerProvider, meterProvider );
    }

    @Override
    public OutputStream decorateWrite( final OutputStream stream, final Transfer transfer, final TransferOperation op,
                                       final EventMetadata metadata )
            throws IOException
    {
        if ( op == TransferOperation.UPLOAD )
        {
            return new TimingOutputStream( new CountingOutputStream( stream ), timerProvider, meterProvider );
        }

        return super.decorateWrite( stream, transfer, op, metadata );
    }
}
