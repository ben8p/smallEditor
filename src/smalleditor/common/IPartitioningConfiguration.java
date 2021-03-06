/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package smalleditor.common;

import org.eclipse.jface.text.rules.IPredicateRule;

import smalleditor.common.rules.ISubPartitionScanner;


/**
 * @author Max Stepanov
 *
 */
public interface IPartitioningConfiguration {

	public String[] getContentTypes();
	
	public IPredicateRule[] getPartitioningRules();
	
	public ISubPartitionScanner createSubPartitionScanner();
	
	public String getDocumentContentType(String contentType);
	
}
