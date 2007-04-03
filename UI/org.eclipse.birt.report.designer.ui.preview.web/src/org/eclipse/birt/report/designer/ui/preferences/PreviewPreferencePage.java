/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.designer.ui.preferences;

import com.ibm.icu.util.ULocale;

import org.eclipse.birt.report.designer.internal.ui.util.IHelpContextIds;
import org.eclipse.birt.report.designer.internal.ui.util.UIUtil;
import org.eclipse.birt.report.designer.nls.Messages;
import org.eclipse.birt.report.viewer.ViewerPlugin;
import org.eclipse.birt.report.viewer.browsers.BrowserDescriptor;
import org.eclipse.birt.report.viewer.browsers.BrowserManager;
import org.eclipse.birt.report.viewer.browsers.custom.CustomBrowser;
import org.eclipse.birt.report.viewer.utilities.WebViewer;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage </samp>,
 * we can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */
public class PreviewPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage
{

	private Button alwaysExternal;
	private Button svgFlag;
	private Button masterPageContent;
	private Button[] externalBrowsers;
	private Button customBrowserRadio;
	private Label customBrowserPathLabel;
	private Text customBrowserPath;
	private Button customBrowserBrowse;
	private Combo localeCombo;

	private IntegerFieldEditor maxRowEditor;
	/** default value of max number */
	public static final int DEFAULT_MAX_ROW = 500;
	private static final int MAX_MAX_ROW = Integer.MAX_VALUE;

	/** max Row preference name */
	public static final String PREVIEW_MAXROW = "preview_maxrow"; //$NON-NLS-1$

