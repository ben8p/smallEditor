package smalleditor.editors.javascript;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.WorkbenchJob;

import smalleditor.Activator;
import smalleditor.editors.common.ACommonEditor;
import smalleditor.editors.common.ACommonReconcilingStrategy;
import smalleditor.editors.common.parsing.AFoldingPositionsBuilder;
import smalleditor.editors.common.parsing.ATaskPositionsBuilder;
import smalleditor.editors.javascript.parsing.JavascriptFoldingPositionsBuilder;
import smalleditor.editors.javascript.parsing.JavascriptTaskPositionsBuilder;
import smalleditor.linters.javascript.JavascriptLinterBuilder;
import smalleditor.preferences.IPreferenceNames;
import smalleditor.tokenizer.DocumentTokenBuilder;

public class JavascriptReconcilingStrategy extends ACommonReconcilingStrategy {
	protected JavascriptLinterBuilder linterBuilder;

	public JavascriptReconcilingStrategy(ACommonEditor editor) {
		super(editor);
		try {
			linterBuilder = new JavascriptLinterBuilder();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	@Override
	protected AFoldingPositionsBuilder getFoldingPositionsBuilder() {
		return new JavascriptFoldingPositionsBuilder(this.document);
	}
	@Override
	protected ATaskPositionsBuilder getTaskPositionsBuilder() {
		return new JavascriptTaskPositionsBuilder(this.document);
	}
	@Override
	protected DocumentTokenBuilder getDocumentTokenBuilder() {
		return null;
	}
	
	@Override
	protected void processReconcile() {
		super.processReconcile();
		
		Boolean useLinters = Activator.getDefault().getPreferenceStore().getBoolean(
				IPreferenceNames.P_LINT_CODE);
		if (useLinters == false) {
			return;
		}
		
		WorkbenchJob workbenchJob = new WorkbenchJob("Lint content") {//$NON-NLS-1$
			public IStatus runInUIThread(IProgressMonitor monitor) {
				lintContent();
				return Status.OK_STATUS;
			}
		};
		workbenchJob.setPriority(WorkbenchJob.DECORATE);
		workbenchJob.schedule();
//		Display.getDefault().asyncExec(new Runnable() {
//			public void run() {
//				lintContent();
//			}
//		});
	}

	private void lintContent() {
		try {
			//System.out.println("linContent after reconcile");
			if(!(this.editor.getEditorInput() instanceof FileEditorInput)) { return; }
			IFile file = ((FileEditorInput) this.editor.getEditorInput())
					.getFile();

			linterBuilder.check(this.document, file);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

}
