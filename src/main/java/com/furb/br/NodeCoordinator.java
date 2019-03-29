package com.furb.br;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

import lombok.Data;

@Data
public class NodeCoordinator {

	private final ElectionManager electionManagerInstance = ElectionManager.getInstance();
	private Queue<Node> queue = new LinkedList<>();
	private Node node;
	
	public NodeCoordinator(Node node) {
		this.node = node;
	}
	
	/**
	 * Locks the resource in a new Thread for a random time, between 5-15 secs.
	 * After unlocking the resource, it looks up to the Queue and continues it.
	 */
	public void lockResourceInNewThread(Node n) {
		new Thread(() -> {
			electionManagerInstance.startUsingResource(n);
			try {
				Thread.sleep(ThreadLocalRandom.current().nextInt(AppConstants.LOCK_RESOURCE_INIT,
						AppConstants.LOCK_RESOURCE_LIMIT));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			electionManagerInstance.stopUsingResource(n);

			NodeCoordinator coordinator = electionManagerInstance.getCoordinator();
			if (coordinator != null) {
				Node next = coordinator.getQueue().poll();
				if (next != null) {
					lockResourceInNewThread(next);
				}
			}
		}).run();
	}

}