	/**
	 * Creates preference page controls on demand.
	 * 
	 * @param parent
	 *            the parent for the preference page
	 */
	protected Control createContents( Composite parent )
	{
		UIUtil.bindHelp( parent, IHelpContextIds.PREFERENCE_BIRT_PREVIEW_ID );
		Composite mainComposite = new Composite( parent, SWT.NULL );
		GridData data = new GridData( );
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		mainComposite.setLayoutData( data );
		GridLayout layout = new GridLayout( );
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		mainComposite.setLayout( layout );

		// Description
		Label description = new Label( mainComposite, SWT.NULL );
		description.setText( Messages.getString( "designer.preview.preference.browser.description" ) ); //$NON-NLS-1$

		createSpacer( mainComposite );

		Label localeDescription = new Label( mainComposite, SWT.NULL );
		localeDescription.setText( Messages.getString( "designer.preview.preference.locale.description" ) ); //$NON-NLS-1$

		localeCombo = new Combo( mainComposite, SWT.DROP_DOWN );
		localeCombo.setLayoutData( new GridData( GridData.GRAB_HORIZONTAL ) );
		assert WebViewer.LocaleTable != null;
		String[] localeDisplayNames = new String[WebViewer.LocaleTable.size( )];
		WebViewer.LocaleTable.keySet( ).toArray( localeDisplayNames );
		localeCombo.setItems( localeDisplayNames );
		String defaultLocale = ViewerPlugin.getDefault( )
				.getPluginPreferences( )
				.getString( WebViewer.USER_LOCALE );
		if ( defaultLocale == null || defaultLocale.trim( ).length( ) <= 0 )
		{
			assert ULocale.getDefault( ) != null;
			defaultLocale = ULocale.getDefault( ).getDisplayName( );
		}
		localeCombo.setText( defaultLocale );

		createSpacer( mainComposite );

		// Enable svg or not.
		svgFlag = new Button( mainComposite, SWT.CHECK );
		svgFlag.setLayoutData( new GridData( GridData.GRAB_HORIZONTAL ) );
		svgFlag.setText( Messages.getString( "designer.preview.preference.browser.svg" ) ); //$NON-NLS-1$
		svgFlag.setSelection( ViewerPlugin.getDefault( )
				.getPluginPreferences( )
				.getBoolean( WebViewer.SVG_FLAG ) );

		// Show mastet page or not.
		masterPageContent = new Button( mainComposite, SWT.CHECK );
		masterPageContent.setLayoutData( new GridData( GridData.GRAB_HORIZONTAL ) );
		masterPageContent.setText( Messages.getString( "designer.preview.preference.masterpagecontent" ) ); //$NON-NLS-1$
		masterPageContent.setSelection( ViewerPlugin.getDefault( )
				.getPluginPreferences( )
				.getBoolean( WebViewer.MASTER_PAGE_CONTENT ) );

		// Always use external browsers
		if ( BrowserManager.getInstance( ).isEmbeddedBrowserPresent( ) )
		{
			alwaysExternal = new Button( mainComposite, SWT.CHECK );
			alwaysExternal.setLayoutData( new GridData( GridData.GRAB_HORIZONTAL ) );
			alwaysExternal.setText( Messages.getString( "designer.preview.preference.browser.useExternal" ) ); //$NON-NLS-1$
			alwaysExternal.setSelection( ViewerPlugin.getDefault( )
					.getPluginPreferences( )
					.getBoolean( BrowserManager.ALWAYS_EXTERNAL_BROWSER_KEY ) );

			createSpacer( mainComposite );
		}

		// Current external browser adapters
		Label tableDescription = new Label( mainComposite, SWT.NULL );
		tableDescription.setText( Messages.getString( "designer.preview.preference.browser.currentBrowsers" ) ); //$NON-NLS-1$

		// Grid for browser adapters
		Color bgColor = parent.getDisplay( )
				.getSystemColor( SWT.COLOR_LIST_BACKGROUND );
		Color fgColor = parent.getDisplay( )
				.getSystemColor( SWT.COLOR_LIST_FOREGROUND );
		final ScrolledComposite externalBrowsersScrollable = new ScrolledComposite( mainComposite,
				SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL );
		GridData gd = new GridData( GridData.FILL_BOTH );
		gd.heightHint = convertHeightInCharsToPixels( 2 );
		externalBrowsersScrollable.setLayoutData( gd );
		externalBrowsersScrollable.setBackground( bgColor );
		externalBrowsersScrollable.setForeground( fgColor );

		Composite externalBrowsersComposite = new Composite( externalBrowsersScrollable,
				SWT.NONE );
		externalBrowsersScrollable.setContent( externalBrowsersComposite );
		GridLayout layout2 = new GridLayout( );
		externalBrowsersComposite.setLayout( layout2 );
		externalBrowsersComposite.setBackground( bgColor );
		externalBrowsersComposite.setForeground( fgColor );

		// List of browser adapters
		BrowserDescriptor[] descriptors = BrowserManager.getInstance( )
				.getBrowserDescriptors( );
		externalBrowsers = new Button[descriptors.length];

		for ( int i = 0; i < descriptors.length; i++ )
		{
			Button radio = new Button( externalBrowsersComposite, SWT.RADIO );
			org.eclipse.jface.dialogs.Dialog.applyDialogFont( radio );
			radio.setBackground( bgColor );
			radio.setForeground( fgColor );
			radio.setText( descriptors[i].getLabel( ) );

			if ( BrowserManager.getInstance( )
					.getCurrentBrowserID( )
					.equals( descriptors[i].getID( ) ) )
			{
				radio.setSelection( true );
			}
			else
			{
				radio.setSelection( false );
			}

			radio.setData( descriptors[i] );
			externalBrowsers[i] = radio;

			if ( BrowserManager.BROWSER_ID_CUSTOM.equals( descriptors[i].getID( ) ) )
			{
				customBrowserRadio = radio;
				radio.addSelectionListener( new SelectionListener( ) {

					public void widgetSelected( SelectionEvent selEvent )
					{
						setCustomBrowserPathEnabled( );
					}

					public void widgetDefaultSelected( SelectionEvent selEvent )
					{
						widgetSelected( selEvent );
					}
				} );
			}
		}

		externalBrowsersComposite.setSize( externalBrowsersComposite.computeSize( SWT.DEFAULT,
				SWT.DEFAULT ) );

		// Custom browser
		createCustomBrowserPathPart( mainComposite );
		org.eclipse.jface.dialogs.Dialog.applyDialogFont( mainComposite );

		createSpacer( mainComposite );

		createIntFieldEditor( mainComposite );

		return mainComposite;
	}

