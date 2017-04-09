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
import pipe.work.Work.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.container.RoutingConf.RoutingEntry;
import gash.router.server.ServerState;
import gash.router.server.communication.CommConnection;
import io.netty.channel.Channel;
import pipe.common.Common.Header;
import pipe.work.Work.Heartbeat;
import pipe.work.Work.WorkMessage;
import pipe.work.Work.WorkState;

public class EdgeMonitor implements EdgeListener, Runnable {
	protected static Logger logger = LoggerFactory.getLogger("edge monitor");

	private EdgeList outboundEdges;
	private EdgeList inboundEdges;
	private long dt = 2000;
	private ServerState state;
	private boolean forever = true;

	public EdgeMonitor(ServerState state) {
		if (state == null)
			throw new RuntimeException("state is null");

		this.outboundEdges = new EdgeList();
		this.inboundEdges = new EdgeList();
		this.state = state;
		this.state.setEmon(this);

		if (state.getConf().getRouting() != null) {
			for (RoutingEntry e : state.getConf().getRouting()) {
				EdgeInfo ei = outboundEdges.addNode(e.getId(), e.getHost(), e.getPort());
				onAdd(ei);//try to connect thru creating channle.
			}
		}

		// cannot go below 2 sec
		if (state.getConf().getHeartbeatDt() > this.dt)
			this.dt = state.getConf().getHeartbeatDt();
	}

	public void createInboundIfNew(int ref, String host, int port) {
		inboundEdges.createIfNew(ref, host, port);
	}

	private WorkMessage createHB(EdgeInfo ei) {
		WorkState.Builder sb = WorkState.newBuilder();
		sb.setEnqueued(-1);
		sb.setProcessed(-1);

		Heartbeat.Builder bb = Heartbeat.newBuilder();
		bb.setState(sb);

		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(state.getConf().getNodeId());
		hb.setDestination(-1);
		hb.setTime(System.currentTimeMillis());

		WorkMessage.Builder wb = WorkMessage.newBuilder();
		wb.setHeader(hb);
		wb.setSecret(new Integer(123123123));
		wb.setBeat(bb);

		return wb.build();
	}

	public void shutdown() {
		forever = false;
	}

	@Override
	public void run() {
		while (forever) {
			try {
				for (EdgeInfo ei : this.outboundEdges.map.values()) {
					if (ei.getChannel() == null){
						onAdd(ei);
					}
					if (ei.isActive() && ei.getChannel() != null) {	
						WorkMessage wm = createHB(ei);
						//ChannelFuture cf = ei.getChannel().writeAndFlush(wm);
					} else {
						// TODO create a client to the node
						logger.info("trying to connect to node " + ei.getRef());
					}
				}
				Thread.sleep(dt);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public synchronized void onAdd(EdgeInfo ei) {
		// TODO check connection
		try{
			CommConnection cc = CommConnection.initConnection(ei.getHost(), ei.getPort());
			ei.setChannel(cc.connect());
			ei.setActive(true);
		}
		catch(Exception e){
			logger.error("Cannot connect to host!! Server is down!!");
		}
	}

	@Override
	public synchronized void onRemove(EdgeInfo ei) {
		// TODO ?
	}
	public Channel getOutBoundChannel(int nodeId){
		//Single Directional 
		
		EdgeInfo ei = null;
		
		try{
			ei = outboundEdges.map.get(nodeId);
			return ei.getChannel();
		}catch(Exception e){
			logger.info("no problem till here");
			for(EdgeInfo edgei : outboundEdges.map.values()){
				return edgei.getChannel();//first connected node
			}
		}
		return null;
	}
	
	public ArrayList<Node> getOutBoundRouteTable(){
		return outboundEdges.getRoutingTable();
		
	}
}
