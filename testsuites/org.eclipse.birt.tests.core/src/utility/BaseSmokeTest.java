
package utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.birt.report.engine.api.EngineConstants;
import org.eclipse.birt.report.engine.api.EngineException;
import org.eclipse.birt.report.engine.api.HTMLRenderContext;
import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: Actuate Corporation -
 * initial API and implementation
 ******************************************************************************/

public abstract class BaseSmokeTest extends EngineCase
{

	private Map testStatus = new LinkedHashMap( );
	private Map engineInternalErrors = new LinkedHashMap( );

	/**
	 * Working folder that containing the smoke test cases.
	 * 
	 * @return Working folder that containing the 'TestCases' folder containing
	 *         the smoke test collections.
	 */

	protected abstract String getWorkingFolder( );

	/**
	 * @throws Exception
	 * @throws Exception
	 */

	public void setUp( ) throws Exception
	{
		super.setUp( );
		// removeResource( );
	}

	public void tearDown( )
	{
		// removeResource( );
	}

	public final void testSmoke( ) throws Exception
	{
		List resultList = new ArrayList( );
		String path = "TestCases/EngineSmokeTestReport.txt";
		InputStream input = this.getClass( ).getResourceAsStream( path );
		BufferedReader reader = new BufferedReader( new InputStreamReader(
				input ) );

		while ( true )
		{
			try
			{
				String content = reader.readLine( );
				if ( content == null )
					break;
				resultList.add( content );
			}
			catch ( Exception e )
			{
				// do nothing.
			}
		}

		Iterator iterator = resultList.iterator( );
		while ( iterator.hasNext( ) )
		{
			String inputPath = (String) iterator.next( );
			inputPath = inputPath.replace( '\\', '/' );
			copyResource( inputPath, inputPath, "TestCases" );
		}

		String inputFolder = this.getFullQualifiedClassName( )
				+ "/TestCases/input/"; //$NON-NLS-1$
		String tempDir = System.getProperty( "java.io.tmpdir" );
		String outputFolder = tempDir + getFullQualifiedClassName( )
				+ "/TestCases/output/";

		File inputFile = new File( inputFolder );
		if ( !inputFile.isDirectory( ) || !inputFile.exists( ) )
		{
			throw new Exception( "Input foler: " + inputFolder
					+ " doesn't exist." );
		}

		File[] reports = inputFile.listFiles( new FilenameFilter( ) {

			public boolean accept( File dir, String name )
			{
				if ( name.endsWith( ".xml" ) ) //$NON-NLS-1$
					return true;
				return false;
			}
		} );

		for ( int i = 0; i < reports.length; i++ )
		{
			File report = reports[i];
			String html = report.getName( ).replaceAll( ".xml", ".html" );

			try
			{
				List engineErrors = runAndRender( inputFolder
						+ report.getName( ), outputFolder + html );
				compareHTML( html, html );

				if ( engineErrors != null && engineErrors.size( ) > 0 )
				{
					StringBuffer sb = new StringBuffer( );
					for ( Iterator iter = engineErrors.iterator( ); iter
							.hasNext( ); )
					{
						sb.append( iter.next( ).toString( ) );
						sb.append( "\n" ); //$NON-NLS-1$
					}

					engineInternalErrors
							.put( report.getName( ), sb.toString( ) );
				}

				// success

				testStatus.put( report.getName( ), null );
			}
			catch ( Exception e )
			{
				testStatus.put( report.getName( ), e.toString( ) );
			}
		}

		// reporting:

		DomWriter domwriter = new DomWriter( );
		domwriter.setOutput( new FileWriter( this.getBasePath( )
				+ "TESTS-SmokeTests.xml" ) ); //$NON-NLS-1$
		domwriter.setCanonical( true );

		// reporting.
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory
				.newInstance( );
		DocumentBuilder builder = builderFactory.newDocumentBuilder( );
		Document doc = builder.newDocument( );

		Element testsuite = doc.createElement( "testsuite" ); //$NON-NLS-1$
		testsuite.setAttribute( "name", getName( ) ); //$NON-NLS-1$

		int failuresCount = 0;

		Iterator iter = this.testStatus.entrySet( ).iterator( );
		while ( iter.hasNext( ) )
		{
			Map.Entry entry = (Map.Entry) iter.next( );
			String testCaseName = (String) entry.getKey( );
			Object status = entry.getValue( );

			Element testcase = doc.createElement( "testcase" ); //$NON-NLS-1$
			testcase.setAttribute( "name", testCaseName ); //$NON-NLS-1$
			if ( null == status )
			{
				// no error;

				testcase.setAttribute( "errors", null ); //$NON-NLS-1$
			}
			else
			{
				testcase.setAttribute( "errors", status.toString( ) ); //$NON-NLS-1$
				++failuresCount;
			}

			String internalError = (String) engineInternalErrors
					.get( testCaseName );
			if ( null == internalError )
			{
				// no internal error;

				testcase.setAttribute( "internalErrors", null ); //$NON-NLS-1$

			}
			else
			{
				testcase.setAttribute( "internalErrors", internalError );
				//$NON-NLS-1$
			}

			testsuite.appendChild( testcase );
		}

		testsuite.setAttribute( "failures", String.valueOf( failuresCount ) ); //$NON-NLS-1$
		testsuite.setAttribute( "tests", String.valueOf( testStatus
		//$NON-NLS-1$
				.keySet( ).size( ) ) );

		domwriter.write( testsuite );
	}

