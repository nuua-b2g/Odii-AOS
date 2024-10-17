package kto.smarttour.common.utils;

import java.util.ArrayList;
import java.util.List;

public class WorkerChain {

	private int currentStep = -1;
	private List<SimpleWorker> workerList;

	public static class SimpleWorker {
		public final String jobName;
		public final Object jobId;

		public SimpleWorker(Object jobId, String jobName) {
			this.jobId = jobId;
			this.jobName = jobName;
		}

		public void work() {
		}
	}

	public WorkerChain() {
		workerList = new ArrayList<SimpleWorker>();
	}

	public synchronized void add(SimpleWorker worker) {
		workerList.add(worker);
	}

	public synchronized boolean hasNext() {
		return currentStep < workerList.size() - 1;
	}

	public synchronized int currentStep() {
		if (hasNext()) return currentStep;
		return -1;
	}

	public synchronized void workNext() {
		workAt(currentStep + 1);
	}

	public synchronized void clearWork() {
		workerList.clear();
	}

	public synchronized void workAt(int step) {
		if (step < workerList.size()) {
			currentStep = step;
			SimpleWorker w = workerList.get(currentStep);
			w.work();
		}
	}

	public synchronized Object getJobId() {
		if(workerList.isEmpty()) {
			return workerList.get(currentStep).jobId;
		}
		return -1;
	}
}
