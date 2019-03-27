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
		ses.schedule(getRunnable(), ThreadLocalRandom.current().nextInt(10, 26), TimeUnit.SECONDS);
	}

	public boolean sendMessage() {
		return active;
	};

	private Runnable getRunnable() {
		return () -> {
			try {
				if (electionManagerInstance.getCoordinator() == null) {
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
				if (electionManagerInstance.getNodes().indexOf(this) == -1
						|| this.equals(electionManagerInstance.getCoordinator().getNode())) {
					ses.shutdown();
					return;
				}

				System.out.println(
						String.format("[%s] Processo %s solicitou consumir um recurso.", LocalDateTime.now(), this));

				/*
				 * If there's no process consuming, starts to consume the resource. (Open a new
				 * Thread locking the resource for 5-15 sec.)
				 */
				if (!electionManagerInstance.isUsingResource()) {
					lockResourceInNewThread();
				} else {
					// If there's some process consuming, add it to the Queue
					electionManagerInstance.getCoordinator().getQueue().add(this);
					System.out.println(String.format("[%s] Node %s foi adicionado a fila.", LocalDateTime.now(), this));
				}

				int nextInt = ThreadLocalRandom.current().nextInt(10, 26);
				System.out.println(String.format(
						"[%s] Execucao finalizada. Processo %s agendou a proxima execucao. Daqui %s segundos.",
						LocalDateTime.now(), this, nextInt));

				ses.schedule(getRunnable(), nextInt, TimeUnit.SECONDS);
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		};
	}

	private void lockResourceInNewThread() {
		Thread thread = new Thread(() -> {
			electionManagerInstance.setUsingResource(true);
			System.out.println(String.format("[%s] Processo %s esta consumindo o recurso.", LocalDateTime.now(), this));
			try {
				// Locks the resource for 5-15 sec.
				Thread.sleep(ThreadLocalRandom.current().nextInt(5000, 15000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			electionManagerInstance.setUsingResource(false);
			System.out
					.println(String.format("[%s] Processo %s parou de consumir o recurso.", LocalDateTime.now(), this));

			NodeCoordinator coordinator = electionManagerInstance.getCoordinator();
			if (coordinator != null) {
				Node next = coordinator.getQueue().poll();
				if (next != null) {
					next.lockResourceInNewThread();
				}
			}
		});
		thread.run();
	}

	private void startsElection() {
		// Starts an election process
		NodeCoordinator newCoordinator = startElection();
		electionManagerInstance.setCoordinator(newCoordinator);
		System.out.println(String.format("[%s] Processo de Elei��o finalizado. O novo coordenador � %s.",
				LocalDateTime.now(), newCoordinator));
	}

	private NodeCoordinator startElection() {
		System.out.println(String.format("[%s] Processo de Elei��o iniciado pelo %s.", LocalDateTime.now(), this));
		electionManagerInstance.setInElection(true);
		return getCoordinator(new NodeCoordinator(this));
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
