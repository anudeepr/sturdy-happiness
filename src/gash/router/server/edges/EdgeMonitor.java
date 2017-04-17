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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import pipe.work.Work.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.container.RoutingConf.RoutingEntry;
import gash.router.server.ServerState;
import gash.router.server.communication.CommConnection;
import io.netty.channel.Channel;
import pipe.common.Common.Header;
import pipe.work.Work.Discovery;
import pipe.work.Work.Heartbeat;
import pipe.work.Work.WorkMessage;
import pipe.work.Work.WorkState;
import pipe.work.Work.WorkMessage.MessageType;

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
				onAdd(ei); //try to connect thru creating channel.
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

	public InetAddress getCurrentIp() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) networkInterfaces
                        .nextElement();
                Enumeration<InetAddress> nias = ni.getInetAddresses();
                while(nias.hasMoreElements()) {
                    InetAddress ia= (InetAddress) nias.nextElement();
                    if (!ia.isLinkLocalAddress() 
                     && !ia.isLoopbackAddress()
                     && ia instanceof Inet4Address) {
                        return ia;
                    }
                }
            }
        } catch (SocketException e) {
            logger.error("unable to get current IP " + e.getMessage(), e);
        }
        return null;
    }
	
	public void discoverCluster(){
		//todo should read all entries

		System.out.println("Processing till hear");
		List<RoutingEntry> re = state.getConf().getRouting();
		WorkMessage.Builder wmb = WorkMessage.newBuilder();
		Header.Builder hdb = Header.newBuilder();
		hdb.setNodeId(state.getConf().getNodeId());
		hdb.setDestination(re.get(0).getId());
		hdb.setTime(System.currentTimeMillis());
		wmb.setHeader(hdb.build());
		wmb.setSecret(1111);
		
		Discovery.Builder db = Discovery.newBuilder(); 
		Node.Builder discover = Node.newBuilder(); 
		discover.setNodeId(state.getConf().getNodeId());
		discover.setIpAddr(getCurrentIp().getHostAddress());
		discover.setWorkPort(state.getConf().getWorkPort());
		db.setNode(discover.build());
		wmb.setType(MessageType.DISCOVERNODE);
		wmb.setDiscovery(db.build());
		state.getOutBoundMessageQueue().addMessage(wmb.build());
		System.out.println("Still working");
		
	}
	
	@Override
	public void run() {
		while (forever) {
			try {
				System.out.println("Routing table is : ");
				for (EdgeInfo ei : this.outboundEdges.map.values()) {
					if (ei.getChannel() == null){
						onAdd(ei);
					}
					if (ei.isActive() && ei.getChannel() != null) {	
						//WorkMessage wm = createHB(ei); this will come from leader
						//ChannelFuture cf = ei.getChannel().writeAndFlush(wm);
						
					} else {
						// TODO create a client to the node
						logger.info("trying to connect to node " + ei.getRef());
					}
					System.out.println(ei.toString());
					
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
			if (ei != null && ei.getChannel() == null){
				logger.info("making connection with : " + ei.getHost() + " on port : " + ei.getPort()  );
				CommConnection cc = new CommConnection(ei.getHost(), ei.getPort());
				ei.setChannel(cc.connect());
				ei.setActive(true);
				discoverCluster();
			}
		}
		catch(Exception e){
			logger.error("Cannot connect to host!! Server is down!! nodeid =  " + ei.getRef()+" reason: " + e );
		}
	}

	@Override
	public synchronized void onRemove(EdgeInfo ei) {
		// TODO ?
	}
	public ArrayList<EdgeInfo> getOutBoundChannel(int nodeId){
		//Single Directional 
		logger.info("Getting Channle for destination : " +  nodeId);
		ArrayList<EdgeInfo> ei = null;
		
		try{
			if (outboundEdges != null && outboundEdges.map.size() > 0){
				if (nodeId == -1){

					return new ArrayList<EdgeInfo>(outboundEdges.map.values());
				}
				ei = new ArrayList<EdgeInfo>();
				ei.add(outboundEdges.map.get(nodeId));
				return ei;
			}
		}catch(Exception e){
			logger.error("Getting Error while looking for outbound channel with error : " + e);
		}
		return null;
	}
	
	public ArrayList<Node> getOutBoundRouteTable(){
		return outboundEdges.getRoutingTable();	
	}
	
	public int getTotalNodes(){
		return outboundEdges.map.size() + 1;//including self	
	}
	
	public boolean addNewEdgeInfo(int ref, String host, int port){
		logger.info("Got a new connection from  nodeid : " + ref + " ip :" + host +  " port : " + port  );
		if (outboundEdges.hasNode(ref)){
			EdgeInfo ei = outboundEdges.getNode(ref);
			logger.info("Trying to make reverse connection");
			onAdd(ei);
			return false;
		}
			
		EdgeInfo ei = outboundEdges.createIfNew(ref, host, port);
		if (!ei.isActive()) {
			logger.info("Trying to make reverse connection");
		    onAdd(ei);
		    
		}
		return true;
	}
}
