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
			if (electionManagerInstance.getCoordinator() == null) {
				startsElection();
				return; // end of the process by now.
			}

			System.out.println(
					String.format("[%s] Processo %s solicitou consumir um recurso.", LocalDateTime.now(), this));

			// Caso n�o tenha ningu�m consumindo, passa a consumir o recurso.
			// (Abre uma nova Thread lockando o arquivo por 5-15 seg. e continua executando)
			if (!electionManagerInstance.getCoordinator().isUsingResource()) {
				lockResourceInNewThread();
			} else {
				// Caso tenha alguem consumindo, joga o Node para a fila.
				electionManagerInstance.getCoordinator().getQueue().add(this);
				System.out.println(String.format("[%s] Node %s foi adicionado a fila.", LocalDateTime.now(), this));
			}

			int nextInt = ThreadLocalRandom.current().nextInt(10, 26);
			System.out.println(String.format(
					"[%s] Execucao finalizada. Processo %s agendou a pr�xima execu��o. Daqui %s segundos.",
					LocalDateTime.now(), this, nextInt));
			ses.schedule(getRunnable(), nextInt, TimeUnit.SECONDS);
		};
	}

	private void lockResourceInNewThread() {
		Thread thread = new Thread(() -> {
			electionManagerInstance.getCoordinator().setUsingResource(true);
			System.out.println(
					String.format("[%s] Processo %s est� consumindo o recurso.", LocalDateTime.now(), this));
			try {
				// Locks the resource for 5-15 sec.
				Thread.sleep(ThreadLocalRandom.current().nextInt(5000, 15000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			electionManagerInstance.getCoordinator().setUsingResource(false);
			System.out.println(
					String.format("[%s] Processo %s parou de consumir o recurso.", LocalDateTime.now(), this));

			Node next = electionManagerInstance.getCoordinator().getQueue().poll();
			if(next != null) {
				next.lockResourceInNewThread();
			}
		});
		thread.run();
	}

	private void startsElection() {
		// starts an election process
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
