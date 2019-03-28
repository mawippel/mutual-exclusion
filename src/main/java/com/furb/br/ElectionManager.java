package com.furb.br;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

public class ElectionManager {

	private static ElectionManager INSTANCE;
	@Getter
	private volatile List<Node> nodes = new CopyOnWriteArrayList<>();
	@Getter
	@Setter
	private volatile NodeCoordinator coordinator;
	@Getter
	@Setter
	private volatile boolean isInElection;
	private volatile boolean usingResource = false;
	

	// Private default constructor for a singleton instance
	private ElectionManager() {
	}

	public static ElectionManager getInstance() {
		return INSTANCE == null ? INSTANCE = new ElectionManager() : INSTANCE;
	}

	public void killCoordinator() {
		if (coordinator == null)
			return;
		Node node = nodes.get(nodes.indexOf(coordinator.getNode()));
		nodes.remove(node);
		System.out.println(String.format("[%s] Coordenador %s morreu.", LocalDateTime.now(), node));
		
		// Sets the instances to null, to save memory.
		coordinator = null;
		System.gc();
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

	public boolean isUsingResource() {
		return usingResource;
	}

	public void startUsingResource(Node n) {
		this.usingResource = true;
		System.out.println(String.format("[%s] Processo %s esta consumindo o recurso.", LocalDateTime.now(), n));
	}
	
	public void stopUsingResource(Node n) {
		this.usingResource = false;
		System.out.println(String.format("[%s] Processo %s parou de consumir o recurso.", LocalDateTime.now(), n));
	}
	
}