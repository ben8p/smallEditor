package smalleditor.linters.common.marker;

import org.eclipse.core.runtime.CoreException;

import smalleditor.linters.common.problem.IProblem;
import smalleditor.linters.common.problem.IProblemHandler;
import smalleditor.linters.common.text.Text;

public final class MarkerHandler implements IProblemHandler {

	private final MarkerAdapter markerAdapter;
	private final Text code;

	public MarkerHandler(MarkerAdapter markerAdapter, Text code) {
		this.markerAdapter = markerAdapter;
		this.code = code;
	}

	public void handleProblem(IProblem problem) {
		int line = problem.getLine();
		int character = problem.getCharacter();
		String message = problem.getMessage();
		String type = problem.getMessage().contains("Stopping.") 
				? "ERROR"
				: "WARNING";
		/*problem.getId().equals("(error)") 
				? "ERROR"
				: problem.getId().equals("(warning)") 
					? "WARNING"
		*/ 
					
		if (isValidLine(line)) {
			int start = code.getLineOffset(line - 1) + 1; //start at the 1st character of the line
			if (isValidCharacter(line, character)) {
				start += character - 1;
			}
			createMarker(line, start, message, type);
		} else {
			createMarker(-1, -1, message, type);
		}
	}

	private void createMarker(int line, int start, String message,
			String codeStr) {
		try {
			markerAdapter.createMarker(line, start, start, message, codeStr);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private boolean isValidLine(int line) {
		return line >= 1 && line <= code.getLineCount();
	}

	private boolean isValidCharacter(int line, int character) {
		return character >= 0 && character <= code.getLineLength(line - 1);
	}

}
