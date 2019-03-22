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
			// TODO call the coordinator, asking to consume something
			System.out.println(
					String.format("[%s] Processo %s solicitou consumir um recurso.", LocalDateTime.now(), this));

			// Caso não tenha ninguém consumindo, passa a consumir o recurso.
			// (Abre uma nova Thread lockando o arquivo por 5-15 seg.)
			if (!electionManagerInstance.getCoordinator().isUsingResource()) {
				new Thread(() -> {
					// lockar o arquivo?
					System.out.println(
							String.format("[%s] Processo %s está consumindo o recurso.", LocalDateTime.now(), this));
					try {
						Thread.sleep(ThreadLocalRandom.current().nextInt(10000, 26000));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}).run();
			} else {
				// Caso tenha alguem consumindo, joga o Node para a fila.
				electionManagerInstance.getCoordinator().getQueue().add(this);
			}

			int nextInt = ThreadLocalRandom.current().nextInt(10, 26);
			System.out.println(String.format("[%s] Processo %s agendou a próxima execução. Daqui %s segundos.",
					LocalDateTime.now(), this, nextInt));
			ses.schedule(getRunnable(), nextInt, TimeUnit.SECONDS);
		};
	}

}
