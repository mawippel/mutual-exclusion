package com.furb.br;

import java.util.LinkedList;
import java.util.Queue;

import lombok.Data;

@Data
public class NodeCoordinator {

	private Queue<Node> queue = new LinkedList<>();
	private Node node;
	private boolean usingResource;

	public NodeCoordinator(Node node) {
		this.node = node;
	}

}