	/**
	 * Create UI section for custom browser.
	 * 
	 * @param mainComposite
	 */
	private void createCustomBrowserPathPart( Composite mainComposite )
	{
		Font font = mainComposite.getFont( );
		// vertical space
		new Label( mainComposite, SWT.NULL );

		Composite bPathComposite = new Composite( mainComposite, SWT.NULL );
		GridLayout layout = new GridLayout( );
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 3;
		bPathComposite.setLayout( layout );
		bPathComposite.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

		customBrowserPathLabel = new Label( bPathComposite, SWT.LEFT );
		customBrowserPathLabel.setFont( font );
		customBrowserPathLabel.setText( Messages.getString( "designer.preview.preference.browser.program" ) ); //$NON-NLS-1$

		// Browser path text
		customBrowserPath = new Text( bPathComposite, SWT.BORDER );
		customBrowserPath.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
		customBrowserPath.setFont( font );
		customBrowserPath.setText( ViewerPlugin.getDefault( )
				.getPluginPreferences( )
				.getString( CustomBrowser.CUSTOM_BROWSER_PATH_KEY ) );
		GridData data = new GridData( GridData.FILL_HORIZONTAL );
		data.horizontalAlignment = GridData.FILL;
		data.widthHint = convertWidthInCharsToPixels( 10 );
		customBrowserPath.setLayoutData( data );

		// Custom browser button
		customBrowserBrowse = new Button( bPathComposite, SWT.NONE );
		customBrowserBrowse.setFont( font );
		customBrowserBrowse.setText( Messages.getString( "designer.preview.preference.browser.browse" ) ); //$NON-NLS-1$
		data = new GridData( );
		data.horizontalAlignment = GridData.FILL;
		data.heightHint = convertVerticalDLUsToPixels( IDialogConstants.BUTTON_HEIGHT );
		int widthHint = convertHorizontalDLUsToPixels( IDialogConstants.BUTTON_WIDTH );
		data.widthHint = Math.max( widthHint,
				customBrowserBrowse.computeSize( SWT.DEFAULT, SWT.DEFAULT, true ).x );
		customBrowserBrowse.setLayoutData( data );
		customBrowserBrowse.addSelectionListener( new SelectionListener( ) {

			public void widgetDefaultSelected( SelectionEvent event )
			{
			}

			public void widgetSelected( SelectionEvent event )
			{
				FileDialog d = new FileDialog( getShell( ) );

				d.setText( Messages.getString( "designer.preview.preference.browser.details" ) ); //$NON-NLS-1$

				String file = d.open( );

				if ( file != null )
				{
					customBrowserPath.setText( "\"" + file + "\" %1" ); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		} );

		// Toggle custom browser enabled or not
		setCustomBrowserPathEnabled( );
	}

	/**
	 * Create the maximum number of rows to be previewed in
	 * ResultSetPreviewPage.
	 * 
	 * @param mainComposite
	 */
	private void createIntFieldEditor( Composite mainComposite )
	{
		Composite intFieldEditorComposite = new Composite( mainComposite,
				SWT.NULL );
		maxRowEditor = new IntegerFieldEditor( PREVIEW_MAXROW,
				"",
				intFieldEditorComposite );
		GridLayout layout3 = new GridLayout( );
		layout3.marginHeight = 0;
		layout3.marginWidth = 0;
		layout3.numColumns = 2;
		intFieldEditorComposite.setLayout( layout3 );
		intFieldEditorComposite.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

		Label lab2 = maxRowEditor.getLabelControl( intFieldEditorComposite );
		lab2.setText( Messages.getString( "designer.preview.preference.resultset.maxrow.description" ) );

		// maxRowEditor.setPage(this);
		maxRowEditor.setPage( this );
		maxRowEditor.setTextLimit( Integer.toString( MAX_MAX_ROW ).length( ) );
		maxRowEditor.setErrorMessage( Messages.getFormattedString( "designer.preview.preference.resultset.maxrow.errormessage",
				new Object[]{
					new Integer( MAX_MAX_ROW )
				} ) );
		maxRowEditor.setValidateStrategy( StringFieldEditor.VALIDATE_ON_KEY_STROKE );
		maxRowEditor.setValidRange( 1, MAX_MAX_ROW );
		maxRowEditor.setPropertyChangeListener( new IPropertyChangeListener( ) {

			public void propertyChange( PropertyChangeEvent event )
			{
				if ( event.getProperty( ).equals( FieldEditor.IS_VALID ) )
					setValid( maxRowEditor.isValid( ) );
			}
		} );

		String defaultMaxRow = ViewerPlugin.getDefault( )
				.getPluginPreferences( )
				.getString( PREVIEW_MAXROW );

		if ( defaultMaxRow == null || defaultMaxRow.trim( ).length( ) <= 0 )
		{
			defaultMaxRow = String.valueOf( DEFAULT_MAX_ROW );
		}
		maxRowEditor.setStringValue( defaultMaxRow );
	}

	/**
	 * Performs special processing when this page's Defaults button has been
	 * pressed.
	 * <p>
	 * This is a framework hook method for sublcasses to do special things when
	 * the Defaults button has been pressed. Subclasses may override, but should
	 * call <code>super.performDefaults</code>.
	 * </p>
	 */
	protected void performDefaults( )
	{
		String defaultBrowserID = BrowserManager.getInstance( )
				.getDefaultBrowserID( );

		for ( int i = 0; i < externalBrowsers.length; i++ )
		{
			BrowserDescriptor descriptor = (BrowserDescriptor) externalBrowsers[i].getData( );
			externalBrowsers[i].setSelection( descriptor.getID( ) == defaultBrowserID );
		}

		customBrowserPath.setText( ViewerPlugin.getDefault( )
				.getPluginPreferences( )
				.getDefaultString( CustomBrowser.CUSTOM_BROWSER_PATH_KEY ) );
		setCustomBrowserPathEnabled( );

		if ( svgFlag != null )
		{
			svgFlag.setSelection( ViewerPlugin.getDefault( )
					.getPluginPreferences( )
					.getDefaultBoolean( WebViewer.SVG_FLAG ) );
		}

		if ( masterPageContent != null )
		{
			masterPageContent.setSelection( ViewerPlugin.getDefault( )
					.getPluginPreferences( )
					.getDefaultBoolean( WebViewer.MASTER_PAGE_CONTENT ) );
		}

		if ( alwaysExternal != null )
		{
			alwaysExternal.setSelection( ViewerPlugin.getDefault( )
					.getPluginPreferences( )
					.getDefaultBoolean( BrowserManager.ALWAYS_EXTERNAL_BROWSER_KEY ) );
		}

		if ( localeCombo != null )
		{
			ULocale defaultLocale = ULocale.getDefault( );
			assert defaultLocale != null;
			localeCombo.setText( defaultLocale.getDisplayName( ) );
		}

		// performDefaults of max row preference
		maxRowEditor.setStringValue( String.valueOf( DEFAULT_MAX_ROW ) );

		super.performDefaults( );
	}

	/**
	 * @see IPreferencePage
	 */
	public boolean performOk( )
	{
		Preferences pref = ViewerPlugin.getDefault( ).getPluginPreferences( );

		for ( int i = 0; i < externalBrowsers.length; i++ )
		{
			if ( externalBrowsers[i].getSelection( ) )
			{
				// set new current browser
				String browserID = ( (BrowserDescriptor) externalBrowsers[i].getData( ) ).getID( );
				BrowserManager.getInstance( ).setCurrentBrowserID( browserID );
				// save id in help preferences
				pref.setValue( BrowserManager.DEFAULT_BROWSER_ID_KEY, browserID );
				break;
			}
		}

		customBrowserPath.getText( );
		pref.setValue( CustomBrowser.CUSTOM_BROWSER_PATH_KEY,
				customBrowserPath.getText( ) );

		if ( svgFlag != null )
		{
			pref.setValue( WebViewer.SVG_FLAG, svgFlag.getSelection( ) );
		}

		if ( masterPageContent != null )
		{
			pref.setValue( WebViewer.MASTER_PAGE_CONTENT,
					masterPageContent.getSelection( ) );
		}

		if ( alwaysExternal != null )
		{
			pref.setValue( BrowserManager.ALWAYS_EXTERNAL_BROWSER_KEY,
					alwaysExternal.getSelection( ) );
			BrowserManager.getInstance( )
					.setAlwaysUseExternal( alwaysExternal.getSelection( ) );
		}

		if ( localeCombo != null )
		{
			pref.setValue( WebViewer.USER_LOCALE, localeCombo.getText( ) );
		}

		ViewerPlugin.getDefault( ).savePluginPreferences( );

		// performOK of max row preference
		ViewerPlugin.getDefault( )
				.getPluginPreferences( )
				.setValue( PREVIEW_MAXROW, maxRowEditor.getStringValue( ) );

		return true;
	}

	/**
	 * Toggle custom browser enabled or not
	 */
	private void setCustomBrowserPathEnabled( )
	{
		boolean enabled = customBrowserRadio.getSelection( );
		customBrowserPathLabel.setEnabled( enabled );
		customBrowserPath.setEnabled( enabled );
		customBrowserBrowse.setEnabled( enabled );
	}

	/**
	 * Creates a horizontal spacer line that fills the width of its container.
	 * 
	 * @param parent
	 *            the parent control
	 */
	private void createSpacer( Composite parent )
	{
		Label spacer = new Label( parent, SWT.NONE );
		GridData data = new GridData( );
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.BEGINNING;
		spacer.setLayoutData( data );
	}

	public void init( IWorkbench workbench )
	{
		;
	}
}