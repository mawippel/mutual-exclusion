package com.furb.br;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(of = { "id" })
@EqualsAndHashCode(of = { "id" })
public class Node {

	private final ElectionManager electionManagerInstance = ElectionManager.getInstance();
	private int id;
	private ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
	private boolean active = true;

	public Node() {
		this.id = electionManagerInstance.generateNextID();
		ses.schedule(getRunnable(),
				ThreadLocalRandom.current().nextInt(AppConstants.CREATE_NODE_INIT, AppConstants.CREATE_NODE_LIMIT),
				TimeUnit.SECONDS);
	}

	private Runnable getRunnable() {
		return () -> {
			try {
				if (electionManagerInstance.getCoordinator() == null) {
					/*
					 * If the actual node doesn't exist in the list, it's because it was previously
					 * scheduled, so, it shouldn't run.
					 */
					if (electionManagerInstance.getNodes().indexOf(this) == -1) {
						ses.shutdown();
						return;
					}

					startsElection();
				}

				/*
				 * If the actual node is the coordinator or, the actual node doesn't exist in
				 * the list, stop scheduling this Thread.
				 */
				if (this.equals(electionManagerInstance.getCoordinator().getNode())
						|| electionManagerInstance.getNodes().indexOf(this) == -1) {
					ses.shutdown();
					return;
				}

				consumeResource();

				scheduleNextExecution();
			} catch (Exception e) {
				System.out.println(e);
			}
		};
	}

	private void startsElection() {
		var newCoordinator = findNewCoordinator();
		electionManagerInstance.setCoordinator(newCoordinator);
		System.out.println(String.format("[%s] Processo de Eleição finalizado. O novo coordenador é %s.",
				LocalDateTime.now(), newCoordinator));
	}

	/**
	 * Searchs for a new coordinator. The process with the highest ID will be the
	 * new coordinator.
	 * 
	 * @return a {@link NodeCoordinator}
	 */
	private NodeCoordinator findNewCoordinator() {
		System.out.println(String.format("[%s] Processo de Eleição iniciado pelo %s.", LocalDateTime.now(), this));
		electionManagerInstance.setInElection(true);
		return getCoordinator(new NodeCoordinator(this));
	}

	/**
	 * If there's no process consuming, starts to consume the resource. (Open a new
	 * Thread locking the resource for 5-15 sec.)
	 */
	private void consumeResource() {
		System.out.println(String.format("[%s] Processo %s solicitou consumir um recurso.", LocalDateTime.now(), this));
		if (!electionManagerInstance.isUsingResource()) {
			lockResourceInNewThread();
		} else {
			// If there's some process consuming, add it to the Queue
			electionManagerInstance.getCoordinator().getQueue().add(this);
			System.out.println(String.format("[%s] Node %s foi adicionado a fila.", LocalDateTime.now(), this));
		}
	}

	/**
	 * Locks the resource in a new Thread for a random time, between 5-15 secs.
	 * After unlocking the resource, it looks up to the Queue and continues it.
	 */
	private void lockResourceInNewThread() {
		new Thread(() -> {
			try {
				electionManagerInstance.startUsingResource(this);
				Thread.sleep(ThreadLocalRandom.current().nextInt(5000, 15000));
				electionManagerInstance.stopUsingResource(this);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				electionManagerInstance.stopUsingResource(this);
			}

			NodeCoordinator coordinator = electionManagerInstance.getCoordinator();
			if (coordinator != null) {
				Node next = coordinator.getQueue().poll();
				if (next != null) {
					next.lockResourceInNewThread();
				}
			}
		}).run();
	}

	private void scheduleNextExecution() {
		var nextInt = ThreadLocalRandom.current().nextInt(AppConstants.LOCK_RESOURCE_INIT,
				AppConstants.LOCK_RESOURCE_LIMIT);
		System.out.println(
				String.format("[%s] Execucao finalizada. Processo %s agendou a proxima execucao. Daqui %s segundos.",
						LocalDateTime.now(), this, nextInt));
		ses.schedule(getRunnable(), nextInt, TimeUnit.SECONDS);
	}

	private NodeCoordinator getCoordinator(NodeCoordinator actualNode) {
		var aheadNodes = ElectionManagerUtils.getSortedList().stream().filter(n -> n.id > actualNode.getNode().getId())
				.collect(Collectors.toList());
		var possibleCoordinators = aheadNodes.stream().map(n -> {
			return new NodeCoordinator(n);
		}).collect(Collectors.toList());
		if (possibleCoordinators.isEmpty()) {
			return new NodeCoordinator(this);
		}
		return getCoordinator(possibleCoordinators.get(0));
	}

}
