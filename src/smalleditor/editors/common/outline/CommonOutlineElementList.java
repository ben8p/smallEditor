/*
 * $RCSfile: JSElementList.java,v $
 *
 * Copyright 2002
 * CH-1700 Fribourg, Switzerland
 * All rights reserved.
 *
 *========================================================================
 * Modifications history
 *========================================================================
 * $Log: JSElementList.java,v $
 * Revision 1.1  2003/05/30 20:53:08  agfitzp
 * 0.0.2 : Outlining is now done as the user types. Some other bug fixes.
 *
 * Revision 1.1  2003/05/28 15:17:11  agfitzp
 * net.sourceforge.jseditor 0.0.1 code base
 *
 *========================================================================
 */

package smalleditor.editors.common.outline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

import smalleditor.utils.TextUtility;


public class CommonOutlineElementList implements IWorkbenchAdapter, IAdaptable {
	protected List children = new ArrayList();

	/**
	 * Creates a new adaptable list with the given children.
	 */
	public CommonOutlineElementList() {
	}

	/**
	 * Creates a new adaptable list with the given children.
	 * 
	 * @param newChildren
	 */
	public CommonOutlineElementList(ACommonOutlineElement[] newChildren) {
		for (int i = 0; i < newChildren.length; i++) {
			children.add(newChildren[i]);
		}
	}

	/**
	 * Creates a new adaptable list with the given children.
	 * 
	 * @param newChildren
	 */
	public CommonOutlineElementList(List newChildren) {
		for (int i = 0; i < newChildren.size(); i++) {
			children.add(newChildren.get(i));
		}
	}

	/**
	 * Adds all the adaptable objects in the given enumeration to this list.
	 * Returns this list.
	 * 
	 * @param iterator
	 * 
	 * @return
	 */
	public CommonOutlineElementList add(Iterator iterator) {
		while (iterator.hasNext()) {
			add((ACommonOutlineElement) iterator.next());
		}

		return this;
	}

	/**
	 * Adds the given adaptable object to this list. Returns this list.
	 * 
	 * @param adaptable
	 * 
	 * @return
	 */
	public CommonOutlineElementList add(ACommonOutlineElement anElement) {
		children.add(anElement);

		return this;
	}

	/**
	 * 
	 * 
	 * @param adapter
	 * 
	 * @return
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) {
			return this;
		}

		return null;
	}

	/**
	 * Returns the elements in this list.
	 * 
	 * @return
	 */
	public Object[] getChildren() {
		return children.toArray();
	}

	/**
	 * 
	 * 
	 * @param o
	 * 
	 * @return
	 */
	public Object[] getChildren(Object o) {
		return children.toArray();
	}

	/**
	 * 
	 * 
	 * @param object
	 * 
	 * @return
	 */
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

	/**
	 * 
	 * 
	 * @param object
	 * 
	 * @return
	 */
	public String getLabel(Object object) {
		return object == null ? TextUtility.EMPTY_STRING : object.toString(); //$NON-NLS-1$
	}

	/**
	 * 
	 * 
	 * @param object
	 * 
	 * @return
	 */
	public Object getParent(Object object) {
		return null;
	}

	/**
	 * Removes the given adaptable object from this list.
	 * 
	 * @param adaptable
	 */
	public void remove(ACommonOutlineElement anElement) {
		children.remove(anElement);
	}

	/**
	 * Returns the number of items in the list
	 * 
	 * @return
	 */
	public int size() {
		return children.size();
	}

	public ACommonOutlineElement findEquivilent(ACommonOutlineElement anElement) {
		for (int i = 0; i < size(); i++) {
			ACommonOutlineElement aCandidate = (ACommonOutlineElement) children
					.get(i);
			if (anElement.equals(aCandidate)) {
				return aCandidate;
			}
		}

		return null;
	}

	public ACommonOutlineElement get(int index) {
		if (index >= size()) {
			return null;
		}
		return (ACommonOutlineElement) children.get(index);
	}
}
