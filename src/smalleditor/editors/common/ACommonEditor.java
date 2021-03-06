package smalleditor.editors.common;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.manipulation.RemoveTrailingWhitespaceOperation;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.WorkbenchJob;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import smalleditor.Activator;
import smalleditor.common.HashedProjectionAnnotation;
import smalleditor.editors.common.actions.FoldingActionsGroup;
import smalleditor.editors.common.outline.ACommonOutlineElement;
import smalleditor.nls.Messages;
import smalleditor.preferences.IPreferenceNames;
import smalleditor.tokenizer.DocumentNodeType;
import smalleditor.tokenizer.NodePosition;
import smalleditor.utils.TextUtility;

public abstract class ACommonEditor extends TextEditor implements IEditorPart, ISelectionChangedListener {
	protected ProjectionSupport projectionSupport;
	protected ProjectionAnnotationModel annotationModel;
	protected ACommonOutlinePage outlinePage;
	
	private FoldingActionsGroup foldingActionsGroup;
	private Boolean initialFoldingDone = false;

	private HashMap<Integer, HashedProjectionAnnotation> oldMappedAnnotations = null;
	private HashedProjectionAnnotation[] oldAnnotations = null;
	protected boolean updatingContentDependentActions = false;
	
	private WorkbenchJob markOccurenceWorkbenchJob = null;
	
	private final String OCCURENCE_MARKER_TYPE = "slicemarker"; //$NON-NLS-1$

	public ACommonEditor() {
		super();

		// merge the preferences
		IPreferenceStore[] stores = { getPreferenceStore(),
				Activator.getDefault().getPreferenceStore() };
		setPreferenceStore(new ChainedPreferenceStore(stores));

	}

	@Override
	protected void createActions() {
		super.createActions();
		// Folding setup
		foldingActionsGroup = new FoldingActionsGroup(this);
	}
	
	@Override
	protected void rulerContextMenuAboutToShow(IMenuManager menu) {
		super.rulerContextMenuAboutToShow(menu);
		
		IMenuManager foldingMenu = new MenuManager(Messages.getString("Folding.GroupName"), "folding"); //$NON-NLS-1$
		menu.appendToGroup(ITextEditorActionConstants.GROUP_RULERS, foldingMenu);
		getFoldingActionsGroup().fillMenu(foldingMenu);
	}
	
	protected FoldingActionsGroup getFoldingActionsGroup() {
		return foldingActionsGroup;
	}
	
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();

		projectionSupport = new ProjectionSupport(viewer,
				getAnnotationAccess(), getSharedColors());
				
		projectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.error");
		projectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.warning");
		projectionSupport.addSummarizableAnnotationType("org.eclipse.core.resources.problemmarker");
		projectionSupport.addSummarizableAnnotationType("org.eclipse.core.resources.markers");
		projectionSupport.addSummarizableAnnotationType("smallEditor.lintermarkerproblem");
		projectionSupport.addSummarizableAnnotationType("smallEditor.lintermarker");
		
		projectionSupport.setHoverControlCreator(new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell shell) {
				return new DefaultInformationControl(shell, true);
			}
		});

		projectionSupport.install();
		
		
		// turn projection mode on
		viewer.doOperation(ProjectionViewer.TOGGLE);

		annotationModel = viewer.getProjectionAnnotationModel();
	}

	protected ISourceViewer createSourceViewer(Composite parent,
			IVerticalRuler ruler, int styles) {
		ISourceViewer viewer = new ProjectionViewer(parent, ruler,
				getOverviewRuler(), isOverviewRulerVisible(), styles);

		// ensure decoration support has been created and configured.
		getSourceViewerDecorationSupport(viewer);

		return viewer;
	}
	
	public Object getAdapter(Class key) {
		if (key.equals(IContentOutlinePage.class)) {
			IDocument document = getDocument();

			outlinePage = getOutlinePage(document);
			if(outlinePage != null) {
				outlinePage.addSelectionChangedListener(this);
				return outlinePage;
			}
		}

		return super.getAdapter(key);
	}
	
	protected abstract ACommonOutlinePage getOutlinePage(IDocument document);
	/**
	 * Updates all content dependent actions.
	 * 
	 * This might be a hack: We're trapping this update to ensure that the 
	 * outline is always up to date.
	 */
