package com.furb.br;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class TaskScheduler {

	/**
	 * Creates all the tasks of the application
	 */
	public void scheduleAllTasks() {
		createTask(AppConstants.KILL_COORDINATOR_TIMER, AppConstants.KILL_COORDINATOR_INTERVAL,
				AppConstants.KILL_COORDINATOR_METHOD);
		createTask(AppConstants.CREATE_PROCESS_TIMER, AppConstants.CREATE_PROCESS_INTERVAL,
				AppConstants.CREATE_PROCESS_METHOD);

	}

	/**
	 * Creates a task
	 */
	private void createTask(String timerName, long jobInterval, String methodName) {
		Timer timer = new Timer(timerName);
		TimerTask task = new TimerTask() {
			public void run() {
				try {
					var em = ElectionManager.getInstance();
					Method method = em.getClass().getMethod(methodName);
					method.invoke(em);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		Date startDate = getStartJobDate(jobInterval);
		timer.schedule(task, startDate, jobInterval);
	}

	private Date getStartJobDate(long jobInterval) {
		LocalDateTime localDate = LocalDateTime.now()
				.plusNanos(TimeUnit.NANOSECONDS.convert(jobInterval, TimeUnit.MILLISECONDS));
		Date startDate = Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant());
		return startDate;
	}

}
