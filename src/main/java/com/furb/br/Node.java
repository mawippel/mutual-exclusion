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
			ses.schedule(getRunnable(), ThreadLocalRandom.current().nextInt(10, 26), TimeUnit.SECONDS);
		};
	}

}