//	protected void updateContentDependentActions() {
//		super.updateContentDependentActions();
//
//		if (!updatingContentDependentActions && outlinePage != null) {
//			updatingContentDependentActions = true;
////			System.out.println("outlinePage.update");
//			outlinePage.update();
////			System.out.println("outlinePage.update DONE");
//			updatingContentDependentActions = false;
//		}
//		
//		
//	}
	public void updateOutline() {
		if(outlinePage != null) {
			outlinePage.setDocument(getDocument());
			outlinePage.update();
		}
	}
	
	@Override
	protected void configureSourceViewerDecorationSupport(
			SourceViewerDecorationSupport support) {
		super.configureSourceViewerDecorationSupport(support);

		char[] matchChars = getMatchingBrackets(); // which brackets to match
		if(matchChars != null) {
			ICharacterPairMatcher matcher = new DefaultCharacterPairMatcher(
					matchChars, IDocumentExtension3.DEFAULT_PARTITIONING);
			support.setCharacterPairMatcher(matcher);
			support.setMatchingCharacterPainterPreferenceKeys(
					IPreferenceNames.P_SHOW_MATCHING_BRACKETS,
					IPreferenceNames.P_MATCHING_BRACKETS_COLOR);
		}

	}
	

	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();

		markOccurrences();
	}

	protected char[] getMatchingBrackets() {
		// return the brckets to match
		char[] empty = {};
		return empty;
	}

	public void updateFoldingStructure(List<NodePosition> fPositions) {
		if(annotationModel == null) { return; }
		HashedProjectionAnnotation[] annotations = new HashedProjectionAnnotation[fPositions.size()];
		HashMap<Integer, HashedProjectionAnnotation> mappedAnnotations = new HashMap<Integer, HashedProjectionAnnotation>();

		// this will hold the new annotations along
		// with their corresponding positions
		HashMap<HashedProjectionAnnotation, Position> newAnnotations = new HashMap<HashedProjectionAnnotation, Position>();
		
		
		String startFolded = getPreferenceStore().getString(
				IPreferenceNames.P_INITIAL_FOLDING);
		
//		if(oldAnnotations != null) {
//			for(int i = 0; i < oldAnnotations.length ; i++) {
//				System.out.println(oldAnnotations[i].isCollapsed() + " -- " + oldAnnotations[i].getText());
//			}
//		}
		
		for (int i = 0; i < fPositions.size(); i++) {
			NodePosition position = fPositions.get(i);
			HashedProjectionAnnotation annotation = new HashedProjectionAnnotation();
			
			annotation.setHashCode(position.getHashCode());
			
			newAnnotations.put(annotation, position);
			annotations[i] = annotation;
			mappedAnnotations.put(position.getHashCode(), annotation);
			
//			if(position.getType() == DocumentNodeType.OpenFunction) {
//				System.out.println("function");
//			}
//			System.out.println(startFolded + "  --  " + IPreferenceNames.P_FOLDING_STATUS_FUNCTION);
//			try {
//				System.out.println("initial function fold:" + (startFolded.equals(IPreferenceNames.P_FOLDING_STATUS_FUNCTION)) + " level: " + position.getLevel() + " string: " + getDocument().get(position.offset, 1));
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
			if (initialFoldingDone == false) {
				if(startFolded.equals(IPreferenceNames.P_FOLDING_STATUS_ALL)) {
					annotation.markCollapsed();
				} else if (startFolded.equals(IPreferenceNames.P_FOLDING_STATUS_FUNCTION)
						&& position.getType() == DocumentNodeType.OpenFunction) {
					annotation.markCollapsed();
				} else if (TextUtility.isNumeric(startFolded) == true && position.getLevel() == Integer.parseInt(startFolded)) {
					annotation.markCollapsed();
				}
			} else {
				HashedProjectionAnnotation oldAnnotation = getMatchingAnnotation(position.getHashCode());
//				if (oldAnnotation != null) {
//					System.out.println(oldAnnotation.isCollapsed() + " -- " + oldAnnotation.getText());
//				}
				if (oldAnnotation != null && oldAnnotation.isCollapsed()) {
					annotation.markCollapsed();
				}
			}
		}

		initialFoldingDone = true;
		annotationModel.modifyAnnotations(oldAnnotations, newAnnotations, null);

		oldAnnotations = annotations;
		oldMappedAnnotations = mappedAnnotations;
	}
	
	private HashedProjectionAnnotation getMatchingAnnotation(int hasCode) {
		if(oldMappedAnnotations == null) { return null; }
		return oldMappedAnnotations.get(hasCode);
	}


	public void updateTask(List<NodePosition> positions) {
		IDocument document = getDocument();
		if(!(getEditorInput() instanceof FileEditorInput)) { return; }
		IFile file = ((FileEditorInput) this.getEditorInput()).getFile();

		IMarker marker;

		try {
			file.deleteMarkers(IMarker.TASK, true, IResource.DEPTH_INFINITE);

			for (int i = 0; i < positions.size(); i++) {
				Position position = positions.get(i);

				marker = file.createMarker(IMarker.TASK);
				marker.setAttribute(IMarker.LINE_NUMBER,
						document.getLineOfOffset(position.getOffset()) + 1);
				marker.setAttribute(IMarker.MESSAGE, document.get(
						position.getOffset(), position.getLength()));

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void doSave(IProgressMonitor progressMonitor) {
		Boolean removeTrailingSpaces = Activator.getDefault().getPreferenceStore().getBoolean(
				IPreferenceNames.P_TRAILING_SPACE);
		if (removeTrailingSpaces == true) {
			RemoveTrailingWhitespaceOperation removeSpacesOperation = new RemoveTrailingWhitespaceOperation();
			try {
				removeSpacesOperation.run(
						FileBuffers.getTextFileBufferManager().getTextFileBuffer(getDocument()),
						progressMonitor);
			} catch (Exception e) {
				e.printStackTrace();
//				System.out
//						.println("Error while removing the trailing whitespaces."); //$NON-NLS-1$
			}
		}
		
		super.doSave(progressMonitor);
	}

	private IDocument getDocument() {
		//return getSourceViewer().getDocument();
		return this.getDocumentProvider().getDocument(getEditorInput());
	}
	
	private void markOccurrences() {
		final IDocument document = getDocument();
		if(!(getEditorInput() instanceof FileEditorInput)) { return; }
		final IFile file = ((FileEditorInput) this.getEditorInput()).getFile();
		ISelection selection = getSelectionProvider().getSelection();
		

		// cleanup old annotations
		try {
			file.deleteMarkers(OCCURENCE_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		// if it is disabled, we get out
		Boolean markOccurences = getPreferenceStore().getBoolean(
				IPreferenceNames.P_MARK_OCCURENCES);
		if (!markOccurences || !(selection instanceof ITextSelection)) {
			return;
		}

		final ITextSelection textSelection = (ITextSelection) selection;
		// only react when cursor move, not when selected
		if (textSelection.getLength() > 0) {
			return;
		}
		if(markOccurenceWorkbenchJob != null) {
			markOccurenceWorkbenchJob.cancel();
		}
		
		markOccurenceWorkbenchJob = new WorkbenchJob(Messages.getString("Occurence.Job")) {//$NON-NLS-1$
			public IStatus runInUIThread(IProgressMonitor monitor) {
				try {
					int lineOffset = document.getLineOffset(textSelection
							.getStartLine());
					int caretOffset = textSelection.getOffset() - lineOffset;
					String line = document.get(lineOffset,
							document.getLineLength(textSelection.getStartLine()));
					String[] words = line.split("[^\\w]"); //$NON-NLS-1$
					int offset = 0;
					String word = null;
					List<Position> positions = new LinkedList<Position>();

					// get word under caret
					for (int i = 0; i < words.length; i++) {
						offset += words[i].length() + 1; // 1 for the splittted
															// char
						if (offset > caretOffset) {
							// System.out.println("word under carret is:" + words[i]);
							word = words[i];
							break;
						}
					}
					if (word != null && !word.isEmpty()) {
						// get all the positions of that word

						Pattern p = Pattern.compile("\\b" + word + "\\b"); //$NON-NLS-1$ //$NON-NLS-2$
						Matcher m = p.matcher(document.get());
						while (m.find()) {
							Position position = new Position(m.start());
							position.setLength(word.length());
							positions.add(position);
						}
					}
					if (positions.size() > 0) {
						// create markers
						IMarker marker;

						for (int index = 0; index < positions.size(); index++) {
							Position position = positions.get(index);
							marker = file.createMarker(OCCURENCE_MARKER_TYPE);
							marker.setAttribute(IMarker.CHAR_START,
									position.getOffset());
							marker.setAttribute(IMarker.CHAR_END, position.getOffset()
									+ position.getLength());

						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}
		};
		
		markOccurenceWorkbenchJob.setPriority(WorkbenchJob.DECORATE);
		markOccurenceWorkbenchJob.schedule(300);

		
	}

	/**
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if (null != event) {
			if (event.getSelection() instanceof IStructuredSelection) {
				IStructuredSelection sel = (IStructuredSelection) event.getSelection();
				if (null != sel) {
					ACommonOutlineElement fe = (ACommonOutlineElement) sel.getFirstElement();
					if (null != fe) {
						selectAndReveal(fe.getStart(), fe.getLength());
					}
				}
			}
		}
	}

}
