package smalleditor.editors.css;

import smalleditor.editors.common.CommonContentAssistProvider;
import smalleditor.util.xml.ResourceReader;

public class CssContentAssistProvider extends CommonContentAssistProvider {

	public CssContentAssistProvider() {
		super();

		this.elementsList = ResourceReader.read(this.getClass().getClassLoader()
				.getResourceAsStream("res/data/css.xml")); //$NON-NLS-1$
	}
}