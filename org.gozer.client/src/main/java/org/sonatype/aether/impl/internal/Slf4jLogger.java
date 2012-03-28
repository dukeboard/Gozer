package org.sonatype.aether.impl.internal;

/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import org.slf4j.LoggerFactory;
import org.sonatype.aether.spi.log.Logger;

/**
 * A logger that delegates to Slf4J logging.
 * 
 * @author Benjamin Bentmann
 */
public class Slf4jLogger
    implements Logger
{

    private org.slf4j.Logger logger = LoggerFactory.getLogger(this.getClass());

    public Slf4jLogger()
    {
        // enables default constructor
    }

    public Slf4jLogger( org.slf4j.Logger logger )
    {
        setLogger( logger );
    }

    public void setLogger( org.slf4j.Logger logger )
    {
        this.logger = logger;
    }

    public boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    public void debug( String msg )
    {
        logger.debug( msg );
    }

    public void debug( String msg, Throwable error )
    {
        logger.debug( msg, error );
    }

    public boolean isWarnEnabled()
    {
        return logger.isWarnEnabled();
    }

    public void warn( String msg )
    {
        logger.warn( msg );
    }

    public void warn( String msg, Throwable error )
    {
        logger.warn( msg, error );
    }

}
