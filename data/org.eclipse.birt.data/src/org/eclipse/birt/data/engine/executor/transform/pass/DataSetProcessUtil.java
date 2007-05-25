/*******************************************************************************
 * Copyright (c) 2004, 2005 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.data.engine.executor.transform.pass;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.data.engine.aggregation.AggregationFactory;
import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.aggregation.IAggregation;
import org.eclipse.birt.data.engine.api.querydefn.ComputedColumn;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.aggregation.AggrDefnManager;
import org.eclipse.birt.data.engine.executor.aggregation.AggrInfo;
import org.eclipse.birt.data.engine.executor.aggregation.AggregationHelper;
import org.eclipse.birt.data.engine.executor.transform.OdiResultSetWrapper;
import org.eclipse.birt.data.engine.executor.transform.ResultSetPopulator;
import org.eclipse.birt.data.engine.executor.transform.TransformationConstants;
import org.eclipse.birt.data.engine.expression.ExpressionCompiler;
import org.eclipse.birt.data.engine.impl.ComputedColumnHelper;
import org.eclipse.birt.data.engine.impl.DataEngineSession;
import org.eclipse.birt.data.engine.impl.FilterByRow;
import org.eclipse.birt.data.engine.odi.IAggrInfo;
import org.mozilla.javascript.Context;

/**
 * The class used to populate DataSet data.
 * 
 */
class DataSetProcessUtil extends RowProcessUtil
{
	/**
	 * 
	 * @param populator
	 * @param iccState
	 * @param computedColumnHelper
	 * @param filterByRow
	 * @param psController
	 */
	private DataSetProcessUtil( ResultSetPopulator populator,
			ComputedColumnsState iccState,
			ComputedColumnHelper computedColumnHelper,
			FilterByRow filterByRow,
			PassStatusController psController,
			DataEngineSession session)
	{
		super( populator,
				iccState,
				computedColumnHelper,
				filterByRow,
				psController, session);
	}
	
	/**
	 * Populate the data set data of an IResultIterator instance.
	 * 
	 * @param populator
	 * @param iccState
	 * @param computedColumnHelper
	 * @param filterByRow
	 * @param psController
	 * @throws DataException
	 */
	public static void doPopulate( ResultSetPopulator populator, ComputedColumnsState iccState,
			ComputedColumnHelper computedColumnHelper, FilterByRow filterByRow,
			PassStatusController psController, DataEngineSession session ) throws DataException
	{
		DataSetProcessUtil instance = new DataSetProcessUtil( populator,
				iccState,
				computedColumnHelper,
				filterByRow,
				psController,
				session );
		instance.populateDataSet( );
	}
	
	/**
	 * 
	 * @throws DataException
	 */
	private void populateDataSet() throws DataException
	{
		int originalMaxRows = this.populator.getQuery( ).getMaxRows( );
		
		boolean changeMaxRows = filterByRow == null?false:filterByRow.getFilterList( FilterByRow.QUERY_FILTER )
			.size( )
			+ filterByRow.getFilterList( FilterByRow.GROUP_FILTER ).size( ) > 0;
		if ( changeMaxRows )
			this.populator.getQuery( ).setMaxRows( 0 );
			
		List aggCCList = prepareComputedColumns(TransformationConstants.DATA_SET_MODEL );

		doDataSetFilter( changeMaxRows );

		populateAggrCCs( this.getAggrComputedColumns( aggCCList, true ));
		
		//Begin populate computed columns with aggregations.
		//TODO:remove me
		populateComputedColumns( this.getAggrComputedColumns( aggCCList, false ));
		
		
		
		this.populator.getQuery( ).setMaxRows( originalMaxRows );
	}
	
