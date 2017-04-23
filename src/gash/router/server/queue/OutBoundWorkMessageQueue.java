package gash.router.server.queue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import gash.router.server.ServerState;
import pipe.work.*;
import pipe.work.Work.WorkMessage;

public class OutBoundWorkMessageQueue implements MessageQueue{

	ExecutorService exeService;
    ServerState state;
    
    public OutBoundWorkMessageQueue(ServerState state, int threadCount){
	    this.state = state;
    	exeService = Executors.newFixedThreadPool(threadCount);
    }
    
    public void addMessage(WorkMessage wm) {
    	exeService.execute(new OutBoundMessageTask(wm, state));
    }

    @Override
    public WorkMessage getQueuedMessage() {
        return null;
    }
}
