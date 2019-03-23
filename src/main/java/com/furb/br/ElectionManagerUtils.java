package com.furb.br;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utils class for {@link ElectionManager} singleton.
 */
public class ElectionManagerUtils {

	private static final ElectionManager electionManagerInstance = ElectionManager.getInstance();

	public static List<Node> getSortedList() {
		// Copy to a new List to don't modify the original one.
		List<Node> sortedList = new ArrayList<>(electionManagerInstance.getNodes());
		sortedList.sort(Comparator.comparing(Node::getId));
		return sortedList;
	}

}
