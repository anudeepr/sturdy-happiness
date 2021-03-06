/*
 * An interface defining basic operations any log must support.
 */
package gash.router.server.log;

import pipe.work.Work.LogEntry;

public interface LogOperations {

	long size();
	
	void appendEntry(int logId, LogEntry entry);
	
	int firstIndex();
	
	int lastIndex();
	
	LogEntry getEntry(int index);
	
	public int lastLogTerm(int index);
}