	/**
	 * Returns base path of the plugin test project.
	 */

	protected String getBasePath( )
	{
		return new File( this.getClass( ).getProtectionDomain( )
				.getCodeSource( ).getLocation( ).getPath( ) ).getParent( )
				+ "/"; //$NON-NLS-1$
	}

	private List runAndRender( String inputFile, String outputFile )
			throws EngineException
	{
		IReportRunnable runnable = engine.openReportDesign( inputFile );
		IRunAndRenderTask task = engine.createRunAndRenderTask( runnable );

		task.setLocale( Locale.ENGLISH );

		IRenderOption options = new HTMLRenderOption( );
		options.setOutputFileName( outputFile );
		HTMLRenderContext renderContext = new HTMLRenderContext( );
		renderContext.setImageDirectory( "image" ); //$NON-NLS-1$
		HashMap appContext = new HashMap( );
		appContext.put( EngineConstants.APPCONTEXT_HTML_RENDER_CONTEXT,
				renderContext );
		task.setAppContext( appContext );

		options.setOutputFormat( "html" ); //$NON-NLS-1$
		options.getOutputSetting( )
				.put( HTMLRenderOption.URL_ENCODING, "UTF-8" ); //$NON-NLS-1$

		task.setRenderOption( options );
		task.run( );
		List errors = task.getErrors( );
		task.close( );

		return errors;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see utility.EngineCase#compareHTML(java.lang.String, java.lang.String)
	 */

	protected boolean compareHTML( String golden, String output )
			throws Exception
	{
		FileReader readerA = null;
		FileReader readerB = null;
		boolean same = true;
		StringBuffer errorText = new StringBuffer( );

		try
		{
			golden = this.getFullQualifiedClassName( ) + "/TestCases/golden/" + golden; //$NON-NLS-1$
//			output = getClassFolder( ) + "/TestCases/output/" + output; //$NON-NLS-1$
//			output = this.genOutputFile( output );
			String tempDir = System.getProperty( "java.io.tmpdir" ); //$NON-NLS-1$
			output = tempDir + getFullQualifiedClassName( ) //$NON-NLS-1$
					+ "/TestCases/output/" + output;

			readerA = new FileReader( golden );
			readerB = new FileReader( output );

			same = compareTextFile( readerA, readerB, output );
		}
		catch ( IOException e )
		{
			errorText.append( e.toString( ) );
			errorText.append( "\n" ); //$NON-NLS-1$
			e.printStackTrace( );
		}
		finally
		{
			try
			{
				readerA.close( );
				readerB.close( );
			}
			catch ( Exception e )
			{
				readerA = null;
				readerB = null;

				errorText.append( e.toString( ) );
				throw new Exception( errorText.toString( ) );
			}
		}

		return same;
	}

}
