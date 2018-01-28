package edu.kit.aifb.ldbwebservice;

import java.util.ArrayList;
import java.util.Iterator;

import org.openrdf.model.Statement;
import org.openrdf.repository.RepositoryException;

import info.aduna.iteration.CloseableIteration;


@SuppressWarnings("serial")
public class ClosableList<S extends Statement, E extends RepositoryException> extends ArrayList<S> implements CloseableIteration<Statement, RepositoryException> {

	//Iterator<S> iter = super.iterator();
	int current_index;

	public ClosableList () {
		//this.modCount = 0;
		current_index = -1;
	}

	@Override
	public boolean hasNext() {
		try {
			super.get(current_index + 1);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public S next() {
		if ( hasNext() ) {
			return super.get(++current_index);
		} else {
			return null;
		}
	}

	@Override
	public void close() {
		super.clear();
		current_index = -1;
	}


	@Override
	public void remove() throws RepositoryException {
		if (current_index < 0) throw new IndexOutOfBoundsException();
		super.remove(super.get(current_index));
	}

}