	/**
	 * 
	 * @param aggrComputedColumns
	 * @throws DataException
	 */
	private void populateAggrCCs( List aggrComputedColumns )
			throws DataException
	{
		if ( aggrComputedColumns.size( ) == 0 )
			return;
		ExpressionCompiler compiler = new ExpressionCompiler( );
		compiler.setDataSetMode( true );
		try
		{

			Context cx = Context.enter( );

			List aggrInfos = new ArrayList( );
			List aggrNames = new ArrayList( );
			for ( int i = 0; i < aggrComputedColumns.size( ); i++ )
			{
				ComputedColumn cc = (ComputedColumn) aggrComputedColumns.get( i );
				List args = cc.getAggregateArgument( );

				IBaseExpression[] exprs = null;
				int offset = 0;
				if ( cc.getExpression( ) != null )
				{
					exprs = new IBaseExpression[args.size( ) + 1];
					offset = 1;
					exprs[0] = cc.getExpression( );
				}
				else
					exprs = new IBaseExpression[args.size( )];

				for ( int j = offset; j < args.size( ); j++ )
				{
					exprs[j] = (IBaseExpression) args.get( j - offset );
				}

				for ( int j = 0; j < exprs.length; j++ )
				{
					compiler.compile( exprs[j], cx );
				}

				IAggregation aggrFunction = AggregationFactory.getInstance( )
						.getAggregation( cc.getAggregateFunction( ) );
				IAggrInfo aggrInfo = new AggrInfo( cc.getName( ),
						0,
						aggrFunction,
						exprs,
						cc.getAggregateFilter( ) );
				aggrInfos.add( aggrInfo );
				aggrNames.add( cc.getName( ) );
			}

			// All the computed column aggregations should only have one round.

			if ( !psController.needDoOperation( PassStatusController.DATA_SET_FILTERING ) )
				PassUtil.pass( populator,
						new OdiResultSetWrapper( populator.getResultIterator( ) ),
						false,
						this.session );

			AggregationHelper helper = new AggregationHelper( new AggrDefnManager( aggrInfos ),
					this.populator );

			AggrComputedColumnHelper ccHelper = new AggrComputedColumnHelper( helper,
					aggrNames );
			this.populator.getQuery( ).getFetchEvents( ).add( 0, ccHelper );

			PassUtil.pass( populator,
					new OdiResultSetWrapper( populator.getResultIterator( ) ),
					false,
					this.session );

			this.populator.getQuery( ).getFetchEvents( ).remove( 0 );
		}
		finally
		{
			Context.exit( );
		}
	}

	/**
	 * 
	 * @throws DataException
	 */
	private void doDataSetFilter( boolean changeMaxRows ) throws DataException
	{
		if(	!psController.needDoOperation( PassStatusController.DATA_SET_FILTERING ))
			return;
				
		

		applyFilters( FilterByRow.DATASET_FILTER,
				changeMaxRows );
	}

	/**
	 * 
	 * @return
	 * @throws DataException
	 */
	private void populateComputedColumns( List aggCCList ) throws DataException
	{
		if ( !psController.needDoOperation( PassStatusController.DATA_SET_COMPUTED_COLUMN_POPULATING ) )
			return;
		// if no group pass has been made, made one.
		if ( !psController.needDoOperation( PassStatusController.DATA_SET_FILTERING ) )
		{
			if ( iccState != null )
			{
				for ( int i = 0; i < iccState.getCount( ); i++ )
				{
					if ( iccState.isValueAvailable( i ) )
					{
						for ( int k = 0; k < this.populator.getQuery( )
								.getFetchEvents( )
								.size( ); k++ )
						{
							if ( this.populator.getQuery( )
									.getFetchEvents( )
									.get( k ) instanceof ComputedColumnHelper )
							{
								ComputedColumnHelper helper = (ComputedColumnHelper) this.populator.getQuery( )
										.getFetchEvents( )
										.get( k );
								helper.getComputedColumnList( )
										.remove( iccState.getComputedColumn( i ) );
								break;
							}
						}
					}
				}
			}
			PassUtil.pass( this.populator,
					new OdiResultSetWrapper( populator.getResultIterator( ) ),
					false,
					this.session );
		}
		computedColumnHelper.getComputedColumnList( ).clear( );
		computedColumnHelper.getComputedColumnList( ).addAll( aggCCList );
		computedColumnHelper.setModel( TransformationConstants.DATA_SET_MODEL );
		iccState.setModel( TransformationConstants.DATA_SET_MODEL );
		// If there are computed columns cached in iccState, then begin
		// multipass.
		if ( iccState.getCount( ) > 0 )
		{
			ComputedColumnCalculator.populateComputedColumns( this.populator,
					new OdiResultSetWrapper( this.populator.getResultIterator( ) ),
					iccState,
					computedColumnHelper, this.session );
		}
		computedColumnHelper.setModel( TransformationConstants.NONE_MODEL );
	}
}
