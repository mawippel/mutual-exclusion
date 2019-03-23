package com.furb.br;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

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

	public Node() {
		this.id = electionManagerInstance.generateNextID();
		ses.schedule(getRunnable(), ThreadLocalRandom.current().nextInt(10, 26), TimeUnit.SECONDS);
	}

	private Runnable getRunnable() {
		return () -> {
			if (electionManagerInstance.getCoordinator() == null) {
				// starts an election process
				return;
			}

			System.out.println(
					String.format("[%s] Processo %s solicitou consumir um recurso.", LocalDateTime.now(), this));

			// Caso n�o tenha ningu�m consumindo, passa a consumir o recurso.
			// (Abre uma nova Thread lockando o arquivo por 5-15 seg. e continua executando)
			if (!electionManagerInstance.getCoordinator().isUsingResource()) {
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
					System.out.println(
							String.format("[%s] Processo %s parou de consumir o recurso.", LocalDateTime.now(), this));
				});
				thread.run();
			} else {
				// Caso tenha alguem consumindo, joga o Node para a fila.
				electionManagerInstance.getCoordinator().getQueue().add(this);
			}

			int nextInt = ThreadLocalRandom.current().nextInt(10, 26);
			System.out.println(String.format(
					"[%s] Execucao finalizada. Processo %s agendou a pr�xima execu��o. Daqui %s segundos.",
					LocalDateTime.now(), this, nextInt));
			ses.schedule(getRunnable(), nextInt, TimeUnit.SECONDS);
		};
	}

}
