/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2017 by Hitachi Vantara : http://www.pentaho.com
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

package org.apache.hop.ui.hopgui.dialog;

import org.apache.hop.core.CheckResultInterface;
import org.apache.hop.core.ProgressMonitorAdapter;
import org.apache.hop.core.variables.VariableSpace;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metastore.api.IMetaStore;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.hopgui.HopGui;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Takes care of displaying a dialog that will handle the wait while checking a pipeline...
 *
 * @author Matt
 * @since 16-mrt-2005
 */
public class CheckPipelineProgressDialog {
  private static Class<?> PKG = CheckPipelineProgressDialog.class; // for i18n purposes, needed by Translator!!

  private Shell shell;
  private PipelineMeta pipelineMeta;
  private List<CheckResultInterface> remarks;
  private boolean onlySelected;

  private VariableSpace space;

  private IMetaStore metaStore;

  /**
   * Creates a new dialog that will handle the wait while checking a pipeline...
   */
  public CheckPipelineProgressDialog( Shell shell, PipelineMeta pipelineMeta, List<CheckResultInterface> remarks,
                                      boolean onlySelected ) {
    this( shell, pipelineMeta, remarks, onlySelected, pipelineMeta, HopGui.getInstance().getMetaStore() );
  }

  /**
   * Creates a new dialog that will handle the wait while checking a pipeline...
   */
  public CheckPipelineProgressDialog( Shell shell, PipelineMeta pipelineMeta, List<CheckResultInterface> remarks,
                                      boolean onlySelected, VariableSpace space, IMetaStore metaStore ) {
    this.shell = shell;
    this.pipelineMeta = pipelineMeta;
    this.onlySelected = onlySelected;
    this.remarks = remarks;
    this.space = space;
    this.metaStore = metaStore;
  }

  public void open() {
    final ProgressMonitorDialog pmd = new ProgressMonitorDialog( shell );

    IRunnableWithProgress op = new IRunnableWithProgress() {
      public void run( IProgressMonitor monitor ) throws InvocationTargetException, InterruptedException {
        try {
          pipelineMeta.checkTransforms(
            remarks, onlySelected, new ProgressMonitorAdapter( monitor ), space, metaStore );
        } catch ( Exception e ) {
          throw new InvocationTargetException( e, BaseMessages.getString(
            PKG, "AnalyseImpactProgressDialog.RuntimeError.ErrorCheckingPipeline.Exception", e.toString() ) );
        }
      }
    };

    try {
      // Run something in the background to cancel active database queries, force this if needed!
      Runnable run = new Runnable() {
        public void run() {
          IProgressMonitor monitor = pmd.getProgressMonitor();
          while ( pmd.getShell() == null || ( !pmd.getShell().isDisposed() && !monitor.isCanceled() ) ) {
            try {
              Thread.sleep( 250 );
            } catch ( InterruptedException e ) {
              // Ignore sleep interruption exception
            }
          }

          if ( monitor.isCanceled() ) { // Disconnect and see what happens!

            try {
              pipelineMeta.cancelQueries();
            } catch ( Exception e ) {
              // Ignore cancel errors
            }
          }
        }
      };
      // Dump the cancel looker in the background!
      new Thread( run ).start();

      pmd.run( true, true, op );
    } catch ( InvocationTargetException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "CheckPipelineProgressDialog.Dialog.ErrorCheckingPipeline.Title" ),
        BaseMessages.getString(
          PKG, "CheckPipelineProgressDialog.Dialog.ErrorCheckingPipeline.Message" ), e );
    } catch ( InterruptedException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "CheckPipelineProgressDialog.Dialog.ErrorCheckingPipeline.Title" ),
        BaseMessages.getString( PKG, "CheckPipelineProgressDialog.Dialog.ErrorCheckingPipeline.Message" ), e );
    }
  }
}