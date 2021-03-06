/**
 * Copyright 2016 Gash.
 *
 * This file and intellectual content is protected under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package gash.router.server.edges;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import pipe.common.Common.Node;

public class EdgeList {
	protected ConcurrentHashMap<Integer, EdgeInfo> map = new ConcurrentHashMap<Integer, EdgeInfo>();

	public EdgeList() {
	}

	public EdgeInfo createIfNew(int ref, String host, int port, int cmdPort) {
		if (hasNode(ref))
			return getNode(ref);
		else
			return addNode(ref, host, port, cmdPort);
	}

	public synchronized EdgeInfo addNode(int ref, String host, int port, int cmdPort) {
		if (!verify(ref, host, port)) {
			// TODO log error
			throw new RuntimeException("Invalid node info");
		}

		if (!hasNode(ref)) {
			EdgeInfo ei = new EdgeInfo(ref, host, port, cmdPort);
			map.put(ref, ei);
			return ei;
		} else
			return null;
	}

	private boolean verify(int ref, String host, int port) {
		if (ref < 0 || host == null || port < 1024)
			return false;
		else
			return true;
	}

	public boolean hasNode(int ref) {
		return map.containsKey(ref);

	}

	public EdgeInfo getNode(int ref) {
		return map.get(ref);
	}

	public void removeNode(int ref) {
		map.remove(ref);
	}

	public void clear() {
		map.clear();
	}
	
	public ArrayList<Node> getRoutingTable() {
		ArrayList<Node> nodes = new ArrayList<Node>();
		for (EdgeInfo ei  : map.values()){
			Node.Builder node = Node.newBuilder();
			node.setNodeId(ei.getRef());
			node.setHost(ei.getHost());
			node.setPort(ei.getPort());
			node.setCmdPort(ei.getCmdPort());
			nodes.add(node.build());
		}
		return nodes;
	}
}
