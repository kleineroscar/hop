/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2018 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.apache.hop.pipeline.transforms.databaselookup;

import org.apache.hop.core.HopEnvironment;
import org.apache.hop.core.QueueRowSet;
import org.apache.hop.core.RowSet;
import org.apache.hop.core.database.Database;
import org.apache.hop.core.database.DatabaseMeta;
import org.apache.hop.core.exception.HopDatabaseException;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.logging.LoggingObjectInterface;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.RowMetaInterface;
import org.apache.hop.core.row.ValueMetaInterface;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.core.variables.VariableSpace;
import org.apache.hop.junit.rules.RestoreHopEngineEnvironment;
import org.apache.hop.metastore.api.IMetaStore;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transforms.mock.TransformMockHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Tests for DatabaseLookup transform
 *
 * @author Pavel Sakun
 * @see DatabaseLookup
 */
public class PDI5436Test {
  private TransformMockHelper<DatabaseLookupMeta, DatabaseLookupData> smh;
  @ClassRule public static RestoreHopEngineEnvironment env = new RestoreHopEngineEnvironment();

  @BeforeClass
  public static void setupClass() throws HopException {
    HopEnvironment.init();
  }

  @AfterClass
  public static void tearDown() {
    HopEnvironment.reset();
  }

  @Before
  public void setUp() {
    smh =
      new TransformMockHelper<>( "Database Lookup", DatabaseLookupMeta.class,
        DatabaseLookupData.class );
    when( smh.logChannelInterfaceFactory.create( any(), any( LoggingObjectInterface.class ) ) ).thenReturn(
      smh.logChannelInterface );
    when( smh.pipeline.isRunning() ).thenReturn( true );
  }

  @After
  public void cleanUp() {
    smh.cleanUp();
  }

  private RowMeta mockInputRowMeta() {
    RowMeta inputRowMeta = new RowMeta();
    ValueMetaString nameMeta = new ValueMetaString( "name" );
    nameMeta.setStorageType( ValueMetaInterface.STORAGE_TYPE_BINARY_STRING );
    nameMeta.setStorageMetadata( new ValueMetaString( "name" ) );
    inputRowMeta.addValueMeta( nameMeta );
    ValueMetaString idMeta = new ValueMetaString( "id" );
    idMeta.setStorageType( ValueMetaInterface.STORAGE_TYPE_BINARY_STRING );
    idMeta.setStorageMetadata( new ValueMetaString( "id" ) );
    inputRowMeta.addValueMeta( idMeta );

    return inputRowMeta;
  }

  private RowSet mockInputRowSet() {
    RowSet inputRowSet = smh.getMockInputRowSet( new Object[][] { { "name".getBytes(), "1".getBytes() } } );
    inputRowSet.setRowMeta( mockInputRowMeta() );
    return inputRowSet;
  }

  private DatabaseLookupMeta mockTransformMeta() throws HopTransformException {
    DatabaseLookupMeta transformMeta = smh.initTransformMetaInterface;
    doReturn( mock( DatabaseMeta.class ) ).when( transformMeta ).getDatabaseMeta();
    doReturn( new String[] { "=" } ).when( transformMeta ).getKeyCondition();

    doCallRealMethod().when( transformMeta ).getFields( any( RowMetaInterface.class ), anyString(),
      any( RowMetaInterface[].class ), any( TransformMeta.class ), any( VariableSpace.class ),
      any( IMetaStore.class ) );
    doReturn( new String[] { "value" } ).when( transformMeta ).getReturnValueNewName();
    doReturn( new int[] { ValueMetaInterface.TYPE_STRING } ).when( transformMeta ).getReturnValueDefaultType();
    doReturn( true ).when( transformMeta ).isCached();
    doReturn( true ).when( transformMeta ).isLoadingAllDataInCache();
    doReturn( new String[] { "id" } ).when( transformMeta ).getStreamKeyField1();
    doReturn( new String[] { null } ).when( transformMeta ).getStreamKeyField2();
    doReturn( new String[] { "id" } ).when( transformMeta ).getTableKeyField();
    doReturn( new String[] { "value" } ).when( transformMeta ).getReturnValueField();
    doReturn( new String[] { "" } ).when( transformMeta ).getReturnValueDefault();
    doReturn( new int[] { ValueMetaInterface.TYPE_STRING } ).when( transformMeta ).getReturnValueDefaultType();
    when( transformMeta.getStreamKeyField2() ).thenReturn( new String[] { "a", "b", "c" } );

    return transformMeta;
  }

  private Database mockDatabase() throws HopDatabaseException {
    Database databaseMock = mock( Database.class );

    RowMeta databaseRowMeta = new RowMeta();
    databaseRowMeta.addValueMeta( new ValueMetaString( "id" ) );
    databaseRowMeta.addValueMeta( new ValueMetaString( "value" ) );
    doReturn( databaseRowMeta ).when( databaseMock ).getTableFields( anyString() );
    doReturn( databaseRowMeta ).when( databaseMock ).getTableFieldsMeta( anyString(), anyString() );
    doReturn( Arrays.asList( new Object[][] { { "1", "value" } } ) ).when( databaseMock ).getRows( anyString(),
      anyInt() );
    doReturn( databaseRowMeta ).when( databaseMock ).getReturnRowMeta();

    return databaseMock;
  }

  @Test
  public void testCacheAllTable() throws HopException {
    DatabaseLookup transformSpy = spy( new DatabaseLookup( smh.transformMeta, smh.transformDataInterface, 0, smh.pipelineMeta, smh.pipeline ) );

    Database database = mockDatabase();
    doReturn( database ).when( transformSpy ).getDatabase( any( DatabaseMeta.class ) );

    transformSpy.addRowSetToInputRowSets( mockInputRowSet() );
    transformSpy.setInputRowMeta( mockInputRowMeta() );
    RowSet outputRowSet = new QueueRowSet();
    transformSpy.addRowSetToOutputRowSets( outputRowSet );

    DatabaseLookupMeta meta = mockTransformMeta();
    DatabaseLookupData data = smh.initTransformDataInterface;

    Assert.assertTrue( "Transform init failed", transformSpy.init( meta, data ) );
    Assert.assertTrue( "Error processing row", transformSpy.processRow( meta, data ) );
    Assert.assertEquals( "Cache lookup failed", "value", outputRowSet.getRow()[ 2 ] );
  }
}