package com.furb.br;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import lombok.Getter;

public class ElectionManager {

	private static ElectionManager INSTANCE;
	@Getter
	private List<Node> nodes = new CopyOnWriteArrayList<>();
	@Getter
	private volatile NodeCoordinator coordinator;

	// Private default constructor for a singleton instance
	private ElectionManager() {
	}

	public static ElectionManager getInstance() {
		return INSTANCE == null ? INSTANCE = new ElectionManager() : INSTANCE;
	}

	public void killCoordinator() {
		if (coordinator == null)
			return;
		Node removed = nodes.remove(nodes.indexOf(coordinator.getNode()));
		System.out.println(String.format("[%s] Processo %s morreu.", LocalDateTime.now(), removed));
		coordinator = null;
	}

	public void createProcess() {
		var node = new Node();
		var addedCordinator = addCoordinatorIfFirst(node);
		nodes.add(node);
		System.out.println(String.format("[%s] Processo %s criado.", LocalDateTime.now(), node));

		if (addedCordinator)
			System.out.println(String.format("[%s] Processo %s virou coordenador.", LocalDateTime.now(), node));
	}

	private boolean addCoordinatorIfFirst(Node node) {
		if (nodes.isEmpty()) {
			coordinator = new NodeCoordinator(node);
			return true;
		}
		return false;
	}

	public int generateNextID() {
		var empty = false;
		var newID = 0;
		do {
			var nextId = new Random().nextInt(1000);
			newID = nextId;
			empty = nodes.parallelStream().filter(n -> {
				return n.getId() == nextId;
			}).collect(Collectors.toList()).isEmpty();
		} while (!empty);
		return newID;
	}

}